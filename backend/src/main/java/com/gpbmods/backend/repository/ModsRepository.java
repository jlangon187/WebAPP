package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Mods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModsRepository extends JpaRepository<Mods, Long> {
    List<Mods> findByDestacadoHomeTrueOrderByOrdenShowroomAsc();
    long countByDestacadoHomeTrue();
    boolean existsByDestacadoHomeTrueAndOrdenShowroom(Integer ordenShowroom);
    boolean existsByDestacadoHomeTrueAndOrdenShowroomAndIdNot(Integer ordenShowroom, Long id);
}
