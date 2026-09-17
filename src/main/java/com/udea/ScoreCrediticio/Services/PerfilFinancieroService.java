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
import com.udea.ScoreCrediticio.Model.TipoDocumento;
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
    public PerfilFinancieroResponseDTO registrarPerfil(PerfilFinancieroRequestDTO dto) {
        Solicitante solicitante = buscarSolicitantePorDocumento(
                dto.getTipoDocumento(), dto.getNumeroDocumento());

        if (perfilFinancieroDAO.findBySolicitanteId(solicitante.getId()).isPresent()) {
            throw new DuplicateResourceException(
                    "El solicitante ya tiene un perfil financiero registrado");
        }

        PerfilFinanciero perfil = perfilFinancieroMapper.toEntity(dto, solicitante);
        PerfilFinanciero guardado = perfilFinancieroDAO.save(perfil);

        return perfilFinancieroMapper.toResponseDto(guardado);
    }

    @Transactional(readOnly = true)
    public PerfilFinancieroResponseDTO consultarPorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        Solicitante solicitante = buscarSolicitantePorDocumento(tipoDocumento, numeroDocumento);

        PerfilFinanciero perfil = perfilFinancieroDAO.findBySolicitanteId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El solicitante con documento " + tipoDocumento + " " + numeroDocumento
                                + " no tiene perfil financiero registrado"));

        return perfilFinancieroMapper.toResponseDto(perfil);
    }

    private Solicitante buscarSolicitantePorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        return solicitanteDAO.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitante con documento " + tipoDocumento + " " + numeroDocumento + " no encontrado"));
    }
}
