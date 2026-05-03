package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.AdminUserUpdateRequest;
import com.gpbmods.backend.dto.AdminPurchaseGuidUpdateRequest;
import com.gpbmods.backend.model.Compra;
import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.model.Ticket;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.EncryptionJobRepository;
import com.gpbmods.backend.repository.TicketRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import com.gpbmods.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('admin')")
public class AdminController {

    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;
    private final EncryptionJobRepository encryptionJobRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminController(CompraRepository compraRepository,
                           UsuarioRepository usuarioRepository,
                           TicketRepository ticketRepository,
                           EncryptionJobRepository encryptionJobRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.ticketRepository = ticketRepository;
        this.encryptionJobRepository = encryptionJobRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Value("${mods.images.directory:/data/home-images}")
    private String homeImagesDirectory;

    @Value("${mods.files.directory:/data/mods-files}")
    private String modsFilesDirectory;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${mods.downloads.public-base-url:}")
    private String publicDownloadBaseUrl;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30From = now.minusDays(30);
        LocalDateTime prev30From = now.minusDays(60);

        BigDecimal totalSales = compraRepository.sumTotalSales();
        BigDecimal salesLast30 = safeDecimal(compraRepository.sumSalesBetween(last30From, now));
        BigDecimal salesPrev30 = safeDecimal(compraRepository.sumSalesBetween(prev30From, last30From));

        long newUsers = usuarioRepository.countByCreadoEnAfter(last30From);
        long usersLast30 = usuarioRepository.countByCreadoEnBetween(last30From, now);
        long usersPrev30 = usuarioRepository.countByCreadoEnBetween(prev30From, last30From);

        long activeTickets = ticketRepository.countByEstadoNot(Ticket.Estado.cerrado);
        long ticketsLast30 = ticketRepository.countByEstadoNotAndCreadoEnBetween(Ticket.Estado.cerrado, last30From, now);
        long ticketsPrev30 = ticketRepository.countByEstadoNotAndCreadoEnBetween(Ticket.Estado.cerrado, prev30From, last30From);

        stats.put("totalSales", totalSales == null ? BigDecimal.ZERO : totalSales);
        stats.put("newUsers", newUsers);
        stats.put("activeTickets", activeTickets);
        stats.put("salesTrendPercent", calculateTrendPercent(salesLast30.doubleValue(), salesPrev30.doubleValue()));
        stats.put("usersTrendPercent", calculateTrendPercent((double) usersLast30, (double) usersPrev30));
        stats.put("ticketsTrendPercent", calculateTrendPercent((double) ticketsLast30, (double) ticketsPrev30));
        stats.put("nas", getNasStats());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsersWithPurchases() {
        List<Usuario> users = usuarioRepository.findAllByOrderByCreadoEnDesc();
        List<Map<String, Object>> payload = new ArrayList<>();

        for (Usuario user : users) {
            List<Compra> compras = compraRepository.findByUsuarioId(user.getId());
            payload.add(buildAdminUserResponse(user, compras));
        }

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/encryption-jobs/overview")
    public ResponseEntity<?> getEncryptionJobsOverview() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pending", encryptionJobRepository.countByStatus(EncryptionJob.Status.PENDING));
        payload.put("running", encryptionJobRepository.countByStatus(EncryptionJob.Status.RUNNING));
        payload.put("done", encryptionJobRepository.countByStatus(EncryptionJob.Status.DONE));
        payload.put("failed", encryptionJobRepository.countByStatus(EncryptionJob.Status.FAILED));
        payload.put("doneWithoutNotification", encryptionJobRepository.countByStatusAndNotifiedAtIsNull(EncryptionJob.Status.DONE));
        payload.put("failedWithError", encryptionJobRepository.countByStatusAndErrorMessageIsNotNull(EncryptionJob.Status.FAILED));
        payload.put("mailConfigured", mailHost != null && !mailHost.isBlank() && mailUsername != null && !mailUsername.isBlank());
        payload.put("mailHost", mailHost == null || mailHost.isBlank() ? "-" : mailHost);
        payload.put("mailUsername", mailUsername == null || mailUsername.isBlank() ? "-" : mailUsername);

        List<Map<String, Object>> recent = new ArrayList<>();
        encryptionJobRepository.findTop20ByOrderByCreatedAtDesc().forEach(job -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", job.getId());
            row.put("status", job.getStatus().name());
            row.put("mod", job.getMod().getNombre());
            row.put("userEmail", job.getUsuario().getEmail());
            row.put("guid", job.getGuid());
            row.put("createdAt", job.getCreatedAt());
            row.put("updatedAt", job.getUpdatedAt());
            row.put("expiresAt", job.getExpiresAt());
            row.put("notifiedAt", job.getNotifiedAt());
            row.put("errorMessage", job.getErrorMessage());
            recent.add(row);
        });
        payload.put("recent", recent);
        return ResponseEntity.ok(payload);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUserByAdmin(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        Optional<Usuario> userOpt = usuarioRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuario no encontrado.");
        }

        Usuario user = userOpt.get();

        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            user.setNombre(request.getNombre().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && usuarioRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest().body("Ese email ya está en uso por otro usuario.");
            }
            user.setEmail(newEmail);
        }

