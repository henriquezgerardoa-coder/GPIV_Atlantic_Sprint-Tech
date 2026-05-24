/* ═══════════════════════════════════════════════════
   Configuración inicial (primer ingreso EMPRESA sin empresa)
═══════════════════════════════════════════════════ */

const ConfiguracionInicial = (() => {
    let _empresaSeleccionadaId = null;
    let _modal = null;
    let _todasLasEmpresas = [];

    function mostrar() {
        _modal = new bootstrap.Modal(document.getElementById('modalConfiguracionInicial'));
        _modal.show();
        document.getElementById('tabsConfigInicial').addEventListener('shown.bs.tab', e => {
            if (e.target.dataset.bsTarget === '#panel-empresa-existente-ci') {
                _cargarEmpresas();
            }
        });
    }

    async function _cargarEmpresas() {
        _empresaSeleccionadaId = null;
        _todasLasEmpresas = [];
        document.getElementById('campoBusquedaEmpresaCI').value = '';
        const contenedor = document.getElementById('resultadosBusquedaEmpresaCI');
        contenedor.innerHTML = '<p class="text-muted small text-center"><span class="spinner-border spinner-border-sm me-2"></span>Cargando empresas...</p>';
        const respuesta = await ApiCliente.obtener('/api/empresas');
        if (!respuesta?.ok) {
            contenedor.innerHTML = '<p class="text-danger small">Error al cargar las empresas. Intente nuevamente.</p>';
            return;
        }
        _todasLasEmpresas = await respuesta.json();
        _renderizarEmpresas(_todasLasEmpresas);
    }

    function _renderizarEmpresas(empresas) {
        const contenedor = document.getElementById('resultadosBusquedaEmpresaCI');
        if (!empresas.length) {
            contenedor.innerHTML = '<p class="text-muted small text-center">No hay empresas registradas.</p>';
            return;
        }
        contenedor.innerHTML = '<div class="list-group">' + empresas.map(e => `
            <div class="list-group-item d-flex justify-content-between align-items-center${_empresaSeleccionadaId === e.id ? ' active' : ''}" id="itemEmpresaCI-${e.id}">
                <div>
                    <strong>${e.nombre}</strong>
                    <small class="d-block text-muted">${e.razonSocial} — CUIT: ${e.cuit}</small>
                </div>
                <button type="button" class="btn btn-outline-primary btn-sm flex-shrink-0 ms-2"
                        onclick="ConfiguracionInicial.seleccionarEmpresa(${e.id})">
                    Seleccionar
                </button>
            </div>`).join('') + '</div>';
    }

    function filtrarEmpresas() {
        const q = document.getElementById('campoBusquedaEmpresaCI').value.trim().toLowerCase();
        const filtradas = q
            ? _todasLasEmpresas.filter(e =>
                e.nombre.toLowerCase().includes(q) ||
                e.razonSocial.toLowerCase().includes(q) ||
                e.cuit.toLowerCase().includes(q))
            : _todasLasEmpresas;
        _renderizarEmpresas(filtradas);
    }

    function seleccionarEmpresa(id) {
        _empresaSeleccionadaId = id;
        document.querySelectorAll('#resultadosBusquedaEmpresaCI .list-group-item').forEach(el => el.classList.remove('active'));
        document.getElementById('itemEmpresaCI-' + id)?.classList.add('active');
        ocultarAlertaModal('alertaConfiguracionInicial');
    }

    async function confirmar() {
        ocultarAlertaModal('alertaConfiguracionInicial');
        const tabActiva = document.querySelector('#tabsConfigInicial .nav-link.active')?.dataset.bsTarget;
        if (tabActiva === '#panel-nueva-empresa-ci') {
            await _crearNuevaEmpresa();
        } else {
            await _vincularEmpresaExistente();
        }
    }

    async function _crearNuevaEmpresa() {
        const campos = {
            nombre:             document.getElementById('ciNombre').value.trim(),
            razonSocial:        document.getElementById('ciRazonSocial').value.trim(),
            cuit:               document.getElementById('ciCuit').value.trim(),
            direccion:          document.getElementById('ciDireccion').value.trim(),
            actividadEconomica: document.getElementById('ciActividad').value.trim(),
            correoElectronico:  document.getElementById('ciCorreo').value.trim(),
            telefono:           document.getElementById('ciTelefono').value.trim() || null,
        };
        if (!campos.nombre || !campos.razonSocial || !campos.cuit ||
            !campos.direccion || !campos.actividadEconomica || !campos.correoElectronico) {
            mostrarAlertaModal('alertaConfiguracionInicial', 'Completá todos los campos obligatorios.');
            return;
        }
        const btn = document.getElementById('btnConfirmarConfiguracionInicial');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Guardando...';
        const respuesta = await ApiCliente.crear('/api/empresas', campos);
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i>Confirmar y continuar';
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaConfiguracionInicial', error?.mensaje || 'No se pudo crear la empresa. Verificá los datos.');
            return;
        }
        await _completarSesion();
    }

    async function _vincularEmpresaExistente() {
        if (!_empresaSeleccionadaId) {
            mostrarAlertaModal('alertaConfiguracionInicial', 'Seleccioná una empresa de la lista.');
            return;
        }
        const btn = document.getElementById('btnConfirmarConfiguracionInicial');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Vinculando...';
        const respuesta = await ApiCliente.parche('/api/yo/empresa', { empresaId: _empresaSeleccionadaId });
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i>Confirmar y continuar';
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaConfiguracionInicial', error?.mensaje || 'No se pudo vincular la empresa.');
            return;
        }
        await _completarSesion();
    }

    async function _completarSesion() {
        const respYo = await ApiCliente.obtener('/api/yo');
        if (respYo?.ok) {
            const datosActualizados = await respYo.json();
            sessionStorage.setItem('usuario', JSON.stringify(datosActualizados));
        }
        _modal?.hide();
        navegarA('empresas');
    }

    return { mostrar, filtrarEmpresas, seleccionarEmpresa, confirmar };
})();

