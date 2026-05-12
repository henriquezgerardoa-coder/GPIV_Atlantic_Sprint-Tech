package com.gpiv.atlanticsprinttech.backend.lote.application;

import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioLote {

    Page<Lote> listar(String identificadorIngreso, Pageable pageable);

    Lote obtenerPorId(Long id, String identificadorIngreso);

    Lote crear(
        String codigo,
        Double superficieMetrosCuadrados,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String zona,
        String identificadorIngreso
    );

    Lote actualizar(
        Long id,
        String codigo,
        Double superficieMetrosCuadrados,
        Long empresaId,
        String estadoAsignacion,
        String numeroExpedienteReferencia,
        String zona,
        String identificadorIngreso
    );

    void eliminar(Long id, String identificadorIngreso);
}
