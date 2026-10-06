package com.udea.ScoreCrediticio.DAOs;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@Repository
public interface VariableRiesgoDAO extends JpaRepository<VariableRiesgo, Long> {
    // Genera automaticamente los metodos CRUD para la entidad VariableRiesgo

    // La comparacion ignora mayusculas y minusculas para evitar nombres duplicados
    Optional<VariableRiesgo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<VariableRiesgo> findAllByOrderByNombreAsc();

    // Suma de los pesos de las variables del estado indicado. Devuelve null si no hay ninguna.
    @Query("select sum(v.peso) from VariableRiesgo v where v.estado = :estado")
    BigDecimal sumarPesosPorEstado(@Param("estado") EstadoVariable estado);
}
