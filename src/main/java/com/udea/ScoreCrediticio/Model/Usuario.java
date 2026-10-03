package com.udea.ScoreCrediticio.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {
    @GeneratedValue (strategy = GenerationType.SEQUENCE)
    @Id
    @Column (name = "id")
    private Long id;

    @Column (name = "email", nullable = false, unique = true)
    private String email;

    @Column (name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column (name = "tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    public Usuario() {
    }

    public Usuario(Long id, String email, String password, TipoUsuario tipoUsuario) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.tipoUsuario = tipoUsuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String toString() {
        return "Usuario(id=" + this.getId() + ", email=" + this.getEmail() + ", tipoUsuario=" + this.getTipoUsuario() + ")";
    }
    
}
