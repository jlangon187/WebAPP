package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.CompraRequest;
import com.gpbmods.backend.model.Compra;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.ModsRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import com.gpbmods.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ModsRepository modsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> simulatePurchase(@RequestBody CompraRequest request, Authentication authentication) {
        String email = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }
        Usuario usuario = usuarioOpt.get();

        Optional<Mods> modOpt = modsRepository.findById(request.getModId());
        if (modOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Mod not found.");
        }
        Mods mod = modOpt.get();

        if (compraRepository.existsByUsuarioIdAndModId(usuario.getId(), mod.getId())) {
            return ResponseEntity.badRequest().body("You already own this Mod.");
        }

        String guidUsuario = usuario.getGuid();
        if (guidUsuario == null || !guidUsuario.matches("^[A-F0-9]{18}$")) {
            return ResponseEntity.badRequest().body("Debes completar tu GUID de juego (18 hex) antes de comprar mods.");
        }

        // Handle simulation
        if (!"Simulacion".equalsIgnoreCase(request.getMetodoPago()) &&
            !"Stripe".equalsIgnoreCase(request.getMetodoPago()) &&
            !"Paypal".equalsIgnoreCase(request.getMetodoPago())) {
            return ResponseEntity.badRequest().body("Invalid Payment Method.");
        }

        // Simulated Success
        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setMod(mod);
        compra.setPrecioPagado(mod.getPrecio());
        compra.setMetodoPago(request.getMetodoPago());
        compra.setGuidCompra(guidUsuario.toUpperCase());
        compraRepository.save(compra);

        try {
            emailService.sendPurchaseReceiptEmail(
                    usuario.getEmail(),
                    usuario.getNombre(),
                    List.of(mod.getNombre()),
                    request.getMetodoPago(),
                    mod.getPrecio().toString()
            );
            emailService.sendPurchaseAdminNotification(
                    usuario.getNombre(),
                    usuario.getEmail(),
                    List.of(mod.getNombre()),
                    request.getMetodoPago(),
                    mod.getPrecio().toString()
            );
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok("Purchase successful via " + request.getMetodoPago() + "!");
    }

    @GetMapping("/mis-compras")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> getMisCompras(Authentication authentication) {
        String email = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }
        return ResponseEntity.ok(compraRepository.findByUsuarioId(usuarioOpt.get().getId()));
    }
}
