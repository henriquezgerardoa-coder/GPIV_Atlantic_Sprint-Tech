const ModuloInfraestructura = (() => {
    let _servicioActualId = null;
    let _esGestor = false;

    async function cargar() {
        _esGestor = !Autenticacion.tieneAcceso(['EMPRESA']) ||
            Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        document.getElementById('btnNuevoServicio')?.classList.toggle('d-none', !_esGestor);
        await _cargarServicios();
    }

    async function _cargarServicios() {
        const grid = document.getElementById('gridServicios');
        if (!grid) return;
        grid.innerHTML = '<div class="col-12 text-muted text-center py-4 small"><i class="bi bi-hourglass-split me-2"></i>Cargando servicios...</div>';
        const resp = await ApiCliente.obtener('/api/infraestructura');
        if (!resp?.ok) { grid.innerHTML = '<div class="col-12 text-danger text-center py-4 small">Error al cargar servicios</div>'; return; }
        const servicios = await resp.json();

        _actualizarResumenEstado(servicios);

        if (!servicios.length) { grid.innerHTML = '<div class="col-12 text-muted text-center py-4 small">No hay servicios registrados.</div>'; return; }
        grid.innerHTML = servicios.map(s => {
            const { clsCard, clsBadge, label, icono } = _cfgEstado(s.estadoActual);
            return `
            <div class="col-md-6 col-lg-4">
                <div class="card border-0 shadow-sm h-100 ${clsCard}" style="cursor:pointer"
                     onclick="ModuloInfraestructura.abrirDetalle(${s.id})">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="fw-bold mb-0">${s.nombre}</h6>
                            <span class="badge ${clsBadge}"><i class="bi ${icono} me-1"></i>${label}</span>
                        </div>
                        ${s.descripcionTecnica ? `<p class="text-muted small mb-2">${s.descripcionTecnica}</p>` : ''}
                        ${s.ultimoComentario ? `<p class="small fst-italic text-muted mb-1">"${s.ultimoComentario}"</p>` : ''}
                        <small class="text-muted">
                            ${s.ultimoTecnicoResponsable ? `Por: ${s.ultimoTecnicoResponsable} · ` : ''}
                            ${s.fechaUltimaActualizacion ? s.fechaUltimaActualizacion.substring(0, 10) : ''}
                        </small>
                    </div>
                </div>
            </div>`;
        }).join('');
    }

    function _actualizarResumenEstado(servicios) {
        const el = document.getElementById('resumenEstadoServicios');
        if (!el) return;
        const operativos = servicios.filter(s => s.estadoActual === 'OPERATIVO').length;
        const fallas = servicios.filter(s => s.estadoActual === 'FALLA_CRITICA').length;
        const mant = servicios.filter(s => s.estadoActual === 'MANTENIMIENTO').length;
        el.innerHTML = `
            <span class="badge bg-success-subtle text-success me-1">${operativos} operativos</span>
            ${mant ? `<span class="badge bg-warning-subtle text-warning me-1">${mant} mantenimiento</span>` : ''}
            ${fallas ? `<span class="badge bg-danger-subtle text-danger me-1">${fallas} falla crítica</span>` : ''}`;
    }

    async function abrirDetalle(servicioId) {
        _servicioActualId = servicioId;
        const resp = await ApiCliente.obtener('/api/infraestructura');
        if (!resp?.ok) return;
        const servicios = await resp.json();
        const servicio = servicios.find(s => s.id === servicioId);
        if (!servicio) return;

        document.getElementById('tituloModalServicio').textContent = servicio.nombre;
        const { clsBadge, label, icono } = _cfgEstado(servicio.estadoActual);
        document.getElementById('estadoBadgeServicio').innerHTML =
            `<span class="badge ${clsBadge}"><i class="bi ${icono} me-1"></i>${label}</span>`;

        document.getElementById('bloqueActualizarEstado')?.classList.toggle('d-none', !_esGestor);
        if (_esGestor) {
            const selector = document.getElementById('selectorNuevoEstado');
            if (selector) selector.value = servicio.estadoActual;
            const campoComentario = document.getElementById('campoComentarioServicio');
            if (campoComentario) campoComentario.value = '';
        }

        await _cargarHistorial(servicioId);
        ocultarAlertaModal('alertaModalServicio');
        new bootstrap.Modal(document.getElementById('modalDetalleServicio')).show();
    }

    async function _cargarHistorial(servicioId) {
        const contenedor = document.getElementById('historialServicioLista');
        if (!contenedor) return;
        contenedor.innerHTML = '<p class="text-muted small">Cargando historial...</p>';
        const resp = await ApiCliente.obtener(`/api/infraestructura/${servicioId}/historial`);
        if (!resp?.ok) { contenedor.innerHTML = '<p class="text-danger small">Error al cargar historial.</p>'; return; }
        const eventos = await resp.json();
        if (!eventos.length) { contenedor.innerHTML = '<p class="text-muted small">Sin eventos registrados.</p>'; return; }
        contenedor.innerHTML = '<ul class="list-group list-group-flush">'
            + eventos.map(e => {
                const { clsBadge, label, icono } = _cfgEstado(e.estado);
                return `
                <li class="list-group-item px-0">
                    <div class="d-flex justify-content-between align-items-start">
                        <span class="badge ${clsBadge}"><i class="bi ${icono} me-1"></i>${label}</span>
                        <small class="text-muted">${e.fechaEvento ? e.fechaEvento.substring(0, 10) : ''}</small>
                    </div>
                    ${e.comentario ? `<p class="small text-muted mb-0 mt-1">${e.comentario}</p>` : ''}
                    ${e.usuario ? `<small class="text-muted">Por: ${e.usuario}</small>` : ''}
                </li>`;
            }).join('')
            + '</ul>';
    }

    async function guardarEstado() {
        if (!_servicioActualId) return;
        ocultarAlertaModal('alertaModalServicio');
        const estado = document.getElementById('selectorNuevoEstado').value;
        const comentario = document.getElementById('campoComentarioServicio').value.trim() || null;
        if ((estado === 'MANTENIMIENTO' || estado === 'FALLA_CRITICA') && !comentario) {
            mostrarAlertaModal('alertaModalServicio', 'El comentario es obligatorio para este estado.');
            return;
        }
        const resp = await ApiCliente.parche(`/api/infraestructura/${_servicioActualId}/estado`, { estado, comentario });
        if (resp?.ok) {
            await _cargarHistorial(_servicioActualId);
            const respLista = await ApiCliente.obtener('/api/infraestructura');
            if (respLista?.ok) {
                const servicios = await respLista.json();
                const s = servicios.find(x => x.id === _servicioActualId);
                if (s) {
                    const { clsBadge, label, icono } = _cfgEstado(s.estadoActual);
                    document.getElementById('estadoBadgeServicio').innerHTML =
                        `<span class="badge ${clsBadge}"><i class="bi ${icono} me-1"></i>${label}</span>`;
                    _actualizarResumenEstado(servicios);
                }
            }
            await _cargarServicios();
            mostrarAlerta('Estado actualizado.');
        } else {
            const err = await resp.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalServicio', err?.mensaje || 'Error al actualizar el estado.');
        }
    }

    function abrirCreacion() {
        document.getElementById('formNuevoServicio')?.reset();
        ocultarAlertaModal('alertaModalNuevoServicio');
        new bootstrap.Modal(document.getElementById('modalNuevoServicio')).show();
    }

    async function guardarNuevoServicio() {
        ocultarAlertaModal('alertaModalNuevoServicio');
        const nombre = document.getElementById('campoNombreServicio').value.trim();
        const descripcion = document.getElementById('campoDescripcionServicio').value.trim() || null;
        if (!nombre) { mostrarAlertaModal('alertaModalNuevoServicio', 'El nombre del servicio es obligatorio.'); return; }
        const resp = await ApiCliente.crear('/api/infraestructura', { nombre, descripcionTecnica: descripcion });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalNuevoServicio', err?.mensaje || 'Error al crear el servicio.');
            return;
        }
        bootstrap.Modal.getInstance(document.getElementById('modalNuevoServicio'))?.hide();
        await _cargarServicios();
        mostrarAlerta('Servicio creado correctamente.');
    }

    function _cfgEstado(estado) {
        const cfg = {
            OPERATIVO:    { clsCard: 'border-success border-opacity-25', clsBadge: 'bg-success-subtle text-success', label: 'Operativo',    icono: 'bi-check-circle-fill' },
            MANTENIMIENTO:{ clsCard: 'border-warning border-opacity-25', clsBadge: 'bg-warning-subtle text-warning', label: 'Mantenimiento', icono: 'bi-tools' },
            FALLA_CRITICA:{ clsCard: 'border-danger border-opacity-25',  clsBadge: 'bg-danger-subtle text-danger',   label: 'Falla crítica', icono: 'bi-exclamation-triangle-fill' },
        };
        return cfg[estado] ?? { clsCard: '', clsBadge: 'bg-light text-muted', label: estado, icono: 'bi-circle' };
    }

    return { cargar, abrirDetalle, guardarEstado, abrirCreacion, guardarNuevoServicio };
})();
