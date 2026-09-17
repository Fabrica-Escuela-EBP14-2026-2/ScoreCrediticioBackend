package com.udea.ScoreCrediticio.Mapper;

import org.springframework.stereotype.Component;

import com.udea.ScoreCrediticio.DTOs.Request.PerfilFinancieroRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.PerfilFinancieroResponseDTO;
import com.udea.ScoreCrediticio.Model.PerfilFinanciero;
import com.udea.ScoreCrediticio.Model.Solicitante;

@Component
public class PerfilFinancieroMapper {

    public PerfilFinanciero toEntity(PerfilFinancieroRequestDTO dto, Solicitante solicitante) {
        PerfilFinanciero perfil = new PerfilFinanciero();
        perfil.setIngresos(dto.getIngresos());
        perfil.setEgresos(dto.getEgresos());
        perfil.setSolicitante(solicitante);
        return perfil;
    }

    public PerfilFinancieroResponseDTO toResponseDto(PerfilFinanciero perfil) {
        PerfilFinancieroResponseDTO dto = new PerfilFinancieroResponseDTO();
        dto.setId(perfil.getId());
        dto.setSolicitanteId(perfil.getSolicitante().getId());
        dto.setTipoDocumento(perfil.getSolicitante().getTipoDocumento());
        dto.setNumeroDocumento(perfil.getSolicitante().getNumeroDocumento());
        dto.setIngresos(perfil.getIngresos());
        dto.setEgresos(perfil.getEgresos());
        dto.setIngresoNetoDisponible(perfil.getIngresoNetoDisponible());
        return dto;
    }
}
