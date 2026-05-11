package com.gpiv.atlanticsprinttech.backend.registro.application;

public interface ServicioCorreoVerificacion {
    void enviarCorreoVerificacion(String correoDestino, String enlaceVerificacion, long expiracionHoras, String contactoSoporte);
}

