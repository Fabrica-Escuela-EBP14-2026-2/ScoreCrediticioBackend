package com.udea.ScoreCrediticio.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.Exceptions.DuplicateResourceException;
import com.udea.ScoreCrediticio.Mapper.SolicitanteMapper;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.TipoDocumento;

class SolicitanteServiceTest {

    private static final String MENSAJE_DUPLICADO =
            "El solicitante con este tipo y número de documento ya se encuentra registrado en la plataforma";

    private SolicitanteDAO solicitanteDAO;
    private SolicitanteMapper solicitanteMapper;
    private SolicitanteService solicitanteService;

    @BeforeEach
    void setUp() {
        solicitanteDAO = mock(SolicitanteDAO.class);
        solicitanteMapper = mock(SolicitanteMapper.class);
        solicitanteService = new SolicitanteService(solicitanteDAO, solicitanteMapper);
    }

    @Test
    void duplicadoDetectadoEnLaConsultaDevuelveElCampoDelDocumento() {
        when(solicitanteDAO.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, "12345678"))
                .thenReturn(Optional.of(new Solicitante()));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> solicitanteService.registrarSolicitante(dto()));

        assertEquals(MENSAJE_DUPLICADO, ex.getMessage());
        assertEquals("numeroDocumento", ex.getCampo());
    }

    @Test
    void violacionDeLaRestriccionUnicaEnConcurrenciaSeMapeaADuplicado() {
        when(solicitanteDAO.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, "12345678"))
                .thenReturn(Optional.empty());
        when(solicitanteMapper.toEntity(any(SolicitanteRequestDTO.class)))
                .thenReturn(new Solicitante());
        when(solicitanteDAO.saveAndFlush(any(Solicitante.class)))
                .thenThrow(new DataIntegrityViolationException("restriccion unica"));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> solicitanteService.registrarSolicitante(dto()));

        assertEquals(MENSAJE_DUPLICADO, ex.getMessage());
        assertEquals("numeroDocumento", ex.getCampo());
    }

    private SolicitanteRequestDTO dto() {
        SolicitanteRequestDTO dto = new SolicitanteRequestDTO();
        dto.setTipoDocumento(TipoDocumento.CC);
        dto.setNumeroDocumento("12345678");
        dto.setNombre("Ana");
        dto.setApellido("Pérez");
        dto.setTelefono("3001234567");
        dto.setEmail("ana@test.com");
        return dto;
    }
}
