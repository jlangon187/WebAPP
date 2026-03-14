package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUsuarioId(Long usuarioId);
    long countByEstadoNot(Ticket.Estado estado);
    long countByEstadoNotAndCreadoEnBetween(Ticket.Estado estado, LocalDateTime fromDate, LocalDateTime toDate);
}
