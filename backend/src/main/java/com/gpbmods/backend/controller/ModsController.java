package com.gpbmods.backend.controller;

import com.gpbmods.backend.model.Mods;
import com.gpbmods.backend.repository.ModsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mods")
public class ModsController {

    @Autowired
    private ModsRepository modsRepository;

    @GetMapping("/catalog")
    public List<Mods> getAllMods() {
        return modsRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('registrado', 'admin')")
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
    public ResponseEntity<Mods> createMod(@RequestBody Mods mod) {
        // En una app real, aquí se subiría y validaría el archivo.
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
}
