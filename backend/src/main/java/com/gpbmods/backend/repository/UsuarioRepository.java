package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByGuid(String guid);
    boolean existsByEmail(String email);
    boolean existsByGuid(String guid);
    List<Usuario> findAllByOrderByCreadoEnDesc();
    long countByCreadoEnAfter(LocalDateTime date);
    long countByCreadoEnBetween(LocalDateTime fromDate, LocalDateTime toDate);
}
