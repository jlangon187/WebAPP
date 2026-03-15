package com.gpbmods.backend.controller;

import com.gpbmods.backend.model.Categoria;
import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.repository.CategoriaRepository;
import com.gpbmods.backend.repository.ModsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/mods")
public class ModsController {

    @Autowired
    private ModsRepository modsRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Value("${mods.images.directory:../frontend/public/home}")
    private String homeImagesDirectory;

    @GetMapping("/catalog")
    public List<Mods> getAllMods() {
        return modsRepository.findAll();
    }

    @GetMapping("/showroom")
    public List<Mods> getShowroomMods() {
        return modsRepository.findByDestacadoHomeTrueOrderByOrdenShowroomAsc().stream().limit(3).toList();
    }

    @GetMapping("/home-images")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<List<String>> getHomeImages() {
        try {
            Path basePath = Paths.get(homeImagesDirectory).normalize();
            if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
                return ResponseEntity.ok(List.of());
            }

            try (Stream<Path> files = Files.list(basePath)) {
                List<String> imagePaths = files
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(this::isSupportedImage)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .map(fileName -> "/home/" + fileName)
                        .toList();

                return ResponseEntity.ok(imagePaths);
            }
        } catch (IOException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getModById(@PathVariable Long id) {
        Optional<Mods> mod = modsRepository.findById(id);
        if (mod.isPresent()) {
            return ResponseEntity.ok(mod.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> createMod(@RequestBody Mods mod) {
        ResponseEntity<?> validationError = validateShowroomSelection(mod, null);
        if (validationError != null) {
            return validationError;
        }

        Optional<Categoria> categoriaOpt = resolveCategoria(mod);
        if (mod.getCategoria() != null && categoriaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: categoría inválida.");
        }

        categoriaOpt.ifPresent(mod::setCategoria);
        Mods savedMod = modsRepository.save(mod);
        return ResponseEntity.ok(savedMod);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> updateMod(@PathVariable Long id, @RequestBody Mods modDetails) {
        Optional<Mods> optionalMod = modsRepository.findById(id);
        if (!optionalMod.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Mods existingMod = optionalMod.get();
        existingMod.setNombre(modDetails.getNombre());
        existingMod.setDescripcion(modDetails.getDescripcion());
        existingMod.setPrecio(modDetails.getPrecio());
        existingMod.setVersion(modDetails.getVersion());
        existingMod.setArchivoOriginal(modDetails.getArchivoOriginal());
        existingMod.setYoutubeUrl(modDetails.getYoutubeUrl());
        existingMod.setDestacadoHome(modDetails.isDestacadoHome());
        existingMod.setOrdenShowroom(modDetails.getOrdenShowroom());

        Optional<Categoria> categoriaOpt = resolveCategoria(modDetails);
        if (modDetails.getCategoria() != null && categoriaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: categoría inválida.");
        }
        existingMod.setCategoria(categoriaOpt.orElse(null));

        ResponseEntity<?> validationError = validateShowroomSelection(existingMod, id);
        if (validationError != null) {
            return validationError;
        }

        Mods updatedMod = modsRepository.save(existingMod);
        return ResponseEntity.ok(updatedMod);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> deleteMod(@PathVariable Long id) {
        if (!modsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        modsRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private Optional<Categoria> resolveCategoria(Mods mod) {
        if (mod.getCategoria() == null) {
            return Optional.empty();
        }

        if (mod.getCategoria().getId() != null) {
            return categoriaRepository.findById(mod.getCategoria().getId());
        }

        String nombre = mod.getCategoria().getNombre();
        if (nombre == null || nombre.trim().isEmpty()) {
            return Optional.empty();
        }

        String nombreNormalizado = nombre.trim();
        Optional<Categoria> categoriaExistente = categoriaRepository.findByNombre(nombreNormalizado);
        if (categoriaExistente.isPresent()) {
            return categoriaExistente;
        }

        Categoria nuevaCategoria = new Categoria();
        nuevaCategoria.setNombre(nombreNormalizado);
        return Optional.of(categoriaRepository.save(nuevaCategoria));
    }

    private ResponseEntity<?> validateShowroomSelection(Mods mod, Long currentModId) {
        if (!mod.isDestacadoHome()) {
            mod.setOrdenShowroom(null);
            return null;
        }

        Integer orden = mod.getOrdenShowroom();
        if (orden == null || orden < 1 || orden > 3) {
            return ResponseEntity.badRequest().body("Error: el orden del showroom debe ser 1, 2 o 3.");
        }

        long featuredCount = modsRepository.countByDestacadoHomeTrue();
        if (currentModId == null && featuredCount >= 3) {
            return ResponseEntity.badRequest().body("Error: el showroom solo permite 3 mods destacados.");
        }

        if (currentModId != null) {
            boolean wasAlreadyFeatured = modsRepository.findById(currentModId)
                    .map(Mods::isDestacadoHome)
                    .orElse(false);

            if (!wasAlreadyFeatured && featuredCount >= 3) {
                return ResponseEntity.badRequest().body("Error: el showroom solo permite 3 mods destacados.");
            }
        }

        boolean orderTaken;
        if (currentModId == null) {
            orderTaken = modsRepository.existsByDestacadoHomeTrueAndOrdenShowroom(orden);
        } else {
            orderTaken = modsRepository.existsByDestacadoHomeTrueAndOrdenShowroomAndIdNot(orden, currentModId);
        }

        if (orderTaken) {
            return ResponseEntity.badRequest().body("Error: ya existe otro mod destacado en ese orden del showroom.");
        }

        return null;
    }

    private boolean isSupportedImage(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".avif");
    }
}
