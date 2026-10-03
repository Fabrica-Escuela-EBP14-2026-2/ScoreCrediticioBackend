package com.udea.ScoreCrediticio.Services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.udea.ScoreCrediticio.DAOs.UsuarioDAO;
import com.udea.ScoreCrediticio.DTOs.Request.LoginRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.LoginResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.CredencialesInvalidasException;
import com.udea.ScoreCrediticio.Model.Usuario;
import com.udea.ScoreCrediticio.Security.JwtService;

@Service
public class AuthService {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS =
            "Usuario o contraseña incorrectos. Por favor intente nuevamente";

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO iniciarSesion(LoginRequestDTO dto) {
        Usuario usuario = usuarioDAO.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
        }

        LoginResponseDTO respuesta = new LoginResponseDTO();
        respuesta.setToken(jwtService.generarToken(usuario));
        respuesta.setTipoToken("Bearer");
        respuesta.setExpiraEn(jwtService.getExpirationMs());
        respuesta.setEmail(usuario.getEmail());
        respuesta.setRol(usuario.getTipoUsuario());
        return respuesta;
    }
}
