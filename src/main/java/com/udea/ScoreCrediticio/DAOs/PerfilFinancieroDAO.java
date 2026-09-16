package com.udea.ScoreCrediticio.DAOs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.PerfilFinanciero;

@Repository
public interface PerfilFinancieroDAO extends JpaRepository<PerfilFinanciero, Long> {
    // Genera automáticamente los métodos CRUD para la entidad PerfilFinanciero

    Optional<PerfilFinanciero> findBySolicitanteId(Long solicitanteId);
}
