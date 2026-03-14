package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioId(Long usuarioId);
    List<Compra> findByModId(Long modId);
    boolean existsByUsuarioIdAndModId(Long usuarioId, Long modId);

    @Query("SELECT COALESCE(SUM(c.precioPagado), 0) FROM Compra c")
    BigDecimal sumTotalSales();

    @Query("SELECT COALESCE(SUM(c.precioPagado), 0) FROM Compra c WHERE c.fecha >= :fromDate AND c.fecha < :toDate")
    BigDecimal sumSalesBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
