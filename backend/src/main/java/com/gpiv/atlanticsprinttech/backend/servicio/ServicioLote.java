package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;

public interface ServicioLote {

    List<Lote> listar(String identificadorIngreso);

    Lote obtenerPorId(Long id, String identificadorIngreso);

    Lote crear(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId, String identificadorIngreso);

    Lote actualizar(Long id, String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId, String identificadorIngreso);

    void eliminar(Long id, String identificadorIngreso);
}

