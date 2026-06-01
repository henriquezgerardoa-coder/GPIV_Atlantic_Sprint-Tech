const ModuloInfraestructura = (() => {
    let _servicioModificarId = null;
    let _esGestor = false;

    function _esc(val) {
        if (val == null) return '';
        return String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

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
                <div class="card border-0 shadow-sm h-100 ${clsCard}">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="fw-bold mb-0">${_esc(s.nombre)}</h6>
                            <span class="badge ${clsBadge}"><i class="bi ${icono} me-1"></i>${label}</span>
                        </div>
                        ${s.descripcionTecnica ? `<p class="text-muted small mb-2">${_esc(s.descripcionTecnica)}</p>` : ''}
                        ${s.ultimoComentario ? `<p class="small fst-italic text-muted mb-1">"${_esc(s.ultimoComentario)}"</p>` : ''}
                        <small class="text-muted">
                            ${s.ultimoTecnicoResponsable ? `Por: ${_esc(s.ultimoTecnicoResponsable)} · ` : ''}
                            ${s.fechaUltimaActualizacion ? s.fechaUltimaActualizacion.substring(0, 10) : ''}
                        </small>
                        ${_esGestor ? `
                        <div class="mt-2 pt-2 border-top text-end">
                            <button class="btn btn-sm btn-outline-primary"
                                onclick="ModuloInfraestructura.abrirModificacion(${s.id})">
                                <i class="bi bi-pencil-square me-1"></i>Modificar
                            </button>
                        </div>` : ''}
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

    async function abrirModificacion(servicioId) {
        _servicioModificarId = servicioId;
        const resp = await ApiCliente.obtener('/api/infraestructura');
        if (!resp?.ok) return;
        const servicios = await resp.json();
        const servicio = servicios.find(s => s.id === servicioId);
        if (!servicio) return;

        document.getElementById('campoNombreModificarServicio').value = servicio.nombre || '';
        document.getElementById('campoDescripcionModificarServicio').value = servicio.descripcionTecnica || '';
        document.getElementById('alertaModalModificarServicio').classList.add('d-none');

        const selector = document.getElementById('selectorEstadoModificar');
        if (selector) selector.value = servicio.estadoActual;
        const campoComentario = document.getElementById('campoComentarioModificar');
        if (campoComentario) campoComentario.value = '';
        document.getElementById('alertaEstadoModificar')?.classList.add('d-none');

        const btnEliminar = document.getElementById('btnEliminarServicio');
        if (btnEliminar) btnEliminar.classList.toggle('d-none', !Autenticacion.tieneAcceso(['ADMINISTRADOR']));

        await _cargarHistorialModificar(servicioId);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalModificarServicio')).show();
    }

    async function _cargarHistorialModificar(servicioId) {
        const contenedor = document.getElementById('historialModificarLista');
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
                    ${e.comentario ? `<p class="small text-muted mb-0 mt-1">${_esc(e.comentario)}</p>` : ''}
                    ${e.usuario ? `<small class="text-muted">Por: ${_esc(e.usuario)}</small>` : ''}
                </li>`;
            }).join('')
            + '</ul>';
    }

    async function guardarModificacion() {
        if (!_servicioModificarId) return;
        const alerta = document.getElementById('alertaModalModificarServicio');
        alerta.classList.add('d-none');
        const nombre = document.getElementById('campoNombreModificarServicio').value.trim();
        const descripcion = document.getElementById('campoDescripcionModificarServicio').value.trim() || null;
        if (!nombre) { alerta.textContent = 'El nombre del servicio es obligatorio.'; alerta.classList.remove('d-none'); return; }
        const resp = await ApiCliente.actualizar(`/api/infraestructura/${_servicioModificarId}`, { nombre, descripcionTecnica: descripcion });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || 'Error al modificar el servicio.';
            alerta.classList.remove('d-none');
            return;
        }
        await _cargarServicios();
        mostrarAlerta('Datos del servicio actualizados.');
    }

    async function guardarEstadoModificar() {
        if (!_servicioModificarId) return;
        const alerta = document.getElementById('alertaEstadoModificar');
        alerta?.classList.add('d-none');
        const estado = document.getElementById('selectorEstadoModificar').value;
        const comentario = document.getElementById('campoComentarioModificar').value.trim() || null;
        if ((estado === 'MANTENIMIENTO' || estado === 'FALLA_CRITICA') && !comentario) {
            if (alerta) { alerta.textContent = 'El comentario es obligatorio para este estado.'; alerta.classList.remove('d-none'); }
            return;
        }
        const resp = await ApiCliente.parche(`/api/infraestructura/${_servicioModificarId}/estado`, { estado, comentario });
        if (resp?.ok) {
            document.getElementById('campoComentarioModificar').value = '';
            await _cargarHistorialModificar(_servicioModificarId);
            await _cargarServicios();
            mostrarAlerta('Estado actualizado.');
        } else {
            const err = await resp?.json().catch(() => ({}));
            if (alerta) { alerta.textContent = err?.mensaje || 'Error al actualizar el estado.'; alerta.classList.remove('d-none'); }
        }
    }

    async function eliminar() {
        if (!_servicioModificarId) return;
        if (!confirm('¿Eliminar este servicio y todo su historial? Esta acción no se puede deshacer.')) return;
        const resp = await ApiCliente.eliminar(`/api/infraestructura/${_servicioModificarId}`);
        if (resp?.ok || resp?.status === 204) {
            bootstrap.Modal.getInstance(document.getElementById('modalModificarServicio'))?.hide();
            await _cargarServicios();
            mostrarAlerta('Servicio eliminado.');
        } else {
            const err = await resp?.json().catch(() => ({}));
            const alerta = document.getElementById('alertaModalModificarServicio');
            if (alerta) { alerta.textContent = err?.mensaje || 'Error al eliminar el servicio.'; alerta.classList.remove('d-none'); }
        }
    }

    function abrirCreacion() {
        document.getElementById('formNuevoServicio')?.reset();
        ocultarAlertaModal('alertaModalNuevoServicio');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevoServicio')).show();
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
        return cfg[estado] ?? { clsCard: '', clsBadge: 'bg-light text-muted', label: _esc(estado), icono: 'bi-circle' };
    }

    return { cargar, abrirCreacion, guardarNuevoServicio, abrirModificacion, guardarModificacion, guardarEstadoModificar, eliminar };
})();