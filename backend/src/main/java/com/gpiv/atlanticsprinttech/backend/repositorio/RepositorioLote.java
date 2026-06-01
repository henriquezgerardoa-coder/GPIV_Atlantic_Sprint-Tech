package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.EtapaCicloLote;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro WHERE l.id = :id")
    Optional<Lote> findByIdConEmpresa(@Param("id") Long id);

    @Cacheable("lotes")
    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro")
    List<Lote> findAllConEmpresa();

    @Cacheable(value = "lotes", key = "'principales'")
    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro WHERE l.parentLote IS NULL")
    List<Lote> findAllPrincipalesConEmpresa();

    @Cacheable(value = "lotes", key = "'disponibles'")
    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro WHERE l.ocupado = false")
    List<Lote> findAllDisponiblesConEmpresa();

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro WHERE e.id = :empresaId")
    List<Lote> findAllByEmpresaIdConEmpresa(@Param("empresaId") Long empresaId);

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro WHERE l.id = :id AND e.id = :empresaId")
    Optional<Lote> findByIdAndEmpresaIdConEmpresa(@Param("id") Long id, @Param("empresaId") Long empresaId);

    long countByParentLoteIsNull();

    long countByOcupado(boolean ocupado);

    long countByOcupadoAndParentLoteIsNull(boolean ocupado);

    long countByEtapa(EtapaCicloLote etapa);

    long countByEtapaAndParentLoteIsNull(EtapaCicloLote etapa);

    @Query("SELECT l.etapa, COUNT(l) FROM Lote l WHERE l.parentLote IS NULL GROUP BY l.etapa")
    List<Object[]> contarPorEtapa();

    @Query("SELECT l FROM Lote l LEFT JOIN FETCH l.empresa e LEFT JOIN FETCH e.rubro " +
           "WHERE l.fechaLimiteEtapaActual IS NOT NULL " +
           "AND l.fechaLimiteEtapaActual < CURRENT_DATE " +
           "AND l.etapa NOT IN ('DISPONIBLE', 'ESCRITURADO', 'REVERTIDO')")
    List<Lote> findConEtapaVencida();

    @Query("SELECT l FROM Lote l WHERE l.geom IS NOT NULL")
    List<Lote> findAllConGeometriaLoaded();

    List<Lote> findByParentLoteId(Long parentLoteId);

    Optional<Lote> findByExternalId(String externalId);

    long countByGeomIsNull();

    @Query("SELECT l FROM Lote l WHERE l.parentLote IS NULL")
    List<Lote> findAllLotesPrincipales();

    @Transactional
    @Modifying
    @Query(value = "UPDATE lotes SET geom = ST_GeomFromText(:wkt, 4326) WHERE id = :id", nativeQuery = true)
    void actualizarGeom(@Param("id") Long id, @Param("wkt") String wkt);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lotes SET properties = :json::jsonb WHERE id = :id", nativeQuery = true)
    void actualizarProperties(@Param("id") Long id, @Param("json") String json);
}
