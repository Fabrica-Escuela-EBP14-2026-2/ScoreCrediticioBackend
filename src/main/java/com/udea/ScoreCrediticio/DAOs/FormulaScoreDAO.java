package com.udea.ScoreCrediticio.DAOs;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.udea.ScoreCrediticio.Model.FormulaScore;

@Repository
public interface FormulaScoreDAO extends JpaRepository<FormulaScore, Long> {

    Optional<FormulaScore> findByVigenteTrue();

    @Modifying
    @Query("update FormulaScore f set f.vigente = false, f.claveVigencia = null where f.vigente = true")
    void desactivarFormulasVigentes();
}
