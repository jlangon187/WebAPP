package com.gpbmods.backend.service;

import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.repository.EncryptionJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EncryptionJobMaintenanceService {

    private final EncryptionJobRepository encryptionJobRepository;

    @Value("${mods.files.directory:/data/mods-files}")
    private String modsFilesDirectory;

    @Value("${mods.encryption.running-timeout-minutes:30}")
    private int runningTimeoutMinutes;

    public EncryptionJobMaintenanceService(EncryptionJobRepository encryptionJobRepository) {
        this.encryptionJobRepository = encryptionJobRepository;
    }

    @Scheduled(cron = "${mods.encryption.maintenance-cron:0 */15 * * * *}")
    @Transactional
    public void runMaintenance() {
        markStaleRunningJobsAsFailed();
        cleanupExpiredGeneratedFiles();
    }

    private void markStaleRunningJobsAsFailed() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(Math.max(5, runningTimeoutMinutes));
        List<EncryptionJob> stale = encryptionJobRepository.findByStatusAndUpdatedAtBefore(EncryptionJob.Status.RUNNING, threshold);
        for (EncryptionJob job : stale) {
            job.setStatus(EncryptionJob.Status.FAILED);
            job.setErrorMessage("Job marcado como fallido por timeout de ejecucion.");
            job.setUpdatedAt(LocalDateTime.now());
        }
        if (!stale.isEmpty()) {
            encryptionJobRepository.saveAll(stale);
        }
    }

    private void cleanupExpiredGeneratedFiles() {
        List<EncryptionJob> doneJobs = encryptionJobRepository.findByStatusAndUpdatedAtBefore(
                EncryptionJob.Status.DONE,
                LocalDateTime.now().minusDays(1)
        );

        Path base = Paths.get(modsFilesDirectory).normalize();
        for (EncryptionJob job : doneJobs) {
            if (job.getExpiresAt() == null || job.getExpiresAt().isAfter(LocalDateTime.now())) {
                continue;
            }

            if (job.getOutputRelativePath() == null || job.getOutputRelativePath().isBlank()) {
                continue;
            }

            try {
                Path target = base.resolve(job.getOutputRelativePath()).normalize();
                if (target.startsWith(base) && Files.exists(target)) {
                    Files.delete(target);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
