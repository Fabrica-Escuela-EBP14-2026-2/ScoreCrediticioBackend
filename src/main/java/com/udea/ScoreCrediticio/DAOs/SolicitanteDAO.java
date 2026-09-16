package com.udea.ScoreCrediticio.DAOs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Model.Solicitante;

@Repository
public interface SolicitanteDAO extends JpaRepository<Solicitante, Long> {
    // Genera automáticamente los métodos CRUD para la entidad Solicitante

    Optional<Solicitante> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
