package com.gpiv.atlanticsprinttech.backend.seguridad;

import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
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

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_DIRECTIVO = "DIRECTIVO";
    private static final String ROL_EMPRESA = "EMPRESA";

    private static final String API_USUARIOS = "/api/usuarios/**";
    private static final String API_USUARIOS_MI_CLAVE = "/api/usuarios/mi-clave";
    private static final String API_USUARIOS_ROLES = "/api/usuarios/roles";
    private static final String API_EMPRESAS = "/api/empresas/**";
    private static final String API_EMPRESAS_ADMIN_VISTA = "/api/empresas/admin/vista/**";
    private static final String API_EMPRESAS_UNICA = "/api/empresas/*";
    private static final String API_EMPRESAS_SERVICIOS_POST_RADICACION = "/api/empresas/*/servicios-post-radicacion";
    private static final String API_LOTES = "/api/lotes/**";
    private static final String API_LOTES_UNICO = "/api/lotes/*";
    private static final String API_RADICACIONES = "/api/radicaciones/**";
    private static final String API_RADICACIONES_CREAR = "/api/radicaciones";
    private static final String API_RADICACIONES_DOCUMENTOS = "/api/radicaciones/*/documentos";
    private static final String API_RADICACIONES_ESTADO = "/api/radicaciones/*/estado";
    private static final String API_RADICACIONES_OBSERVACIONES = "/api/radicaciones/*/observaciones";

    // Solicitudes
    private static final String API_SOLICITUDES = "/api/solicitudes/**";
    private static final String API_SOLICITUDES_CREAR = "/api/solicitudes";
    private static final String API_SOLICITUDES_ESTADO = "/api/solicitudes/*/estado";

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, FiltroLimitacionIngreso filtroLimitacionIngreso) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(encabezados -> encabezados
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self'"))
            )
            .authorizeHttpRequests(autorizacion -> autorizacion
                // Publico (sin autenticacion).
                .requestMatchers("/", "/index.html", "/ingreso.html", "/app.html", "/css/**", "/js/**", "/img/**",
                    "/registro.html", "/verificar.html", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/salud", "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/verificacion").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/verificacion/reenviar").permitAll()

                // General autenticado.
                .requestMatchers("/api/yo").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/roles").authenticated()
                .requestMatchers(API_USUARIOS_MI_CLAVE).authenticated()

                // Usuarios.
                .requestMatchers(HttpMethod.GET, API_USUARIOS_ROLES).hasRole(ROL_ADMINISTRADOR)
                .requestMatchers(API_USUARIOS).hasRole(ROL_ADMINISTRADOR)

                // Empresas: DIRECTIVO solo lectura; EMPRESA lectura + operaciones habilitadas por negocio.
                .requestMatchers(HttpMethod.GET, API_EMPRESAS_ADMIN_VISTA).hasRole(ROL_ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, API_EMPRESAS).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO, ROL_EMPRESA)
                .requestMatchers(HttpMethod.POST, "/api/empresas").hasAnyRole(ROL_ADMINISTRADOR, ROL_EMPRESA)
                .requestMatchers(HttpMethod.PUT, API_EMPRESAS_UNICA).hasAnyRole(ROL_ADMINISTRADOR, ROL_EMPRESA)
                .requestMatchers(HttpMethod.PATCH, API_EMPRESAS_SERVICIOS_POST_RADICACION).hasAnyRole(ROL_ADMINISTRADOR, ROL_EMPRESA)
                .requestMatchers(HttpMethod.DELETE, API_EMPRESAS_UNICA).hasRole(ROL_ADMINISTRADOR)

                // Lotes: EMPRESA solo lectura.
                .requestMatchers(HttpMethod.GET, API_LOTES).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO, ROL_EMPRESA)
                .requestMatchers(HttpMethod.POST, "/api/lotes").hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)
                .requestMatchers(HttpMethod.PUT, API_LOTES_UNICO).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)
                .requestMatchers(HttpMethod.DELETE, API_LOTES_UNICO).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)

                // R-14: informes/estadisticas solo para ADMINISTRADOR y DIRECTIVO (lectura legislativa).
                // El rol EMPRESA no accede a estadisticas globales.
                .requestMatchers("/api/estadisticas/**").hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)

                // Radicaciones.
                .requestMatchers(HttpMethod.GET, API_RADICACIONES).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO, ROL_EMPRESA)
                .requestMatchers(HttpMethod.POST, API_RADICACIONES_CREAR).hasRole(ROL_EMPRESA)
                .requestMatchers(HttpMethod.POST, API_RADICACIONES_DOCUMENTOS).hasRole(ROL_EMPRESA)
                .requestMatchers(HttpMethod.PATCH, API_RADICACIONES_ESTADO).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)
                .requestMatchers(HttpMethod.POST, API_RADICACIONES_OBSERVACIONES).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)

                // Solicitudes: EMPRESA crea, ADMIN/DIRECTIVO consultan y cambian estado
                .requestMatchers(HttpMethod.GET, API_SOLICITUDES).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO, ROL_EMPRESA)
                .requestMatchers(HttpMethod.POST, API_SOLICITUDES_CREAR).hasRole(ROL_EMPRESA)
                .requestMatchers(HttpMethod.PUT, API_SOLICITUDES_ESTADO).hasAnyRole(ROL_ADMINISTRADOR, ROL_DIRECTIVO)

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

            Usuario usuario = repositorioUsuario.findByNombreUsuarioIgnoreCase(identificadorNormalizado)
                .or(() -> repositorioUsuario.findByCorreoElectronicoIgnoreCase(identificadorNormalizado))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            if (!usuario.estaActivo()) {
                throw new UsernameNotFoundException("Usuario inactivo");
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