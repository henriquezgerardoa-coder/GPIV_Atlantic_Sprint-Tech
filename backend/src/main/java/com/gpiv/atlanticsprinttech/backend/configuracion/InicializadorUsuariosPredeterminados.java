package com.gpiv.atlanticsprinttech.backend.configuracion;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
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
        if (repositorioUsuario.existsByNombreUsuario(nombreUsuario)) {
            return;
        }
        Usuario usuario = Usuario.crear(
            nombreUsuario,
            nombreCompleto,
            codificadorClave.encode(clavePlano),
            true,
            roles
        );
        repositorioUsuario.save(usuario);
    }
}