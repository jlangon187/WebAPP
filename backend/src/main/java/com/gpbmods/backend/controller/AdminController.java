package com.gpbmods.backend.controller;

import com.gpbmods.backend.model.Ticket;
import com.gpbmods.backend.repository.CompraRepository;
import com.gpbmods.backend.repository.TicketRepository;
import com.gpbmods.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('admin')")
public class AdminController {

    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;

    public AdminController(CompraRepository compraRepository, UsuarioRepository usuarioRepository, TicketRepository ticketRepository) {
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.ticketRepository = ticketRepository;
    }

    @Value("${mods.images.directory:/data/home-images}")
    private String homeImagesDirectory;

    @Value("${mods.files.directory:/data/mods-files}")
    private String modsFilesDirectory;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30From = now.minusDays(30);
        LocalDateTime prev30From = now.minusDays(60);

        BigDecimal totalSales = compraRepository.sumTotalSales();
        BigDecimal salesLast30 = safeDecimal(compraRepository.sumSalesBetween(last30From, now));
        BigDecimal salesPrev30 = safeDecimal(compraRepository.sumSalesBetween(prev30From, last30From));

        long newUsers = usuarioRepository.countByCreadoEnAfter(last30From);
        long usersLast30 = usuarioRepository.countByCreadoEnBetween(last30From, now);
        long usersPrev30 = usuarioRepository.countByCreadoEnBetween(prev30From, last30From);

        long activeTickets = ticketRepository.countByEstadoNot(Ticket.Estado.cerrado);
        long ticketsLast30 = ticketRepository.countByEstadoNotAndCreadoEnBetween(Ticket.Estado.cerrado, last30From, now);
        long ticketsPrev30 = ticketRepository.countByEstadoNotAndCreadoEnBetween(Ticket.Estado.cerrado, prev30From, last30From);

        stats.put("totalSales", totalSales == null ? BigDecimal.ZERO : totalSales);
        stats.put("newUsers", newUsers);
        stats.put("activeTickets", activeTickets);
        stats.put("salesTrendPercent", calculateTrendPercent(salesLast30.doubleValue(), salesPrev30.doubleValue()));
        stats.put("usersTrendPercent", calculateTrendPercent((double) usersLast30, (double) usersPrev30));
        stats.put("ticketsTrendPercent", calculateTrendPercent((double) ticketsLast30, (double) ticketsPrev30));
        stats.put("nas", getNasStats());
        return ResponseEntity.ok(stats);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int calculateTrendPercent(double current, double previous) {
        if (current == 0 && previous == 0) {
            return 0;
        }
        if (previous == 0) {
            return 100;
        }

        double delta = ((current - previous) / previous) * 100.0;
        return (int) Math.round(delta);
    }

    private Map<String, Object> getNasStats() {
        Map<String, Object> nas = new HashMap<>();

        Path homePath = Paths.get(homeImagesDirectory).normalize();
        Path modsPath = Paths.get(modsFilesDirectory).normalize();

        boolean homeAvailable = Files.exists(homePath) && Files.isDirectory(homePath);
        boolean modsAvailable = Files.exists(modsPath) && Files.isDirectory(modsPath);
        boolean online = homeAvailable || modsAvailable;

        nas.put("online", online);
        nas.put("homeImagesPath", homePath.toString());
        nas.put("modsFilesPath", modsPath.toString());

        long homeImagesCount = countFiles(homePath);
        long modsFilesCount = countFiles(modsPath);

        nas.put("homeImagesCount", homeImagesCount);
        nas.put("modsFilesCount", modsFilesCount);

        if (!online) {
            nas.put("totalBytes", 0L);
            nas.put("usedBytes", 0L);
            nas.put("freeBytes", 0L);
            nas.put("usagePercent", 0);
            return nas;
        }

        try {
            Path diskReference = homeAvailable ? homePath : modsPath;
            FileStore fileStore = Files.getFileStore(diskReference);
            long totalBytes = fileStore.getTotalSpace();
            long freeBytes = fileStore.getUsableSpace();
            long usedBytes = Math.max(0, totalBytes - freeBytes);
            int usagePercent = totalBytes > 0 ? (int) Math.min(100, Math.round((usedBytes * 100.0) / totalBytes)) : 0;

            nas.put("totalBytes", totalBytes);
            nas.put("usedBytes", usedBytes);
            nas.put("freeBytes", freeBytes);
            nas.put("usagePercent", usagePercent);
        } catch (IOException e) {
            nas.put("online", false);
            nas.put("totalBytes", 0L);
            nas.put("usedBytes", 0L);
            nas.put("freeBytes", 0L);
            nas.put("usagePercent", 0);
            nas.put("error", "No se pudo leer el estado del almacenamiento NAS.");
        }

        return nas;
    }

    private long countFiles(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return 0;
        }

        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
