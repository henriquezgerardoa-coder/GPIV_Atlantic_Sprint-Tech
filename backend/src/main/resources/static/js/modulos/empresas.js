const ModuloEmpresas = (() => {
    let empresas = [];
    let modoEdicion = false;
    let idEdicion = null;

    async function cargar() {
        if (Autenticacion.tieneRol('DIRECTIVO')) {
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

    function mostrarPanelAdmin(mostrar) {
        document.getElementById('panelEmpresasAdminVisualizacion')?.classList.toggle('d-none', !mostrar);
        document.getElementById('panelEmpresasGestion')?.classList.toggle('d-none', mostrar);
        document.getElementById('btnNuevaEmpresa')?.classList.toggle('d-none', mostrar);
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
        const modal = bootstrap.Modal.getOrCreateInstance(modalElemento);
        const estado = document.getElementById('estadoCargaDetalleEmpresaAdmin');
        const error = document.getElementById('errorDetalleEmpresaAdmin');
        const contenido = document.getElementById('contenidoDetalleEmpresaAdmin');
        estado?.classList.remove('d-none');
        error?.classList.add('d-none');
        contenido?.classList.add('d-none');
        modal.show();

        // DIRECTIVO usa detalle de solo lectura desde endpoint base.
        if (Autenticacion.tieneRol('DIRECTIVO')) {
            const respuestaDirectivo = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            estado?.classList.add('d-none');
            if (!respuestaDirectivo?.ok) {
                const errorJson = await respuestaDirectivo?.json().catch(() => ({}));
                error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el detalle de la empresa.';
                error.classList.remove('d-none');
                return;
            }
            const detalleDirectivo = construirDetalleDesdeEmpresaBasica(await respuestaDirectivo.json());
            renderizarDetalleAdmin(detalleDirectivo);
            contenido?.classList.remove('d-none');
            return;
        }

        let respuesta = await ApiCliente.obtener(`/api/empresas/admin/vista/${idEmpresa}`);
        estado?.classList.add('d-none');
        if (!respuesta?.ok) {
            // Para DIRECTIVO (solo lectura) y compatibilidad, usar endpoint base cuando falle vista admin.
            const respuestaFallback = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            if (respuestaFallback?.ok) {
                const detalleFallback = construirDetalleDesdeEmpresaBasica(await respuestaFallback.json());
                renderizarDetalleAdmin(detalleFallback);
                contenido?.classList.remove('d-none');
                return;
            }
            respuesta = respuestaFallback;
            const errorJson = await respuesta?.json().catch(() => ({}));
            error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el detalle de la empresa.';
            error.classList.remove('d-none');
            return;
        }
        const detalle = await respuesta.json();
        renderizarDetalleAdmin(detalle);
        contenido?.classList.remove('d-none');
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
            usuarioAsociado: null,
            vehiculos: []
        };
    }

    function renderizarDetalleAdmin(detalle) {
        const usuario = detalle?.usuarioAsociado;
        setTexto('detEmpresaNombre', detalle?.nombre || '-');
        setTexto('detEmpresaRazonSocial', detalle?.razonSocial || '-');
        setTexto('detEmpresaNit', detalle?.nit || '-');
        setTexto('detEmpresaCuit', detalle?.cuit || '-');
        setTexto('detEmpresaTelefono', detalle?.telefono || '-');
        setTexto('detEmpresaCorreo', detalle?.correoElectronico || '-');
        setTexto('detEmpresaDireccion', detalle?.direccion || '-');
        setTexto('detEmpresaFechaRegistro', formatearFechaHora(detalle?.fechaRegistro));
        setTexto('detEmpresaStatus', detalle?.statusEmpresa || '-');
        setTexto('detEmpresaActividad', detalle?.actividadEconomica || '-');
        setTexto('detEmpresaEstado', detalle?.estadoExpediente || 'Sin expediente');

        setTexto('detUsuarioNombre', usuario?.nombreCompleto || 'Sin usuario asociado');
        setTexto('detUsuarioCorreo', usuario?.correoElectronico || '-');
        setTexto('detUsuarioRol', (usuario?.roles || []).join(', ') || '-');
        setTexto('detUsuarioUltimoAcceso', formatearFechaHora(usuario?.fechaUltimoAcceso));

        setTexto('detTotalEmpleados', `${detalle?.totalEmpleados ?? 0}`);
        setTexto('detTotalVehiculos', `${detalle?.totalVehiculos ?? 0}`);

        const cuerpoVehiculos = document.getElementById('cuerpoVehiculosEmpresaAdmin');
        const vehiculos = detalle?.vehiculos || [];
        if (!cuerpoVehiculos) {
            return;
        }
        if (!vehiculos.length) {
            cuerpoVehiculos.innerHTML = '<tr><td colspan="3" class="text-muted">Sin vehículos registrados</td></tr>';
            return;
        }
        cuerpoVehiculos.innerHTML = vehiculos.map(v => `
            <tr>
                <td>${v.placa || '-'}</td>
                <td>${v.tipo || '-'}</td>
                <td>${v.descripcion || '-'}</td>
            </tr>
        `).join('');
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = valor;
        }
    }

    function asegurarModalDetalleAdmin() {
        let modal = document.getElementById('modalDetalleEmpresaAdmin');
        if (modal) {
            return modal;
        }
        document.body.insertAdjacentHTML('beforeend', `
            <div class="modal fade" id="modalDetalleEmpresaAdmin" tabindex="-1">
                <div class="modal-dialog modal-lg modal-dialog-scrollable">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"><i class="bi bi-building me-2 text-primary"></i>Detalle de empresa</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div id="estadoCargaDetalleEmpresaAdmin" class="text-muted small mb-2 d-none">
                                <span class="spinner-border spinner-border-sm me-2"></span>Cargando detalle...
                            </div>
                            <div id="errorDetalleEmpresaAdmin" class="alert alert-danger d-none"></div>
                            <div id="contenidoDetalleEmpresaAdmin" class="d-none">
                                <div class="row g-2 mb-3">
                                    <div class="col-md-6"><strong>Nombre:</strong> <span id="detEmpresaNombre">-</span></div>
                                    <div class="col-md-6"><strong>Razón social:</strong> <span id="detEmpresaRazonSocial">-</span></div>
                                    <div class="col-md-6"><strong>NIT / RFC:</strong> <span id="detEmpresaNit">-</span></div>
                                    <div class="col-md-6"><strong>CUIT:</strong> <span id="detEmpresaCuit">-</span></div>
                                    <div class="col-md-6"><strong>Teléfono:</strong> <span id="detEmpresaTelefono">-</span></div>
                                    <div class="col-md-6"><strong>Correo:</strong> <span id="detEmpresaCorreo">-</span></div>
                                    <div class="col-md-6"><strong>Dirección:</strong> <span id="detEmpresaDireccion">-</span></div>
                                    <div class="col-md-6"><strong>Fecha de registro:</strong> <span id="detEmpresaFechaRegistro">-</span></div>
                                    <div class="col-md-6"><strong>Status empresa:</strong> <span id="detEmpresaStatus">-</span></div>
                                    <div class="col-md-6"><strong>Actividad económica:</strong> <span id="detEmpresaActividad">-</span></div>
                                    <div class="col-md-6"><strong>Estado expediente:</strong> <span id="detEmpresaEstado">-</span></div>
                                    <div class="col-md-6"><strong>Usuario:</strong> <span id="detUsuarioNombre">-</span></div>
                                    <div class="col-md-6"><strong>Correo usuario:</strong> <span id="detUsuarioCorreo">-</span></div>
                                    <div class="col-md-6"><strong>Rol:</strong> <span id="detUsuarioRol">-</span></div>
                                    <div class="col-md-6"><strong>Último acceso:</strong> <span id="detUsuarioUltimoAcceso">-</span></div>
                                    <div class="col-md-6"><strong>Total empleados:</strong> <span id="detTotalEmpleados">0</span></div>
                                    <div class="col-md-6"><strong>Total vehículos:</strong> <span id="detTotalVehiculos">0</span></div>
                                </div>
                                <div class="table-responsive">
                                    <table class="table table-sm mb-0">
                                        <thead><tr><th>Placa</th><th>Tipo</th><th>Descripción</th></tr></thead>
                                        <tbody id="cuerpoVehiculosEmpresaAdmin"><tr><td colspan="3" class="text-muted">Sin vehículos registrados</td></tr></tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                        </div>
                    </div>
                </div>
            </div>
        `);
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

        const puedeEditar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'EMPRESA']);
        const puedeEliminar = Autenticacion.tieneAcceso(['ADMINISTRADOR']);
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
        modoEdicion = false;
        idEdicion = null;
        document.getElementById('tituloModalEmpresa').textContent = 'Nueva empresa';
        document.getElementById('formularioEmpresa').reset();
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function abrirEdicion(id) {
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
        document.getElementById('mensajeConfirmacionEliminar').textContent =
            `¿Está seguro que desea eliminar la empresa "${nombre}"? Esta acción no se puede deshacer.`;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarId   = id;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarTipo = 'empresa';
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConfirmacion')).show();
    }

    async function eliminar(id) {
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
        cargarServiciosPostRadicacion,
        guardarServiciosPostRadicacion
    };
})();

