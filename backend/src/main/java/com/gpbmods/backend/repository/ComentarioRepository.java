package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByModId(Long modId);

    boolean existsByUsuarioIdAndModId(Long usuarioId, Long modId);

    @Query("SELECT c.mod.id, AVG(c.puntuacion), COUNT(c.id) FROM Comentario c GROUP BY c.mod.id")
    List<Object[]> getRatingsSummaryByMod();
}
