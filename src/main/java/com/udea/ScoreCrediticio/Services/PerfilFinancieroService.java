package com.udea.ScoreCrediticio.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.PerfilFinancieroDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.PerfilFinancieroRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.PerfilFinancieroResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.DuplicateResourceException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Mapper.PerfilFinancieroMapper;
import com.udea.ScoreCrediticio.Model.PerfilFinanciero;
import com.udea.ScoreCrediticio.Model.Solicitante;

@Service
public class PerfilFinancieroService {

    private final PerfilFinancieroDAO perfilFinancieroDAO;
    private final SolicitanteDAO solicitanteDAO;
    private final PerfilFinancieroMapper perfilFinancieroMapper;

    public PerfilFinancieroService(PerfilFinancieroDAO perfilFinancieroDAO, SolicitanteDAO solicitanteDAO,
            PerfilFinancieroMapper perfilFinancieroMapper) {
        this.perfilFinancieroDAO = perfilFinancieroDAO;
        this.solicitanteDAO = solicitanteDAO;
        this.perfilFinancieroMapper = perfilFinancieroMapper;
    }

    @Transactional
    public PerfilFinancieroResponseDTO registrarPerfil(Long solicitanteId, PerfilFinancieroRequestDTO dto) {
        Solicitante solicitante = solicitanteDAO.findById(solicitanteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitante con id " + solicitanteId + " no encontrado"));

        if (perfilFinancieroDAO.findBySolicitanteId(solicitanteId).isPresent()) {
            throw new DuplicateResourceException(
                    "El solicitante ya tiene un perfil financiero registrado");
        }

        PerfilFinanciero perfil = perfilFinancieroMapper.toEntity(dto, solicitante);
        PerfilFinanciero guardado = perfilFinancieroDAO.save(perfil);

        return perfilFinancieroMapper.toResponseDto(guardado);
    }
}
