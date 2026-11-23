package com.udea.ScoreCrediticio.DAOs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.DTOs.Response.CarteraReporteFilaDTO;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;

@Repository
public interface HistorialEvaluacionDAO extends JpaRepository<HistorialEvaluacion, Long> {
    // Genera automaticamente los metodos CRUD para la entidad HistorialEvaluacion

    @EntityGraph(attributePaths = "variables")
    List<HistorialEvaluacion> findBySolicitanteIdOrderByFechaCalculoDescIdDesc(Long solicitanteId);

    interface ConteoNivelRiesgo {
        NivelRiesgo getNivel();

        Long getTotal();
    }

    /**
     * HU13: agrupa por nivel usando exclusivamente la ultima evaluacion de cada
     * solicitante dentro del rango [desde, hastaExclusivo), desempatando por id
     * para garantizar que un mismo solicitante nunca se cuente mas de una vez.
     */
    @Query("""
            select h.nivel as nivel, count(h) as total
            from HistorialEvaluacion h
            where h.fechaCalculo >= :desde
              and h.fechaCalculo < :hastaExclusivo
              and not exists (
                  select h2.id
                  from HistorialEvaluacion h2
                  where h2.solicitante.id = h.solicitante.id
                    and h2.fechaCalculo >= :desde
                    and h2.fechaCalculo < :hastaExclusivo
                    and (
                        h2.fechaCalculo > h.fechaCalculo
                        or (h2.fechaCalculo = h.fechaCalculo and h2.id > h.id)
                    )
              )
            group by h.nivel
            """)
    List<ConteoNivelRiesgo> contarUltimaEvaluacionPorNivelEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo);

    /**
     * HU14: obtiene las evaluaciones dentro del rango [desde, hastaExclusivo)
     * proyectadas con su solicitante en una sola consulta sin duplicados.
     */
    @Query("""
            select new com.udea.ScoreCrediticio.DTOs.Response.CarteraReporteFilaDTO(
                s.tipoDocumento,
                s.numeroDocumento,
                s.nombre,
                s.apellido,
                h.fechaCalculo,
                h.puntaje,
                h.nivel
            )
            from HistorialEvaluacion h
            join h.solicitante s
            where h.fechaCalculo >= :desde
              and h.fechaCalculo < :hastaExclusivo
            order by h.fechaCalculo desc, h.id desc
            """)
    List<CarteraReporteFilaDTO> buscarEvaluacionesEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo);
}
