package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;

public interface ServicioLote {

    List<Lote> listar();

    Lote obtenerPorId(Long id);

    Lote crear(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId);

    Lote actualizar(Long id, String codigo, Double superficieMetrosCuadrados, boolean ocupado, Long empresaId);

    void eliminar(Long id);
}

