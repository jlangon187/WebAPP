package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.PasswordResetToken;
import com.gpbmods.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByUsuario(Usuario usuario);
    
    void deleteByUsuario(Usuario usuario);
}
