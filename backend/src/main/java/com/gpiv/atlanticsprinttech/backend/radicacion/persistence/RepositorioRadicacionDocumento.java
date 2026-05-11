package com.gpiv.atlanticsprinttech.backend.radicacion.persistence;

import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionDocumento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRadicacionDocumento extends JpaRepository<RadicacionDocumento, Long> {

    List<RadicacionDocumento> findByRadicacionIdOrderByFechaSubidaDesc(Long radicacionId);

    @Query("SELECT COALESCE(SUM(d.tamanoBytes), 0) FROM RadicacionDocumento d WHERE d.radicacion.id = :radicacionId")
    long totalTamanoPorRadicacion(@Param("radicacionId") Long radicacionId);
}
