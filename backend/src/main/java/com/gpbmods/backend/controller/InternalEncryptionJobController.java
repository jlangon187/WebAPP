package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.EncryptionJobUpdateRequest;
import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.repository.EncryptionJobRepository;
import com.gpbmods.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/encryption-jobs")
public class InternalEncryptionJobController {

    @Autowired
    private EncryptionJobRepository encryptionJobRepository;

    @Autowired
    private EmailService emailService;

    @Value("${mods.encryption.worker-api-key:}")
    private String workerApiKey;

    @Value("${mods.downloads.retention-days:15}")
    private int retentionDays;

    @Value("${mods.downloads.public-base-url:http://localhost:8080/api/descargas/file}")
    private String publicDownloadBaseUrl;

    @PostMapping("/next")
    public ResponseEntity<?> getNextJob(@RequestHeader(value = "X-Worker-Key", required = false) String key) {
        ResponseEntity<?> unauthorized = requireWorkerKey(key);
        if (unauthorized != null) {
            return unauthorized;
        }

        Optional<EncryptionJob> nextJobOpt = encryptionJobRepository.findTopByStatusOrderByCreatedAtAsc(EncryptionJob.Status.PENDING);
        if (nextJobOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        EncryptionJob job = nextJobOpt.get();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", job.getId());
        payload.put("modId", job.getMod().getId());
        payload.put("modBaseFolder", job.getModBaseFolder());
        payload.put("guid", job.getGuid());
        payload.put("userId", job.getUsuario().getId());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> markStarted(@PathVariable Long id,
                                         @RequestHeader(value = "X-Worker-Key", required = false) String key) {
        ResponseEntity<?> unauthorized = requireWorkerKey(key);
        if (unauthorized != null) {
            return unauthorized;
        }

        Optional<EncryptionJob> jobOpt = encryptionJobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EncryptionJob job = jobOpt.get();
        job.setStatus(EncryptionJob.Status.RUNNING);
        job.setErrorMessage(null);
        job.setUpdatedAt(LocalDateTime.now());
        encryptionJobRepository.save(job);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> markCompleted(@PathVariable Long id,
                                           @RequestHeader(value = "X-Worker-Key", required = false) String key,
                                           @RequestBody EncryptionJobUpdateRequest request) {
        ResponseEntity<?> unauthorized = requireWorkerKey(key);
        if (unauthorized != null) {
            return unauthorized;
        }

        Optional<EncryptionJob> jobOpt = encryptionJobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (request.getOutputRelativePath() == null || request.getOutputRelativePath().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("outputRelativePath es obligatorio.");
        }

        EncryptionJob job = jobOpt.get();
        job.setStatus(EncryptionJob.Status.DONE);
        job.setOutputRelativePath(request.getOutputRelativePath().trim());
        job.setErrorMessage(null);
        job.setDownloadToken(UUID.randomUUID().toString().replace("-", ""));
        job.setUpdatedAt(LocalDateTime.now());
        job.setExpiresAt(LocalDateTime.now().plusDays(retentionDays));
        encryptionJobRepository.save(job);

        if (job.getNotifiedAt() == null) {
            String downloadUrl = publicDownloadBaseUrl + "/" + job.getDownloadToken();
            try {
                emailService.sendDownloadReadyEmail(
                        job.getUsuario().getEmail(),
                        job.getMod().getNombre(),
                        downloadUrl,
                        job.getExpiresAt()
                );
                job.setNotifiedAt(LocalDateTime.now());
                encryptionJobRepository.save(job);
            } catch (Exception e) {
                job.setErrorMessage("Aviso: descarga generada pero no se pudo enviar email: " + e.getMessage());
                encryptionJobRepository.save(job);
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> markFailed(@PathVariable Long id,
                                        @RequestHeader(value = "X-Worker-Key", required = false) String key,
                                        @RequestBody EncryptionJobUpdateRequest request) {
        ResponseEntity<?> unauthorized = requireWorkerKey(key);
        if (unauthorized != null) {
            return unauthorized;
        }

        Optional<EncryptionJob> jobOpt = encryptionJobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EncryptionJob job = jobOpt.get();
        job.setStatus(EncryptionJob.Status.FAILED);
        job.setErrorMessage(request.getErrorMessage() == null ? "Error no especificado" : request.getErrorMessage());
        job.setUpdatedAt(LocalDateTime.now());
        encryptionJobRepository.save(job);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> requireWorkerKey(String key) {
        if (workerApiKey == null || workerApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("La clave interna del worker no esta configurada.");
        }
        if (key == null || !workerApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Worker key invalida.");
        }
        return null;
    }
}
