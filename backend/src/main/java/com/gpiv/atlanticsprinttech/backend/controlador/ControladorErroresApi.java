package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaOperacion;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ControladorErroresApi {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaOperacion> manejarResponseStatus(ResponseStatusException ex) {
        HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus estadoRespuesta = estado == null ? HttpStatus.BAD_REQUEST : estado;
        return ResponseEntity.status(estadoRespuesta)
            .body(new RespuestaOperacion(ex.getReason() == null ? "No se pudo completar la operacion" : ex.getReason()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RespuestaOperacion> manejarEstadoInvalido(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new RespuestaOperacion(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaOperacion> manejarArgumentoInvalido(IllegalArgumentException ex) {
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
}

