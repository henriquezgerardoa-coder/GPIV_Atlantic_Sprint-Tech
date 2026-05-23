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
    List<Usuario> findByEmpresa_IdOrderByIdAsc(Long empresaId);
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.roles r WHERE u.activo = true AND r IN :roles ORDER BY u.nombreCompleto ASC")
    List<Usuario> findActivosConRolesGestion(@Param("roles") Set<RolUsuario> roles);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);
}