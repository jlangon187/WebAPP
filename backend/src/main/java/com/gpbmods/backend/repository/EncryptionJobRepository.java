package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.EncryptionJob;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
