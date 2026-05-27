package com.gpiv.atlanticsprinttech.backend.configuracion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {
    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, FiltroLimitacionIngreso filtroLimitacionIngreso) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .authorizeHttpRequests(autorizacion -> autorizacion
                .requestMatchers("/", "/index.html", "/ingreso.html", "/app.html", "/css/**", "/js/**",
                    "/img/**", "/registro.html", "/verificar.html", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/salud", "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/consulta").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/verificacion").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/verificacion/reenviar").permitAll()
                .requestMatchers("/api/yo").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/yo/vincular-empresa").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/yo/perfil").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/roles").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/rubros").authenticated()
                .requestMatchers("/api/usuarios/mi-clave").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/roles").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/usuarios/mi-empresa").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/usuarios/empresa").hasRole("EMPRESA")
                .requestMatchers("/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.GET,   "/api/cambios-rubro").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO", "EMPRESA")
                .requestMatchers(HttpMethod.POST,  "/api/cambios-rubro").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/cambios-rubro/*/resolver").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO")
                .requestMatchers("/api/empresas/*/censo/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO")
                .requestMatchers(HttpMethod.GET, "/api/empresas/disponibles").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/empresas/*/empleados/cantidad").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers("/api/empresas/*/empleados/**").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.GET, "/api/empresas/admin/vista/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO")
                .requestMatchers(HttpMethod.GET, "/api/empresas", "/api/empresas/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA", "SECRETARIO")
                .requestMatchers(HttpMethod.GET, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA", "SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.PUT, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.DELETE, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/empresas").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "EMPRESA")
                .requestMatchers(HttpMethod.PUT, "/api/empresas/*").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "EMPRESA")
                .requestMatchers(HttpMethod.DELETE, "/api/empresas/*").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.GET, "/api/empresas/*/servicios-post-radicacion").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO", "EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/empresas/*/servicios-post-radicacion").hasAnyRole("SECRETARIO", "EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/empresas/*/rubro-inicial").hasAnyRole("SECRETARIO", "EMPRESA")
                .requestMatchers("/api/empresas/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO", "EMPRESA")
                // R-14: informes/estadisticas solo para ADMINISTRADOR y DIRECTIVO (lectura legislativa).
                // El rol EMPRESA no accede a estadisticas globales.
                .requestMatchers("/api/estadisticas/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers(HttpMethod.GET, "/api/proyectos", "/api/proyectos/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.POST, "/api/proyectos").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/proyectos/*/hitos").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.PATCH, "/api/proyectos/*/hitos/*/cumplido").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.PUT, "/api/proyectos/*/hitos/*").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.DELETE, "/api/proyectos/*/hitos/*").hasAnyRole("ADMINISTRADOR", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.PATCH, "/api/proyectos/*/estado").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers("/api/proyectos/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "SECRETARIO", "TECNICO")
                .requestMatchers("/api/infraestructura/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers("/api/monitor/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers(HttpMethod.GET, "/api/audit-log/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "AUDITOR")
                .requestMatchers("/api/mensajeria/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA", "SECRETARIO", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/radicaciones/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA", "SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones/*/documentos").hasAnyRole("EMPRESA", "SECRETARIO")
                .requestMatchers(HttpMethod.PATCH, "/api/radicaciones/*/estado").hasRole("SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones/*/observaciones").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                .requestMatchers(HttpMethod.GET, "/api/radicaciones/*/rubrica").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA", "SECRETARIO")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones/*/rubrica").hasRole("SECRETARIO")
                .requestMatchers(HttpMethod.PATCH, "/api/radicaciones/*/lote").hasRole("SECRETARIO")
                .anyRequest().authenticated()
            )
            .addFilterBefore(filtroLimitacionIngreso, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    @Bean
    public UserDetailsService servicioDetallesUsuario(RepositorioUsuario repositorioUsuario) {
        return identificadorIngreso -> {
            String identificadorNormalizado = identificadorIngreso == null
                ? ""
                : identificadorIngreso.trim().toLowerCase(Locale.ROOT);

            Usuario usuario = repositorioUsuario.findByNombreUsuario(identificadorIngreso)
                .or(() -> repositorioUsuario.findByCorreoElectronicoIgnoreCase(identificadorNormalizado))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            if (!usuario.isActivo()) {
                throw new UsernameNotFoundException("Usuario inactivo");
            }
            if (usuario.getCorreoElectronico() != null && !usuario.isEmailVerificado()) {
                throw new UsernameNotFoundException("Debes verificar tu correo electronico antes de ingresar");
            }
            return crearUsuarioSeguridad(usuario);
        };
    }
    @Bean
    public PasswordEncoder codificadorClave() {
        return new BCryptPasswordEncoder();
    }
    private User crearUsuarioSeguridad(Usuario usuario) {
        String[] autoridades = usuario.getRoles().stream()
            .map(rol -> "ROLE_" + rol.name())
            .toArray(String[]::new);
        return (User) User.withUsername(usuario.getNombreUsuario())
            .password(usuario.getClaveAccesoHash())
            .authorities(autoridades)
            .build();
    }
}