package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioServicio;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioServicioEvento;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioInfraestructura;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoServicio;
import com.gpiv.atlanticsprinttech.entities.dominio.Servicio;
import com.gpiv.atlanticsprinttech.entities.dominio.ServicioEvento;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioInfraestructuraImpl implements ServicioInfraestructura {

    private final RepositorioServicio repositorioServicio;
    private final RepositorioServicioEvento repositorioEvento;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioInfraestructuraImpl(
        RepositorioServicio repositorioServicio,
        RepositorioServicioEvento repositorioEvento,
        RepositorioUsuario repositorioUsuario,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioServicio = repositorioServicio;
        this.repositorioEvento = repositorioEvento;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servicio> listar() {
        return repositorioServicio.findAllConTecnico();
    }

    @Override
    @Transactional(readOnly = true)
    public Servicio obtener(Long id) {
        return repositorioServicio.findByIdConTecnico(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
    }

    @Override
    public Servicio crear(String identificadorIngreso, String nombre, String descripcionTecnica) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, DIRECTIVO o SECRETARIO pueden crear servicios");
        }
        Servicio servicio = Servicio.crear(nombre, descripcionTecnica);
        servicio = repositorioServicio.save(servicio);
        repositorioEvento.save(ServicioEvento.crear(servicio, EstadoServicio.OPERATIVO, "Servicio creado", identificadorIngreso));
        return servicio;
    }

    @Override
    public Servicio actualizarEstado(String identificadorIngreso, Long servicioId, String estado, String comentario) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, DIRECTIVO o SECRETARIO pueden actualizar el estado de servicios");
        }

        EstadoServicio nuevoEstado;
        try {
            nuevoEstado = EstadoServicio.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + estado);
        }

        Servicio servicio = repositorioServicio.findByIdConTecnico(servicioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));

        switch (nuevoEstado) {
            case FALLA_CRITICA  -> servicio.reportarFalla(comentario, usuario);
            case MANTENIMIENTO  -> servicio.registrarMantenimiento(comentario, usuario);
            case OPERATIVO      -> servicio.marcarOperativo(comentario, usuario);
        }

        servicio = repositorioServicio.save(servicio);
        repositorioEvento.save(ServicioEvento.crear(servicio, nuevoEstado, comentario, identificadorIngreso));
        return servicio;
    }

    @Override
    public Servicio modificar(String identificadorIngreso, Long servicioId, String nombre, String descripcionTecnica) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR, DIRECTIVO o SECRETARIO pueden modificar servicios");
        }
        Servicio servicio = repositorioServicio.findByIdConTecnico(servicioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        servicio.actualizarDatos(nombre, descripcionTecnica);
        return repositorioServicio.save(servicio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioEvento> listarHistorial(Long servicioId) {
        if (!repositorioServicio.existsById(servicioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado");
        }
        return repositorioEvento.findByServicioIdOrderByFechaEventoDesc(servicioId);
    }

    @Override
    public void eliminar(String identificadorIngreso, Long servicioId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolAdministrador(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo ADMINISTRADOR puede eliminar servicios");
        }
        if (!repositorioServicio.existsById(servicioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado");
        }
        repositorioEvento.deleteByServicioId(servicioId);
        repositorioServicio.deleteById(servicioId);
    }
}
