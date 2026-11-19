package com.udea.ScoreCrediticio.Mapper;

import org.springframework.stereotype.Component;

import com.udea.ScoreCrediticio.DTOs.Response.DetalleEvaluacionResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.HistorialEvaluacionResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.VariableEvaluadaResponseDTO;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.VariableEvaluada;

@Component
public class HistorialEvaluacionMapper {

    public HistorialEvaluacionResponseDTO toResponseDto(HistorialEvaluacion historial) {
        HistorialEvaluacionResponseDTO dto = new HistorialEvaluacionResponseDTO();
        dto.setId(historial.getId());
        dto.setSolicitanteId(historial.getSolicitante().getId());
        dto.setPuntaje(historial.getPuntaje());
        dto.setNivel(historial.getNivel());
        dto.setFechaCalculo(historial.getFechaCalculo());
        dto.setEvaluadorEmail(historial.getEvaluadorEmail());
        dto.setDetalle(toDetalleDto(historial));
        return dto;
    }

    private DetalleEvaluacionResponseDTO toDetalleDto(HistorialEvaluacion historial) {
        DetalleEvaluacionResponseDTO detalle = new DetalleEvaluacionResponseDTO();
        detalle.setIngresos(historial.getIngresos());
        detalle.setEgresos(historial.getEgresos());

        if (historial.getIngresos() != null && historial.getEgresos() != null) {
            detalle.setIngresoNetoDisponible(historial.getIngresos().subtract(historial.getEgresos()));
        }

        detalle.setVariables(historial.getVariables().stream()
                .map(this::toVariableDto)
                .toList());
        return detalle;
    }

    private VariableEvaluadaResponseDTO toVariableDto(VariableEvaluada variable) {
        VariableEvaluadaResponseDTO dto = new VariableEvaluadaResponseDTO();
        dto.setVariableId(variable.getVariableId());
        dto.setNombre(variable.getNombre());
        dto.setValor(variable.getValor());
        dto.setPeso(variable.getPeso());
        return dto;
    }
}
