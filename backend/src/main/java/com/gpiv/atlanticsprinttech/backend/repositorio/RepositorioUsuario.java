package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioAndActivoTrue(String nombreUsuario);
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuarioConEmpresa(@Param("nombreUsuario") String nombreUsuario);
    Optional<Usuario> findByCorreoElectronicoIgnoreCase(String correoElectronico);
    Optional<Usuario> findByTokenVerificacionEmail(String tokenVerificacionEmail);
    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.roles
        WHERE u.empresa.id = :empresaId
        ORDER BY u.id ASC
        """)
    List<Usuario> findByEmpresaIdConRolesOrderByIdAsc(@Param("empresaId") Long empresaId);

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.roles
        WHERE u.activo = true
          AND u.id IN (SELECT u2.id FROM Usuario u2 JOIN u2.roles r WHERE r IN :roles)
        ORDER BY u.nombreCompleto ASC
        """)
    List<Usuario> findActivosConRolesGestion(@Param("roles") Set<RolUsuario> roles);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles ORDER BY u.nombreCompleto ASC")
    List<Usuario> findAllConRolesOrderByNombreCompletoAsc();

    List<Usuario> findByActivoTrueOrderByNombreCompletoAsc();
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);
}