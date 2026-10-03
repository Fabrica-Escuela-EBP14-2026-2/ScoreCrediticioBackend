package com.udea.ScoreCrediticio.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.udea.ScoreCrediticio.DAOs.UsuarioDAO;
import com.udea.ScoreCrediticio.Model.TipoUsuario;
import com.udea.ScoreCrediticio.Model.Usuario;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email}")
    private String adminEmail;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Value("${seed.analista.email}")
    private String analistaEmail;

    @Value("${seed.analista.password}")
    private String analistaPassword;

    public DataInitializer(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder) {
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste(adminEmail, adminPassword, TipoUsuario.ADMINISTRADOR);
        crearSiNoExiste(analistaEmail, analistaPassword, TipoUsuario.ANALISTA);
    }

    private void crearSiNoExiste(String email, String password, TipoUsuario tipoUsuario) {
        if (usuarioDAO.findByEmail(email).isPresent()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setTipoUsuario(tipoUsuario);
        usuarioDAO.save(usuario);
    }
}
