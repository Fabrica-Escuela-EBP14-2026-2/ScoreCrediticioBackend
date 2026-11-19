package com.udea.ScoreCrediticio.DAOs;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.udea.ScoreCrediticio.Model.BitacoraAuditoria;

/**
 * Repositorio de solo lectura: al extender Repository (y no JpaRepository)
 * no se exponen operaciones de escritura sobre la bitacora.
 */
public interface BitacoraAuditoriaDAO extends Repository<BitacoraAuditoria, Long> {

    @Query("select b from BitacoraAuditoria b order by b.fechaUtc desc, b.id desc")
    List<BitacoraAuditoria> listarCronologicoDescendente();
}
