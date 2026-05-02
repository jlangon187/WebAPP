package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.ComentarioCreateRequest;
import com.gpbmods.backend.model.Comentario;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.ComentarioRepository;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.ModsRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ComentarioController {

    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModsRepository modsRepository;
    private final CompraRepository compraRepository;

    public ComentarioController(ComentarioRepository comentarioRepository,
                                UsuarioRepository usuarioRepository,
                                ModsRepository modsRepository,
                                CompraRepository compraRepository) {
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.modsRepository = modsRepository;
        this.compraRepository = compraRepository;
    }

    @GetMapping("/mods/{modId}/comentarios")
    public ResponseEntity<?> getComentariosByMod(@PathVariable Long modId) {
        List<Comentario> comentarios = comentarioRepository.findByModId(modId);
        List<Map<String, Object>> payload = comentarios.stream().map(c -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("puntuacion", c.getPuntuacion());
            item.put("mensaje", c.getMensaje());
            item.put("creadoEn", c.getCreadoEn());
            item.put("usuarioNombre", c.getUsuario().getNombre());
            item.put("usuarioId", c.getUsuario().getId());
            return item;
        }).toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/mods/ratings")
    public ResponseEntity<?> getRatingsSummary() {
        List<Map<String, Object>> payload = comentarioRepository.getRatingsSummaryByMod().stream().map(row -> {
            Long modId = row[0] == null ? null : ((Number) row[0]).longValue();
            Double avg = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            Long total = row[2] == null ? 0L : ((Number) row[2]).longValue();

            Map<String, Object> item = new HashMap<>();
            item.put("modId", modId);
            item.put("avgPuntuacion", avg);
            item.put("totalComentarios", total);
            return item;
        }).toList();

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/comentarios/mis")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> getMisComentarios(Authentication authentication) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado.");
        }

        Long usuarioId = usuarioOpt.get().getId();
        List<Map<String, Object>> payload = comentarioRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && usuarioId.equals(c.getUsuario().getId()))
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", c.getId());
                    item.put("puntuacion", c.getPuntuacion());
                    item.put("mensaje", c.getMensaje());
                    item.put("creadoEn", c.getCreadoEn());
                    item.put("modId", c.getMod() != null ? c.getMod().getId() : null);
                    item.put("modNombre", c.getMod() != null ? c.getMod().getNombre() : "-");
                    return item;
                }).toList();

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/mods/{modId}/comentarios")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> createComentario(@PathVariable Long modId,
                                              @RequestBody ComentarioCreateRequest request,
                                              Authentication authentication) {
        String mensaje = request.getMensaje() == null ? "" : request.getMensaje().trim();
        Integer puntuacion = request.getPuntuacion();

        if (mensaje.isEmpty()) {
            return ResponseEntity.badRequest().body("El comentario no puede estar vacío.");
        }
        if (puntuacion == null || puntuacion < 1 || puntuacion > 5) {
            return ResponseEntity.badRequest().body("La puntuación debe estar entre 1 y 5.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado.");
        }

        Optional<Mods> modOpt = modsRepository.findById(modId);
        if (modOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Mod no encontrado.");
        }

        Usuario usuario = usuarioOpt.get();
        if (!compraRepository.existsByUsuarioIdAndModId(usuario.getId(), modId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo puedes valorar y comentar mods que hayas comprado.");
        }

        Comentario comentario = new Comentario();
        comentario.setUsuario(usuario);
        comentario.setMod(modOpt.get());
        comentario.setPuntuacion(puntuacion);
        comentario.setMensaje(mensaje);

        Comentario saved = comentarioRepository.save(comentario);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "puntuacion", saved.getPuntuacion(),
                "mensaje", saved.getMensaje(),
                "creadoEn", saved.getCreadoEn(),
                "usuarioNombre", usuario.getNombre(),
                "usuarioId", usuario.getId()
        ));
    }

    @DeleteMapping("/admin/comentarios/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> deleteComentario(@PathVariable Long id) {
        if (!comentarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        comentarioRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
