package com.udea.ScoreCrediticio.DAOs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;

@Repository
public interface HistorialEvaluacionDAO extends JpaRepository<HistorialEvaluacion, Long> {
    // Genera automaticamente los metodos CRUD para la entidad HistorialEvaluacion
}
