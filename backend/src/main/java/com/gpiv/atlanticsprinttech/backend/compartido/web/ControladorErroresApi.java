package com.gpiv.atlanticsprinttech.backend.compartido.web;

import com.gpiv.atlanticsprinttech.commons.compartido.dto.RespuestaOperacion;
import com.gpiv.atlanticsprinttech.entities.compartido.ExcepcionNegocio;
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
    @SuppressWarnings("unused")
    public ResponseEntity<RespuestaOperacion> manejarResponseStatus(ResponseStatusException ex) {
        HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus estadoRespuesta = estado == null ? HttpStatus.BAD_REQUEST : estado;
        return ResponseEntity.status(estadoRespuesta)
            .body(new RespuestaOperacion(ex.getReason() == null ? "No se pudo completar la operacion" : ex.getReason()));
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    @SuppressWarnings("unused")
    public ResponseEntity<RespuestaOperacion> manejarExcepcionNegocio(ExcepcionNegocio ex) {
        return ResponseEntity.badRequest().body(new RespuestaOperacion(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("unused")
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
