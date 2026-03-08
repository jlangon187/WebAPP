package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Descarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DescargaRepository extends JpaRepository<Descarga, Long> {
    List<Descarga> findByUsuarioId(Long usuarioId);
}
