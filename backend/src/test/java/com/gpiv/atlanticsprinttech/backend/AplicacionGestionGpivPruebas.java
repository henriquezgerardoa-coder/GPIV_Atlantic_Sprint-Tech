package com.gpiv.atlanticsprinttech.backend;

import com.gpiv.atlanticsprinttech.backend.configuracion.PropiedadesApiUsuarios;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AplicacionGestionGpivPruebas {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropiedadesApiUsuarios propiedadesApiUsuarios;

    @Autowired
    private RepositorioUsuario repositorioUsuario;

    @Test
    void deberiaResponderEstadoOkEnSalud() throws Exception {
        mockMvc.perform(get("/salud"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ok"));
    }

    @Test
    void deberiaCompletarFlujoCrudDeEmpresa() throws Exception {
        SolicitudEmpresa nuevaEmpresa = crearSolicitudEmpresa(
            "Empresa Uno",
            "Empresa Uno SA",
            "NIT-UNO-001",
            "20-12345678-9",
            "contacto@empresa.com"
        );

        String cuerpoSolicitud = objectMapper.writeValueAsString(nuevaEmpresa);

        String respuestaCreacion = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoSolicitud))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Empresa Uno"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaCreacion).get("id").asLong();

        mockMvc.perform(get("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cuit").value("20-12345678-9"));

        SolicitudEmpresa empresaActualizada = crearSolicitudEmpresa(
            "Empresa Uno SA",
            "Empresa Uno Sociedad Anonima",
            "NIT-UNO-001",
            "20-12345678-9",
            "nuevo@empresa.com"
        );

        mockMvc.perform(put("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(empresaActualizada)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Empresa Uno SA"))
            .andExpect(jsonPath("$.correoElectronico").value("nuevo@empresa.com"));

        mockMvc.perform(delete("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNotFound());
    }

    @Test
    void deberiaPermitirCrudDeUsuariosSoloAdministrador() throws Exception {
        SolicitudUsuario solicitudUsuario = new SolicitudUsuario(
            "jlopez",
            "Juan Lopez",
            "clave12345",
            true,
            Set.of(RolUsuario.DIRECTIVO),
            null
        );

        String respuestaCreacion = mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudUsuario)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombreUsuario").value("jlopez"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idUsuario = objectMapper.readTree(respuestaCreacion).get("id").asLong();

        mockMvc.perform(get("/api/usuarios/{id}", idUsuario)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreCompleto").value("Juan Lopez"));

        mockMvc.perform(delete("/api/usuarios/{id}", idUsuario)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNoContent());
    }

    @Test
    void deberiaListarRolesDisponibles() throws Exception {
        mockMvc.perform(get("/api/catalogos/roles")
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("ADMINISTRADOR")));
    }

    @Test
    void deberiaListarRolesDisponiblesEnRutaLegada() throws Exception {
        mockMvc.perform(get("/api/usuarios/roles")
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isOk())
            .andExpect(header().string("Deprecation", propiedadesApiUsuarios.getRolesLegado().getDeprecation()))
            .andExpect(header().string("Sunset", propiedadesApiUsuarios.getRolesLegado().getSunset()))
            .andExpect(header().string("Link", propiedadesApiUsuarios.getRolesLegado().getLink()))
            .andExpect(jsonPath("$", hasItem("ADMINISTRADOR")));
    }

    @Test
    void deberiaDenegarRolesSiNoEstaAutenticado() throws Exception {
        mockMvc.perform(get("/api/catalogos/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaDenegarRolesLegadoSiNoEstaAutenticado() throws Exception {
        mockMvc.perform(get("/api/usuarios/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deberiaDenegarRolesLegadoParaDirectivo() throws Exception {
        mockMvc.perform(get("/api/usuarios/roles")
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminDebeVerPanelVisualizacionEmpresasYDirectivoNoDebeAcceder() throws Exception {
        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Panel Admin", "Empresa Panel Admin SA", "NIT-PANEL-001", "20-98989898-9", "panel.admin@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_panel_admin\",\"nombreCompleto\":\"Empresa Panel Admin\",\"clave\":\"EmpresaPanel123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/empresas")
                .with(httpBasic("empresa_panel_admin", "EmpresaPanel123")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/empresas/admin/vista")
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].nombre", hasItem("Empresa Panel Admin")));

        mockMvc.perform(get("/api/empresas/admin/vista/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Empresa Panel Admin"))
            .andExpect(jsonPath("$.telefono").value("2990000000"))
            .andExpect(jsonPath("$.fechaRegistro").exists())
            .andExpect(jsonPath("$.statusEmpresa").value("ACTIVA"))
            .andExpect(jsonPath("$.usuarioAsociado.nombreCompleto").value("Empresa Panel Admin"))
            .andExpect(jsonPath("$.usuarioAsociado.fechaUltimoAcceso").exists())
            .andExpect(jsonPath("$.totalEmpleados").value(0))
            .andExpect(jsonPath("$.totalVehiculos").value(0));

        mockMvc.perform(get("/api/empresas/admin/vista")
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deberiaDenegarCrearUsuarioSiNoEsAdministrador() throws Exception {
        SolicitudUsuario solicitudUsuario = new SolicitudUsuario(
            "usuariox",
            "Usuario X",
            "clave12345",
            true,
            Set.of(RolUsuario.DIRECTIVO),
            null
        );

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("directivo", "directivo123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudUsuario)))
            .andExpect(status().isForbidden());
    }

    @Test
    void deberiaDenegarListadoUsuariosParaDirectivo() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void directivoDebeTenerSoloLecturaEnEmpresasYAdminDebeConservarEscritura() throws Exception {
        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Solo Lectura", "Empresa Solo Lectura SA", "NIT-SL-001", "20-51515151-5", "sl@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(get("/api/empresas")
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("directivo", "directivo123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa No Editable", "Empresa No Editable SA", "NIT-SL-001", "20-51515151-5", "sl2@empresa.com")))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/empresas/{id}/servicios-post-radicacion", idEmpresa)
                .with(httpBasic("directivo", "directivo123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidadEmpleados\":10,\"vehiculos\":[]}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Editable Admin", "Empresa Editable Admin SA", "NIT-SL-001", "20-51515151-5", "sl3@empresa.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Empresa Editable Admin"));
    }

    @Test
    void empresaDebePoderRegistrarSuEmpresaInicialUnaVez() throws Exception {
        SolicitudEmpresa nuevaEmpresa = crearSolicitudEmpresa(
            "Empresa Dos",
            "Empresa Dos SRL",
            "NIT-DOS-001",
            "20-98765432-1",
            "dos@empresa.com"
        );

        mockMvc.perform(post("/api/empresas")
                .with(httpBasic("empresa", "empresa12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevaEmpresa)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Empresa Dos"));

        mockMvc.perform(post("/api/empresas")
                .with(httpBasic("empresa", "empresa12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crearSolicitudEmpresa(
                    "Empresa Dos Reintento",
                    "Empresa Dos Reintento SRL",
                    "NIT-DOS-002",
                    "20-98765432-2",
                    "dos2@empresa.com"
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    void deberiaCompletarFlujoCrudDeLoteYBloquearEliminacionDeEmpresaConLotes() throws Exception {
        SolicitudEmpresa nuevaEmpresa = crearSolicitudEmpresa(
            "Empresa Lotes",
            "Empresa Lotes SRL",
            "NIT-LOTES-001",
            "20-55555555-5",
            "lotes@empresa.com"
        );

        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevaEmpresa)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        String respuestaLote = mockMvc.perform(post("/api/lotes")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"L-01\",\"superficieMetrosCuadrados\":350.5,\"ocupado\":false,\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("L-01"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idLote = objectMapper.readTree(respuestaLote).get("id").asLong();

        mockMvc.perform(get("/api/lotes/{id}", idLote)
                .with(httpBasic("directivo", "directivo123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empresaId").value(idEmpresa));

        mockMvc.perform(put("/api/lotes/{id}", idLote)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"L-01-A\",\"superficieMetrosCuadrados\":360.0,\"ocupado\":true,\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ocupado").value(true));

        mockMvc.perform(delete("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/lotes/{id}", idLote)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminDebePoderCrearLoteSinEmpresaYAsignarloLuego() throws Exception {
        String respuestaLote = mockMvc.perform(post("/api/lotes")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"LIB-001\",\"superficieMetrosCuadrados\":5000.0,\"ocupado\":false,\"empresaId\":null}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("LIB-001"))
            .andExpect(jsonPath("$.empresaId").isEmpty())
            .andExpect(jsonPath("$.nombreEmpresa").value("Sin asignar"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idLote = objectMapper.readTree(respuestaLote).get("id").asLong();

        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Asignacion Lote", "Empresa Asignacion Lote SA", "NIT-ASIG-001", "20-90909090-9", "asignacion@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(put("/api/lotes/{id}", idLote)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"LIB-001\",\"superficieMetrosCuadrados\":5000.0,\"ocupado\":false,\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empresaId").value(idEmpresa));

        mockMvc.perform(put("/api/lotes/{id}", idLote)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"LIB-001\",\"superficieMetrosCuadrados\":5000.0,\"ocupado\":false,\"empresaId\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empresaId").isEmpty())
            .andExpect(jsonPath("$.nombreEmpresa").value("Sin asignar"))
            .andExpect(jsonPath("$.estadoAsignacion").isEmpty())
            .andExpect(jsonPath("$.fechaAsignacion").isEmpty())
            .andExpect(jsonPath("$.numeroExpedienteReferencia").isEmpty());
    }

    @Test
    void deberiaDenegarCrearLoteConRolEmpresa() throws Exception {
        SolicitudEmpresa nuevaEmpresa = crearSolicitudEmpresa(
            "Empresa Tres",
            "Empresa Tres SRL",
            "NIT-TRES-001",
            "20-33333333-3",
            "tres@empresa.com"
        );

        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevaEmpresa)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(post("/api/lotes")
                .with(httpBasic("empresa", "empresa12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"L-EMP\",\"superficieMetrosCuadrados\":120.0,\"ocupado\":false,\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("admin", "admin12345")))
            .andExpect(status().isNoContent());
    }

    @Test
    void empresaSoloDebeAccederASuEmpresaAsignada() throws Exception {
        String respuestaEmpresaA = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa A", "Empresa A SA", "NIT-EMP-A", "20-11111111-1", "a@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaA = objectMapper.readTree(respuestaEmpresaA).get("id").asLong();

        String respuestaEmpresaB = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa B", "Empresa B SA", "NIT-EMP-B", "20-22222222-2", "b@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaB = objectMapper.readTree(respuestaEmpresaB).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_asignada\",\"nombreCompleto\":\"Empresa Asignada\",\"clave\":\"Empresa123A\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresaA + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/empresas")
                .with(httpBasic("empresa_asignada", "Empresa123A")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(idEmpresaA));

        mockMvc.perform(get("/api/empresas/{id}", idEmpresaB)
                .with(httpBasic("empresa_asignada", "Empresa123A")))
            .andExpect(status().isForbidden());
    }

    @Test
    void empresaSoloDebeAccederALotesDeSuEmpresaAsignada() throws Exception {
        String respuestaEmpresaA = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Lote A", "Empresa Lote A SA", "NIT-LOT-A", "20-71111111-1", "lotea@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaA = objectMapper.readTree(respuestaEmpresaA).get("id").asLong();

        String respuestaEmpresaB = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Lote B", "Empresa Lote B SA", "NIT-LOT-B", "20-72222222-2", "loteb@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaB = objectMapper.readTree(respuestaEmpresaB).get("id").asLong();

        String respuestaLoteA = mockMvc.perform(post("/api/lotes")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"LA-01\",\"superficieMetrosCuadrados\":1500.0,\"ocupado\":false,\"empresaId\":" + idEmpresaA + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idLoteA = objectMapper.readTree(respuestaLoteA).get("id").asLong();

        String respuestaLoteB = mockMvc.perform(post("/api/lotes")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"LB-01\",\"superficieMetrosCuadrados\":1800.0,\"ocupado\":false,\"empresaId\":" + idEmpresaB + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idLoteB = objectMapper.readTree(respuestaLoteB).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_lotes_asignada\",\"nombreCompleto\":\"Empresa Lotes Asignada\",\"clave\":\"EmpresaLote123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresaA + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/lotes")
                .with(httpBasic("empresa_lotes_asignada", "EmpresaLote123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(idLoteA))
            .andExpect(jsonPath("$[0].empresaId").value(idEmpresaA));

        mockMvc.perform(get("/api/lotes/{id}", idLoteB)
                .with(httpBasic("empresa_lotes_asignada", "EmpresaLote123")))
            .andExpect(status().isNotFound());
    }

    @Test
    void empresaDebePoderActualizarSoloSuEmpresaAsignada() throws Exception {
        String respuestaEmpresaA = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Editable", "Empresa Editable SA", "NIT-EDIT-001", "20-77777777-7", "editable@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaA = objectMapper.readTree(respuestaEmpresaA).get("id").asLong();

        String respuestaEmpresaB = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa No Editable", "Empresa No Editable SA", "NIT-EDIT-002", "20-66666666-6", "noeditable@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaB = objectMapper.readTree(respuestaEmpresaB).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_edita\",\"nombreCompleto\":\"Empresa Edita\",\"clave\":\"EmpresaEdita123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresaA + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/empresas/{id}", idEmpresaA)
                .with(httpBasic("empresa_edita", "EmpresaEdita123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa(
                    "Empresa Editable Actualizada",
                    "Empresa Editable Actualizada SA",
                    "NIT-EDIT-001",
                    "20-77777777-7",
                    "editable.actualizada@empresa.com"
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(idEmpresaA))
            .andExpect(jsonPath("$.nombre").value("Empresa Editable Actualizada"));

        mockMvc.perform(put("/api/empresas/{id}", idEmpresaB)
                .with(httpBasic("empresa_edita", "EmpresaEdita123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Intento Invalido", "Intento Invalido SA", "NIT-EDIT-002", "20-66666666-6", "x@empresa.com")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deberiaPermitirCambiarClavePropia() throws Exception {
        mockMvc.perform(patch("/api/usuarios/mi-clave")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claveActual\":\"admin12345\",\"claveNueva\":\"admin123456\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios")
                .with(httpBasic("admin", "admin123456")))
            .andExpect(status().isOk());
    }

    @Test
    void empresaDebePoderActualizarSusDatosPersonalesYVisualizarRadicaciones() throws Exception {
        mockMvc.perform(get("/api/radicaciones")
                .with(httpBasic("empresa", "empresa12345")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/yo/perfil")
                .with(httpBasic("empresa", "empresa12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreCompleto\":\"Empresa Demo Actualizada\",\"correoElectronico\":\"empresa.demo@gpiv.local\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value("Datos personales actualizados"));

        Usuario usuarioEmpresa = repositorioUsuario.findByNombreUsuario("empresa").orElseThrow();
        assertEquals("Empresa Demo Actualizada", usuarioEmpresa.getNombreCompleto());
        assertEquals("empresa.demo@gpiv.local", usuarioEmpresa.getCorreoElectronico());
    }

    @Test
    void deberiaCrearRadicacionPedidoLotesConRelevamiento() throws Exception {
        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Formulario", "Empresa Formulario SRL", "NIT-FORM-001", "20-33334444-5", "formulario@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_formulario\",\"nombreCompleto\":\"Empresa Formulario\",\"clave\":\"EmpresaForm123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isCreated());

        String solicitud = """
            {
              "tipoSolicitud": "PEDIDO_LOTES",
              "descripcion": "Relevamiento integral para solicitud de lote",
              "usoEstimativo": "Agua y energia",
              "relevamientoPedidoLotes": {
                "correo": "empresa_formulario@gpiv.local",
                "razonSocialEmpresa": "Empresa Formulario SRL",
                "cuit": "20333444556",
                "ingresosBrutos": "Convenio local",
                "actividadPrincipal": "Produccion metalmecanica",
                "actividadSecundaria": "Mantenimiento",
                "tipoEmpresa": "EXISTENTE",
                "objetoProyecto": "INCREMENTAR_PRODUCCION",
                "direccion": "Calle 123",
                "personaReferente": "Ana Perez",
                "telefono": "2991234567",
                "correoElectronico": "contacto@empresaformulario.com",
                "rubro": "BIENES_Y_SERVICIOS",
                "rubroOtro": "",
                "descripcionServicioBienOfrecido": "Fabricacion de piezas",
                "emplazamientoActual": "ALQUILADO",
                "personalJerarquico": 3,
                "personalProduccion": 20,
                "personalAdministrativo": 4,
                "tiempoRadicacionMeses": 24,
                "necesidadMetrosCuadrados": 2500,
                "superficieCubiertaTrabajo": 1200.0,
                "superficieCubiertaDeposito": 600.0,
                "superficieFuturaExpansion": 400.0,
                "superficieEstacionamiento": 300.0,
                "tienePlanos": true,
                "personalAOcupar": 12,
                "materiasPrimas": "Acero laminado",
                "destinoProduccion": "Mercado nacional",
                "tensionAlimentacion": "MEDIA",
                "potenciaInstaladaKw": 320.0,
                "aguaLtsMes": 15000.0,
                "requiereGas": true,
                "tipoResiduosEfluentes": "Solidos y liquidos",
                "tratamientoEnPlanta": true,
                "necesitaBalanzaPublica": true,
                "necesitaComedorUnitario": false,
                "necesitaSalonCoworking": true
              }
            }
            """;

        mockMvc.perform(post("/api/radicaciones")
                .with(httpBasic("empresa_formulario", "EmpresaForm123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoSolicitud").value("PEDIDO_LOTES"))
            .andExpect(jsonPath("$.tieneRelevamientoPedidoLotes").value(true));
    }

    @Test
    void adminDebePoderCambiarEstadosDeRadicacionYEmpresaDebeVisualizarEstadoFinal() throws Exception {
        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Estados", "Empresa Estados SA", "NIT-EST-001", "20-88889999-5", "estados@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_estados\",\"nombreCompleto\":\"Empresa Estados\",\"clave\":\"EmpresaEstados123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isCreated());

        String respuestaRadicacion = mockMvc.perform(post("/api/radicaciones")
                .with(httpBasic("empresa_estados", "EmpresaEstados123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoSolicitud\":\"SERVICIO\",\"descripcion\":\"Solicitud para pruebas de estado\",\"usoEstimativo\":\"Basico\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idRadicacion = objectMapper.readTree(respuestaRadicacion).get("id").asLong();

        String[] estados = {
            "EN_REVISION",
            "REQUIERE_INFORMACION_ADICIONAL",
            "EN_REVISION",
            "APROBADA",
            "RADICADA"
        };

        for (String estado : estados) {
            mockMvc.perform(patch("/api/radicaciones/{id}/estado", idRadicacion)
                    .with(httpBasic("admin", "admin12345"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"" + estado + "\",\"comentario\":\"Cambio de estado de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(estado));
        }

        mockMvc.perform(get("/api/radicaciones/{id}", idRadicacion)
                .with(httpBasic("empresa_estados", "EmpresaEstados123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RADICADA"))
            .andExpect(jsonPath("$.etapaActual").value(3));
    }

    @Test
    void empresaDebeGestionarServiciosPostRadicacionSoloCuandoExpedienteEsteRadicada() throws Exception {
        String respuestaEmpresa = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Servicios", "Empresa Servicios SA", "NIT-SRV-001", "20-45454545-4", "servicios@empresa.com")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresa = objectMapper.readTree(respuestaEmpresa).get("id").asLong();

        mockMvc.perform(post("/api/usuarios")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"empresa_servicios\",\"nombreCompleto\":\"Empresa Servicios\",\"clave\":\"EmpresaServ123\",\"activo\":true,\"roles\":[\"EMPRESA\"],\"empresaId\":" + idEmpresa + "}"))
            .andExpect(status().isCreated());

        String respuestaRadicacion = mockMvc.perform(post("/api/radicaciones")
                .with(httpBasic("empresa_servicios", "EmpresaServ123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoSolicitud\":\"SERVICIO\",\"descripcion\":\"Solicitud inicial\",\"usoEstimativo\":\"Basico\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idRadicacion = objectMapper.readTree(respuestaRadicacion).get("id").asLong();

        mockMvc.perform(get("/api/empresas/{id}/servicios-post-radicacion", idEmpresa)
                .with(httpBasic("empresa_servicios", "EmpresaServ123")))
            .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/radicaciones/{id}/estado", idRadicacion)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_REVISION\",\"comentario\":\"Revision inicial\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/radicaciones/{id}/estado", idRadicacion)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"APROBADA\",\"comentario\":\"Aprobada por mesa\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/radicaciones/{id}/estado", idRadicacion)
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RADICADA\",\"comentario\":\"Radicacion confirmada\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/empresas/{id}/servicios-post-radicacion", idEmpresa)
                .with(httpBasic("empresa_servicios", "EmpresaServ123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cantidadEmpleados": 42,
                      "vehiculos": [
                        {"placa":"ABC123","tipo":"CAMION","descripcion":"Carga"},
                        {"placa":"DEF456","tipo":"AUTO","descripcion":"Visitas"}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidadEmpleados").value(42))
            .andExpect(jsonPath("$.vehiculos.length()").value(2));

        mockMvc.perform(get("/api/empresas/{id}/servicios-post-radicacion", idEmpresa)
                .with(httpBasic("empresa_servicios", "EmpresaServ123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidadEmpleados").value(42))
            .andExpect(jsonPath("$.vehiculos[0].placa").value("ABC123"));

        mockMvc.perform(put("/api/empresas/{id}", idEmpresa)
                .with(httpBasic("empresa_servicios", "EmpresaServ123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoEmpresa("Empresa Servicios Editada", "Empresa Servicios SA", "NIT-SRV-001", "20-45454545-4", "servicios2@empresa.com")))
            .andExpect(status().isConflict());
    }

    @Test
    void deberiaRegistrarVerificarYPermitirLoginConCorreo() throws Exception {
        String correo = "registro1@gpiv.local";
        String clave = "ClaveSegura123";

        mockMvc.perform(post("/api/public/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correoElectronico\":\"" + correo + "\",\"clave\":\"" + clave + "\",\"confirmacionClave\":\"" + clave + "\"}"))
            .andExpect(status().isCreated());

        Usuario usuario = repositorioUsuario.findByCorreoElectronicoIgnoreCase(correo)
            .orElseThrow();

        mockMvc.perform(get("/api/yo")
                .with(httpBasic(correo, clave)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/public/verificacion")
                .param("token", usuario.getTokenVerificacionEmail()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/yo")
                .with(httpBasic(correo, clave)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreUsuario").value(usuario.getNombreUsuario()));
    }

    private SolicitudEmpresa crearSolicitudEmpresa(
        String nombre,
        String razonSocial,
        String nit,
        String cuit,
        String correoElectronico
    ) {
        return new SolicitudEmpresa(
            nombre,
            razonSocial,
            nit,
            cuit,
            "Direccion " + nombre,
            "Actividad " + nombre,
            correoElectronico,
            0,
            "2990000000"
        );
    }

    private String cuerpoEmpresa(
        String nombre,
        String razonSocial,
        String nit,
        String cuit,
        String correoElectronico
    ) {
        return "{\"nombre\":\"" + nombre + "\","
            + "\"razonSocial\":\"" + razonSocial + "\","
            + "\"nit\":\"" + nit + "\","
            + "\"cuit\":\"" + cuit + "\","
            + "\"direccion\":\"Direccion " + nombre + "\","
            + "\"actividadEconomica\":\"Actividad " + nombre + "\","
            + "\"correoElectronico\":\"" + correoElectronico + "\","
            + "\"telefono\":\"2990000000\","
            + "\"cantidadEmpleados\":0}";
    }
}



