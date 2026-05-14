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
        case 'informes':   ModuloEstadisticas.cargar();  break;
        case 'usuarios':   ModuloUsuarios.cargar();      break;
    }
}

async function cargarPanel() {
    try {
        const [respEmpresas, respLotes] = await Promise.all([
            ApiCliente.obtener('/api/empresas'),
            ApiCliente.obtener('/api/lotes')
        ]);
        const empresas  = respEmpresas?.ok ? await respEmpresas.json() : [];
        const lotes     = respLotes?.ok    ? await respLotes.json()    : [];
        const ocupados  = lotes.filter(l => l.ocupado).length;

        document.getElementById('statEmpresas').textContent      = empresas.length;
        document.getElementById('statLotes').textContent         = lotes.length;
        document.getElementById('statLotesOcupados').textContent = ocupados;
        document.getElementById('statLotesLibres').textContent   = lotes.length - ocupados;
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

    // Ocultar Informes para el rol EMPRESA (R-14: solo ADMINISTRADOR y DIRECTIVO)
    if (Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])) {
        document.getElementById('itemNavInformes')?.classList.add('d-none');
        document.getElementById('itemNavInformesMovil')?.classList.add('d-none');
        document.querySelectorAll('.acceso-informes').forEach(b => b.classList.add('d-none'));
    }

    if (Autenticacion.tieneAcceso(['EMPRESA'])) {
        ocultarAccesosEmpresaRestringidos();
        document.getElementById('btnNuevaEmpresa')?.classList.add('d-none');
        document.getElementById('btnNuevoLote')?.classList.add('d-none');
    }

    // Botón cerrar sesión
    document.getElementById('btnCerrarSesion').addEventListener('click', Autenticacion.cerrarSesion);

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
    if (Autenticacion.tieneAcceso(['EMPRESA'])) {
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

    document.querySelectorAll("a.enlace-nav[onclick*='navegarA(\"panel\")'], a.enlace-nav[onclick*='navegarA(\"lotes\")'], a.enlace-nav[onclick*=\"navegarA('panel')\"], a.enlace-nav[onclick*=\"navegarA('lotes')\"]")
        .forEach(el => el.closest('.nav-item')?.classList.add('d-none'));
}

