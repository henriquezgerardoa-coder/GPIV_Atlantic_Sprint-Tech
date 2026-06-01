package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RepositorioLote extends JpaRepository<Lote, Long> {

    boolean existsByEmpresa_IdAndCodigo(Long empresaId, String codigo);

    boolean existsByEmpresaIsNullAndCodigo(String codigo);

    Optional<Lote> findByEmpresa_IdAndCodigo(Long empresaId, String codigo);

    boolean existsByEmpresa_Id(Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id")
    Optional<Lote> findByIdConEmpresa(@Param("id") Long id);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa")
    List<Lote> findAllConEmpresa();

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.empresa.id = :empresaId")
    List<Lote> findAllByEmpresaIdConEmpresa(@Param("empresaId") Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa WHERE l.id = :id AND l.empresa.id = :empresaId")
    Optional<Lote> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    /** Cuenta lotes ocupados — para estadisticas (R-14). */
    long countByOcupado(boolean ocupado);

    /** Cuenta lotes por estado de asignación — para estadisticas (R-14). */
    long countByEstadoAsignacion(EstadoAsignacionLote estado);

    /** Encuentra lotes con geometría. */
    @Query("SELECT l FROM Lote l WHERE l.geom IS NOT NULL")
    List<Lote> findAllConGeometriaLoaded();

    /** Encuentra sub-lotes de un lote padre. */
    List<Lote> findByParentLoteId(Long parentLoteId);

    /** Encuentra lote por external_id (usado en sincronización con GeoJSON). */
    Optional<Lote> findByExternalId(String externalId);

    /** Cuenta lotes sin geometría (útil para identificar dónde faltan datos). */
    long countByGeomIsNull();

    /** Encuentra lotes sin padre (lotes principales, no subdivisiones). */
    @Query("SELECT l FROM Lote l WHERE l.parentLote IS NULL")
    List<Lote> findAllLotesPrincipales();

    /** Actualiza la geometría de un lote usando WKT y SRID 4326. Necesario porque JPA no puede bindear geometry. */
    @Transactional
    @Modifying
    @Query(value = "UPDATE lotes SET geom = ST_GeomFromText(:wkt, 4326) WHERE id = :id", nativeQuery = true)
    void actualizarGeom(@Param("id") Long id, @Param("wkt") String wkt);

    /** Actualiza las properties JSON de un lote. Necesario porque JPA no puede bindear jsonb como varchar. */
    @Transactional
    @Modifying
    @Query(value = "UPDATE lotes SET properties = :json::jsonb WHERE id = :id", nativeQuery = true)
    void actualizarProperties(@Param("id") Long id, @Param("json") String json);
}
