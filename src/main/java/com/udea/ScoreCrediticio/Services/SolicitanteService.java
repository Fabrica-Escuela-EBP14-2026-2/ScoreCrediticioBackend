package com.udea.ScoreCrediticio.Services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.DuplicateResourceException;
import com.udea.ScoreCrediticio.Mapper.SolicitanteMapper;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.TipoDocumento;

@Service
public class SolicitanteService {

    private static final String MENSAJE_DUPLICADO =
            "El solicitante con este tipo y número de documento ya se encuentra registrado en la plataforma";

    private final SolicitanteDAO solicitanteDAO;
    private final SolicitanteMapper solicitanteMapper;

    public SolicitanteService(SolicitanteDAO solicitanteDAO, SolicitanteMapper solicitanteMapper) {
        this.solicitanteDAO = solicitanteDAO;
        this.solicitanteMapper = solicitanteMapper;
    }

    @Transactional
    public SolicitanteResponseDTO registrarSolicitante(SolicitanteRequestDTO solicitanteDTO) {

        if (existeDuplicado(solicitanteDTO.getTipoDocumento(), solicitanteDTO.getNumeroDocumento())) {
            throw new DuplicateResourceException(MENSAJE_DUPLICADO, "numeroDocumento");
        }

        Solicitante solicitante = solicitanteMapper.toEntity(solicitanteDTO);
        try {
            // saveAndFlush fuerza el INSERT para capturar aqui la violacion de la
            // restriccion unica si dos peticiones concurrentes pasaron la consulta.
            Solicitante solicitanteGuardado = solicitanteDAO.saveAndFlush(solicitante);
            return solicitanteMapper.toResponseDto(solicitanteGuardado);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(MENSAJE_DUPLICADO, "numeroDocumento");
        }
    }

    @Transactional(readOnly = true)
    public boolean existeDuplicado(TipoDocumento tipoDocumento, String numeroDocumento) {
        return solicitanteDAO
                .findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .isPresent();
    }
}
