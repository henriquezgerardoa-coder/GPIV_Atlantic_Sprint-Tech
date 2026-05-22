package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRadicacionDocumento extends JpaRepository<RadicacionDocumento, Long> {

    List<RadicacionDocumento> findByRadicacionIdOrderByFechaSubidaDesc(Long radicacionId);

    Optional<RadicacionDocumento> findByIdAndRadicacionId(Long id, Long radicacionId);

    Optional<RadicacionDocumento> findTopByRadicacionIdAndTipoDocumentoOrderByFechaSubidaDesc(
        Long radicacionId, TipoDocumentoRadicacion tipoDocumento);

    @Query("SELECT COALESCE(SUM(d.tamanoBytes), 0) FROM RadicacionDocumento d WHERE d.radicacion.id = :radicacionId")
    long totalTamanoPorRadicacion(@Param("radicacionId") Long radicacionId);
}
