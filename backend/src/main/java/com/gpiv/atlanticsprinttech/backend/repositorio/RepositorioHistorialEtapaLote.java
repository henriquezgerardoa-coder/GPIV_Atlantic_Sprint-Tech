package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.HistorialEtapaLote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioHistorialEtapaLote extends JpaRepository<HistorialEtapaLote, Long> {

    @Query("SELECT h FROM HistorialEtapaLote h WHERE h.lote.id = :loteId ORDER BY h.fechaCambio ASC")
    List<HistorialEtapaLote> findByLoteIdOrdenado(@Param("loteId") Long loteId);
}
