package com.autopecas.autopecas.domain.exception;

/** Recurso inexistente. Traduzida para HTTP 404 pelo adapter web. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
