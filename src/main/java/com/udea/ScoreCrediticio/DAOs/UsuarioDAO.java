package com.udea.ScoreCrediticio.DAOs;

import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.udea.ScoreCrediticio.Model.Usuario;

@Repository 
public interface UsuarioDAO extends JpaRepository<Usuario, Long> {
    // Genera automáticamente los métodos CRUD para la entidad Usuario

    Optional<Usuario> findByEmail(String email);

}
