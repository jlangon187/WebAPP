package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EncryptionJobRepository extends JpaRepository<EncryptionJob, Long> {
    Optional<EncryptionJob> findByDownloadToken(String downloadToken);

    Optional<EncryptionJob> findTopByUsuarioAndModAndGuidAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Usuario usuario,
            Mods mod,
            String guid,
            EncryptionJob.Status status,
            LocalDateTime expiresAt
    );

    Optional<EncryptionJob> findTopByUsuarioAndModAndGuidAndStatusInOrderByCreatedAtDesc(
            Usuario usuario,
            Mods mod,
            String guid,
            List<EncryptionJob.Status> statuses
    );

    Optional<EncryptionJob> findTopByStatusOrderByCreatedAtAsc(EncryptionJob.Status status);

    @Modifying
    @Query("UPDATE EncryptionJob j SET j.status = :runningStatus, j.updatedAt = :updatedAt, j.errorMessage = null WHERE j.id = :jobId AND j.status = :pendingStatus")
    int claimPendingJob(@Param("jobId") Long jobId,
                        @Param("pendingStatus") EncryptionJob.Status pendingStatus,
                        @Param("runningStatus") EncryptionJob.Status runningStatus,
                        @Param("updatedAt") LocalDateTime updatedAt);

    List<EncryptionJob> findByStatusAndUpdatedAtBefore(EncryptionJob.Status status, LocalDateTime threshold);

    List<EncryptionJob> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(EncryptionJob.Status status);

    long countByStatusAndNotifiedAtIsNull(EncryptionJob.Status status);

    long countByStatusAndErrorMessageIsNotNull(EncryptionJob.Status status);
}
