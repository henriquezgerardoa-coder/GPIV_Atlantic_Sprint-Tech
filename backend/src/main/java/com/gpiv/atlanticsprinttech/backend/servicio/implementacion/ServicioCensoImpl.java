package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioCenso;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioPersonalRegistrado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioVehiculo;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCenso;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaResumenDeclaracion;
import com.gpiv.atlanticsprinttech.entities.dominio.Censo;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.PersonalRegistrado;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Vehiculo;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioCensoImpl implements ServicioCenso {

    private static final int ANIO_MINIMO = 2000;
    private static final String PATRON_CUIL_DIGITOS = "^[0-9]{11}$";
    private static final String PATRON_PATENTE_ARGENTINA = "[A-Z]{3}\\d{3}";
    private static final String PATRON_PATENTE_MERCOSUR = "[A-Z]{2}\\d{3}[A-Z]{2}";

    private final RepositorioCenso repositorioCenso;
    private final RepositorioPersonalRegistrado repositorioPersonal;
    private final RepositorioVehiculo repositorioVehiculo;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioCensoImpl(
        RepositorioCenso repositorioCenso,
        RepositorioPersonalRegistrado repositorioPersonal,
        RepositorioVehiculo repositorioVehiculo,
        RepositorioEmpresa repositorioEmpresa,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioCenso = repositorioCenso;
        this.repositorioPersonal = repositorioPersonal;
        this.repositorioVehiculo = repositorioVehiculo;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public List<Censo> listarCensos(String identificadorIngreso, Long empresaId) {
        verificarAcceso(identificadorIngreso, empresaId);
        return repositorioCenso.findByEmpresaIdOrderByAnioPeriodoDescSemestreDesc(empresaId);
    }

    @Override
    public Censo declararCenso(
        String identificadorIngreso,
        Long empresaId,
        int anioPeriodo,
        int cantidadPersonalRegistrado,
        int cantidadPersonalNoRegistrado,
        String observacion
    ) {
        verificarAcceso(identificadorIngreso, empresaId);
        int anioActual = Year.now().getValue();
        if (anioPeriodo < ANIO_MINIMO || anioPeriodo > anioActual) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El año de período debe estar entre " + ANIO_MINIMO + " y " + anioActual);
        }
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        repositorioCenso.findByEmpresaIdAndAnioPeriodoAndSemestre(empresaId, anioPeriodo, 0)
            .ifPresent(repositorioCenso::delete);

        Censo nuevo = Censo.crear(empresa, anioPeriodo, 0, cantidadPersonalRegistrado, cantidadPersonalNoRegistrado,
            observacion == null || observacion.isBlank() ? null : observacion.trim());
        return repositorioCenso.save(nuevo);
    }

    @Override
    public List<PersonalRegistrado> listarPersonal(String identificadorIngreso, Long empresaId) {
        verificarAcceso(identificadorIngreso, empresaId);
        return repositorioPersonal.findByEmpresaIdOrderByNombreCompletoAsc(empresaId);
    }

    @Override
    public PersonalRegistrado agregarPersonal(
        String identificadorIngreso,
        Long empresaId,
        String cuil,
        String nombreCompleto,
        LocalDate fechaIngreso
    ) {
        verificarAcceso(identificadorIngreso, empresaId);
        String soloDigitos = cuil.replaceAll("[^0-9]", "");
        if (!soloDigitos.matches(PATRON_CUIL_DIGITOS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El CUIL debe tener exactamente 11 dígitos");
        }
        String cuilFormateado = soloDigitos.substring(0, 2) + "-"
            + soloDigitos.substring(2, 10) + "-"
            + soloDigitos.substring(10);
        if (repositorioPersonal.existsByEmpresaIdAndCuit(empresaId, cuilFormateado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe un empleado con ese CUIL en esta empresa");
        }
        if (fechaIngreso.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La fecha de ingreso no puede ser futura");
        }
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
        PersonalRegistrado personal = PersonalRegistrado.crear(empresa, cuilFormateado,
            nombreCompleto.trim(), fechaIngreso);
        return repositorioPersonal.save(personal);
    }

    @Override
    public void eliminarPersonal(String identificadorIngreso, Long empresaId, Long personalId) {
        verificarAcceso(identificadorIngreso, empresaId);
        PersonalRegistrado personal = repositorioPersonal.findByIdAndEmpresaId(personalId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        repositorioPersonal.delete(personal);
    }

    @Override
    public RespuestaResumenDeclaracion obtenerResumenDeclaracion(String identificadorIngreso, Long empresaId) {
        verificarAcceso(identificadorIngreso, empresaId);
        long declarados = repositorioPersonal.countByEmpresaIdAndDeclarado(empresaId, true);
        long pendientes = repositorioPersonal.countByEmpresaIdAndDeclarado(empresaId, false);
        String ultimaDeclaracion = repositorioPersonal
            .findTopByEmpresaIdAndDeclaradoTrueOrderByFechaDeclaracionDesc(empresaId)
            .map(p -> p.getFechaDeclaracion().toString())
            .orElse(null);
        return new RespuestaResumenDeclaracion(declarados, pendientes, ultimaDeclaracion);
    }

    @Override
    public Censo generarDeclaracionJurada(String identificadorIngreso, Long empresaId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!usuario.tieneRol(RolUsuario.EMPRESA)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo el rol EMPRESA puede generar declaraciones juradas");
        }
        servicioContextoUsuario.exigirAccesoAEmpresa(usuario, empresaId);

        List<PersonalRegistrado> pendientes =
            repositorioPersonal.findByEmpresaIdAndDeclaradoFalseOrderByNombreCompletoAsc(empresaId);
        if (pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No hay empleados pendientes de declarar");
        }

        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        int semestre = hoy.getMonthValue() <= 6 ? 1 : 2;

        pendientes.forEach(PersonalRegistrado::declarar);
        repositorioPersonal.saveAll(pendientes);

        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        repositorioCenso.findByEmpresaIdAndAnioPeriodoAndSemestre(empresaId, anio, semestre)
            .ifPresent(repositorioCenso::delete);

        long totalDeclarados = repositorioPersonal.countByEmpresaIdAndDeclarado(empresaId, true);
        String obs = "Declaración jurada S" + semestre + " " + anio
            + " — " + pendientes.size() + " empleados incorporados"
            + " (total declarados: " + totalDeclarados + ")";
        Censo censo = Censo.crear(empresa, anio, semestre, (int) totalDeclarados, 0, obs);
        return repositorioCenso.save(censo);
    }

    @Override
    public List<Vehiculo> listarVehiculos(String identificadorIngreso, Long empresaId) {
        verificarAcceso(identificadorIngreso, empresaId);
        return repositorioVehiculo.findByEmpresaIdOrderByPatenteAsc(empresaId);
    }

    @Override
    public Vehiculo agregarVehiculo(
        String identificadorIngreso,
        Long empresaId,
        String patente,
        String marcaModelo,
        String tipo
    ) {
        verificarAcceso(identificadorIngreso, empresaId);
        String patenteNormalizada = patente.trim().toUpperCase().replace("-", "").replace(" ", "");
        if (!patenteNormalizada.matches(PATRON_PATENTE_ARGENTINA) && !patenteNormalizada.matches(PATRON_PATENTE_MERCOSUR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Formato de patente inválido. Use el formato argentino (ABC123) o Mercosur (AB123CD)");
        }
        if (repositorioVehiculo.existsByEmpresaIdAndPatente(empresaId, patenteNormalizada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe un vehículo con esa patente en esta empresa");
        }
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
        Vehiculo vehiculo = Vehiculo.crear(empresa, patenteNormalizada, marcaModelo.trim(), tipo.trim());
        return repositorioVehiculo.save(vehiculo);
    }

    @Override
    public void eliminarVehiculo(String identificadorIngreso, Long empresaId, Long vehiculoId) {
        verificarAcceso(identificadorIngreso, empresaId);
        Vehiculo vehiculo = repositorioVehiculo.findByIdAndEmpresaId(vehiculoId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
        repositorioVehiculo.delete(vehiculo);
    }

    private void verificarAcceso(String identificadorIngreso, Long empresaId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        servicioContextoUsuario.exigirAccesoAEmpresa(usuario, empresaId);
    }
}
