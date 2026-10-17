package com.udea.ScoreCrediticio.DAOs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.RangoRiesgo;

@Repository
public interface RangoRiesgoDAO extends JpaRepository<RangoRiesgo, Long> {

    // Orden descendente por puntaje minimo: Bajo, Medio, Alto.
    List<RangoRiesgo> findAllByOrderByPuntajeMinimoDesc();
}
