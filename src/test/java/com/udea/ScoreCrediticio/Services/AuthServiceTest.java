package com.udea.ScoreCrediticio.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.udea.ScoreCrediticio.DAOs.UsuarioDAO;
import com.udea.ScoreCrediticio.DTOs.Request.LoginRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.LoginResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.CredencialesInvalidasException;
import com.udea.ScoreCrediticio.Model.TipoUsuario;
import com.udea.ScoreCrediticio.Model.Usuario;
import com.udea.ScoreCrediticio.Security.JwtService;

class AuthServiceTest {

    private static final String MENSAJE_ESPERADO =
            "Usuario o contraseña incorrectos. Por favor intente nuevamente";

    private UsuarioDAO usuarioDAO;
    private JwtService jwtService;
    private AuthService authService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        usuarioDAO = mock(UsuarioDAO.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(usuarioDAO, passwordEncoder, jwtService);
    }

    @Test
    void credencialesValidasRetornanTokenConRol() {
        Usuario usuario = new Usuario(1L, "admin@score.local",
                passwordEncoder.encode("Admin123*"), TipoUsuario.ADMINISTRADOR);
        when(usuarioDAO.findByEmail("admin@score.local")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(usuario)).thenReturn("token-de-prueba");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponseDTO respuesta = authService.iniciarSesion(login("admin@score.local", "Admin123*"));

        assertEquals("token-de-prueba", respuesta.getToken());
        assertEquals("Bearer", respuesta.getTipoToken());
        assertEquals(3600000L, respuesta.getExpiraEn());
        assertEquals("admin@score.local", respuesta.getEmail());
        assertEquals(TipoUsuario.ADMINISTRADOR, respuesta.getRol());
    }

    @Test
    void emailNoRegistradoLanzaCredencialesInvalidas() {
        when(usuarioDAO.findByEmail("nadie@score.local")).thenReturn(Optional.empty());

        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> authService.iniciarSesion(login("nadie@score.local", "Admin123*")));

        assertEquals(MENSAJE_ESPERADO, ex.getMessage());
    }

    @Test
    void passwordIncorrectaLanzaCredencialesInvalidas() {
        Usuario usuario = new Usuario(1L, "admin@score.local",
                passwordEncoder.encode("Admin123*"), TipoUsuario.ADMINISTRADOR);
        when(usuarioDAO.findByEmail("admin@score.local")).thenReturn(Optional.of(usuario));

        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> authService.iniciarSesion(login("admin@score.local", "clave-mala")));

        assertEquals(MENSAJE_ESPERADO, ex.getMessage());
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }
}
