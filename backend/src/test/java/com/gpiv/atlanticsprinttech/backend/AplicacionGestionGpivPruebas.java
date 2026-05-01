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
        SolicitudEmpresa nuevaEmpresa = new SolicitudEmpresa("Empresa Uno", "20-12345678-9", "contacto@empresa.com");

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

        SolicitudEmpresa empresaActualizada = new SolicitudEmpresa("Empresa Uno SA", "20-12345678-9", "nuevo@empresa.com");

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
    void deberiaDenegarCrearEmpresaConRolEmpresa() throws Exception {
        SolicitudEmpresa nuevaEmpresa = new SolicitudEmpresa("Empresa Dos", "20-98765432-1", "dos@empresa.com");

        mockMvc.perform(post("/api/empresas")
                .with(httpBasic("empresa", "empresa12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevaEmpresa)))
            .andExpect(status().isForbidden());
    }

    @Test
    void deberiaCompletarFlujoCrudDeLoteYBloquearEliminacionDeEmpresaConLotes() throws Exception {
        SolicitudEmpresa nuevaEmpresa = new SolicitudEmpresa("Empresa Lotes", "20-55555555-5", "lotes@empresa.com");

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
    void deberiaDenegarCrearLoteConRolEmpresa() throws Exception {
        SolicitudEmpresa nuevaEmpresa = new SolicitudEmpresa("Empresa Tres", "20-33333333-3", "tres@empresa.com");

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
                .content("{\"nombre\":\"Empresa A\",\"cuit\":\"20-11111111-1\",\"correoElectronico\":\"a@empresa.com\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaA = objectMapper.readTree(respuestaEmpresaA).get("id").asLong();

        String respuestaEmpresaB = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Empresa B\",\"cuit\":\"20-22222222-2\",\"correoElectronico\":\"b@empresa.com\"}"))
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
                .content("{\"nombre\":\"Empresa Lote A\",\"cuit\":\"20-71111111-1\",\"correoElectronico\":\"lotea@empresa.com\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long idEmpresaA = objectMapper.readTree(respuestaEmpresaA).get("id").asLong();

        String respuestaEmpresaB = mockMvc.perform(post("/api/empresas")
                .with(httpBasic("admin", "admin12345"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Empresa Lote B\",\"cuit\":\"20-72222222-2\",\"correoElectronico\":\"loteb@empresa.com\"}"))
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
}



