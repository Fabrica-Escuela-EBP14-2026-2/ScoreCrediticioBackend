package com.udea.ScoreCrediticio.Mapper;

import org.springframework.stereotype.Component;

import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Model.Solicitante;


@Component 
public class SolicitanteMapper {
    
    public Solicitante toEntity(SolicitanteRequestDTO dto) {
        Solicitante solicitante = new Solicitante();
        solicitante.setTipoDocumento(dto.getTipoDocumento());
        solicitante.setNumeroDocumento(dto.getNumeroDocumento());
        solicitante.setNombre(dto.getNombre());
        solicitante.setApellido(dto.getApellido());
        solicitante.setTelefono(dto.getTelefono());
        solicitante.setEmail(dto.getEmail());
        return solicitante;
    }

    public SolicitanteResponseDTO toResponseDto(Solicitante solicitante){
        SolicitanteResponseDTO dto = new SolicitanteResponseDTO();
        dto.setId(solicitante.getId());
        dto.setTipoDocumento(solicitante.getTipoDocumento());
        dto.setNumeroDocumento(solicitante.getNumeroDocumento());
        dto.setNombre(solicitante.getNombre());
        dto.setApellido(solicitante.getApellido());
        dto.setTelefono(solicitante.getTelefono());
        dto.setEmail(solicitante.getEmail());
        return dto;

    }

}
