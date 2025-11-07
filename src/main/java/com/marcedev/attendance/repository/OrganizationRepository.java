package com.marcedev.attendance.repository;

import com.marcedev.attendance.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de Organization.
 * - No reemplaza nada existente.
 * - Solo habilita búsquedas por id / name / code.
 */
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Buscar por código corto (opcional)
   // Optional<Organization> findByCode(String code);

    // Buscar por nombre (opcional)
    Optional<Organization> findByName(String name);
}
