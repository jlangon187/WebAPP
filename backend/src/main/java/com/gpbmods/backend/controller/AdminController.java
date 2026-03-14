package com.gpbmods.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('admin')")
public class AdminController {

    @Value("${mods.images.directory:/data/home-images}")
    private String homeImagesDirectory;

    @Value("${mods.files.directory:/data/mods-files}")
    private String modsFilesDirectory;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSales", 12840);
        stats.put("newUsers", 1240);
        stats.put("activeTickets", 42);
        stats.put("nas", getNasStats());
        return ResponseEntity.ok(stats);
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
