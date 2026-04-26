package com.gpiv.atlanticsprinttech.backend.configuracion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
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

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {
    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(autorizacion -> autorizacion
                .requestMatchers("/", "/index.html", "/app.html", "/css/**", "/js/**",
                    "/favicon.ico", "/error").permitAll()
                .requestMatchers("/salud", "/health").permitAll()
                .requestMatchers("/api/yo").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/roles").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/roles").authenticated()
                .requestMatchers("/api/usuarios/mi-clave").authenticated()
                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/empresas/**").hasAnyRole("ADMINISTRADOR", "OPERADOR", "VISOR")
                .requestMatchers(HttpMethod.GET, "/api/lotes/**").hasAnyRole("ADMINISTRADOR", "OPERADOR", "VISOR")
                .requestMatchers("/api/empresas/**").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .requestMatchers("/api/lotes/**").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    @Bean
    public UserDetailsService servicioDetallesUsuario(RepositorioUsuario repositorioUsuario) {
        return nombreUsuario -> repositorioUsuario.findByNombreUsuarioAndActivoTrue(nombreUsuario)
            .map(this::crearUsuarioSeguridad)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
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