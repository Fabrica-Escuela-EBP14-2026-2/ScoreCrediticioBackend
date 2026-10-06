package com.udea.ScoreCrediticio.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.DTOs.Request.VariableRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.VariableRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.DuplicateResourceException;
import com.udea.ScoreCrediticio.Exceptions.PesoTotalExcedeLimiteException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Mapper.VariableRiesgoMapper;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@Service
public class VariableRiesgoService {

    private static final BigDecimal PESO_TOTAL_MAXIMO = new BigDecimal("100");
    private static final int ESCALA_PESO = 4;

    private final VariableRiesgoDAO variableRiesgoDAO;
    private final VariableRiesgoMapper variableRiesgoMapper;

    public VariableRiesgoService(VariableRiesgoDAO variableRiesgoDAO, VariableRiesgoMapper variableRiesgoMapper) {
        this.variableRiesgoDAO = variableRiesgoDAO;
        this.variableRiesgoMapper = variableRiesgoMapper;
    }

    // SERIALIZABLE evita que dos altas concurrentes validen contra el mismo total
    // y terminen dejando la suma de pesos por encima del 100%.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VariableRiesgoResponseDTO registrarVariable(VariableRiesgoRequestDTO dto) {
        String nombre = dto.getNombre().trim();

        if (variableRiesgoDAO.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe una variable registrada con este nombre");
        }

        BigDecimal peso = dto.getPeso().setScale(ESCALA_PESO, RoundingMode.UNNECESSARY);
        BigDecimal pesoTotalActual = pesoTotalActivas();
        BigDecimal pesoTotalResultante = pesoTotalActual.add(peso).setScale(ESCALA_PESO, RoundingMode.HALF_UP);

        if (pesoTotalResultante.compareTo(PESO_TOTAL_MAXIMO) > 0) {
            throw new PesoTotalExcedeLimiteException(
                    "La suma de los pesos de las variables activas no puede superar el 100%. "
                            + "Ajuste el peso indicado para poder guardar la variable",
                    formato(pesoTotalActual),
                    formato(peso),
                    formato(pesoTotalResultante));
        }

        VariableRiesgo variable = variableRiesgoMapper.toEntity(dto);
        variable.setNombre(nombre);
        variable.setPeso(peso);
        // Toda variable registrada queda activa desde el momento de su creacion.
        variable.setEstado(EstadoVariable.ACTIVA);

        // saveAndFlush y no save: @CreationTimestamp se asigna en el INSERT, y sin flush
        // la respuesta devolveria fechaRegistro en null.
        VariableRiesgo guardada = variableRiesgoDAO.saveAndFlush(variable);

        return variableRiesgoMapper.toResponseDto(guardada);
    }

    // Desactivar una variable no borra nada: la fila sigue en el catalogo, solo deja de
    // contar para la formula del score. Por eso findById basta, sin borrado logico extra.
    // SERIALIZABLE por la misma razon que el alta: al reactivar se relee el total de
    // activos y dos reactivaciones concurrentes no deben dejar la suma por encima del 100%.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VariableRiesgoResponseDTO cambiarEstado(Long id, EstadoVariable nuevoEstado) {
        VariableRiesgo variable = variableRiesgoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variable de riesgo con id " + id + " no encontrada"));

        // Idempotente: repetir la misma peticion no vuelve a escribir la fila.
        if (variable.getEstado() != nuevoEstado) {
            // Reactivar devuelve el peso de la variable al presupuesto del 100%,
            // asi que hay que revalidarlo igual que en el alta.
            if (nuevoEstado == EstadoVariable.ACTIVA) {
                validarPresupuestoAlReactivar(variable);
            }

            variable.setEstado(nuevoEstado);
            variableRiesgoDAO.saveAndFlush(variable);
        }

        return variableRiesgoMapper.toResponseDto(variable);
    }

    @Transactional(readOnly = true)
    public List<VariableRiesgoResponseDTO> listarVariables() {
        return variableRiesgoDAO.findAllByOrderByNombreAsc()
                .stream()
                .map(variableRiesgoMapper::toResponseDto)
                .toList();
    }

    private BigDecimal pesoTotalActivas() {
        BigDecimal total = variableRiesgoDAO.sumarPesosPorEstado(EstadoVariable.ACTIVA);
        return total == null ? BigDecimal.ZERO.setScale(ESCALA_PESO) : total;
    }

    private void validarPresupuestoAlReactivar(VariableRiesgo variable) {
        BigDecimal pesoTotalActual = pesoTotalActivas();
        BigDecimal pesoTotalResultante = pesoTotalActual.add(variable.getPeso())
                .setScale(ESCALA_PESO, RoundingMode.HALF_UP);

        if (pesoTotalResultante.compareTo(PESO_TOTAL_MAXIMO) > 0) {
            throw new PesoTotalExcedeLimiteException(
                    "La suma de los pesos de las variables activas no puede superar el 100%. "
                            + "Ajuste el peso indicado para poder guardar la variable",
                    formato(pesoTotalActual),
                    formato(variable.getPeso()),
                    formato(pesoTotalResultante));
        }
    }

    private String formato(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString() + "%";
    }
}
