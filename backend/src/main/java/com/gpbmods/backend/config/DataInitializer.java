package com.gpbmods.backend.config;

import com.gpbmods.backend.model.Categoria;
import com.gpbmods.backend.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedCategorias(CategoriaRepository categoriaRepository) {
        return args -> {
            List<String> categoriasFijas = List.of(
                    "Motos",
                    "Bikesets",
                    "Liveries",
                    "Sonidos",
                    "Circuitos",
                    "Equipamiento",
                    "UI"
            );

            for (String nombreCategoria : categoriasFijas) {
                if (categoriaRepository.findByNombre(nombreCategoria).isEmpty()) {
                    categoriaRepository.save(new Categoria(nombreCategoria));
                }
            }
        };
    }
}
