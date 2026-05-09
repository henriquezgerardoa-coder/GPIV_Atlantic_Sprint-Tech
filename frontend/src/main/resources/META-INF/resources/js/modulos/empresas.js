const ModuloEmpresas = (() => {
    let empresas = [];
    let modoEdicion = false;
    let idEdicion = null;
    let detalleActual = null;

    function esDirectivoSoloLectura() {
        return Autenticacion.tieneRol('DIRECTIVO');
    }

    function esAdmin() {
        return Autenticacion.tieneRol('ADMINISTRADOR');
    }

    async function cargar() {
        actualizarIndicadorSoloLectura();
        if (esDirectivoSoloLectura() || esAdmin()) {
            await cargarVistaAdmin();
            return;
        }
        mostrarPanelAdmin(false);
        const respuesta = await ApiCliente.obtener('/api/empresas');
        if (!respuesta?.ok) { mostrarAlerta('Error al cargar empresas.', 'danger'); return; }
        empresas = await respuesta.json();
        renderizarTabla();
        poblarSelectorServiciosPost();
    }

    function actualizarIndicadorSoloLectura() {
        const indicador = document.getElementById('indicadorModoSoloLecturaEmpresas');
        if (!indicador) {
            return;
        }
        indicador.classList.toggle('d-none', !esDirectivoSoloLectura());
    }

    function mostrarPanelAdmin(mostrar) {
        document.getElementById('panelEmpresasAdminVisualizacion')?.classList.toggle('d-none', !mostrar);
        document.getElementById('panelEmpresasGestion')?.classList.toggle('d-none', mostrar);
        // Solo ocultar "Nueva empresa" para DIRECTIVO; el ADMIN siempre puede crear
        if (mostrar && esDirectivoSoloLectura()) {
            document.getElementById('btnNuevaEmpresa')?.classList.add('d-none');
        } else if (!mostrar) {
            document.getElementById('btnNuevaEmpresa')?.classList.remove('d-none');
        }
    }

    async function cargarVistaAdmin() {
        mostrarPanelAdmin(true);
        const estado = document.getElementById('estadoCargaEmpresasAdmin');
        const error = document.getElementById('errorEmpresasAdmin');
        const vacio = document.getElementById('sinEmpresasAdmin');
        const lista = document.getElementById('listaEmpresasAdmin');
        const badge = document.getElementById('badgeTotalEmpresasAdmin');
        if (!lista || !badge) {
            return;
        }
        estado?.classList.remove('d-none');
        error?.classList.add('d-none');
        vacio?.classList.add('d-none');
        lista.innerHTML = '';
        let respuesta = await ApiCliente.obtener('/api/empresas/admin/vista');
        estado?.classList.add('d-none');
        if (!respuesta?.ok) {
            // Compatibilidad: si el endpoint nuevo no esta disponible, usar listado general.
            const respuestaFallback = await ApiCliente.obtener('/api/empresas');
            if (!respuestaFallback?.ok) {
                const errorJson = await respuesta?.json().catch(() => ({}));
                error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el panel de empresas.';
                error.classList.remove('d-none');
                badge.textContent = 'Total: 0';
                return;
            }
            respuesta = respuestaFallback;
        }
        const listado = (await respuesta.json())
            .map(item => ({ id: item.id, nombre: item.nombre }))
            .sort((a, b) => (a.nombre || '').localeCompare(b.nombre || '', 'es', { sensitivity: 'base' }));
        badge.textContent = `Total: ${listado.length}`;
        if (!listado.length) {
            vacio?.classList.remove('d-none');
            return;
        }
        lista.innerHTML = listado.map(item => `
            <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                    onclick="ModuloEmpresas.verDetalleAdmin(${item.id})">
                <span>${item.nombre}</span>
                <i class="bi bi-chevron-right text-muted"></i>
            </button>
        `).join('');
    }

    async function verDetalleAdmin(idEmpresa) {
        const modalElemento = asegurarModalDetalleAdmin();
        if (!modalElemento || !window.bootstrap?.Modal) {
            mostrarAlerta('No se pudo abrir el detalle de empresa. Recargue la página e intente nuevamente.', 'danger');
            return;
        }
        detalleActual = null;
        const modal = bootstrap.Modal.getOrCreateInstance(modalElemento);
        const estado = document.getElementById('estadoCargaDetalleEmpresaAdmin');
        const error = document.getElementById('errorDetalleEmpresaAdmin');
        const contenido = document.getElementById('contenidoDetalleEmpresaAdmin');
        const accionesAdmin = document.getElementById('accionesAdminEmpresaDet');
        estado?.classList.remove('d-none');
        error?.classList.add('d-none');
        contenido?.classList.add('d-none');
        accionesAdmin?.classList.add('d-none');
        modal.show();

        if (esDirectivoSoloLectura()) {
            const respuestaDirectivo = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            estado?.classList.add('d-none');
            if (!respuestaDirectivo?.ok) {
                const errorJson = await respuestaDirectivo?.json().catch(() => ({}));
                error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el detalle de la empresa.';
                error.classList.remove('d-none');
                return;
            }
            detalleActual = construirDetalleDesdeEmpresaBasica(await respuestaDirectivo.json());
            renderizarDetalleAdmin(detalleActual);
            contenido?.classList.remove('d-none');
            return;
        }

        let respuesta = await ApiCliente.obtener(`/api/empresas/admin/vista/${idEmpresa}`);
        estado?.classList.add('d-none');
        if (!respuesta?.ok) {
            const respuestaFallback = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            if (respuestaFallback?.ok) {
                detalleActual = construirDetalleDesdeEmpresaBasica(await respuestaFallback.json());
                renderizarDetalleAdmin(detalleActual);
                contenido?.classList.remove('d-none');
                if (esAdmin()) accionesAdmin?.classList.remove('d-none');
                return;
            }
            const errorJson = await respuestaFallback?.json().catch(() => ({}));
            error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el detalle de la empresa.';
            error.classList.remove('d-none');
            return;
        }
        detalleActual = await respuesta.json();
        renderizarDetalleAdmin(detalleActual);
        contenido?.classList.remove('d-none');
        if (esAdmin()) accionesAdmin?.classList.remove('d-none');
    }

    function construirDetalleDesdeEmpresaBasica(empresa) {
        return {
            id: empresa?.id,
            nombre: empresa?.nombre,
            razonSocial: empresa?.razonSocial,
            nit: empresa?.nit,
            cuit: empresa?.cuit,
            telefono: empresa?.telefono,
            direccion: empresa?.direccion,
            fechaRegistro: null,
            statusEmpresa: null,
            actividadEconomica: empresa?.actividadEconomica,
            correoElectronico: empresa?.correoElectronico,
            totalEmpleados: empresa?.cantidadEmpleados ?? 0,
            totalVehiculos: 0,
            estadoExpediente: null,
            usuariosAsociados: [],
            vehiculos: [],
            radicaciones: []
        };
    }

    function renderizarDetalleAdmin(detalle) {
        // Encabezado del modal
        setTexto('detEmpresaTituloModal', detalle?.nombre || 'Detalle de empresa');
        setTexto('detEmpresaSubtituloModal', detalle?.cuit ? `CUIT ${detalle.cuit}` : '');

        // Tab: Datos generales
        setTexto('detEmpresaNombre', detalle?.nombre || '-');
        setTexto('detEmpresaRazonSocial', detalle?.razonSocial || '-');
        setTexto('detEmpresaNit', detalle?.nit || '-');
        setTexto('detEmpresaCuit', detalle?.cuit || '-');
        setTexto('detEmpresaTelefono', detalle?.telefono || '-');
        setTexto('detEmpresaCorreo', detalle?.correoElectronico || '-');
        setTexto('detEmpresaDireccion', detalle?.direccion || '-');
        setTexto('detEmpresaFechaRegistro', formatearFechaHora(detalle?.fechaRegistro));
        setTexto('detEmpresaActividad', detalle?.actividadEconomica || '-');

        const elStatus = document.getElementById('detEmpresaStatus');
        if (elStatus) {
            elStatus.textContent = detalle?.statusEmpresa || '-';
            elStatus.className = `badge ${detalle?.statusEmpresa === 'ACTIVA' ? 'bg-success' : 'bg-secondary'}`;
        }
        const elEstado = document.getElementById('detEmpresaEstado');
        if (elEstado) {
            elEstado.textContent = formatearEstadoRadicacion(detalle?.estadoExpediente);
            elEstado.className = `badge ${colorEstadoRadicacion(detalle?.estadoExpediente)}`;
        }

        // Tab: Usuarios
        const usuarios = detalle?.usuariosAsociados || [];
        const badgeUsuarios = document.getElementById('badgeUsuariosEmpresaDet');
        if (badgeUsuarios) badgeUsuarios.textContent = usuarios.length;
        const listaUsuarios = document.getElementById('listaUsuariosEmpresaDet');
        if (listaUsuarios) {
            if (!usuarios.length) {
                listaUsuarios.innerHTML = '<p class="text-muted small">Sin usuarios asociados.</p>';
            } else {
                listaUsuarios.innerHTML = usuarios.map(u => `
                    <div class="card border mb-2">
                        <div class="card-body py-2 px-3">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <span class="fw-semibold">${u.nombreCompleto || '-'}</span>
                                    <small class="text-muted ms-2">@${u.nombreUsuario || '-'}</small>
                                </div>
                                <span class="badge ${u.activo ? 'bg-success' : 'bg-danger'}">${u.activo ? 'Activo' : 'Inactivo'}</span>
                            </div>
                            <div class="small text-muted mt-1">
                                <i class="bi bi-envelope me-1"></i>${u.correoElectronico || '-'}
                                &nbsp;·&nbsp;
                                <i class="bi bi-shield me-1"></i>${(u.roles || []).join(', ')}
                                &nbsp;·&nbsp;
                                <i class="bi bi-clock me-1"></i>Último acceso: ${formatearFechaHora(u.fechaUltimoAcceso)}
                            </div>
                        </div>
                    </div>`).join('');
            }
        }

        // Tab: Solicitudes
        const radicaciones = detalle?.radicaciones || [];
        const badgeSolicitudes = document.getElementById('badgeSolicitudesEmpresaDet');
        if (badgeSolicitudes) badgeSolicitudes.textContent = radicaciones.length;
        const cuerpoSolicitudes = document.getElementById('cuerpoSolicitudesEmpresaDet');
        if (cuerpoSolicitudes) {
            if (!radicaciones.length) {
                cuerpoSolicitudes.innerHTML = '<tr><td colspan="6" class="text-muted text-center py-3">Sin solicitudes registradas</td></tr>';
            } else {
                cuerpoSolicitudes.innerHTML = radicaciones.map(r => `
                    <tr>
                        <td class="fw-semibold font-monospace small">${r.numeroRadicado || '-'}</td>
                        <td class="small">${r.tipoSolicitud || '-'}</td>
                        <td><span class="badge ${colorEstadoRadicacion(r.estado)}">${formatearEstadoRadicacion(r.estado)}</span></td>
                        <td class="small">${r.necesidadMetrosCuadrados ? r.necesidadMetrosCuadrados.toLocaleString('es-AR') + ' m²' : '-'}</td>
                        <td class="small">${r.fechaRadicacion || '-'}</td>
                        <td class="small">${(r.fechaUltimaActualizacion || '').replace('T', ' ').slice(0, 16)}</td>
                    </tr>`).join('');
            }
        }

        // Tab: Servicios
        setTexto('detTotalEmpleados', `${detalle?.totalEmpleados ?? 0}`);
        setTexto('detTotalVehiculos', `${detalle?.totalVehiculos ?? 0}`);
        const cuerpoVehiculos = document.getElementById('cuerpoVehiculosEmpresaAdmin');
        if (cuerpoVehiculos) {
            const vehiculos = detalle?.vehiculos || [];
            cuerpoVehiculos.innerHTML = vehiculos.length
                ? vehiculos.map(v => `<tr><td>${v.placa || '-'}</td><td>${v.tipo || '-'}</td><td>${v.descripcion || '-'}</td></tr>`).join('')
                : '<tr><td colspan="3" class="text-muted">Sin vehículos registrados</td></tr>';
        }

        // Activar primer tab al abrir
        const primerTab = document.getElementById('tab-emp-datos');
        if (primerTab && window.bootstrap?.Tab) {
            bootstrap.Tab.getOrCreateInstance(primerTab).show();
        }
    }

    function formatearEstadoRadicacion(estado) {
        const m = {
            PENDIENTE: 'Pendiente', EN_REVISION: 'En revisión', APROBADA: 'Aprobada',
            RADICADA: 'Radicada', RECHAZADA: 'Rechazada',
            REQUIERE_INFORMACION_ADICIONAL: 'Requiere info. adicional', CANCELADA: 'Cancelada'
        };
        return m[estado] || estado || 'Sin expediente';
    }

    function colorEstadoRadicacion(estado) {
        const m = {
            PENDIENTE: 'bg-warning text-dark', EN_REVISION: 'bg-info text-dark',
            APROBADA: 'bg-primary', RADICADA: 'bg-success',
            RECHAZADA: 'bg-danger', CANCELADA: 'bg-secondary',
            REQUIERE_INFORMACION_ADICIONAL: 'bg-warning text-dark'
        };
        return m[estado] || 'bg-secondary';
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = valor;
        }
    }

    function editarDesdeDetalle() {
        if (!detalleActual?.id) return;
        bootstrap.Modal.getInstance(document.getElementById('modalDetalleEmpresaAdmin'))?.hide();
        modoEdicion = true;
        idEdicion = detalleActual.id;
        document.getElementById('tituloModalEmpresa').textContent = 'Editar empresa';
        document.getElementById('campoNombreEmpresa').value             = detalleActual.nombre || '';
        document.getElementById('campoRazonSocialEmpresa').value        = detalleActual.razonSocial || '';
        document.getElementById('campoNitEmpresa').value                = detalleActual.nit || '';
        document.getElementById('campoCuitEmpresa').value               = detalleActual.cuit || '';
        document.getElementById('campoDireccionEmpresa').value          = detalleActual.direccion || '';
        document.getElementById('campoActividadEconomicaEmpresa').value = detalleActual.actividadEconomica || '';
        document.getElementById('campoCorreoEmpresa').value             = detalleActual.correoElectronico || '';
        document.getElementById('campoTelefonoEmpresa').value           = detalleActual.telefono || '';
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function eliminarDesdeDetalle() {
        if (!detalleActual?.id) return;
        bootstrap.Modal.getInstance(document.getElementById('modalDetalleEmpresaAdmin'))?.hide();
        confirmarEliminacion(detalleActual.id, detalleActual.nombre || 'esta empresa');
    }

    function asegurarModalDetalleAdmin() {
        return document.getElementById('modalDetalleEmpresaAdmin');
    }

    function formatearFechaHora(valor) {
        if (!valor) {
            return 'No disponible';
        }
        const fecha = new Date(valor);
        if (Number.isNaN(fecha.getTime())) {
            return valor;
        }
        return fecha.toLocaleString();
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaEmpresas');
        if (!cuerpo) return;

        if (empresas.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin empresas registradas</td></tr>';
            return;
        }

        const puedeEditar = !esDirectivoSoloLectura() && Autenticacion.tieneAcceso(['ADMINISTRADOR', 'EMPRESA']);
        const puedeEliminar = !esDirectivoSoloLectura() && Autenticacion.tieneAcceso(['ADMINISTRADOR']);
        cuerpo.innerHTML = empresas.map(e => `
            <tr>
                <td class="text-muted">${e.id}</td>
                <td class="fw-semibold">${e.nombre}</td>
                <td>${e.razonSocial || '-'}</td>
                <td>${e.nit || '-'}</td>
                <td>${e.cuit}</td>
                <td>${e.correoElectronico}</td>
                <td class="text-center">
                    ${puedeEditar ? `
                    <button class="btn btn-sm btn-outline-primary me-1" title="Editar"
                            onclick="ModuloEmpresas.abrirEdicion(${e.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    ${puedeEliminar ? `<button class="btn btn-sm btn-outline-danger" title="Eliminar"
                            onclick="ModuloEmpresas.confirmarEliminacion(${e.id}, '${e.nombre.replace(/'/g, "\\'")}')">
                        <i class="bi bi-trash"></i>
                    </button>` : ''}` : '<span class="text-muted small">Sin permisos</span>'}
                </td>
            </tr>`).join('');
    }

    function abrirCreacion() {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO tiene permisos de solo lectura para empresas.', 'warning');
            return;
        }
        modoEdicion = false;
        idEdicion = null;
        document.getElementById('tituloModalEmpresa').textContent = 'Nueva empresa';
        document.getElementById('formularioEmpresa').reset();
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function abrirEdicion(id) {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO tiene permisos de solo lectura para empresas.', 'warning');
            return;
        }
        const empresa = empresas.find(e => e.id === id);
        if (!empresa) return;
        modoEdicion = true;
        idEdicion = id;
        document.getElementById('tituloModalEmpresa').textContent = 'Editar empresa';
        document.getElementById('campoNombreEmpresa').value = empresa.nombre;
        document.getElementById('campoRazonSocialEmpresa').value = empresa.razonSocial || '';
        document.getElementById('campoNitEmpresa').value = empresa.nit || '';
        document.getElementById('campoCuitEmpresa').value   = empresa.cuit;
        document.getElementById('campoDireccionEmpresa').value = empresa.direccion || '';
        document.getElementById('campoActividadEconomicaEmpresa').value = empresa.actividadEconomica || '';
        document.getElementById('campoCorreoEmpresa').value = empresa.correoElectronico;
        document.getElementById('campoTelefonoEmpresa').value = empresa.telefono || '';
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    async function guardar() {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO tiene permisos de solo lectura para empresas.', 'warning');
            return;
        }
        const datos = {
            nombre:            document.getElementById('campoNombreEmpresa').value.trim(),
            razonSocial:       document.getElementById('campoRazonSocialEmpresa').value.trim(),
            nit:               document.getElementById('campoNitEmpresa').value.trim(),
            cuit:              document.getElementById('campoCuitEmpresa').value.trim(),
            direccion:         document.getElementById('campoDireccionEmpresa').value.trim(),
            actividadEconomica: document.getElementById('campoActividadEconomicaEmpresa').value.trim(),
            correoElectronico: document.getElementById('campoCorreoEmpresa').value.trim(),
            telefono:          document.getElementById('campoTelefonoEmpresa').value.trim()
        };

        if (!datos.nombre || !datos.razonSocial || !datos.nit || !datos.cuit || !datos.direccion || !datos.actividadEconomica || !datos.correoElectronico) {
            mostrarAlertaModal('alertaModalEmpresa', 'Todos los campos son obligatorios.');
            return;
        }

        const respuesta = modoEdicion
            ? await ApiCliente.actualizar(`/api/empresas/${idEdicion}`, datos)
            : await ApiCliente.crear('/api/empresas', datos);

        if (respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalEmpresa'))?.hide();
            await cargar();
            mostrarAlerta(modoEdicion ? 'Empresa actualizada correctamente.' : 'Empresa creada correctamente.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalEmpresa', error?.message || 'Error al guardar. Verifique los datos ingresados.');
        }
    }

    function confirmarEliminacion(id, nombre) {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO tiene permisos de solo lectura para empresas.', 'warning');
            return;
        }
        document.getElementById('mensajeConfirmacionEliminar').textContent =
            `¿Está seguro que desea eliminar la empresa "${nombre}"? Esta acción no se puede deshacer.`;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarId   = id;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarTipo = 'empresa';
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConfirmacion')).show();
    }

    async function eliminar(id) {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO tiene permisos de solo lectura para empresas.', 'warning');
            return;
        }
        const respuesta = await ApiCliente.eliminar(`/api/empresas/${id}`);
        if (respuesta?.status === 204 || respuesta?.ok) {
            await cargar();
            mostrarAlerta('Empresa eliminada correctamente.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'No se pudo eliminar la empresa.', 'danger');
        }
    }

    function poblarSelectorServiciosPost() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        if (!selector) return;
        selector.innerHTML = empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    function parsearVehiculosDesdeTexto(texto) {
        return texto.split('\n')
            .map(l => l.trim())
            .filter(Boolean)
            .map(l => {
                const [placa, tipo, descripcion] = l.split('|').map(v => (v || '').trim());
                return { placa, tipo, descripcion };
            });
    }

    function serializarVehiculosEnTexto(vehiculos) {
        return (vehiculos || []).map(v => `${v.placa}|${v.tipo}|${v.descripcion || ''}`).join('\n');
    }

    async function cargarServiciosPostRadicacion() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        const empresaId = selector?.value;
        if (!empresaId) {
            mostrarAlerta('Seleccione una empresa.', 'danger');
            return;
        }

        const respuesta = await ApiCliente.obtener(`/api/empresas/${empresaId}/servicios-post-radicacion`);
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'La empresa aún no está habilitada para servicios post-radicación.', 'warning');
            return;
        }

        const datos = await respuesta.json();
        document.getElementById('campoCantidadEmpleadosPost').value = datos.cantidadEmpleados ?? 0;
        document.getElementById('campoVehiculosPost').value = serializarVehiculosEnTexto(datos.vehiculos);
        mostrarAlerta('Servicios post-radicación cargados correctamente.');
    }

    async function guardarServiciosPostRadicacion() {
        if (esDirectivoSoloLectura()) {
            mostrarAlerta('El rol DIRECTIVO no puede actualizar servicios post-radicación.', 'warning');
            return;
        }
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        const empresaId = selector?.value;
        if (!empresaId) {
            mostrarAlerta('Seleccione una empresa.', 'danger');
            return;
        }

        const cantidadEmpleados = Number.parseInt(document.getElementById('campoCantidadEmpleadosPost').value || '0', 10);
        const vehiculos = parsearVehiculosDesdeTexto(document.getElementById('campoVehiculosPost').value || '');
        if (vehiculos.some(v => !v.placa || !v.tipo)) {
            mostrarAlerta('Cada vehículo debe tener placa y tipo.', 'danger');
            return;
        }

        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaId}/servicios-post-radicacion`, {
            cantidadEmpleados,
            vehiculos
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'No se pudieron guardar los servicios post-radicación.', 'danger');
            return;
        }

        mostrarAlerta('Servicios post-radicación actualizados correctamente.');
        await cargar();
    }

    return {
        cargar,
        abrirCreacion,
        abrirEdicion,
        guardar,
        confirmarEliminacion,
        eliminar,
        verDetalleAdmin,
        editarDesdeDetalle,
        eliminarDesdeDetalle,
        cargarServiciosPostRadicacion,
        guardarServiciosPostRadicacion
    };
})();

