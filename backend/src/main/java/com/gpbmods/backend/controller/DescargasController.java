package com.gpbmods.backend.controller;

import com.gpbmods.backend.model.Descarga;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.DescargaRepository;
import com.gpbmods.backend.repository.ModsRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/descargas")
public class DescargasController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private DescargaRepository descargaRepository;

    @Autowired
    private ModsRepository modsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/{modId}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> requestDownloadUrl(@PathVariable Long modId, Authentication authentication) {
        String email = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }
        Usuario usuario = usuarioOpt.get();

        Optional<Mods> modOpt = modsRepository.findById(modId);
        if (modOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Mods mod = modOpt.get();

        boolean hasPurchased = compraRepository.existsByUsuarioIdAndModId(usuario.getId(), mod.getId());
        boolean isAdmin = usuario.getRol() == Usuario.Rol.admin;

        if (!hasPurchased && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You must purchase this mod to download it.");
        }

        // Register download activity
        Descarga descarga = new Descarga();
        descarga.setMod(mod);
        descarga.setUsuario(usuario);
        descargaRepository.save(descarga);

        // Simulated direct link to NAS or redirect (Future: encryption logic here)
        String nasDownloadLink = "http://192.168.1.100/nas/mods/" + mod.getArchivoOriginal();

        return ResponseEntity.ok(nasDownloadLink);
    }
}
