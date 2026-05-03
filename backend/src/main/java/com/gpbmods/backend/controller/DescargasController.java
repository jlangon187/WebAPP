package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.PrepareDownloadResponse;
import com.gpbmods.backend.model.Compra;
import com.gpbmods.backend.model.Descarga;
import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.DescargaRepository;
import com.gpbmods.backend.repository.EncryptionJobRepository;
import com.gpbmods.backend.repository.ModsRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Autowired
    private EncryptionJobRepository encryptionJobRepository;

    @Value("${mods.downloads.retention-days:15}")
    private int retentionDays;

    @Value("${mods.downloads.public-base-url:http://localhost:8080/api/descargas/file}")
    private String publicDownloadBaseUrl;

    @Value("${mods.files.directory:/data/mods-files}")
    private String modsFilesDirectory;

    @GetMapping("/{modId}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> requestDownloadUrl(@PathVariable Long modId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Usa POST /api/descargas/" + modId + "/prepare para generar la descarga personalizada por GUID.");
    }

    @PostMapping("/{modId}/prepare")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> prepareDownload(@PathVariable Long modId, Authentication authentication) {
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

        if (usuario.getGuid() == null || !usuario.getGuid().matches("^[A-F0-9]{18}$")) {
            return ResponseEntity.badRequest().body("Debes tener un GUID valido (18 hex) para preparar la descarga personalizada.");
        }

        String folder = sanitizeFolder(mod.getCarpetaBaseMod());
        if (folder == null || folder.isBlank()) {
            return ResponseEntity.badRequest().body("Este mod no tiene carpeta base configurada para cifrado.");
        }

        // Register download activity
        Descarga descarga = new Descarga();
        descarga.setMod(mod);
        descarga.setUsuario(usuario);
        descargaRepository.save(descarga);

        LocalDateTime now = LocalDateTime.now();
        String guid = usuario.getGuid().trim().toUpperCase();

        Optional<EncryptionJob> reusableDone = encryptionJobRepository
                .findTopByUsuarioAndModAndGuidAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        usuario,
                        mod,
                        guid,
                        EncryptionJob.Status.DONE,
                        now
                );

        if (reusableDone.isPresent()) {
            EncryptionJob doneJob = reusableDone.get();
            return ResponseEntity.ok(new PrepareDownloadResponse(
                    doneJob.getId(),
                    doneJob.getStatus().name(),
                    "Paquete ya generado.",
                    doneJob.getDownloadToken()
            ));
        }

        Optional<EncryptionJob> runningJob = encryptionJobRepository
                .findTopByUsuarioAndModAndGuidAndStatusInOrderByCreatedAtDesc(
                        usuario,
                        mod,
                        guid,
                        List.of(EncryptionJob.Status.PENDING, EncryptionJob.Status.RUNNING)
                );

        if (runningJob.isPresent()) {
            EncryptionJob job = runningJob.get();
            return ResponseEntity.ok(new PrepareDownloadResponse(
                    job.getId(),
                    job.getStatus().name(),
                    "La descarga se esta preparando.",
                    null
            ));
        }

        EncryptionJob newJob = new EncryptionJob();
        newJob.setUsuario(usuario);
        newJob.setMod(mod);
        newJob.setGuid(guid);
        newJob.setModBaseFolder(folder);
        newJob.setStatus(EncryptionJob.Status.PENDING);
        newJob.setUpdatedAt(now);
        newJob.setExpiresAt(now.plusDays(retentionDays));

        encryptionJobRepository.save(newJob);

        return ResponseEntity.ok(new PrepareDownloadResponse(
                newJob.getId(),
                newJob.getStatus().name(),
                "Trabajo de cifrado en cola.",
                null
        ));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> getJobStatus(@PathVariable Long jobId, Authentication authentication) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Usuario usuario = usuarioOpt.get();
        Optional<EncryptionJob> jobOpt = encryptionJobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EncryptionJob job = jobOpt.get();
        boolean isOwner = job.getUsuario().getId().equals(usuario.getId());
        boolean isAdmin = usuario.getRol() == Usuario.Rol.admin;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes consultar este trabajo.");
        }

        return ResponseEntity.ok(new PrepareDownloadResponse(
                job.getId(),
                job.getStatus().name(),
                job.getErrorMessage(),
                job.getStatus() == EncryptionJob.Status.DONE ? job.getDownloadToken() : null
        ));
    }

    @GetMapping("/file/{token}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> downloadFile(@PathVariable String token, Authentication authentication) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Optional<EncryptionJob> jobOpt = encryptionJobRepository.findByDownloadToken(token);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EncryptionJob job = jobOpt.get();
        Usuario usuario = usuarioOpt.get();
        boolean isOwner = job.getUsuario().getId().equals(usuario.getId());
        boolean isAdmin = usuario.getRol() == Usuario.Rol.admin;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes descargar este archivo.");
        }

        if (job.getStatus() != EncryptionJob.Status.DONE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El paquete aun no esta listo.");
        }

        if (job.getExpiresAt() == null || LocalDateTime.now().isAfter(job.getExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE).body("El enlace ha expirado. Vuelve a preparar la descarga.");
        }

        if (job.getOutputRelativePath() == null || job.getOutputRelativePath().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontro la ruta del archivo generado.");
        }

        try {
            Path base = Paths.get(modsFilesDirectory).normalize();
            Path filePath = base.resolve(job.getOutputRelativePath()).normalize();
            if (!filePath.startsWith(base)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ruta de archivo invalida.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Archivo no encontrado.");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo preparar la descarga del archivo.");
        }
    }

    private String sanitizeFolder(String folder) {
        if (folder == null) {
            return null;
        }

        String clean = folder.trim();
        if (clean.isEmpty()) {
            return null;
        }

        if (clean.contains("..") || clean.contains("/") || clean.contains("\\")) {
            return null;
        }

        if (!clean.matches("^[A-Za-z0-9._-]+$")) {
            return null;
        }

        return clean;
    }

    public String buildPublicDownloadUrl(String token) {
        return URI.create(publicDownloadBaseUrl + "/" + token).toString();
    }
}
