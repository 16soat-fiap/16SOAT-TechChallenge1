package com.autopecas.autopecas.domain.exception;

/**
 * Violação de uma regra de negócio. Traduzida para HTTP 422 pelo adapter web.
 *
 * <p>É lançada pelo próprio domínio — não existe rewrapping de
 * {@code IllegalStateException} na camada de aplicação.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
