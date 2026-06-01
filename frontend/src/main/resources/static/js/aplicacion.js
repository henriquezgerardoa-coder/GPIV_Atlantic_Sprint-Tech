/* ═══════════════════════════════════════════════════
   Configuración inicial (primer ingreso EMPRESA sin empresa)
═══════════════════════════════════════════════════ */

const ConfiguracionInicial = (() => {
    let _modal = null;

    function mostrar() {
        const modalEl = document.getElementById("modalConfiguracionInicial");
        _modal = new bootstrap.Modal(modalEl, {
            backdrop: "static",
            keyboard: false,
        });
        modalEl.addEventListener("shown.bs.modal", () => _cargarRubros(), {
            once: true,
        });
        _modal.show();
    }

    async function _cargarRubros() {
        const selector = document.getElementById("ciRubro");
        if (!selector) return;
        const resp = await ApiCliente.obtener("/api/catalogos/rubros");
        if (!resp?.ok) return;
        const rubros = await resp.json();
        selector.innerHTML =
            '<option value="">Seleccionar rubro (opcional)...</option>' +
            rubros
                .map((r) => `<option value="${r.id}">${r.nombre}</option>`)
                .join("");
    }

    async function confirmar() {
        ocultarAlertaModal("alertaConfiguracionInicial");
        const campos = {
            nombre: document.getElementById("ciNombre").value.trim(),
            razonSocial: document.getElementById("ciRazonSocial").value.trim(),
            cuit: document.getElementById("ciCuit").value.trim(),
            direccion: document.getElementById("ciDireccion").value.trim(),
            actividadEconomica: document
                .getElementById("ciActividad")
                .value.trim(),
            correoElectronico: document.getElementById("ciCorreo").value.trim(),
            telefono:
                document.getElementById("ciTelefono").value.trim() || null,
        };
        const rubroId =
            parseInt(document.getElementById("ciRubro")?.value) || null;
        if (
            !campos.nombre ||
            !campos.razonSocial ||
            !campos.cuit ||
            !campos.direccion ||
            !campos.actividadEconomica ||
            !campos.correoElectronico
        ) {
            mostrarAlertaModal(
                "alertaConfiguracionInicial",
                "Completá todos los campos obligatorios.",
            );
            return;
        }
        const btn = document.getElementById("btnConfirmarConfiguracionInicial");
        btn.disabled = true;
        btn.innerHTML =
            '<span class="spinner-border spinner-border-sm me-1"></span>Guardando...';
        const respuesta = await ApiCliente.crear("/api/empresas", campos);
        if (!respuesta?.ok) {
            btn.disabled = false;
            btn.innerHTML =
                '<i class="bi bi-check-lg me-1"></i>Confirmar y continuar';
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal(
                "alertaConfiguracionInicial",
                error?.mensaje ||
                    "No se pudo crear la empresa. Verificá los datos.",
            );
            return;
        }
        if (rubroId) {
            const empresa = await respuesta.json();
            await ApiCliente.parche(
                `/api/empresas/${empresa.id}/rubro-inicial`,
                { rubroId },
            );
        }
        btn.disabled = false;
        btn.innerHTML =
            '<i class="bi bi-check-lg me-1"></i>Confirmar y continuar';
        const respYo = await ApiCliente.obtener("/api/yo");
        if (respYo?.ok) {
            sessionStorage.setItem(
                "usuario",
                JSON.stringify(await respYo.json()),
            );
        }
        _modal?.hide();
        navegarA("empresas");
    }

    return { mostrar, confirmar };
})();

/* ═══════════════════════════════════════════════════
   Utilidades globales (usadas por los módulos)
═══════════════════════════════════════════════════ */

function mostrarAlerta(mensaje, tipo = "success") {
    const contenedor = document.getElementById("contenedorAlertas");
    if (!contenedor) return;
    const id = "alerta_" + Date.now();
    const icono =
        tipo === "success" ? "check-circle-fill" : "exclamation-triangle-fill";
    contenedor.insertAdjacentHTML(
        "beforeend",
        `
        <div id="${id}" class="alert alert-${tipo} alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-${icono} me-2"></i>${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`,
    );
    setTimeout(() => document.getElementById(id)?.remove(), 5000);
}

