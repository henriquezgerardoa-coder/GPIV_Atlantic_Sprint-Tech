const ModuloEmpresas = (() => {
    let empresas = [];
    let modoEdicion = false;
    let idEdicion = null;
    let empresaPropia = null;
    let vehiculosEmpresaPropia = [];
    let empleadosEmpresaPropia = [];
    let empleadoEnEdicion = null;
    let serviciosHabilitados = false;
    const serviciosPostAdminPorEmpresa = new Map();
    let detalleServiciosPostRadicacion = {
        solicitaAguaCruda: false,
        consumoAguaCrudaM3: null,
        consumoLuzKwh: null,
        consumoGasM3: null,
        consumoInternetMbps: null,
        consumosAdicionales: []
    };

    // ─── Punto de entrada ───────────────────────────────────────────────────────

    async function cargar() {
        if (Autenticacion.tieneRol('DIRECTIVO')) {
            await cargarVistaAdmin();
            return;
        }
        if (esRolEmpresaSolo()) {
            await cargarVistaEmpresa();
            return;
        }
        // Vista ADMIN completa (CRUD)
        mostrarPanelAdmin(false);
        mostrarPanelEmpresaPropia(false);
        const respuesta = await ApiCliente.obtener('/api/empresas');
        if (!respuesta?.ok) { mostrarAlerta('Error al cargar empresas.', 'danger'); return; }
        empresas = await respuesta.json();
        renderizarTabla();
        poblarSelectorServiciosPost();
    }

    function esRolEmpresaSolo() {
        return Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR']);
    }

    // ─── Panel ADMIN / DIRECTIVO ─────────────────────────────────────────────────

    function mostrarPanelAdmin(mostrar) {
        document.getElementById('panelEmpresasAdminVisualizacion')?.classList.toggle('d-none', !mostrar);
        document.getElementById('panelEmpresasGestion')?.classList.toggle('d-none', mostrar);
        document.getElementById('btnNuevaEmpresa')?.classList.toggle('d-none', mostrar);
    }

    function mostrarPanelEmpresaPropia(mostrar) {
        document.getElementById('panelEmpresaPropia')?.classList.toggle('d-none', !mostrar);
        document.getElementById('panelEmpresasGestion')?.classList.toggle('d-none', mostrar);
        document.getElementById('panelEmpresasAdminVisualizacion')?.classList.toggle('d-none', true);
        document.getElementById('btnNuevaEmpresa')?.classList.add('d-none');
    }

    async function cargarVistaAdmin() {
        mostrarPanelAdmin(true);
        mostrarPanelEmpresaPropia(false);
        const estado = document.getElementById('estadoCargaEmpresasAdmin');
        const error  = document.getElementById('errorEmpresasAdmin');
        const vacio  = document.getElementById('sinEmpresasAdmin');
        const lista  = document.getElementById('listaEmpresasAdmin');
        const badge  = document.getElementById('badgeTotalEmpresasAdmin');
        if (!lista || !badge) return;
        estado?.classList.remove('d-none');
        error?.classList.add('d-none');
        vacio?.classList.add('d-none');
        lista.innerHTML = '';
        let respuesta = await ApiCliente.obtener('/api/empresas/admin/vista');
        estado?.classList.add('d-none');
        if (!respuesta?.ok) {
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
        if (!listado.length) { vacio?.classList.remove('d-none'); return; }
        lista.innerHTML = listado.map(item => `
            <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                    onclick="ModuloEmpresas.verDetalleAdmin(${item.id})">
                <span>${item.nombre}</span>
                <i class="bi bi-chevron-right text-muted"></i>
            </button>`).join('');
    }

    async function verDetalleAdmin(idEmpresa) {
        const modalElemento = asegurarModalDetalleAdmin();
        if (!modalElemento || !window.bootstrap?.Modal) {
            mostrarAlerta('No se pudo abrir el detalle de empresa.', 'danger');
            return;
        }
        const modal     = bootstrap.Modal.getOrCreateInstance(modalElemento);
        const estado    = document.getElementById('estadoCargaDetalleEmpresaAdmin');
        const error     = document.getElementById('errorDetalleEmpresaAdmin');
        const contenido = document.getElementById('contenidoDetalleEmpresaAdmin');
        estado?.classList.remove('d-none');
        error?.classList.add('d-none');
        contenido?.classList.add('d-none');
        modal.show();

        if (Autenticacion.tieneRol('DIRECTIVO')) {
            const respD = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            estado?.classList.add('d-none');
            if (!respD?.ok) {
                error.textContent = 'No se pudo cargar el detalle de la empresa.';
                error.classList.remove('d-none');
                return;
            }
            renderizarDetalleAdmin(construirDetalleDesdeEmpresaBasica(await respD.json()));
            contenido?.classList.remove('d-none');
            return;
        }

        let respuesta = await ApiCliente.obtener(`/api/empresas/admin/vista/${idEmpresa}`);
        estado?.classList.add('d-none');
        if (!respuesta?.ok) {
            const respuestaFallback = await ApiCliente.obtener(`/api/empresas/${idEmpresa}`);
            if (respuestaFallback?.ok) {
                renderizarDetalleAdmin(construirDetalleDesdeEmpresaBasica(await respuestaFallback.json()));
                contenido?.classList.remove('d-none');
                return;
            }
            const errorJson = await respuesta?.json().catch(() => ({}));
            error.textContent = errorJson?.mensaje || errorJson?.message || 'No se pudo cargar el detalle de la empresa.';
            error.classList.remove('d-none');
            return;
        }
        renderizarDetalleAdmin(await respuesta.json());
        contenido?.classList.remove('d-none');
    }

    function construirDetalleDesdeEmpresaBasica(empresa) {
        return {
            id: empresa?.id, nombre: empresa?.nombre, razonSocial: empresa?.razonSocial,
            cuit: empresa?.cuit, telefono: empresa?.telefono,
            direccion: empresa?.direccion, actividadEconomica: empresa?.actividadEconomica,
            correoElectronico: empresa?.correoElectronico, totalEmpleados: empresa?.cantidadEmpleados ?? 0,
            totalVehiculos: 0, vehiculos: [], lotes: [], fechaRegistro: null, statusEmpresa: null, estadoExpediente: null, usuarioAsociado: null
        };
    }

    const ETIQUETA_ESTADO_LOTE = {
        PREADJUDICADO: { texto: 'Preadjudicado', color: 'warning' },
        ADJUDICADO:    { texto: 'Adjudicado',    color: 'success' },
        DESADJUDICADO: { texto: 'Desadjudicado', color: 'danger'  }
    };

    const ETIQUETA_ZONA_EMPRESA = {
        PARQUE_VIEJO: 'Parque Viejo',
        PARQUE_NUEVO: 'Parque Nuevo'
    };

    function renderizarDetalleAdmin(detalle) {
        const usuario = detalle?.usuarioAsociado;
        setTexto('detEmpresaNombre', detalle?.nombre);
        setTexto('detEmpresaRazonSocial', detalle?.razonSocial);
        setTexto('detEmpresaCuit', detalle?.cuit);
        setTexto('detEmpresaTelefono', detalle?.telefono);
        setTexto('detEmpresaCorreo', detalle?.correoElectronico);
        setTexto('detEmpresaDireccion', detalle?.direccion);
        setTexto('detEmpresaFechaRegistro', formatearFechaHora(detalle?.fechaRegistro));
        setTexto('detEmpresaStatus', detalle?.statusEmpresa);
        setTexto('detEmpresaActividad', detalle?.actividadEconomica);
        setTexto('detEmpresaRubro', detalle?.rubroNombre || 'Sin rubro asignado');
        setTexto('detEmpresaEstado', detalle?.estadoExpediente || 'Sin expediente');
        setTexto('detUsuarioNombre', usuario?.nombreCompleto || 'Sin usuario asociado');
        setTexto('detUsuarioCorreo', usuario?.correoElectronico);
        setTexto('detUsuarioRol', (usuario?.roles || []).join(', '));
        setTexto('detUsuarioUltimoAcceso', formatearFechaHora(usuario?.fechaUltimoAcceso));
        setTexto('detTotalEmpleados', `${detalle?.totalEmpleados ?? 0}`);
        setTexto('detTotalVehiculos', `${detalle?.totalVehiculos ?? 0}`);

        const contenidoLotes = document.getElementById('contenidoLotesEmpresaAdmin');
        const lotes = detalle?.lotes || [];
        if (contenidoLotes) {
            if (!lotes.length) {
                contenidoLotes.innerHTML = '<span class="text-muted fst-italic small"><i class="bi bi-dash-circle me-1"></i>Sin lote asignado</span>';
            } else {
                const estadoInfo = estado => ETIQUETA_ESTADO_LOTE[estado] || { texto: estado || 'Sin estado', color: 'secondary' };
                contenidoLotes.innerHTML = lotes.map(l => {
                    const { texto, color } = estadoInfo(l.estadoAsignacion);
                    return `<div class="border rounded p-2 mb-2 bg-light-subtle">
                        <div class="d-flex justify-content-between align-items-center flex-wrap gap-1">
                            <span class="fw-semibold"><i class="bi bi-map me-1 text-primary"></i>${l.codigo}</span>
                            <span class="badge bg-${color}">${texto}</span>
                        </div>
                        <div class="text-muted small mt-1">
                            <span class="me-3"><i class="bi bi-arrows-angle-expand me-1"></i>${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</span>
                            ${l.zona ? `<span class="me-3"><i class="bi bi-geo-alt me-1"></i>${ETIQUETA_ZONA_EMPRESA[l.zona] || l.zona}</span>` : ''}
                            ${l.fechaAsignacion ? `<span><i class="bi bi-calendar me-1"></i>${l.fechaAsignacion}</span>` : ''}
                        </div>
                    </div>`;
                }).join('');
            }
        }

        const cuerpoVehiculos = document.getElementById('cuerpoVehiculosEmpresaAdmin');
        const vehiculos = detalle?.vehiculos || [];
        if (!cuerpoVehiculos) return;
        cuerpoVehiculos.innerHTML = vehiculos.length
            ? vehiculos.map(v => `<tr><td>${v.placa || '-'}</td><td>${v.tipo || '-'}</td><td>${v.descripcion || '-'}</td></tr>`).join('')
            : '<tr><td colspan="3" class="text-muted">Sin vehículos registrados</td></tr>';
    }

    // ─── Panel EMPRESA (su propia empresa) ──────────────────────────────────────

    async function cargarVistaEmpresa() {
        mostrarPanelEmpresaPropia(true);
        const respuesta = await ApiCliente.obtener('/api/empresas');
        if (!respuesta?.ok) { mostrarAlerta('Error al cargar empresa.', 'danger'); return; }
        const lista = await respuesta.json();
        empresaPropia = lista[0] || null;
        if (!empresaPropia) {
            mostrarAlerta('No hay empresa asociada a su usuario.', 'warning');
            return;
        }
        renderizarPanelEmpresaPropia(empresaPropia);
        const respServ = await ApiCliente.obtener(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`);
        if (respServ?.ok) {
            serviciosHabilitados = true;
            const servicios = await respServ.json();
            vehiculosEmpresaPropia = servicios.vehiculos || [];
            empresaPropia._cantidadEmpleados = servicios.cantidadEmpleados ?? 0;
            detalleServiciosPostRadicacion = {
                solicitaAguaCruda: servicios.solicitaAguaCruda === true,
                consumoAguaCrudaM3: servicios.consumoAguaCrudaM3 ?? null,
                consumoLuzKwh: servicios.consumoLuzKwh ?? null,
                consumoGasM3: servicios.consumoGasM3 ?? null,
                consumoInternetMbps: servicios.consumoInternetMbps ?? null,
                consumosAdicionales: servicios.consumosAdicionales || []
            };
        } else {
            serviciosHabilitados = false;
            vehiculosEmpresaPropia = parsearVehiculosDesdeTexto(empresaPropia.vehiculosAsignadosJson || '');
            empresaPropia._cantidadEmpleados = empresaPropia.cantidadEmpleados ?? 0;
            detalleServiciosPostRadicacion = {
                solicitaAguaCruda: false,
                consumoAguaCrudaM3: null,
                consumoLuzKwh: null,
                consumoGasM3: null,
                consumoInternetMbps: null,
                consumosAdicionales: []
            };
        }
        actualizarBotonServicios();
        actualizarIndicadoresEmpresaPropia();
    }

    function actualizarBotonServicios() {
        const btn = document.getElementById('btnServiciosPostRadicacion');
        if (!btn) return;
        btn.disabled = !serviciosHabilitados;
        btn.title = serviciosHabilitados
            ? 'Solicitar o modificar el uso de servicios post-radicación'
            : 'Disponible cuando su radicación alcance el estado Radicada';
    }


    function renderizarPanelEmpresaPropia(emp) {
        setTexto('empNombrePropio', emp.nombre || '-');
        setTexto('empRazonSocialPropio', emp.razonSocial || '-');
        setTexto('empCuitPropio', emp.cuit || '-');
        setTexto('empActividadPropia', emp.actividadEconomica || '-');
        setTexto('empRubroPropio', emp.rubroNombre || 'Sin rubro asignado');
        setTexto('empDireccionPropia', emp.direccion || '-');
        setTexto('empCorreoPropio', emp.correoElectronico || '-');
        setTexto('empTelefonoPropio', emp.telefono || '-');
    }

    function actualizarIndicadoresEmpresaPropia() {
        setTexto('empCantidadEmpleados', `${empresaPropia?._cantidadEmpleados ?? 0}`);
        setTexto('empCantidadVehiculos', `${vehiculosEmpresaPropia.length}`);
    }

    // ─── Gestión de nómina de empleados ──────────────────────────────────────────

    function abrirAgregarEmpleados() {
        empleadoEnEdicion = null;
        const modal = asegurarModalGestionEmpleados();
        limpiarFormEmpleado();
        bootstrap.Modal.getOrCreateInstance(modal).show();
        cargarListadoEmpleados();
    }

    function asegurarModalGestionEmpleados() {
        let m = document.getElementById('modalGestionEmpleados');
        if (!m) {
            document.body.insertAdjacentHTML('beforeend', `
                <div class="modal fade" id="modalGestionEmpleados" tabindex="-1">
                    <div class="modal-dialog modal-lg modal-dialog-scrollable">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><i class="bi bi-people me-2 text-primary"></i>Nómina de empleados</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="alertaGestionEmpleados" class="alert alert-danger alerta-modal d-none"></div>
                                <div id="contenedorListaEmpleados" class="mb-3">
                                    <p class="text-muted small">Cargando...</p>
                                </div>
                                <hr class="my-2">
                                <p class="fw-semibold small mb-2" id="tituloFormEmpleado">Nuevo empleado</p>
                                <div class="row g-2 align-items-end">
                                    <input type="hidden" id="idEmpleadoEdicion">
                                    <div class="col-md-4">
                                        <label class="form-label small">CUIT <span class="text-danger">*</span> <span class="text-muted">(11 dígitos)</span></label>
                                        <input id="campoCuitEmpleado" type="text" maxlength="11" class="form-control form-control-sm" placeholder="20xxxxxxxxx">
                                    </div>
                                    <div class="col-md-5">
                                        <label class="form-label small">Nombre completo <span class="text-danger">*</span></label>
                                        <input id="campoNombreEmpleado" type="text" maxlength="120" class="form-control form-control-sm" placeholder="Nombre y apellido">
                                    </div>
                                    <div class="col-md-3 d-grid">
                                        <button type="button" class="btn btn-primary btn-sm" onclick="ModuloEmpresas.guardarEmpleado()">
                                            <i class="bi bi-check-lg me-1"></i>Guardar
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="ModuloEmpresas.cancelarEdicionEmpleado()">Cancelar edición</button>
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                            </div>
                        </div>
                    </div>
                </div>`);
            m = document.getElementById('modalGestionEmpleados');
        }
        return m;
    }

    async function cargarListadoEmpleados() {
        if (!empresaPropia?.id) return;
        const contenedor = document.getElementById('contenedorListaEmpleados');
        if (contenedor) contenedor.innerHTML = '<p class="text-muted small">Cargando...</p>';

        const respuesta = await ApiCliente.obtener(`/api/empresas/${empresaPropia.id}/empleados`);
        if (!respuesta?.ok) {
            if (contenedor) contenedor.innerHTML = '<p class="text-danger small">No se pudo cargar la nómina.</p>';
            return;
        }
        empleadosEmpresaPropia = await respuesta.json();
        renderizarListadoEmpleados();
        empresaPropia._cantidadEmpleados = empleadosEmpresaPropia.length;
        actualizarIndicadoresEmpresaPropia();
    }

    function renderizarListadoEmpleados() {
        const contenedor = document.getElementById('contenedorListaEmpleados');
        if (!contenedor) return;

        if (!empleadosEmpresaPropia.length) {
            contenedor.innerHTML = '<p class="text-muted small">Sin empleados registrados. Agregue el primero con el formulario.</p>';
            return;
        }

        contenedor.innerHTML = `
            <p class="text-muted small mb-2">${empleadosEmpresaPropia.length} empleado${empleadosEmpresaPropia.length !== 1 ? 's' : ''} registrado${empleadosEmpresaPropia.length !== 1 ? 's' : ''}</p>
            <div class="table-responsive">
                <table class="table table-sm table-hover">
                    <thead><tr><th>CUIT</th><th>Nombre</th><th>Ingreso</th><th class="text-end">Acciones</th></tr></thead>
                    <tbody>${empleadosEmpresaPropia.map(e => `
                        <tr>
                            <td class="font-monospace small">${e.cuit}</td>
                            <td>${e.nombre}</td>
                            <td class="text-muted small">${e.fechaRegistro ? String(e.fechaRegistro).slice(0, 10) : '-'}</td>
                            <td class="text-end">
                                <button class="btn btn-sm btn-outline-primary me-1" title="Editar" onclick="ModuloEmpresas.editarEmpleado(${e.id})">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger" title="Eliminar" onclick="ModuloEmpresas.eliminarEmpleado(${e.id})">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>`;
    }

    async function guardarEmpleado() {
        const cuit = document.getElementById('campoCuitEmpleado')?.value?.trim();
        const nombre = document.getElementById('campoNombreEmpleado')?.value?.trim();

        if (!cuit || !nombre) {
            mostrarAlertaModal('alertaGestionEmpleados', 'El CUIT y el nombre son obligatorios.');
            return;
        }
        if (!/^\d{11}$/.test(cuit)) {
            mostrarAlertaModal('alertaGestionEmpleados', 'El CUIT debe tener exactamente 11 dígitos sin guiones ni espacios.');
            return;
        }

        const ruta = empleadoEnEdicion
            ? `/api/empresas/${empresaPropia.id}/empleados/${empleadoEnEdicion}`
            : `/api/empresas/${empresaPropia.id}/empleados`;
        const respuesta = empleadoEnEdicion
            ? await ApiCliente.actualizar(ruta, { cuit, nombre })
            : await ApiCliente.crear(ruta, { cuit, nombre });

        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaGestionEmpleados', err?.message || (empleadoEnEdicion ? 'No se pudo actualizar el empleado.' : 'No se pudo registrar el empleado.'));
            return;
        }

        mostrarAlerta(empleadoEnEdicion ? 'Empleado actualizado correctamente.' : 'Empleado registrado correctamente.');
        cancelarEdicionEmpleado();
        await cargarListadoEmpleados();
    }

    function editarEmpleado(id) {
        const emp = empleadosEmpresaPropia.find(e => e.id === id);
        if (!emp) return;
        empleadoEnEdicion = id;
        const campoCuit = document.getElementById('campoCuitEmpleado');
        const campoNombre = document.getElementById('campoNombreEmpleado');
        const titulo = document.getElementById('tituloFormEmpleado');
        if (campoCuit) campoCuit.value = emp.cuit;
        if (campoNombre) campoNombre.value = emp.nombre;
        if (titulo) titulo.textContent = `Editando: ${emp.nombre}`;
        campoCuit?.focus();
    }

    function cancelarEdicionEmpleado() {
        limpiarFormEmpleado();
    }

    function limpiarFormEmpleado() {
        empleadoEnEdicion = null;
        const campoCuit = document.getElementById('campoCuitEmpleado');
        const campoNombre = document.getElementById('campoNombreEmpleado');
        const titulo = document.getElementById('tituloFormEmpleado');
        if (campoCuit) campoCuit.value = '';
        if (campoNombre) campoNombre.value = '';
        if (titulo) titulo.textContent = 'Nuevo empleado';
    }

    async function eliminarEmpleado(id) {
        const emp = empleadosEmpresaPropia.find(e => e.id === id);
        if (!emp) return;
        if (!window.confirm(`¿Eliminar al empleado "${emp.nombre}" (CUIT: ${emp.cuit})?`)) return;
        const respuesta = await ApiCliente.eliminar(`/api/empresas/${empresaPropia.id}/empleados/${id}`);
        if (!(respuesta?.status === 204 || respuesta?.ok)) {
            mostrarAlertaModal('alertaGestionEmpleados', 'No se pudo eliminar el empleado.');
            return;
        }
        await cargarListadoEmpleados();
    }

    // ─── Listado y alta de vehículos ─────────────────────────────────────────────

    function abrirListadoVehiculos() {
        const modal = asegurarModalVehiculos();
        renderizarListadoVehiculos();
        ocultarFormularioVehiculo();
        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function asegurarModalVehiculos() {
        let m = document.getElementById('modalVehiculosEmpresa');
        if (!m) {
            document.body.insertAdjacentHTML('beforeend', `
                <div class="modal fade" id="modalVehiculosEmpresa" tabindex="-1">
                    <div class="modal-dialog modal-lg modal-dialog-scrollable">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><i class="bi bi-truck me-2 text-info"></i>Vehículos registrados</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="alertaVehiculosEmpresa" class="alert alert-danger alerta-modal d-none"></div>
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <span class="text-muted small">Listado actualizado de su flota</span>
                                    <button class="btn btn-outline-primary btn-sm" onclick="ModuloEmpresas.mostrarFormularioVehiculo()">
                                        <i class="bi bi-plus-lg me-1"></i>Agregar vehículo
                                    </button>
                                </div>
                                <div id="formularioNuevoVehiculo" class="border rounded p-3 mb-3 bg-light-subtle d-none">
                                    <h6 class="text-muted mb-2">Nuevo vehículo</h6>
                                    <div class="row g-2">
                                        <div class="col-md-4">
                                            <label class="form-label small">Patente <span class="text-danger">*</span></label>
                                            <input id="campoPatente" class="form-control form-control-sm" placeholder="ABC123 o AB123CD" maxlength="10">
                                        </div>
                                        <div class="col-md-4">
                                            <label class="form-label small">Marca/Modelo <span class="text-danger">*</span></label>
                                            <input id="campoMarcaModelo" class="form-control form-control-sm" placeholder="Ej: Ford Ranger" maxlength="80">
                                        </div>
                                        <div class="col-md-4">
                                            <label class="form-label small">Tipo</label>
                                            <input id="campoTipoVehiculo" class="form-control form-control-sm" placeholder="Ej: CAMIÓN" maxlength="40">
                                        </div>
                                        <div class="col-12 d-flex gap-2 justify-content-end">
                                            <button class="btn btn-sm btn-secondary" type="button" onclick="ModuloEmpresas.ocultarFormularioVehiculo()">Cancelar</button>
                                            <button class="btn btn-sm btn-primary" type="button" onclick="ModuloEmpresas.guardarVehiculo()">
                                                <i class="bi bi-floppy me-1"></i>Guardar
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="table-responsive">
                                    <table class="table table-sm table-hover">
                                        <thead><tr><th>Patente</th><th>Tipo</th><th>Marca/Modelo</th><th class="text-center">Quitar</th></tr></thead>
                                        <tbody id="cuerpoListadoVehiculos"><tr><td colspan="4" class="text-muted text-center">Sin vehículos</td></tr></tbody>
                                    </table>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                            </div>
                        </div>
                    </div>
                </div>`);
            m = document.getElementById('modalVehiculosEmpresa');
        }
        return m;
    }

    function renderizarListadoVehiculos() {
        const cuerpo = document.getElementById('cuerpoListadoVehiculos');
        if (!cuerpo) return;
        if (!vehiculosEmpresaPropia.length) {
            cuerpo.innerHTML = '<tr><td colspan="4" class="text-muted text-center">Sin vehículos registrados</td></tr>';
            return;
        }
        cuerpo.innerHTML = vehiculosEmpresaPropia.map((v, idx) => `
            <tr>
                <td class="fw-semibold">${v.placa || '-'}</td>
                <td>${v.tipo || '-'}</td>
                <td>${v.descripcion || v.marcaModelo || '-'}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger" title="Quitar" onclick="ModuloEmpresas.quitarVehiculo(${idx})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>`).join('');
    }

    function mostrarFormularioVehiculo() {
        document.getElementById('formularioNuevoVehiculo')?.classList.remove('d-none');
        document.getElementById('campoPatente').value = '';
        document.getElementById('campoMarcaModelo').value = '';
        document.getElementById('campoTipoVehiculo').value = '';
    }

    function ocultarFormularioVehiculo() {
        document.getElementById('formularioNuevoVehiculo')?.classList.add('d-none');
    }

    async function guardarVehiculo() {
        const placa = document.getElementById('campoPatente')?.value?.trim().toUpperCase();
        const marcaModelo = document.getElementById('campoMarcaModelo')?.value?.trim();
        const tipo = document.getElementById('campoTipoVehiculo')?.value?.trim() || 'GENERAL';

        if (!placa || !marcaModelo) {
            mostrarAlertaModal('alertaVehiculosEmpresa', 'Patente y marca/modelo son obligatorios.');
            return;
        }

        const nuevoVehiculo = { placa, tipo, descripcion: marcaModelo };
        const nuevosVehiculos = [...vehiculosEmpresaPropia, nuevoVehiculo];

        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`, {
            cantidadEmpleados: empresaPropia._cantidadEmpleados ?? 0,
            vehiculos: nuevosVehiculos,
            ...detalleServiciosPostRadicacion
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaVehiculosEmpresa', error?.message || 'No se pudo guardar el vehículo.');
            return;
        }
        vehiculosEmpresaPropia = nuevosVehiculos;
        ocultarFormularioVehiculo();
        renderizarListadoVehiculos();
        actualizarIndicadoresEmpresaPropia();
        mostrarAlerta(`Vehículo ${placa} agregado correctamente.`);
    }

    async function quitarVehiculo(indice) {
        if (!confirm('¿Desea quitar este vehículo?')) return;
        const nuevosVehiculos = vehiculosEmpresaPropia.filter((_, i) => i !== indice);
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`, {
            cantidadEmpleados: empresaPropia._cantidadEmpleados ?? 0,
            vehiculos: nuevosVehiculos,
            ...detalleServiciosPostRadicacion
        });
        if (!respuesta?.ok) {
            mostrarAlerta('No se pudo quitar el vehículo.', 'danger');
            return;
        }
        vehiculosEmpresaPropia = nuevosVehiculos;
        renderizarListadoVehiculos();
        actualizarIndicadoresEmpresaPropia();
        mostrarAlerta('Vehículo quitado correctamente.');
    }

    // ─── Modal Servicios ─────────────────────────────────────────────────────────

    function abrirServiciosModal() {
        const modal = asegurarModalServicios();
        setTexto('servEmpresaNombreModal', empresaPropia?.nombre || '-');
        const checkAguaCruda = document.getElementById('campoServSolicitaAguaCruda');
        if (checkAguaCruda) checkAguaCruda.checked = detalleServiciosPostRadicacion.solicitaAguaCruda === true;
        const campoAguaCruda = document.getElementById('campoServConsumoAguaCruda');
        if (campoAguaCruda) campoAguaCruda.value = detalleServiciosPostRadicacion.consumoAguaCrudaM3 ?? '';
        const campoLuz = document.getElementById('campoServConsumoLuz');
        if (campoLuz) campoLuz.value = detalleServiciosPostRadicacion.consumoLuzKwh ?? '';
        const campoGas = document.getElementById('campoServConsumoGas');
        if (campoGas) campoGas.value = detalleServiciosPostRadicacion.consumoGasM3 ?? '';
        const campoInternet = document.getElementById('campoServConsumoInternet');
        if (campoInternet) campoInternet.value = detalleServiciosPostRadicacion.consumoInternetMbps ?? '';
        const campoAdicionales = document.getElementById('campoServConsumosAdicionales');
        if (campoAdicionales) campoAdicionales.value = serializarConsumosAdicionales(detalleServiciosPostRadicacion.consumosAdicionales || []);
        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function asegurarModalServicios() {
        let m = document.getElementById('modalServiciosEmpresa');
        if (!m) {
            document.body.insertAdjacentHTML('beforeend', `
                <div class="modal fade" id="modalServiciosEmpresa" tabindex="-1">
                    <div class="modal-dialog modal-lg modal-dialog-scrollable">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><i class="bi bi-tools me-2 text-secondary"></i>Servicios habilitados</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="alertaServiciosEmpresaModal" class="alert alert-danger alerta-modal d-none"></div>
                                <p class="text-muted small mb-3">Empresa: <strong id="servEmpresaNombreModal">-</strong></p>
                                <hr>
                                <div class="form-check mb-2">
                                    <input class="form-check-input" type="checkbox" id="campoServSolicitaAguaCruda">
                                    <label class="form-check-label" for="campoServSolicitaAguaCruda">Solicitar agua cruda</label>
                                </div>
                                <div class="row g-2 mb-2">
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Consumo estimado de agua cruda (m3/mes)</label>
                                        <input id="campoServConsumoAguaCruda" type="number" min="0" step="0.01" class="form-control" placeholder="Ej: 180.50">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Consumo estimado de luz (kWh/mes)</label>
                                        <input id="campoServConsumoLuz" type="number" min="0" step="0.01" class="form-control" placeholder="Ej: 1200">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Consumo estimado de gas (m3/mes)</label>
                                        <input id="campoServConsumoGas" type="number" min="0" step="0.01" class="form-control" placeholder="Ej: 320">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold">Consumo estimado de internet (Mbps)</label>
                                        <input id="campoServConsumoInternet" type="number" min="0" step="0.01" class="form-control" placeholder="Ej: 200">
                                    </div>
                                </div>
                                <div>
                                    <label class="form-label fw-semibold">Servicios adicionales (futuro)</label>
                                    <small class="text-muted d-block mb-1">Una línea por servicio: <code>NOMBRE|CONSUMO|UNIDAD|DETALLE</code></small>
                                    <textarea id="campoServConsumosAdicionales" rows="3" class="form-control" placeholder="Comedor|1|unidad|Servicio diario"></textarea>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                                <button type="button" class="btn btn-primary" onclick="ModuloEmpresas.guardarServiciosModal()">
                                    <i class="bi bi-floppy me-1"></i>Guardar servicios
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`);
            m = document.getElementById('modalServiciosEmpresa');
        }
        return m;
    }

    async function guardarServiciosModal() {
        if (!empresaPropia?.id) return;
        const cantidadEmpleados = empresaPropia?._cantidadEmpleados ?? 0;
        const vehiculos = vehiculosEmpresaPropia;
        const payloadServicios = construirPayloadServiciosPostRadicacion();
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`, {
            cantidadEmpleados,
            vehiculos,
            ...payloadServicios
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaServiciosEmpresaModal', error?.message || 'No se pudieron guardar los servicios.');
            return;
        }
        empresaPropia._cantidadEmpleados = cantidadEmpleados;
        vehiculosEmpresaPropia = vehiculos;
        detalleServiciosPostRadicacion = payloadServicios;
        actualizarIndicadoresEmpresaPropia();
        bootstrap.Modal.getInstance(document.getElementById('modalServiciosEmpresa'))?.hide();
        mostrarAlerta('Servicios actualizados correctamente.');
    }

    // ─── Vista CRUD (ADMIN) ──────────────────────────────────────────────────────

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaEmpresas');
        if (!cuerpo) return;
        if (empresas.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin empresas registradas</td></tr>';
            return;
        }
        const puedeEditar  = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'EMPRESA']);
        const puedeEliminar = Autenticacion.tieneAcceso(['ADMINISTRADOR']);
        cuerpo.innerHTML = empresas.map(e => `
            <tr>
                <td class="text-muted">${e.id}</td>
                <td class="fw-semibold">${e.nombre}</td>
                <td>${e.razonSocial || '-'}</td>
                <td>${e.cuit}</td>
                <td>${e.correoElectronico}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-secondary me-1" title="Detalle" onclick="ModuloEmpresas.verDetalleAdmin(${e.id})"><i class="bi bi-eye"></i></button>
                    ${puedeEditar ? `
                    <button class="btn btn-sm btn-outline-primary me-1" title="Editar" onclick="ModuloEmpresas.abrirEdicion(${e.id})"><i class="bi bi-pencil"></i></button>
                    ${puedeEliminar ? `<button class="btn btn-sm btn-outline-danger" title="Eliminar" onclick="ModuloEmpresas.confirmarEliminacion(${e.id}, '${e.nombre.replace(/'/g, "\\'")}')"><i class="bi bi-trash"></i></button>` : ''}
                    ` : ''}
                </td>
            </tr>`).join('');
    }

    async function abrirCreacion() {
        modoEdicion = false; idEdicion = null;
        document.getElementById('tituloModalEmpresa').textContent = 'Nueva empresa';
        document.getElementById('formularioEmpresa').reset();
        ocultarAlertaModal('alertaModalEmpresa');

        // Mostrar lista de empresas existentes como referencia
        const panel  = document.getElementById('panelEmpresasExistentes');
        const cuerpo = document.getElementById('cuerpoEmpresasExistentes');
        try {
            const lista = empresas.length > 0
                ? empresas
                : await ApiCliente.obtener('/api/empresas').then(r => r.ok ? r.json() : []);
            if (lista && lista.length > 0) {
                cuerpo.innerHTML = lista.map(e => `
                    <tr>
                        <td>${e.nombre || '-'}</td>
                        <td>${e.cuit || '-'}</td>
                        <td>${e.correoElectronico || '-'}</td>
                    </tr>`).join('');
                panel.classList.remove('d-none');
            } else {
                panel.classList.add('d-none');
            }
        } catch (_) {
            panel.classList.add('d-none');
        }

        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function editarEmpresaPropia() {
        if (!empresaPropia) return;
        modoEdicion = true; idEdicion = empresaPropia.id;
        document.getElementById('tituloModalEmpresa').textContent = 'Editar datos de empresa';
        document.getElementById('campoNombreEmpresa').value = empresaPropia.nombre;
        document.getElementById('campoRazonSocialEmpresa').value = empresaPropia.razonSocial || '';
        document.getElementById('campoCuitEmpresa').value = empresaPropia.cuit;
        document.getElementById('campoDireccionEmpresa').value = empresaPropia.direccion || '';
        document.getElementById('campoActividadEconomicaEmpresa').value = empresaPropia.actividadEconomica || '';
        document.getElementById('campoCorreoEmpresa').value = empresaPropia.correoElectronico;
        document.getElementById('campoTelefonoEmpresa').value = empresaPropia.telefono || '';
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function abrirEdicion(id) {
        const empresa = empresas.find(e => e.id === id);
        if (!empresa) return;
        modoEdicion = true; idEdicion = id;
        document.getElementById('tituloModalEmpresa').textContent = 'Editar empresa';
        document.getElementById('campoNombreEmpresa').value = empresa.nombre;
        document.getElementById('campoRazonSocialEmpresa').value = empresa.razonSocial || '';
        document.getElementById('campoCuitEmpresa').value = empresa.cuit;
        document.getElementById('campoDireccionEmpresa').value = empresa.direccion || '';
        document.getElementById('campoActividadEconomicaEmpresa').value = empresa.actividadEconomica || '';
        document.getElementById('campoCorreoEmpresa').value = empresa.correoElectronico;
        document.getElementById('campoTelefonoEmpresa').value = empresa.telefono || '';
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    async function guardar() {
        const datos = {
            nombre:             document.getElementById('campoNombreEmpresa').value.trim(),
            razonSocial:        document.getElementById('campoRazonSocialEmpresa').value.trim(),
            cuit:               document.getElementById('campoCuitEmpresa').value.trim(),
            direccion:          document.getElementById('campoDireccionEmpresa').value.trim(),
            actividadEconomica: document.getElementById('campoActividadEconomicaEmpresa').value.trim(),
            correoElectronico:  document.getElementById('campoCorreoEmpresa').value.trim(),
            telefono:           document.getElementById('campoTelefonoEmpresa').value.trim()
        };
        if (!datos.nombre || !datos.razonSocial || !datos.cuit || !datos.direccion || !datos.actividadEconomica || !datos.correoElectronico) {
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
            mostrarAlertaModal('alertaModalEmpresa', error?.message || 'Error al guardar.');
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

    // ─── Servicios post-radicación (panel Admin) ─────────────────────────────────

    function poblarSelectorServiciosPost() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        if (!selector) return;
        selector.innerHTML = empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    async function cargarServiciosPostRadicacion() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        const empresaId = selector?.value;
        if (!empresaId) { mostrarAlerta('Seleccione una empresa.', 'danger'); return; }
        const respuesta = await ApiCliente.obtener(`/api/empresas/${empresaId}/servicios-post-radicacion`);
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'La empresa aún no está habilitada para servicios post-radicación.', 'warning');
            return;
        }
        const datos = await respuesta.json();
        serviciosPostAdminPorEmpresa.set(String(empresaId), {
            cantidadEmpleados: datos.cantidadEmpleados ?? 0,
            vehiculos: datos.vehiculos || []
        });
        document.getElementById('campoSolicitaAguaCrudaPost').checked = datos.solicitaAguaCruda === true;
        document.getElementById('campoConsumoAguaCrudaPost').value = datos.consumoAguaCrudaM3 ?? '';
        document.getElementById('campoConsumoLuzPost').value = datos.consumoLuzKwh ?? '';
        document.getElementById('campoConsumoGasPost').value = datos.consumoGasM3 ?? '';
        document.getElementById('campoConsumoInternetPost').value = datos.consumoInternetMbps ?? '';
        document.getElementById('campoConsumosAdicionalesPost').value = serializarConsumosAdicionales(datos.consumosAdicionales || []);
        mostrarAlerta('Servicios post-radicación cargados correctamente.');
    }

    async function guardarServiciosPostRadicacion() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        const empresaId = selector?.value;
        if (!empresaId) { mostrarAlerta('Seleccione una empresa.', 'danger'); return; }
        let baseServicios = serviciosPostAdminPorEmpresa.get(String(empresaId));
        if (!baseServicios) {
            const respuestaBase = await ApiCliente.obtener(`/api/empresas/${empresaId}/servicios-post-radicacion`);
            if (!respuestaBase?.ok) {
                const errorBase = await respuestaBase?.json().catch(() => ({}));
                mostrarAlerta(errorBase?.mensaje || errorBase?.message || 'Primero consulta los servicios actuales de la empresa.', 'warning');
                return;
            }
            const datosBase = await respuestaBase.json();
            baseServicios = {
                cantidadEmpleados: datosBase.cantidadEmpleados ?? 0,
                vehiculos: datosBase.vehiculos || []
            };
            serviciosPostAdminPorEmpresa.set(String(empresaId), baseServicios);
        }
        const payloadServicios = {
            solicitaAguaCruda: document.getElementById('campoSolicitaAguaCrudaPost')?.checked === true,
            consumoAguaCrudaM3: parsearNumeroOpcional(document.getElementById('campoConsumoAguaCrudaPost')?.value),
            consumoLuzKwh: parsearNumeroOpcional(document.getElementById('campoConsumoLuzPost')?.value),
            consumoGasM3: parsearNumeroOpcional(document.getElementById('campoConsumoGasPost')?.value),
            consumoInternetMbps: parsearNumeroOpcional(document.getElementById('campoConsumoInternetPost')?.value),
            consumosAdicionales: parsearConsumosAdicionales(document.getElementById('campoConsumosAdicionalesPost')?.value || '')
        };
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaId}/servicios-post-radicacion`, {
            cantidadEmpleados: baseServicios.cantidadEmpleados ?? 0,
            vehiculos: baseServicios.vehiculos || [],
            ...payloadServicios
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'No se pudieron guardar los servicios post-radicación.', 'danger');
            return;
        }
        const datosActualizados = await respuesta.json().catch(() => ({}));
        serviciosPostAdminPorEmpresa.set(String(empresaId), {
            cantidadEmpleados: datosActualizados?.cantidadEmpleados ?? baseServicios.cantidadEmpleados ?? 0,
            vehiculos: datosActualizados?.vehiculos || baseServicios.vehiculos || []
        });
        mostrarAlerta('Servicios post-radicación actualizados correctamente.');
        await cargar();
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────────

    function parsearVehiculosDesdeTexto(texto) {
        return (texto || '').split('\n').map(l => l.trim()).filter(Boolean).map(l => {
            const [placa, tipo, descripcion] = l.split('|').map(v => (v || '').trim());
            return { placa, tipo, descripcion };
        });
    }

    function serializarVehiculosEnTexto(vehiculos) {
        return (vehiculos || []).map(v => `${v.placa}|${v.tipo}|${v.descripcion || ''}`).join('\n');
    }

    function construirPayloadServiciosPostRadicacion() {
        return {
            solicitaAguaCruda: document.getElementById('campoServSolicitaAguaCruda')?.checked === true,
            consumoAguaCrudaM3: parsearNumeroOpcional(document.getElementById('campoServConsumoAguaCruda')?.value),
            consumoLuzKwh: parsearNumeroOpcional(document.getElementById('campoServConsumoLuz')?.value),
            consumoGasM3: parsearNumeroOpcional(document.getElementById('campoServConsumoGas')?.value),
            consumoInternetMbps: parsearNumeroOpcional(document.getElementById('campoServConsumoInternet')?.value),
            consumosAdicionales: parsearConsumosAdicionales(document.getElementById('campoServConsumosAdicionales')?.value || '')
        };
    }

    function parsearNumeroOpcional(valor) {
        if (valor === null || valor === undefined || String(valor).trim() === '') return null;
        const numero = Number.parseFloat(String(valor).replace(',', '.'));
        return Number.isFinite(numero) ? numero : null;
    }

    function parsearConsumosAdicionales(texto) {
        return (texto || '').split('\n').map(l => l.trim()).filter(Boolean).map(l => {
            const [nombre, consumoEstimado, unidad, detalle] = l.split('|').map(v => (v || '').trim());
            return {
                nombre,
                consumoEstimado: parsearNumeroOpcional(consumoEstimado),
                unidad: unidad || null,
                detalle: detalle || null
            };
        }).filter(item => item.nombre);
    }

    function serializarConsumosAdicionales(consumos) {
        return (consumos || []).map(c => {
            const consumo = c?.consumoEstimado ?? '';
            return `${c?.nombre || ''}|${consumo}|${c?.unidad || ''}|${c?.detalle || ''}`;
        }).join('\n');
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) el.textContent = valor || '-';
    }

    function formatearFechaHora(valor) {
        if (!valor) return 'No disponible';
        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? valor : fecha.toLocaleString();
    }

    function asegurarModalDetalleAdmin() {
        return document.getElementById('modalDetalleEmpresaAdmin');
    }

    return {
        cargar,
        abrirCreacion, abrirEdicion, editarEmpresaPropia, guardar,
        confirmarEliminacion, eliminar,
        verDetalleAdmin,
        cargarServiciosPostRadicacion, guardarServiciosPostRadicacion,
        abrirAgregarEmpleados, guardarEmpleado, editarEmpleado, cancelarEdicionEmpleado, eliminarEmpleado,
        abrirListadoVehiculos, mostrarFormularioVehiculo, ocultarFormularioVehiculo,
        guardarVehiculo, quitarVehiculo,
        abrirServiciosModal, guardarServiciosModal
    };
})();
