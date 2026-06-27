package com.autopecas.autopecas.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.valueOf(422))
                .body(new ErrorResponse(422, "Estoque insuficiente", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.valueOf(422))
                .body(new ErrorResponse(422, "Regra de negócio violada", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erros.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(HttpStatus.BAD_REQUEST.value(), "Dados inválidos", erros));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }

    public record ErrorResponse(int status, String error, String message) {
        public LocalDateTime timestamp() { return LocalDateTime.now(); }
    }

    public record ValidationErrorResponse(int status, String error, Map<String, String> campos) {
        public LocalDateTime timestamp() { return LocalDateTime.now(); }
    }
}