function mostrarAlertaModal(idElemento, mensaje) {
    const el = document.getElementById(idElemento);
    if (el) {
        el.textContent = mensaje;
        el.classList.remove("d-none");
    }
}

function ocultarAlertaModal(idElemento) {
    document.getElementById(idElemento)?.classList.add("d-none");
}

/* ═══════════════════════════════════════════════════
   Navegación entre secciones
═══════════════════════════════════════════════════ */

function navegarA(seccion) {
    const esEmpresaExclusivo =
        Autenticacion.tieneAcceso(["EMPRESA"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);
    const esTecnicoExclusivo =
        Autenticacion.tieneAcceso(["TECNICO"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);
    const seccionesRestringidasEmpresa = new Set([
        "proyectos",
        "infraestructura",
        "dashboard",
        "monitor",
    ]);
    const seccionesRestringidasTecnico = new Set([
        "empresas",
        "lotes",
        "radicaciones",
        "usuarios",
        "audit-log",
        "informes",
        "censo",
        "infraestructura",
        "dashboard",
        "monitor",
        "cambio-rubro",
        "panel",
        "mapa",
    ]);
    if (esEmpresaExclusivo && seccionesRestringidasEmpresa.has(seccion)) {
        mostrarAlerta("No tienes acceso a esta sección.", "danger");
        seccion = "empresas";
    }
    if (
        esEmpresaExclusivo &&
        window._empresaInactiva &&
        seccion !== "mensajeria" &&
        seccion !== "empresas"
    ) {
        mostrarAlerta(
            "Tu empresa está inactiva. Solo tienes acceso a mensajería.",
            "warning",
        );
        seccion = "mensajeria";
    }
    if (esTecnicoExclusivo && seccionesRestringidasTecnico.has(seccion)) {
        mostrarAlerta("No tienes acceso a esta sección.", "danger");
        seccion = "proyectos";
    }

    document
        .querySelectorAll(".seccion-contenido")
        .forEach((s) => s.classList.add("d-none"));
    document
        .querySelectorAll(".enlace-nav")
        .forEach((a) => a.classList.remove("activo"));

    const elSeccion = document.getElementById("seccion-" + seccion);
    const elNav = document.getElementById("nav-" + seccion);
    elSeccion?.classList.remove("d-none");
    elNav?.classList.add("activo");

    _limpiarSeccion(seccion);

    // Cerrar sidebar en móvil tras navegar
    const offcanvas = bootstrap.Offcanvas.getInstance(
        document.getElementById("offcanvasSidebar"),
    );
    offcanvas?.hide();

    switch (seccion) {
        case "panel":
            cargarPanel();
            break;
        case "empresas":
            ModuloEmpresas.cargar();
            break;
        case "lotes":
            ModuloLotes.cargar();
            break;
        case "radicaciones":
            ModuloRadicaciones.cargar();
            break;
        case "mensajeria":
            ModuloMensajeria.cargar();
            break;
        case "informes":
            ModuloEstadisticas.cargar();
            break;
        case "usuarios":
            ModuloUsuarios.cargar();
            break;
        case "audit-log":
            ModuloAuditLog.cargar();
            break;
        case "censo":
            ModuloCenso.cargar();
            break;
        case "cambio-rubro":
            ModuloCambioRubro.cargar();
            break;
        case "proyectos":
            ModuloProyectos.cargar();
            break;
        case "infraestructura":
            ModuloInfraestructura.cargar();
            break;
        case "dashboard":
            ModuloDashboard.cargar();
            break;
        case "monitor":
            ModuloMonitor.cargar();
            break;
        case "mapa":
            ModuloMapa.cargar();
            break;
    }
}

const _FILA_CARGANDO =
    '<tr><td colspan="20" class="text-center text-muted py-3"><span class="spinner-border spinner-border-sm me-2"></span>Cargando...</td></tr>';
const _DIV_CARGANDO =
    '<div class="text-center text-muted py-4"><span class="spinner-border spinner-border-sm me-2"></span>Cargando...</div>';

function _limpiarSeccion(seccion) {
    const tablas = {
        radicaciones: [
            "cuerpoTablaRadicaciones",
            "cuerpoTablaHistorialRadicacion",
        ],
        empresas: ["cuerpoTablaEmpresas", "cuerpoTablaUsuariosEmpresa"],
        lotes: ["cuerpoTablaLotes"],
        censo: [
            "cuerpoTablaCenso",
            "cuerpoTablaPersonal",
            "cuerpoTablaVehiculos",
        ],
        "cambio-rubro": ["cuerpoCambiosRubro"],
        monitor: ["cuerpoCuerpoMonitor"],
        proyectos: ["cuerpoTablaProyectos"],
        usuarios: ["cuerpoTablaUsuarios"],
        "audit-log": ["cuerpoTablaAuditLog"],
        informes: ["cuerpoTablaEstadosRadicacion"],
    };
    const divs = {
        mensajeria: [
            "listaConversacionesMensajeria",
            "contenedorMensajesMensajeria",
        ],
        empresas: ["listaEmpresasAdmin"],
        infraestructura: ["gridServicios"],
        dashboard: ["kpisFila1", "kpisFila2"],
        proyectos: ["listaAlertasHitos", "listaHitosProximos"],
    };
    (tablas[seccion] || []).forEach((id) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = _FILA_CARGANDO;
    });
    (divs[seccion] || []).forEach((id) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = _DIV_CARGANDO;
    });
    if (seccion === "proyectos") {
        document.getElementById("bloqueAlertasHitos")?.classList.add("d-none");
        document.getElementById("bloqueHitosProximos")?.classList.add("d-none");
    }
    if (seccion === "mensajeria") {
        document
            .getElementById("detalleConversacionMensajeria")
            ?.classList.add("d-none");
        document
            .getElementById("panelSinConversacionMensajeria")
            ?.classList.remove("d-none");
    }
    if (seccion === "empresas") {
        [
            "errorEmpresasAdmin",
            "sinEmpresasAdmin",
            "estadoCargaEmpresasAdmin",
            "panelEmpresasAdminVisualizacion",
        ].forEach((id) => document.getElementById(id)?.classList.add("d-none"));
    }
    if (seccion === "panel") {
        [
            "statEmpresas",
            "statLotes",
            "statLotesOcupados",
            "statLotesLibres",
        ].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.textContent = "—";
        });
    }
}