/* ═══════════════════════════════════════════════════
   Utilidades globales (usadas por los módulos)
═══════════════════════════════════════════════════ */

function mostrarAlerta(mensaje, tipo = 'success') {
    const contenedor = document.getElementById('contenedorAlertas');
    if (!contenedor) return;
    const id   = 'alerta_' + Date.now();
    const icono = tipo === 'success' ? 'check-circle-fill' : 'exclamation-triangle-fill';
    contenedor.insertAdjacentHTML('beforeend', `
        <div id="${id}" class="alert alert-${tipo} alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-${icono} me-2"></i>${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`);
    setTimeout(() => document.getElementById(id)?.remove(), 5000);
}

function mostrarAlertaModal(idElemento, mensaje) {
    const el = document.getElementById(idElemento);
    if (el) { el.textContent = mensaje; el.classList.remove('d-none'); }
}

function ocultarAlertaModal(idElemento) {
    document.getElementById(idElemento)?.classList.add('d-none');
}

/* ═══════════════════════════════════════════════════
   Navegación entre secciones
═══════════════════════════════════════════════════ */

function navegarA(seccion) {
    const esEmpresaExclusivo = Autenticacion.tieneAcceso(['EMPRESA'])
        && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
    const seccionesRestringidasEmpresa = new Set(['proyectos', 'infraestructura', 'dashboard', 'monitor', 'censo']);
    if (esEmpresaExclusivo && seccionesRestringidasEmpresa.has(seccion)) {
        mostrarAlerta('No tienes acceso a esta sección.', 'danger');
        seccion = 'empresas';
    }

    document.querySelectorAll('.seccion-contenido').forEach(s => s.classList.add('d-none'));
    document.querySelectorAll('.enlace-nav').forEach(a => a.classList.remove('activo'));

    const elSeccion = document.getElementById('seccion-' + seccion);
    const elNav     = document.getElementById('nav-' + seccion);
    elSeccion?.classList.remove('d-none');
    elNav?.classList.add('activo');

    // Cerrar sidebar en móvil tras navegar
    const offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('offcanvasSidebar'));
    offcanvas?.hide();

    switch (seccion) {
        case 'panel':      cargarPanel();                break;
        case 'empresas':   ModuloEmpresas.cargar();      break;
        case 'lotes':      ModuloLotes.cargar();         break;
        case 'radicaciones': ModuloRadicaciones.cargar(); break;
        case 'mensajeria': ModuloMensajeria.cargar();     break;
        case 'informes':   ModuloEstadisticas.cargar();   break;
        case 'usuarios':   ModuloUsuarios.cargar();       break;
        case 'audit-log':  ModuloAuditLog.cargar();       break;
        case 'censo':         ModuloCenso.cargar();          break;
        case 'cambio-rubro':  ModuloCambioRubro.cargar();    break;
        case 'proyectos':     ModuloProyectos.cargar();      break;
        case 'infraestructura': ModuloInfraestructura.cargar(); break;
        case 'dashboard':       ModuloDashboard.cargar();       break;
        case 'monitor':       ModuloMonitor.cargar();        break;
    }
}

