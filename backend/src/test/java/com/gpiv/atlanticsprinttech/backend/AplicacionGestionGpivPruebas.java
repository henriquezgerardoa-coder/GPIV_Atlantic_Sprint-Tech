package com.gpiv.atlanticsprinttech.backend;

import com.gpiv.atlanticsprinttech.backend.configuracion.PropiedadesApiUsuarios;
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
                .with(httpBasic("directivo", "directivo123")))
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