async function cargarPanel() {
    try {
        const resp = await ApiCliente.obtener("/api/estadisticas/resumen");
        if (!resp?.ok) return;
        const datos = await resp.json();
        document.getElementById("statEmpresas").textContent =
            datos.totalEmpresas;
        document.getElementById("statLotes").textContent = datos.totalLotes;
        document.getElementById("statLotesOcupados").textContent =
            datos.lotesOcupados;
        document.getElementById("statLotesLibres").textContent =
            datos.lotesDisponibles;
    } catch (err) {
        console.error("Error al cargar el panel:", err);
    }
} /* ═══════════════════════════════════════════════════
   Inicialización al cargar la página
═══════════════════════════════════════════════════ */

document.addEventListener("DOMContentLoaded", async () => {
    if (!Autenticacion.verificarYRedirigir()) return;

    const sesion = Autenticacion.obtenerSesion();

    // Mostrar información del usuario en la barra superior
    document.getElementById("nombreUsuarioNavbar").textContent =
        sesion.nombreCompleto || sesion.nombreUsuario;
    document.getElementById("rolesUsuarioNavbar").textContent =
        sesion.roles.join(", ");

    document.getElementById("campoMiNombreCompleto").value =
        sesion.nombreCompleto || "";
    document.getElementById("campoMiCorreoElectronico").value =
        sesion.correoElectronico || "";

    // Ocultar la sección Usuarios si no es administrador ni secretario
    if (!Autenticacion.tieneAcceso(["ADMINISTRADOR", "SECRETARIO"])) {
        document.getElementById("itemNavUsuarios")?.classList.add("d-none");
        document
            .getElementById("itemNavUsuariosMovil")
            ?.classList.add("d-none");
    }

    const esEmpresaExclusivo =
        Autenticacion.tieneAcceso(["EMPRESA"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);

    // Ocultar Informes y Auditoría para el rol EMPRESA (R-14: solo ADMINISTRADOR y DIRECTIVO)
    if (esEmpresaExclusivo) {
        document.getElementById("itemNavInformes")?.classList.add("d-none");
        document
            .getElementById("itemNavInformesMovil")
            ?.classList.add("d-none");
        document
            .querySelectorAll(".acceso-informes")
            .forEach((b) => b.classList.add("d-none"));
        document.getElementById("itemNavAuditLog")?.classList.add("d-none");
        document
            .getElementById("itemNavAuditLogMovil")
            ?.classList.add("d-none");
        document
            .getElementById("itemNavInfraestructura")
            ?.classList.add("d-none");
        document
            .getElementById("itemNavInfraestructuraMovil")
            ?.classList.add("d-none");
        document.getElementById("itemNavMonitor")?.classList.add("d-none");
        document.getElementById("itemNavMonitorMovil")?.classList.add("d-none");
        document.getElementById("itemNavProyectos")?.classList.add("d-none");
        document
            .getElementById("itemNavProyectosMovil")
            ?.classList.add("d-none");
        document.getElementById("itemNavDashboard")?.classList.add("d-none");
        document
            .getElementById("itemNavDashboardMovil")
            ?.classList.add("d-none");
    }

    if (esEmpresaExclusivo) {
        ocultarAccesosEmpresaRestringidos();
        document.getElementById("btnNuevaEmpresa")?.classList.add("d-none");
        document.getElementById("btnNuevoLote")?.classList.add("d-none");
    }

    // SECRETARIO: acceso completo al sistema excepto Dashboard, Monitor y AuditLog
    const esSecretario =
        Autenticacion.tieneAcceso(["SECRETARIO"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);
    if (esSecretario) {
        [
            "itemNavDashboard",
            "itemNavDashboardMovil",
            "itemNavMonitor",
            "itemNavMonitorMovil",
            "itemNavAuditLog",
            "itemNavAuditLogMovil",
        ].forEach((id) => document.getElementById(id)?.classList.add("d-none"));
        document
            .getElementById("nav-panel")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
    }

    // AUDITOR: solo accede al registro de auditoría
    const esAuditor =
        Autenticacion.tieneAcceso(["AUDITOR"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);
    if (esAuditor) {
        [
            "itemNavInformes",
            "itemNavInformesMovil",
            "itemNavCenso",
            "itemNavCensoMovil",
            "itemNavInfraestructura",
            "itemNavInfraestructuraMovil",
            "itemNavDashboard",
            "itemNavDashboardMovil",
            "itemNavMonitor",
            "itemNavMonitorMovil",
            "itemNavCambioRubro",
            "itemNavCambioRubroMovil",
            "itemNavMensajeria",
            "itemNavMensajeriaMovil",
            "itemNavUsuarios",
            "itemNavUsuariosMovil",
            "itemNavProyectos",
            "itemNavProyectosMovil",
            "itemNavMapa",
            "itemNavMapaMovil",
        ].forEach((id) => document.getElementById(id)?.classList.add("d-none"));
        document
            .getElementById("nav-panel")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-empresas")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-lotes")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-radicaciones")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
    }

    // TECNICO: gestiona hitos de proyectos productivos y puede usar mensajería
    const esTecnico =
        Autenticacion.tieneAcceso(["TECNICO"]) &&
        !Autenticacion.tieneAcceso(["ADMINISTRADOR", "DIRECTIVO"]);
    if (esTecnico) {
        [
            "itemNavInformes",
            "itemNavInformesMovil",
            "itemNavCenso",
            "itemNavCensoMovil",
            "itemNavAuditLog",
            "itemNavAuditLogMovil",
            "itemNavInfraestructura",
            "itemNavInfraestructuraMovil",
            "itemNavDashboard",
            "itemNavDashboardMovil",
            "itemNavMonitor",
            "itemNavMonitorMovil",
            "itemNavCambioRubro",
            "itemNavCambioRubroMovil",
            "itemNavUsuarios",
            "itemNavUsuariosMovil",
            "itemNavMapa",
            "itemNavMapaMovil",
        ].forEach((id) => document.getElementById(id)?.classList.add("d-none"));
        document
            .getElementById("nav-panel")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-empresas")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-lotes")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
        document
            .getElementById("nav-radicaciones")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
    }

    // Alertas de vencimiento del ciclo de vida del lote (solo roles con acceso a lotes)
    if (
        Autenticacion.tieneAcceso(["ADMINISTRADOR", "SECRETARIO", "DIRECTIVO"])
    ) {
        ModuloCicloLote.cargarAlertas?.();
    }

    // Botón cerrar sesión
    document
        .getElementById("btnCerrarSesion")
        .addEventListener("click", Autenticacion.cerrarSesion);

    // Indicador de mensajes no leídos en el menú principal
    ModuloMensajeria.actualizarIndicadorNav?.();
    setInterval(() => ModuloMensajeria.actualizarIndicadorNav?.(), 60000);

    // Cambiar mi propia contraseña
    document
        .getElementById("formMiClave")
        .addEventListener("submit", async (e) => {
            e.preventDefault();
            const claveActual =
                document.getElementById("campoMiClaveActual").value;
            const claveNueva =
                document.getElementById("campoMiClaveNueva").value;
            const claveConfirm = document.getElementById(
                "campoMiClaveConfirm",
            ).value;

            if (claveNueva !== claveConfirm) {
                mostrarAlertaModal(
                    "alertaModalMiClave",
                    "Las contraseñas nuevas no coinciden.",
                );
                return;
            }

            const respuesta = await ApiCliente.parche(
                "/api/usuarios/mi-clave",
                { claveActual, claveNueva },
            );
            if (respuesta?.status === 204 || respuesta?.ok) {
                bootstrap.Modal.getInstance(
                    document.getElementById("modalMiClave"),
                )?.hide();
                mostrarAlerta(
                    "Contraseña actualizada. Inicie sesión nuevamente.",
                );
                setTimeout(Autenticacion.cerrarSesion, 2500);
            } else {
                mostrarAlertaModal(
                    "alertaModalMiClave",
                    "La contraseña actual es incorrecta.",
                );
            }
        });

    // Cambiar mis datos personales
    document
        .getElementById("formMiPerfil")
        .addEventListener("submit", async (e) => {
            e.preventDefault();
            const nombreCompleto = document
                .getElementById("campoMiNombreCompleto")
                .value.trim();
            const correoElectronico = document
                .getElementById("campoMiCorreoElectronico")
                .value.trim();

            if (!nombreCompleto) {
                mostrarAlertaModal(
                    "alertaModalMiPerfil",
                    "El nombre completo es obligatorio.",
                );
                return;
            }

            const respuesta = await ApiCliente.parche("/api/yo/perfil", {
                nombreCompleto,
                correoElectronico,
            });
            if (respuesta?.ok) {
                const sesionActualizada = {
                    ...sesion,
                    nombreCompleto,
                    correoElectronico,
                };
                sessionStorage.setItem(
                    "usuario",
                    JSON.stringify(sesionActualizada),
                );
                document.getElementById("nombreUsuarioNavbar").textContent =
                    nombreCompleto;
                bootstrap.Modal.getInstance(
                    document.getElementById("modalMiPerfil"),
                )?.hide();
                mostrarAlerta("Datos personales actualizados.");
            } else {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaModal(
                    "alertaModalMiPerfil",
                    error?.mensaje || "No se pudieron actualizar los datos.",
                );
            }
        });

    // Confirmación genérica de eliminación
    document
        .getElementById("btnConfirmarEliminar")
        .addEventListener("click", async function () {
            const id = parseInt(this.dataset.eliminarId);
            const tipo = this.dataset.eliminarTipo;
            bootstrap.Modal.getInstance(
                document.getElementById("modalConfirmacion"),
            )?.hide();

            switch (tipo) {
                case "empresa":
                    await ModuloEmpresas.eliminar(id);
                    break;
                case "lote":
                    await ModuloLotes.eliminar(id);
                    break;
                case "desactivarUsuario":
                    await ModuloUsuarios.cambiarActivacion(id, false);
                    break;
                case "activarUsuario":
                    await ModuloUsuarios.cambiarActivacion(id, true);
                    break;
                case "inactivarEmpresa":
                    await ModuloEmpresas.inactivar(id);
                    break;
                case "reactivarEmpresa":
                    await ModuloEmpresas.reactivar(id);
                    break;
            }
        });

    // Limpiar alertas de modales al cerrarse
    document
        .querySelectorAll(".modal")
        .forEach((modal) =>
            modal.addEventListener("hidden.bs.modal", () =>
                modal
                    .querySelectorAll(".alerta-modal")
                    .forEach((a) => a.classList.add("d-none")),
            ),
        );

    // Restricción para EMPRESA con empresa INACTIVA: solo mensajería
    let empresaInactiva = false;
    if (esEmpresaExclusivo && sesion.empresaId) {
        try {
            const respEmpresa = await ApiCliente.obtener(
                `/api/empresas/${sesion.empresaId}`,
            );
            if (respEmpresa?.ok) {
                const empData = await respEmpresa.json();
                empresaInactiva = empData.statusEmpresa === "INACTIVA";
            }
        } catch (_) {
            /* ignore */
        }
    }
    window._empresaInactiva = empresaInactiva;

    if (empresaInactiva) {
        [
            "itemNavCenso",
            "itemNavCensoMovil",
            "itemNavCambioRubro",
            "itemNavCambioRubroMovil",
            "itemNavMapa",
            "itemNavMapaMovil",
        ].forEach((id) => document.getElementById(id)?.classList.add("d-none"));
        document
            .getElementById("nav-radicaciones")
            ?.closest(".nav-item")
            ?.classList.add("d-none");
    }

    // Cargar sección inicial
    if (esEmpresaExclusivo && !sesion.empresaId) {
        ConfiguracionInicial.mostrar();
    } else if (esEmpresaExclusivo && empresaInactiva) {
        navegarA("mensajeria");
    } else if (esEmpresaExclusivo) {
        navegarA("empresas");
    } else if (esSecretario) {
        navegarA("radicaciones");
    } else if (esAuditor) {
        navegarA("audit-log");
    } else if (esTecnico) {
        navegarA("proyectos");
    } else {
        navegarA("panel");
    }
});

function ocultarAccesosEmpresaRestringidos() {
    document
        .getElementById("nav-panel")
        ?.closest(".nav-item")
        ?.classList.add("d-none");
    document
        .getElementById("nav-lotes")
        ?.closest(".nav-item")
        ?.classList.add("d-none");
    document.getElementById("itemNavUsuarios")?.classList.add("d-none");
    document.getElementById("itemNavUsuariosMovil")?.classList.add("d-none");
    document.getElementById("itemNavInformes")?.classList.add("d-none");
    document.getElementById("itemNavInformesMovil")?.classList.add("d-none");
    document.getElementById("itemNavAuditLog")?.classList.add("d-none");
    document.getElementById("itemNavAuditLogMovil")?.classList.add("d-none");
    document.getElementById("itemNavInfraestructura")?.classList.add("d-none");
    document
        .getElementById("itemNavInfraestructuraMovil")
        ?.classList.add("d-none");
    document.getElementById("itemNavMonitor")?.classList.add("d-none");
    document.getElementById("itemNavMonitorMovil")?.classList.add("d-none");
    document.getElementById("itemNavProyectos")?.classList.add("d-none");
    document.getElementById("itemNavProyectosMovil")?.classList.add("d-none");
    document.getElementById("itemNavDashboard")?.classList.add("d-none");
    document.getElementById("itemNavDashboardMovil")?.classList.add("d-none");

    document
        .querySelectorAll(
            "a.enlace-nav[onclick*='navegarA(\"panel\")'], a.enlace-nav[onclick*='navegarA(\"lotes\")'], a.enlace-nav[onclick*=\"navegarA('panel')\"], a.enlace-nav[onclick*=\"navegarA('lotes')\"]",
        )
        .forEach((el) => el.closest(".nav-item")?.classList.add("d-none"));
}
