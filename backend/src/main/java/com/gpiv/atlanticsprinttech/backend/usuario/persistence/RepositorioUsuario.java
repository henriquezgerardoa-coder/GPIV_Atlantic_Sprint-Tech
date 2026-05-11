package com.gpiv.atlanticsprinttech.backend.usuario.persistence;

import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioAndActivoTrue(String nombreUsuario);
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa WHERE lower(u.nombreUsuario) = lower(:nombreUsuario)")
    Optional<Usuario> findByNombreUsuarioConEmpresa(@Param("nombreUsuario") String nombreUsuario);
    Optional<Usuario> findByCorreoElectronicoIgnoreCase(String correoElectronico);
    Optional<Usuario> findByTokenVerificacionEmail(String tokenVerificacionEmail);
    List<Usuario> findByEmpresa_IdOrderByIdAsc(Long empresaId);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);
    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);
}