        if (request.getGuid() != null && !request.getGuid().trim().isEmpty()) {
            String guid = request.getGuid().trim().toUpperCase();
            if (!guid.matches("^[A-F0-9]{18}$")) {
                return ResponseEntity.badRequest().body("El GUID debe ser de 18 caracteres hexadecimales.");
            }
            if (!guid.equals(user.getGuid()) && usuarioRepository.existsByGuid(guid)) {
                return ResponseEntity.badRequest().body("Ese GUID ya está en uso por otro usuario.");
            }
            user.setGuid(guid);
        }

        if (request.getRol() != null && !request.getRol().trim().isEmpty()) {
            try {
                user.setRol(Usuario.Rol.valueOf(request.getRol().trim().toLowerCase()));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body("Rol inválido. Usa: invitado, registrado o admin.");
            }
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String password = request.getPassword().trim();
            if (password.length() < 6) {
                return ResponseEntity.badRequest().body("La contraseña debe tener al menos 6 caracteres.");
            }
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        if (request.getActivo() != null) {
            user.setActivo(request.getActivo());
        }

        usuarioRepository.save(user);
        List<Compra> compras = compraRepository.findByUsuarioId(user.getId());
        return ResponseEntity.ok(buildAdminUserResponse(user, compras));
    }

    @PutMapping("/users/{userId}/purchases/{purchaseId}/guid")
    public ResponseEntity<?> updatePurchaseGuidByAdmin(@PathVariable Long userId,
                                                        @PathVariable Long purchaseId,
                                                        @RequestBody AdminPurchaseGuidUpdateRequest request) {
        Optional<Usuario> userOpt = usuarioRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuario no encontrado.");
        }

        Optional<Compra> compraOpt = compraRepository.findById(purchaseId);
        if (compraOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Compra no encontrada.");
        }

        Compra compra = compraOpt.get();
        if (!compra.getUsuario().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("La compra no pertenece al usuario indicado.");
        }

        if (request.getGuidCompra() == null || request.getGuidCompra().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Debes indicar un GUID de compra.");
        }

        String guid = request.getGuidCompra().trim().toUpperCase();
        if (!guid.matches("^[A-F0-9]{18}$")) {
            return ResponseEntity.badRequest().body("El GUID de compra debe tener 18 caracteres hexadecimales.");
        }

        compra.setGuidCompra(guid);
        compraRepository.save(compra);

