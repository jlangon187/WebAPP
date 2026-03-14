package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioId(Long usuarioId);
    List<Compra> findByModId(Long modId);
    boolean existsByUsuarioIdAndModId(Long usuarioId, Long modId);

    @Query("SELECT COALESCE(SUM(c.precioPagado), 0) FROM Compra c")
    BigDecimal sumTotalSales();
}
