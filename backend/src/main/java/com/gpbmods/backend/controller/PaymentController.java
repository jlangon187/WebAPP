package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.PaymentSessionRequest;
import com.gpbmods.backend.dto.PaymentSessionResponse;
import com.gpbmods.backend.dto.PaymentConfirmRequest;
import com.gpbmods.backend.model.Compra;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.ModsRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ModsRepository modsRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraRepository compraRepository;

    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;

    @Value("${paypal.client.id:}")
    private String paypalClientId;

    @Value("${paypal.client.secret:}")
    private String paypalClientSecret;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public PaymentController(ModsRepository modsRepository, UsuarioRepository usuarioRepository, CompraRepository compraRepository) {
        this.modsRepository = modsRepository;
        this.usuarioRepository = usuarioRepository;
        this.compraRepository = compraRepository;
    }

    @PostMapping("/create-session")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> createSession(@RequestBody PaymentSessionRequest request, Authentication authentication) {
        String provider = request.getProvider() == null ? "" : request.getProvider().trim().toLowerCase(Locale.ROOT);
        List<Long> modIds = request.getModIds() == null ? List.of() : request.getModIds();
        if (modIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Debes enviar al menos un mod.");
        }

        Optional<Usuario> userOpt = usuarioRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        List<Mods> mods = modsRepository.findAllById(modIds);
        if (mods.size() != modIds.size()) {
            return ResponseEntity.badRequest().body("Uno o mas mods no existen.");
        }

        if ("stripe".equals(provider)) {
            return createStripeSession(mods);
        }
        if ("paypal".equals(provider)) {
            return createPaypalOrder(mods);
        }

        return ResponseEntity.badRequest().body("Proveedor no soportado.");
    }

    private ResponseEntity<?> createStripeSession(List<Mods> mods) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Stripe no configurado en el servidor.");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(stripeSecretKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mode", "payment");
        body.add("success_url", frontendUrl + "/checkout?payment=success&provider=stripe&session_id={CHECKOUT_SESSION_ID}");
        body.add("cancel_url", frontendUrl + "/checkout?payment=cancel&provider=stripe");

        int index = 0;
        for (Mods mod : mods) {
            int amountCents = toCents(mod.getPrecio());
            body.add("line_items[" + index + "][price_data][currency]", "eur");
            body.add("line_items[" + index + "][price_data][product_data][name]", mod.getNombre());
            body.add("line_items[" + index + "][price_data][unit_amount]", String.valueOf(amountCents));
            body.add("line_items[" + index + "][quantity]", "1");
            index++;
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.stripe.com/v1/checkout/sessions", entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("No se pudo crear sesión de Stripe.");
        }

        String url = (String) response.getBody().get("url");
        String id = (String) response.getBody().get("id");
        return ResponseEntity.ok(new PaymentSessionResponse("stripe", url, id, "Sesion Stripe creada"));
    }

    private ResponseEntity<?> createPaypalOrder(List<Mods> mods) {
        if (paypalClientId == null || paypalClientId.isBlank() || paypalClientSecret == null || paypalClientSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("PayPal no configurado en el servidor.");
        }

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "https://api-m.sandbox.paypal.com";

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        authHeaders.setBasicAuth(paypalClientId, paypalClientSecret);
        MultiValueMap<String, String> authBody = new LinkedMultiValueMap<>();
        authBody.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> authEntity = new HttpEntity<>(authBody, authHeaders);
        ResponseEntity<Map> authResp = restTemplate.postForEntity(baseUrl + "/v1/oauth2/token", authEntity, Map.class);
        if (!authResp.getStatusCode().is2xxSuccessful() || authResp.getBody() == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("No se pudo autenticar contra PayPal.");
        }
        String accessToken = (String) authResp.getBody().get("access_token");

        BigDecimal total = mods.stream().map(Mods::getPrecio).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", "CAPTURE");
        Map<String, Object> amount = Map.of("currency_code", "EUR", "value", total.setScale(2, RoundingMode.HALF_UP).toString());
        payload.put("purchase_units", List.of(Map.of("amount", amount)));
        payload.put("application_context", Map.of(
                "return_url", frontendUrl + "/checkout?payment=success&provider=paypal",
                "cancel_url", frontendUrl + "/checkout?payment=cancel&provider=paypal"
        ));

        HttpHeaders orderHeaders = new HttpHeaders();
        orderHeaders.setContentType(MediaType.APPLICATION_JSON);
        orderHeaders.setBearerAuth(accessToken);
        HttpEntity<Map<String, Object>> orderEntity = new HttpEntity<>(payload, orderHeaders);
        ResponseEntity<Map> orderResp = restTemplate.postForEntity(baseUrl + "/v2/checkout/orders", orderEntity, Map.class);
        if (!orderResp.getStatusCode().is2xxSuccessful() || orderResp.getBody() == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("No se pudo crear orden PayPal.");
        }

        String orderId = (String) orderResp.getBody().get("id");
        String approveUrl = null;
        Object linksObj = orderResp.getBody().get("links");
        if (linksObj instanceof List<?> links) {
            for (Object linkObj : links) {
                if (linkObj instanceof Map<?, ?> linkMap) {
                    Object rel = linkMap.get("rel");
                    if ("approve".equals(rel)) {
                        approveUrl = String.valueOf(linkMap.get("href"));
                        break;
                    }
                }
            }
        }

        return ResponseEntity.ok(new PaymentSessionResponse("paypal", approveUrl, orderId, "Orden PayPal creada"));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest request, Authentication authentication) {
        String provider = request.getProvider() == null ? "" : request.getProvider().trim().toLowerCase(Locale.ROOT);
        String externalId = request.getExternalId() == null ? "" : request.getExternalId().trim();
        List<Long> modIds = request.getModIds() == null ? List.of() : request.getModIds();

        if (externalId.isBlank() || modIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Faltan datos de confirmacion de pago.");
        }

        Optional<Usuario> userOpt = usuarioRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }
        Usuario user = userOpt.get();

        if (user.getGuid() == null || !user.getGuid().matches("^[A-F0-9]{18}$")) {
            return ResponseEntity.badRequest().body("Debes completar tu GUID de juego (18 hex) antes de comprar mods.");
        }

        List<Mods> mods = modsRepository.findAllById(modIds);
        if (mods.size() != modIds.size()) {
            return ResponseEntity.badRequest().body("Uno o mas mods no existen.");
        }

        boolean paid;
        if ("stripe".equals(provider)) {
            paid = verifyStripePaid(externalId);
        } else if ("paypal".equals(provider)) {
            paid = verifyAndCapturePaypal(externalId);
        } else {
            return ResponseEntity.badRequest().body("Proveedor no soportado.");
        }

        if (!paid) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El pago no esta confirmado todavia.");
        }

        int created = 0;
        for (Mods mod : mods) {
            if (compraRepository.existsByUsuarioIdAndModId(user.getId(), mod.getId())) {
                continue;
            }
            Compra compra = new Compra();
            compra.setUsuario(user);
            compra.setMod(mod);
            compra.setPrecioPagado(mod.getPrecio());
            compra.setMetodoPago("stripe".equals(provider) ? "Stripe" : "Paypal");
            compra.setGuidCompra(user.getGuid().toUpperCase());
            compraRepository.save(compra);
            created++;
        }

        return ResponseEntity.ok(Map.of("created", created, "message", "Compra confirmada y registrada."));
    }

    private boolean verifyStripePaid(String sessionId) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            return false;
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(stripeSecretKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange("https://api.stripe.com/v1/checkout/sessions/" + sessionId, HttpMethod.GET, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return false;
        }
        String paymentStatus = String.valueOf(response.getBody().get("payment_status"));
        return "paid".equalsIgnoreCase(paymentStatus);
    }

    private boolean verifyAndCapturePaypal(String orderId) {
        if (paypalClientId == null || paypalClientId.isBlank() || paypalClientSecret == null || paypalClientSecret.isBlank()) {
            return false;
        }
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "https://api-m.sandbox.paypal.com";
        String accessToken = getPaypalAccessToken(restTemplate, baseUrl);
        if (accessToken == null) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> orderResp = restTemplate.exchange(baseUrl + "/v2/checkout/orders/" + orderId, HttpMethod.GET, getEntity, Map.class);
        if (!orderResp.getStatusCode().is2xxSuccessful() || orderResp.getBody() == null) {
            return false;
        }
        String status = String.valueOf(orderResp.getBody().get("status"));
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return true;
        }

        HttpEntity<String> captureEntity = new HttpEntity<>("{}", headers);
        ResponseEntity<Map> captureResp = restTemplate.postForEntity(baseUrl + "/v2/checkout/orders/" + orderId + "/capture", captureEntity, Map.class);
        if (!captureResp.getStatusCode().is2xxSuccessful() || captureResp.getBody() == null) {
            return false;
        }
        String captureStatus = String.valueOf(captureResp.getBody().get("status"));
        return "COMPLETED".equalsIgnoreCase(captureStatus);
    }

    private String getPaypalAccessToken(RestTemplate restTemplate, String baseUrl) {
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        authHeaders.setBasicAuth(paypalClientId, paypalClientSecret);
        MultiValueMap<String, String> authBody = new LinkedMultiValueMap<>();
        authBody.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> authEntity = new HttpEntity<>(authBody, authHeaders);
        ResponseEntity<Map> authResp = restTemplate.postForEntity(baseUrl + "/v1/oauth2/token", authEntity, Map.class);
        if (!authResp.getStatusCode().is2xxSuccessful() || authResp.getBody() == null) {
            return null;
        }
        return (String) authResp.getBody().get("access_token");
    }

    private int toCents(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }
        return amount.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