async function cargarPanel() {
    try {
        const resp = await ApiCliente.obtener('/api/estadisticas/resumen');
        if (!resp?.ok) return;
        const datos = await resp.json();
        document.getElementById('statEmpresas').textContent      = datos.totalEmpresas;
        document.getElementById('statLotes').textContent         = datos.totalLotes;
        document.getElementById('statLotesOcupados').textContent = datos.lotesOcupados;
        document.getElementById('statLotesLibres').textContent   = datos.lotesDisponibles;
    } catch (err) {
        console.error('Error al cargar el panel:', err);
    }
}/* ═══════════════════════════════════════════════════
   Inicialización al cargar la página
═══════════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
    if (!Autenticacion.verificarYRedirigir()) return;

    const sesion = Autenticacion.obtenerSesion();

    // Mostrar información del usuario en la barra superior
    document.getElementById('nombreUsuarioNavbar').textContent = sesion.nombreCompleto || sesion.nombreUsuario;
    document.getElementById('rolesUsuarioNavbar').textContent  = sesion.roles.join(', ');

    document.getElementById('campoMiNombreCompleto').value = sesion.nombreCompleto || '';
    document.getElementById('campoMiCorreoElectronico').value = sesion.correoElectronico || '';

    // Ocultar la sección Usuarios si no es administrador
    if (!Autenticacion.esAdministrador()) {
        document.getElementById('itemNavUsuarios')?.classList.add('d-none');
        document.getElementById('itemNavUsuariosMovil')?.classList.add('d-none');
    }

    const esEmpresaExclusivo = Autenticacion.tieneAcceso(['EMPRESA'])
        && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);

    // Ocultar Informes y Auditoría para el rol EMPRESA (R-14: solo ADMINISTRADOR y DIRECTIVO)
    if (esEmpresaExclusivo) {
        document.getElementById('itemNavInformes')?.classList.add('d-none');
        document.getElementById('itemNavInformesMovil')?.classList.add('d-none');
        document.querySelectorAll('.acceso-informes').forEach(b => b.classList.add('d-none'));
        document.getElementById('itemNavCenso')?.classList.add('d-none');
        document.getElementById('itemNavCensoMovil')?.classList.add('d-none');
        document.getElementById('itemNavAuditLog')?.classList.add('d-none');
        document.getElementById('itemNavAuditLogMovil')?.classList.add('d-none');
        document.getElementById('itemNavInfraestructura')?.classList.add('d-none');
        document.getElementById('itemNavInfraestructuraMovil')?.classList.add('d-none');
        document.getElementById('itemNavMonitor')?.classList.add('d-none');
        document.getElementById('itemNavMonitorMovil')?.classList.add('d-none');
        document.getElementById('itemNavProyectos')?.classList.add('d-none');
        document.getElementById('itemNavProyectosMovil')?.classList.add('d-none');
        document.getElementById('itemNavDashboard')?.classList.add('d-none');
        document.getElementById('itemNavDashboardMovil')?.classList.add('d-none');
    }

    if (esEmpresaExclusivo) {
        ocultarAccesosEmpresaRestringidos();
        document.getElementById('btnNuevaEmpresa')?.classList.add('d-none');
        document.getElementById('btnNuevoLote')?.classList.add('d-none');
    }

    // Botón cerrar sesión
    document.getElementById('btnCerrarSesion').addEventListener('click', Autenticacion.cerrarSesion);

    // Indicador de mensajes no leídos en el menú principal
    ModuloMensajeria.actualizarIndicadorNav?.();
    setInterval(() => ModuloMensajeria.actualizarIndicadorNav?.(), 60000);

    // Cambiar mi propia contraseña
    document.getElementById('formMiClave').addEventListener('submit', async (e) => {
        e.preventDefault();
        const claveActual  = document.getElementById('campoMiClaveActual').value;
        const claveNueva   = document.getElementById('campoMiClaveNueva').value;
        const claveConfirm = document.getElementById('campoMiClaveConfirm').value;

        if (claveNueva !== claveConfirm) {
            mostrarAlertaModal('alertaModalMiClave', 'Las contraseñas nuevas no coinciden.');
            return;
        }

        const respuesta = await ApiCliente.parche('/api/usuarios/mi-clave', { claveActual, claveNueva });
        if (respuesta?.status === 204 || respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalMiClave'))?.hide();
            mostrarAlerta('Contraseña actualizada. Inicie sesión nuevamente.');
            setTimeout(Autenticacion.cerrarSesion, 2500);
        } else {
            mostrarAlertaModal('alertaModalMiClave', 'La contraseña actual es incorrecta.');
        }
    });

    // Cambiar mis datos personales
    document.getElementById('formMiPerfil').addEventListener('submit', async (e) => {
        e.preventDefault();
        const nombreCompleto = document.getElementById('campoMiNombreCompleto').value.trim();
        const correoElectronico = document.getElementById('campoMiCorreoElectronico').value.trim();

        if (!nombreCompleto) {
            mostrarAlertaModal('alertaModalMiPerfil', 'El nombre completo es obligatorio.');
            return;
        }

        const respuesta = await ApiCliente.parche('/api/yo/perfil', { nombreCompleto, correoElectronico });
        if (respuesta?.ok) {
            const sesionActualizada = { ...sesion, nombreCompleto, correoElectronico };
            sessionStorage.setItem('usuario', JSON.stringify(sesionActualizada));
            document.getElementById('nombreUsuarioNavbar').textContent = nombreCompleto;
            bootstrap.Modal.getInstance(document.getElementById('modalMiPerfil'))?.hide();
            mostrarAlerta('Datos personales actualizados.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalMiPerfil', error?.mensaje || 'No se pudieron actualizar los datos.');
        }
    });

    // Confirmación genérica de eliminación
    document.getElementById('btnConfirmarEliminar').addEventListener('click', async function () {
        const id   = parseInt(this.dataset.eliminarId);
        const tipo = this.dataset.eliminarTipo;
        bootstrap.Modal.getInstance(document.getElementById('modalConfirmacion'))?.hide();

        switch (tipo) {
            case 'empresa': await ModuloEmpresas.eliminar(id); break;
            case 'lote':    await ModuloLotes.eliminar(id);    break;
            case 'usuario': await ModuloUsuarios.eliminar(id); break;
        }
    });

    // Limpiar alertas de modales al cerrarse
    document.querySelectorAll('.modal').forEach(modal =>
        modal.addEventListener('hidden.bs.modal', () =>
            modal.querySelectorAll('.alerta-modal').forEach(a => a.classList.add('d-none'))
        )
    );

    // Cargar sección inicial
    if (esEmpresaExclusivo && !sesion.empresaId) {
        ConfiguracionInicial.mostrar();
    } else if (esEmpresaExclusivo) {
        navegarA('empresas');
    } else {
        navegarA('panel');
    }
});

function ocultarAccesosEmpresaRestringidos() {
    document.getElementById('nav-panel')?.closest('.nav-item')?.classList.add('d-none');
    document.getElementById('nav-lotes')?.closest('.nav-item')?.classList.add('d-none');
    document.getElementById('itemNavUsuarios')?.classList.add('d-none');
    document.getElementById('itemNavUsuariosMovil')?.classList.add('d-none');
    document.getElementById('itemNavInformes')?.classList.add('d-none');
    document.getElementById('itemNavInformesMovil')?.classList.add('d-none');
    document.getElementById('itemNavCenso')?.classList.add('d-none');
    document.getElementById('itemNavCensoMovil')?.classList.add('d-none');
    document.getElementById('itemNavAuditLog')?.classList.add('d-none');
    document.getElementById('itemNavAuditLogMovil')?.classList.add('d-none');
    document.getElementById('itemNavInfraestructura')?.classList.add('d-none');
    document.getElementById('itemNavInfraestructuraMovil')?.classList.add('d-none');
    document.getElementById('itemNavMonitor')?.classList.add('d-none');
    document.getElementById('itemNavMonitorMovil')?.classList.add('d-none');
    document.getElementById('itemNavProyectos')?.classList.add('d-none');
    document.getElementById('itemNavProyectosMovil')?.classList.add('d-none');
    document.getElementById('itemNavDashboard')?.classList.add('d-none');
    document.getElementById('itemNavDashboardMovil')?.classList.add('d-none');

    document.querySelectorAll("a.enlace-nav[onclick*='navegarA(\"panel\")'], a.enlace-nav[onclick*='navegarA(\"lotes\")'], a.enlace-nav[onclick*=\"navegarA('panel')\"], a.enlace-nav[onclick*=\"navegarA('lotes')\"]")
        .forEach(el => el.closest('.nav-item')?.classList.add('d-none'));
}

