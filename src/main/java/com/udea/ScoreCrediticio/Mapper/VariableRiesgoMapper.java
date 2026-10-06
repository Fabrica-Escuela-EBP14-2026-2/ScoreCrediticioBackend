package com.udea.ScoreCrediticio.Mapper;

import org.springframework.stereotype.Component;

import com.udea.ScoreCrediticio.DTOs.Request.VariableRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.VariableRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@Component
public class VariableRiesgoMapper {

    public VariableRiesgo toEntity(VariableRiesgoRequestDTO dto) {
        VariableRiesgo variable = new VariableRiesgo();
        variable.setNombre(dto.getNombre().trim());
        variable.setTipoDato(dto.getTipoDato());
        variable.setPeso(dto.getPeso());
        return variable;
    }

    public VariableRiesgoResponseDTO toResponseDto(VariableRiesgo variable) {
        VariableRiesgoResponseDTO dto = new VariableRiesgoResponseDTO();
        dto.setId(variable.getId());
        dto.setNombre(variable.getNombre());
        dto.setTipoDato(variable.getTipoDato());
        dto.setPeso(variable.getPeso());
        dto.setEstado(variable.getEstado());
        dto.setFechaRegistro(variable.getFechaRegistro());
        return dto;
    }
}
