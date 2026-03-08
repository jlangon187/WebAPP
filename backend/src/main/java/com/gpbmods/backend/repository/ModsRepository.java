package com.gpbmods.backend.repository;

import com.gpbmods.backend.model.Mods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModsRepository extends JpaRepository<Mods, Long> {
}
