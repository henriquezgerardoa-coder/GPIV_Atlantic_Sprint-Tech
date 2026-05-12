package com.gpiv.atlanticsprinttech.backend.config;
import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.usuario.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.util.Locale;
import java.util.EnumSet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InicializadorUsuariosPredeterminados implements CommandLineRunner {
    private final RepositorioUsuario repositorioUsuario;
    private final PasswordEncoder codificadorClave;
    private final PropiedadesApiUsuarios propiedadesApiUsuarios;

    public InicializadorUsuariosPredeterminados(
        RepositorioUsuario repositorioUsuario,
        PasswordEncoder codificadorClave,
        PropiedadesApiUsuarios propiedadesApiUsuarios
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.codificadorClave = codificadorClave;
        this.propiedadesApiUsuarios = propiedadesApiUsuarios;
    }
    @Override
    public void run(String... args) {
        crearSiNoExiste(
            "admin",
            "Administrador General",
            propiedadesApiUsuarios.getSemilla().getAdmin().getClave(),
            EnumSet.of(RolUsuario.ADMINISTRADOR)
        );
        crearSiNoExiste(
            "directivo",
            "Directivo GPIV",
            propiedadesApiUsuarios.getSemilla().getDirectivo().getClave(),
            EnumSet.of(RolUsuario.DIRECTIVO)
        );
        crearSiNoExiste(
            "empresa",
            "Empresa GPIV",
            propiedadesApiUsuarios.getSemilla().getEmpresa().getClave(),
            EnumSet.of(RolUsuario.EMPRESA)
        );
    }
    private void crearSiNoExiste(String nombreUsuario, String nombreCompleto, String clavePlano, EnumSet<RolUsuario> roles) {
        String nombreUsuarioNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (repositorioUsuario.existsByNombreUsuarioIgnoreCase(nombreUsuarioNormalizado)) {
            return;
        }
        Usuario usuario = Usuario.crear(
            nombreUsuarioNormalizado,
            nombreCompleto,
            codificadorClave.encode(clavePlano),
            true,
            roles
        );
        repositorioUsuario.save(usuario);
    }
}