package com.gpiv.atlanticsprinttech.backend.servicio;

public interface ServicioCorreoVerificacion {
    void enviarCorreoVerificacion(String correoDestino, String enlaceVerificacion, long expiracionHoras, String contactoSoporte);
}

