package com.udea.ScoreCrediticio.Services;

import org.springframework.stereotype.Service;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Mapper.SolicitanteMapper;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Exceptions.DuplicateResourceException;

@Service
public class SolicitanteService{
    private final SolicitanteDAO solicitanteDAO;
    private final SolicitanteMapper solicitanteMapper;

    public SolicitanteService(SolicitanteDAO solicitanteDAO, SolicitanteMapper solicitanteMapper){
        this.solicitanteDAO = solicitanteDAO;
        this.solicitanteMapper = solicitanteMapper;
    }
    
    public SolicitanteResponseDTO registrarSolicitante(SolicitanteRequestDTO solicitanteDTO){
        
        if(existeDuplicado(solicitanteDTO.getTipoDocumento(), solicitanteDTO.getNumeroDocumento())){
            throw new DuplicateResourceException(
                "El solicitante con este tipo y número de documento ya se encuentra registrado en la plataforma"
            );
        }

        Solicitante solicitante = solicitanteMapper.toEntity(solicitanteDTO);
        Solicitante solicitanteGuardado = solicitanteDAO.save(solicitante);

        return solicitanteMapper.toResponseDto(solicitanteGuardado);

    }

    public boolean existeDuplicado(TipoDocumento tipoDocumento, String numeroDocumento) {
        return solicitanteDAO
                .findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .isPresent();
    }
}
