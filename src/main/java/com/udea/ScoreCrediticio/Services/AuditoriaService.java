package com.udea.ScoreCrediticio.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;
import com.udea.ScoreCrediticio.DAOs.BitacoraAuditoriaDAO;
import com.udea.ScoreCrediticio.DTOs.Response.AuditoriaResponseDTO;
import com.udea.ScoreCrediticio.Model.BitacoraAuditoria;

@Service
public class AuditoriaService {

    private final BitacoraAuditoriaDAO bitacoraAuditoriaDAO;
    private final ObjectMapper objectMapper;

    public AuditoriaService(BitacoraAuditoriaDAO bitacoraAuditoriaDAO, ObjectMapper objectMapper) {
        this.bitacoraAuditoriaDAO = bitacoraAuditoriaDAO;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponseDTO> listar() {
        return bitacoraAuditoriaDAO.listarCronologicoDescendente()
                .stream()
                .map(this::mapear)
                .toList();
    }

    private AuditoriaResponseDTO mapear(BitacoraAuditoria registro) {
        AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
        dto.setId(registro.getId());
        dto.setUsuario(registro.getUsuario());
        dto.setFechaUtc(registro.getFechaUtc());
        dto.setModulo(registro.getModulo());
        dto.setAccion(registro.getAccion());
        dto.setValorAnterior(parsear(registro.getValorAnterior()));
        dto.setValorNuevo(parsear(registro.getValorNuevo()));
        return dto;
    }

    private JsonNode parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(valor);
        } catch (JacksonException ex) {
            // Un registro legado o corrupto no debe impedir que se cargue la bitacora.
            return StringNode.valueOf(valor);
        }
    }
}
