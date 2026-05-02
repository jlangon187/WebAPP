package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.TicketCreateRequest;
import com.gpbmods.backend.dto.TicketReplyRequest;
import com.gpbmods.backend.model.Ticket;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.TicketRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import com.gpbmods.backend.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TicketController {

    private static final String SUPPORT_SEPARATOR = "\n\n--- RESPUESTA SOPORTE ---\n";

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public TicketController(TicketRepository ticketRepository, UsuarioRepository usuarioRepository, EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> createTicket(@RequestBody TicketCreateRequest request, Authentication authentication) {
        String mensaje = request.getMensaje() == null ? "" : request.getMensaje().trim();
        if (mensaje.isEmpty()) {
            return ResponseEntity.badRequest().body("El mensaje del ticket es obligatorio.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado.");
        }

        Ticket ticket = new Ticket();
        ticket.setUsuario(usuarioOpt.get());
        ticket.setEstado(Ticket.Estado.abierto);
        ticket.setMensaje(mensaje);

        Ticket saved = ticketRepository.save(ticket);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/tickets/mis-tickets")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> getMyTickets(Authentication authentication) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado.");
        }
        List<Ticket> tickets = ticketRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioOpt.get().getId());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> getTicketDetail(@PathVariable Long id, Authentication authentication) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();
        boolean isAdmin = hasAdminAuthority(authentication);
        if (!isAdmin && !ticket.getUsuario().getEmail().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes acceder a este ticket.");
        }

        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/tickets/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
    public ResponseEntity<?> closeOwnTicket(@PathVariable Long id, Authentication authentication) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();
        boolean isAdmin = hasAdminAuthority(authentication);
        if (!isAdmin && !ticket.getUsuario().getEmail().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes cerrar este ticket.");
        }

        if (ticket.getEstado() == Ticket.Estado.cerrado) {
            return ResponseEntity.badRequest().body("El ticket ya está cerrado y no puede reabrirse.");
        }

        ticket.setEstado(Ticket.Estado.cerrado);
        ticketRepository.save(ticket);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(ticketRepository.findAllByOrderByCreadoEnDesc());
    }

    @PutMapping("/admin/tickets/{id}/responder")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> replyTicket(@PathVariable Long id, @RequestBody TicketReplyRequest request, Authentication authentication) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();
        if (ticket.getEstado() == Ticket.Estado.cerrado) {
            return ResponseEntity.badRequest().body("El ticket está cerrado y no puede responderse.");
        }

        String respuesta = request.getRespuesta() == null ? "" : request.getRespuesta().trim();
        if (respuesta.isEmpty()) {
            return ResponseEntity.badRequest().body("La respuesta del soporte es obligatoria.");
        }

        String adminEmail = authentication.getName();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fullResponse = "Soporte: " + adminEmail + "\nFecha: " + stamp + "\n\n" + respuesta;

        String baseMessage = ticket.getMensaje();
        int separatorIndex = baseMessage.indexOf(SUPPORT_SEPARATOR);
        if (separatorIndex >= 0) {
            baseMessage = baseMessage.substring(0, separatorIndex);
        }

        ticket.setMensaje(baseMessage + SUPPORT_SEPARATOR + fullResponse);
        ticket.setEstado(Ticket.Estado.respondido);
        Ticket saved = ticketRepository.save(ticket);

        try {
            emailService.sendTicketResponseEmail(saved.getUsuario().getEmail(), saved.getId(), respuesta, saved.getEstado().name());
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/admin/tickets/{id}/cerrar")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> closeTicketByAdmin(@PathVariable Long id) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ticket ticket = ticketOpt.get();
        if (ticket.getEstado() == Ticket.Estado.cerrado) {
            return ResponseEntity.badRequest().body("El ticket ya está cerrado y no puede reabrirse.");
        }

        ticket.setEstado(Ticket.Estado.cerrado);
        ticketRepository.save(ticket);
        return ResponseEntity.ok(ticket);
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "admin".equalsIgnoreCase(a.getAuthority()));
    }
}