        Usuario user = userOpt.get();
        List<Compra> compras = compraRepository.findByUsuarioId(userId);
        return ResponseEntity.ok(buildAdminUserResponse(user, compras));
    }

    @PostMapping("/users/{userId}/purchases/{purchaseId}/resend-download-email")
    public ResponseEntity<?> resendDownloadEmailByAdmin(@PathVariable Long userId,
                                                        @PathVariable Long purchaseId) {
        Optional<Usuario> userOpt = usuarioRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Usuario no encontrado.");
        }

        Optional<Compra> compraOpt = compraRepository.findById(purchaseId);
        if (compraOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Compra no encontrada.");
        }

        Compra compra = compraOpt.get();
        if (!compra.getUsuario().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("La compra no pertenece al usuario indicado.");
        }

        Usuario user = userOpt.get();
        String guid = (compra.getGuidCompra() == null ? "" : compra.getGuidCompra().trim().toUpperCase());
        if (!guid.matches("^[A-F0-9]{18}$")) {
            return ResponseEntity.badRequest().body("La compra no tiene un GUID valido de 18 hex.");
        }

        Optional<EncryptionJob> jobOpt = encryptionJobRepository
                .findTopByUsuarioAndModAndGuidAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        user,
                        compra.getMod(),
                        guid,
                        EncryptionJob.Status.DONE,
                        LocalDateTime.now()
                );

        if (jobOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No hay un enlace de descarga vigente para esta compra. El usuario debe generar uno nuevo.");
        }

        EncryptionJob job = jobOpt.get();
        if (job.getDownloadToken() == null || job.getDownloadToken().isBlank()) {
            return ResponseEntity.badRequest().body("El trabajo de cifrado no tiene token de descarga.");
        }

        String downloadUrl = resolvePublicDownloadBaseUrl() + "/" + job.getDownloadToken();
        emailService.sendDownloadReadyEmail(user.getEmail(), compra.getMod().getNombre(), downloadUrl, job.getExpiresAt());
        job.setNotifiedAt(LocalDateTime.now());
        job.setErrorMessage(null);
        encryptionJobRepository.save(job);

        return ResponseEntity.ok("Correo de descarga reenviado correctamente.");
    }

    private Map<String, Object> buildAdminUserResponse(Usuario user, List<Compra> compras) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("nombre", user.getNombre());
        payload.put("email", user.getEmail());
        payload.put("guid", user.getGuid());
        payload.put("rol", user.getRol().name());
        payload.put("activo", user.isActivo());
        payload.put("creadoEn", user.getCreadoEn());

        double totalSpent = compras.stream()
                .map(Compra::getPrecioPagado)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(value -> value.doubleValue())
                .sum();

        LocalDateTime lastPurchaseAt = compras.stream()
                .map(Compra::getFecha)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        payload.put("purchasesCount", compras.size());
        payload.put("totalSpent", totalSpent);
        payload.put("lastPurchaseAt", lastPurchaseAt);

        List<Map<String, Object>> purchasesPayload = new ArrayList<>();
        for (Compra compra : compras) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", compra.getId());
            row.put("fecha", compra.getFecha());
            row.put("precioPagado", compra.getPrecioPagado());
            row.put("metodoPago", compra.getMetodoPago());
            row.put("guidCompra", compra.getGuidCompra());

            Map<String, Object> mod = new HashMap<>();
            mod.put("id", compra.getMod().getId());
            mod.put("nombre", compra.getMod().getNombre());
            mod.put("version", compra.getMod().getVersion());
            mod.put("archivoOriginal", compra.getMod().getArchivoOriginal());
            row.put("mod", mod);

            purchasesPayload.add(row);
        }

        payload.put("purchases", purchasesPayload);
        return payload;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int calculateTrendPercent(double current, double previous) {
        if (current == 0 && previous == 0) {
            return 0;
        }
        if (previous == 0) {
            return 100;
        }

        double delta = ((current - previous) / previous) * 100.0;
        return (int) Math.round(delta);
    }

    private Map<String, Object> getNasStats() {
        Map<String, Object> nas = new HashMap<>();

        Path homePath = Paths.get(homeImagesDirectory).normalize();
        Path modsPath = Paths.get(modsFilesDirectory).normalize();

        boolean homeAvailable = Files.exists(homePath) && Files.isDirectory(homePath);
        boolean modsAvailable = Files.exists(modsPath) && Files.isDirectory(modsPath);
        boolean online = homeAvailable || modsAvailable;

        nas.put("online", online);
        nas.put("homeImagesPath", homePath.toString());
        nas.put("modsFilesPath", modsPath.toString());

        long homeImagesCount = countFiles(homePath);
        long modsFilesCount = countFiles(modsPath);

        nas.put("homeImagesCount", homeImagesCount);
        nas.put("modsFilesCount", modsFilesCount);

        if (!online) {
            nas.put("totalBytes", 0L);
            nas.put("usedBytes", 0L);
            nas.put("freeBytes", 0L);
            nas.put("usagePercent", 0);
            return nas;
        }

        try {
            Path diskReference = homeAvailable ? homePath : modsPath;
            FileStore fileStore = Files.getFileStore(diskReference);
            long totalBytes = fileStore.getTotalSpace();
            long freeBytes = fileStore.getUsableSpace();
            long usedBytes = Math.max(0, totalBytes - freeBytes);
            int usagePercent = totalBytes > 0 ? (int) Math.min(100, Math.round((usedBytes * 100.0) / totalBytes)) : 0;

            nas.put("totalBytes", totalBytes);
            nas.put("usedBytes", usedBytes);
            nas.put("freeBytes", freeBytes);
            nas.put("usagePercent", usagePercent);
        } catch (IOException e) {
            nas.put("online", false);
            nas.put("totalBytes", 0L);
            nas.put("usedBytes", 0L);
            nas.put("freeBytes", 0L);
            nas.put("usagePercent", 0);
            nas.put("error", "No se pudo leer el estado del almacenamiento NAS.");
        }

        return nas;
    }

    private long countFiles(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return 0;
        }

        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private String resolvePublicDownloadBaseUrl() {
        String configured = publicDownloadBaseUrl == null ? "" : publicDownloadBaseUrl.trim();
        if (!configured.isBlank() && !configured.contains("localhost") && !configured.contains("127.0.0.1")) {
            return configured;
        }

        String frontend = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        if (frontend.endsWith("/")) {
            frontend = frontend.substring(0, frontend.length() - 1);
        }
        return frontend + "/api/descargas/file";
    }
}
