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
    @PreAuthorize("hasAnyRole('REGISTRADO', 'ADMIN')")
    public ResponseEntity<?> getModById(@PathVariable Long id) {
        Optional<Mods> mod = modsRepository.findById(id);
        if (mod.isPresent()) {
            return ResponseEntity.ok(mod.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
