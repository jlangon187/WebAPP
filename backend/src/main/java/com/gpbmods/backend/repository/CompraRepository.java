package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioId(Long usuarioId);
    List<Compra> findByModId(Long modId);
    boolean existsByUsuarioIdAndModId(Long usuarioId, Long modId);
}
