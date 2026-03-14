package com.gpbmods.backend.controller;

import com.gpbmods.backend.model.Categoria;
import com.gpbmods.backend.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping
    public List<Categoria> getCategorias() {
        return categoriaRepository.findAll();
    }
}
