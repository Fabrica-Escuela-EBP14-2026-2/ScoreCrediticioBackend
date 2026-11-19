package com.udea.ScoreCrediticio.Exceptions;

public class DuplicateResourceException extends RuntimeException {

    private final String campo;

    public DuplicateResourceException(String message) {
        this(message, null);
    }

    public DuplicateResourceException(String message, String campo) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
