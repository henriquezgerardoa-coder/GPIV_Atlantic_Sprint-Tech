const ModuloEmpresas = (() => {
    let empresas = [];
    let modoEdicion = false;
    let idEdicion = null;
    let empresaPropia = null;
    let vehiculosEmpresaPropia = [];
    let serviciosHabilitados = false;

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
            nit: empresa?.nit, cuit: empresa?.cuit, telefono: empresa?.telefono,
            direccion: empresa?.direccion, actividadEconomica: empresa?.actividadEconomica,
            correoElectronico: empresa?.correoElectronico, totalEmpleados: empresa?.cantidadEmpleados ?? 0,
            totalVehiculos: 0, vehiculos: [], fechaRegistro: null, statusEmpresa: null, estadoExpediente: null, usuarioAsociado: null
        };
    }

    function renderizarDetalleAdmin(detalle) {
        const usuario = detalle?.usuarioAsociado;
        setTexto('detEmpresaNombre', detalle?.nombre);
        setTexto('detEmpresaRazonSocial', detalle?.razonSocial);
        setTexto('detEmpresaNit', detalle?.nit);
        setTexto('detEmpresaCuit', detalle?.cuit);
        setTexto('detEmpresaTelefono', detalle?.telefono);
        setTexto('detEmpresaCorreo', detalle?.correoElectronico);
        setTexto('detEmpresaDireccion', detalle?.direccion);
        setTexto('detEmpresaFechaRegistro', formatearFechaHora(detalle?.fechaRegistro));
        setTexto('detEmpresaStatus', detalle?.statusEmpresa);
        setTexto('detEmpresaActividad', detalle?.actividadEconomica);
        setTexto('detEmpresaEstado', detalle?.estadoExpediente || 'Sin expediente');
        setTexto('detUsuarioNombre', usuario?.nombreCompleto || 'Sin usuario asociado');
        setTexto('detUsuarioCorreo', usuario?.correoElectronico);
        setTexto('detUsuarioRol', (usuario?.roles || []).join(', '));
        setTexto('detUsuarioUltimoAcceso', formatearFechaHora(usuario?.fechaUltimoAcceso));
        setTexto('detTotalEmpleados', `${detalle?.totalEmpleados ?? 0}`);
        setTexto('detTotalVehiculos', `${detalle?.totalVehiculos ?? 0}`);

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
        } else {
            serviciosHabilitados = false;
            vehiculosEmpresaPropia = parsearVehiculosDesdeTexto(empresaPropia.vehiculosAsignadosJson || '');
            empresaPropia._cantidadEmpleados = empresaPropia.cantidadEmpleados ?? 0;
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
        setTexto('empNitPropio', emp.nit || '-');
        setTexto('empActividadPropia', emp.actividadEconomica || '-');
        setTexto('empDireccionPropia', emp.direccion || '-');
        setTexto('empCorreoPropio', emp.correoElectronico || '-');
        setTexto('empTelefonoPropio', emp.telefono || '-');
    }

    function actualizarIndicadoresEmpresaPropia() {
        setTexto('empCantidadEmpleados', `${empresaPropia?._cantidadEmpleados ?? 0}`);
        setTexto('empCantidadVehiculos', `${vehiculosEmpresaPropia.length}`);
    }

    // ─── Agregar empleados ────────────────────────────────────────────────────────

    function abrirAgregarEmpleados() {
        const actual = empresaPropia?._cantidadEmpleados ?? 0;
        const modal = asegurarModalAgregarEmpleados(actual);
        bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function asegurarModalAgregarEmpleados(cantidadActual) {
        let m = document.getElementById('modalAgregarEmpleados');
        if (!m) {
            document.body.insertAdjacentHTML('beforeend', `
                <div class="modal fade" id="modalAgregarEmpleados" tabindex="-1">
                    <div class="modal-dialog modal-sm">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title"><i class="bi bi-person-plus me-2 text-primary"></i>Agregar empleados</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="alertaAgregarEmpleados" class="alert alert-danger alerta-modal d-none"></div>
                                <p class="text-muted small mb-3">Empleados actuales: <strong id="empActualEnModal">0</strong></p>
                                <label class="form-label fw-semibold">Cantidad a agregar</label>
                                <input id="campoAgregarEmpleados" type="number" min="1" class="form-control" value="1">
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                                <button type="button" class="btn btn-primary" onclick="ModuloEmpresas.confirmarAgregarEmpleados()">
                                    <i class="bi bi-check-lg me-1"></i>Confirmar
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`);
            m = document.getElementById('modalAgregarEmpleados');
        }
        setTexto('empActualEnModal', `${cantidadActual}`);
        const campo = document.getElementById('campoAgregarEmpleados');
        if (campo) campo.value = '1';
        return m;
    }

    async function confirmarAgregarEmpleados() {
        const agregar = Number.parseInt(document.getElementById('campoAgregarEmpleados')?.value || '0', 10);
        if (!empresaPropia?.id || isNaN(agregar) || agregar < 1) {
            mostrarAlertaModal('alertaAgregarEmpleados', 'Ingrese una cantidad válida (mínimo 1).');
            return;
        }
        const nuevaCantidad = (empresaPropia._cantidadEmpleados ?? 0) + agregar;
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`, {
            cantidadEmpleados: nuevaCantidad,
            vehiculos: vehiculosEmpresaPropia
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaAgregarEmpleados', error?.message || 'No se pudo actualizar la cantidad de empleados.');
            return;
        }
        empresaPropia._cantidadEmpleados = nuevaCantidad;
        actualizarIndicadoresEmpresaPropia();
        bootstrap.Modal.getInstance(document.getElementById('modalAgregarEmpleados'))?.hide();
        mostrarAlerta(`Empleados actualizados: ${nuevaCantidad} en total.`);
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
            vehiculos: nuevosVehiculos
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
            vehiculos: nuevosVehiculos
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
        const campoEmpleados = document.getElementById('campoServEmpleados');
        if (campoEmpleados) campoEmpleados.value = empresaPropia?._cantidadEmpleados ?? 0;
        const campoVehiculos = document.getElementById('campoServVehiculos');
        if (campoVehiculos) campoVehiculos.value = serializarVehiculosEnTexto(vehiculosEmpresaPropia);
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
                                <p class="small text-muted">Disponibles con radicación en estado <strong>RADICADA</strong>.</p>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Cantidad total de empleados</label>
                                    <input id="campoServEmpleados" type="number" min="0" class="form-control" value="0">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Vehículos</label>
                                    <small class="text-muted d-block mb-1">Una línea por vehículo: <code>PATENTE|TIPO|DESCRIPCION</code></small>
                                    <textarea id="campoServVehiculos" rows="4" class="form-control" placeholder="ABC123|CAMION|Unidad principal"></textarea>
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
        const cantidadEmpleados = Number.parseInt(document.getElementById('campoServEmpleados')?.value || '0', 10);
        const vehiculos = parsearVehiculosDesdeTexto(document.getElementById('campoServVehiculos')?.value || '');
        if (vehiculos.some(v => !v.placa || !v.tipo)) {
            mostrarAlertaModal('alertaServiciosEmpresaModal', 'Cada vehículo debe tener patente y tipo.');
            return;
        }
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaPropia.id}/servicios-post-radicacion`, { cantidadEmpleados, vehiculos });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaServiciosEmpresaModal', error?.message || 'No se pudieron guardar los servicios.');
            return;
        }
        empresaPropia._cantidadEmpleados = cantidadEmpleados;
        vehiculosEmpresaPropia = vehiculos;
        actualizarIndicadoresEmpresaPropia();
        bootstrap.Modal.getInstance(document.getElementById('modalServiciosEmpresa'))?.hide();
        mostrarAlerta('Servicios actualizados correctamente.');
    }

    // ─── Vista CRUD (ADMIN) ──────────────────────────────────────────────────────

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaEmpresas');
        if (!cuerpo) return;
        if (empresas.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin empresas registradas</td></tr>';
            return;
        }
        const puedeEditar  = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'EMPRESA']);
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
                    <button class="btn btn-sm btn-outline-secondary me-1" title="Detalle" onclick="ModuloEmpresas.verDetalleAdmin(${e.id})"><i class="bi bi-eye"></i></button>
                    ${puedeEditar ? `
                    <button class="btn btn-sm btn-outline-primary me-1" title="Editar" onclick="ModuloEmpresas.abrirEdicion(${e.id})"><i class="bi bi-pencil"></i></button>
                    ${puedeEliminar ? `<button class="btn btn-sm btn-outline-danger" title="Eliminar" onclick="ModuloEmpresas.confirmarEliminacion(${e.id}, '${e.nombre.replace(/'/g, "\\'")}')"><i class="bi bi-trash"></i></button>` : ''}
                    ` : ''}
                </td>
            </tr>`).join('');
    }

    function abrirCreacion() {
        modoEdicion = false; idEdicion = null;
        document.getElementById('tituloModalEmpresa').textContent = 'Nueva empresa';
        document.getElementById('formularioEmpresa').reset();
        ocultarAlertaModal('alertaModalEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
    }

    function editarEmpresaPropia() {
        if (!empresaPropia) return;
        modoEdicion = true; idEdicion = empresaPropia.id;
        document.getElementById('tituloModalEmpresa').textContent = 'Editar datos de empresa';
        document.getElementById('campoNombreEmpresa').value = empresaPropia.nombre;
        document.getElementById('campoRazonSocialEmpresa').value = empresaPropia.razonSocial || '';
        document.getElementById('campoNitEmpresa').value = empresaPropia.nit || '';
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
        document.getElementById('campoNitEmpresa').value = empresa.nit || '';
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
            nit:                document.getElementById('campoNitEmpresa').value.trim(),
            cuit:               document.getElementById('campoCuitEmpresa').value.trim(),
            direccion:          document.getElementById('campoDireccionEmpresa').value.trim(),
            actividadEconomica: document.getElementById('campoActividadEconomicaEmpresa').value.trim(),
            correoElectronico:  document.getElementById('campoCorreoEmpresa').value.trim(),
            telefono:           document.getElementById('campoTelefonoEmpresa').value.trim()
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
        document.getElementById('campoCantidadEmpleadosPost').value = datos.cantidadEmpleados ?? 0;
        document.getElementById('campoVehiculosPost').value = serializarVehiculosEnTexto(datos.vehiculos);
        mostrarAlerta('Servicios post-radicación cargados correctamente.');
    }

    async function guardarServiciosPostRadicacion() {
        const selector = document.getElementById('selectorEmpresaServiciosPost');
        const empresaId = selector?.value;
        if (!empresaId) { mostrarAlerta('Seleccione una empresa.', 'danger'); return; }
        const cantidadEmpleados = Number.parseInt(document.getElementById('campoCantidadEmpleadosPost').value || '0', 10);
        const vehiculos = parsearVehiculosDesdeTexto(document.getElementById('campoVehiculosPost').value || '');
        if (vehiculos.some(v => !v.placa || !v.tipo)) {
            mostrarAlerta('Cada vehículo debe tener placa y tipo.', 'danger');
            return;
        }
        const respuesta = await ApiCliente.parche(`/api/empresas/${empresaId}/servicios-post-radicacion`, { cantidadEmpleados, vehiculos });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(error?.message || 'No se pudieron guardar los servicios post-radicación.', 'danger');
            return;
        }
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
        abrirAgregarEmpleados, confirmarAgregarEmpleados,
        abrirListadoVehiculos, mostrarFormularioVehiculo, ocultarFormularioVehiculo,
        guardarVehiculo, quitarVehiculo,
        abrirServiciosModal, guardarServiciosModal
    };
})();
