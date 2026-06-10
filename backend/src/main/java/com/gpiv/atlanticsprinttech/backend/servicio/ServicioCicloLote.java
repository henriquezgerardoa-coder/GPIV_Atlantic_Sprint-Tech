package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaAlertaVencimiento;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaHistorialEtapa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaLote;
import java.util.List;

public interface ServicioCicloLote {

    RespuestaLote transicionar(Long loteId, String etapaDestino, String motivo, String identificadorIngreso);

    List<RespuestaHistorialEtapa> listarHistorial(Long loteId, String identificadorIngreso);

    List<RespuestaAlertaVencimiento> listarAlertasVencimiento();
}
