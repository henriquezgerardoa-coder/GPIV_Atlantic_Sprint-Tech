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
                .requestMatchers(HttpMethod.GET, "/api/public/verificacion").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/verificacion/reenviar").permitAll()
                .requestMatchers("/api/yo").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/roles").authenticated()
                .requestMatchers("/api/usuarios/mi-clave").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/roles").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/empresas/admin/vista/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/empresas/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA")
                .requestMatchers(HttpMethod.GET, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/empresas").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                .requestMatchers(HttpMethod.PUT, "/api/empresas/*").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                .requestMatchers(HttpMethod.DELETE, "/api/empresas/*").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/empresas/*/servicios-post-radicacion").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/empresas/*/servicios-post-radicacion").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                .requestMatchers("/api/empresas/**").hasAnyRole("ADMINISTRADOR", "EMPRESA")
                .requestMatchers("/api/lotes/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                // R-14: informes/estadisticas solo para ADMINISTRADOR y DIRECTIVO (lectura legislativa).
                // El rol EMPRESA no accede a estadisticas globales.
                .requestMatchers("/api/estadisticas/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers(HttpMethod.GET, "/api/audit-log/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers(HttpMethod.GET, "/api/radicaciones/**").hasAnyRole("ADMINISTRADOR", "DIRECTIVO", "EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones/*/documentos").hasRole("EMPRESA")
                .requestMatchers(HttpMethod.PATCH, "/api/radicaciones/*/estado").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
                .requestMatchers(HttpMethod.POST, "/api/radicaciones/*/observaciones").hasAnyRole("ADMINISTRADOR", "DIRECTIVO")
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