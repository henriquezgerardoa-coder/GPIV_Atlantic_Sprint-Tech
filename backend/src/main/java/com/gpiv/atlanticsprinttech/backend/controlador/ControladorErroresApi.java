package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ControladorErroresApi {

    private static final Logger log = LoggerFactory.getLogger(ControladorErroresApi.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaOperacion> manejarResponseStatus(ResponseStatusException ex) {
        HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus estadoRespuesta = estado == null ? HttpStatus.BAD_REQUEST : estado;
        return ResponseEntity.status(estadoRespuesta)
            .body(new RespuestaOperacion(ex.getReason() == null ? "No se pudo completar la operacion" : ex.getReason()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RespuestaOperacion> manejarEstadoInvalido(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new RespuestaOperacion(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaOperacion> manejarArgumentoInvalido(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(new RespuestaOperacion(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaOperacion> manejarErroresValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                }
                return error.getDefaultMessage();
            })
            .collect(Collectors.joining(". "));

        return ResponseEntity.badRequest().body(new RespuestaOperacion(mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaOperacion> manejarExcepcionGeneral(Exception ex) {
        log.error("Error no controlado en la API", ex);
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new RespuestaOperacion(mensaje));
    }
}

