const ModuloProyectos = (() => {
    let _proyectoActualId = null;
    let _esGestor = false;

    async function cargar() {
        _esGestor = !Autenticacion.tieneAcceso(['EMPRESA']) ||
            Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);

        document.getElementById('btnNuevoProyecto')?.classList.toggle('d-none', !_esGestor);

        await Promise.all([_cargarProyectos(), _verificarAlertas()]);
    }

    async function _cargarProyectos() {
        const resp = await ApiCliente.obtener('/api/proyectos');
        const tbody = document.getElementById('cuerpoTablaProyectos');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="7" class="text-danger text-center py-3">Error al cargar proyectos</td></tr>'; return; }
        const datos = await resp.json();
        if (!datos.length) { tbody.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-3">Sin proyectos registrados</td></tr>'; return; }
        tbody.innerHTML = datos.map(p => {
            const estadoBadge = _badgeEstado(p.estado);
            const avance = p.avanceFisico ?? 0;
            const colorBarra = avance >= 100 ? 'bg-success' : avance >= 50 ? 'bg-primary' : 'bg-warning';
            return `
            <tr>
                <td class="ps-3">
                    <div class="fw-semibold">${p.nombre}</div>
                    ${p.descripcion ? `<small class="text-muted">${p.descripcion.substring(0, 60)}${p.descripcion.length > 60 ? '…' : ''}</small>` : ''}
                </td>
                <td>${p.nombreEmpresa || '-'}</td>
                <td>${estadoBadge}</td>
                <td>
                    <div class="progress" style="height:8px">
                        <div class="progress-bar ${colorBarra}" style="width:${avance}%"></div>
                    </div>
                    <small class="text-muted">${avance}%</small>
                </td>
                <td>${p.fechaEstimadaFin ? p.fechaEstimadaFin.substring(0, 10) : '-'}</td>
                <td>${p.responsableSeguimiento || '-'}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary me-1" title="Ver hitos"
                            onclick="ModuloProyectos.abrirHitos(${p.id})">
                        <i class="bi bi-list-check"></i>
                    </button>
                    ${_esGestor ? `<button class="btn btn-sm btn-outline-secondary" title="Cambiar estado"
                            onclick="ModuloProyectos.abrirCambioEstado(${p.id}, '${p.estado}')">
                        <i class="bi bi-pencil"></i>
                    </button>` : ''}
                </td>
            </tr>`;
        }).join('');
    }

    async function _verificarAlertas() {
        const resp = await ApiCliente.obtener('/api/proyectos/alertas');
        const bloque = document.getElementById('bloqueAlertasHitos');
        const lista = document.getElementById('listaAlertasHitos');
        if (!bloque || !lista) return;
        if (!resp?.ok) { bloque.classList.add('d-none'); return; }
        const alertas = await resp.json();
        if (!alertas.length) { bloque.classList.add('d-none'); return; }
        bloque.classList.remove('d-none');
        lista.innerHTML = alertas.map(a => `
            <li class="list-group-item border-0 py-1 px-0 bg-transparent">
                <i class="bi bi-clock-history me-1 text-danger"></i>
                <strong>${a.descripcion}</strong>
                ${a.fechaVencimiento ? ` — venció el ${a.fechaVencimiento.substring(0, 10)}` : ''}
            </li>`).join('');
    }

    async function abrirHitos(proyectoId) {
        _proyectoActualId = proyectoId;
        const resp = await ApiCliente.obtener('/api/proyectos');
        if (!resp?.ok) return;
        const proyectos = await resp.json();
        const proyecto = proyectos.find(p => p.id === proyectoId);
        if (!proyecto) return;

        document.getElementById('tituloModalHitos').textContent = proyecto.nombre;
        document.getElementById('subtituloModalHitos').textContent = proyecto.nombreEmpresa || '';
        document.getElementById('bloqueFormNuevoHito')?.classList.toggle('d-none', !_esGestor);
        _renderizarHitos(proyecto.hitos || []);
        new bootstrap.Modal(document.getElementById('modalHitosProyecto')).show();
    }

    function _renderizarHitos(hitos) {
        const contenedor = document.getElementById('listaHitosModal');
        if (!contenedor) return;
        if (!hitos.length) { contenedor.innerHTML = '<p class="text-muted small">Sin hitos registrados.</p>'; return; }
        contenedor.innerHTML = '<ul class="list-group list-group-flush">'
            + hitos.map(h => `
            <li class="list-group-item d-flex align-items-start gap-2 px-0">
                <span class="mt-1">
                    ${h.cumplido
                        ? '<i class="bi bi-check-circle-fill text-success"></i>'
                        : h.vencido
                            ? '<i class="bi bi-x-circle-fill text-danger"></i>'
                            : '<i class="bi bi-circle text-muted"></i>'}
                </span>
                <div class="flex-grow-1">
                    <span class="${h.cumplido ? 'text-decoration-line-through text-muted' : ''}">${h.descripcion}</span>
                    ${h.fechaVencimiento ? `<small class="d-block text-muted">Plazo: ${h.fechaVencimiento.substring(0, 10)}</small>` : ''}
                </div>
                ${_esGestor && !h.cumplido ? `
                <button class="btn btn-sm btn-outline-success" onclick="ModuloProyectos.marcarHitoCumplido(${h.id})" title="Marcar cumplido">
                    <i class="bi bi-check-lg"></i>
                </button>` : ''}
                ${_esGestor ? `
                <button class="btn btn-sm btn-outline-danger" onclick="ModuloProyectos.eliminarHito(${h.id})" title="Eliminar">
                    <i class="bi bi-trash3"></i>
                </button>` : ''}
            </li>`).join('')
            + '</ul>';
    }

    async function agregarHito() {
        if (!_proyectoActualId) return;
        ocultarAlertaModal('alertaModalHitos');
        const descripcion = document.getElementById('campoDescripcionHito').value.trim();
        const fecha = document.getElementById('campoFechaHito').value || null;
        if (!descripcion) { mostrarAlertaModal('alertaModalHitos', 'La descripción del hito es obligatoria.'); return; }
        const resp = await ApiCliente.crear(`/api/proyectos/${_proyectoActualId}/hitos`, {
            descripcion, fechaVencimiento: fecha
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalHitos', err?.mensaje || 'Error al agregar el hito.');
            return;
        }
        document.getElementById('formNuevoHito').reset();
        const nuevo = await resp.json();
        const respProy = await ApiCliente.obtener('/api/proyectos');
        if (respProy?.ok) {
            const proyectos = await respProy.json();
            const proyecto = proyectos.find(p => p.id === _proyectoActualId);
            _renderizarHitos(proyecto?.hitos || []);
        }
    }

    async function marcarHitoCumplido(hitoId) {
        if (!_proyectoActualId) return;
        const resp = await ApiCliente.parche(`/api/proyectos/${_proyectoActualId}/hitos/${hitoId}/cumplido`, {});
        if (resp?.ok) {
            await _recargarHitosEnModal();
            await _cargarProyectos();
        } else mostrarAlerta('No se pudo marcar el hito.', 'danger');
    }

    async function eliminarHito(hitoId) {
        if (!_proyectoActualId) return;
        const resp = await ApiCliente.eliminar(`/api/proyectos/${_proyectoActualId}/hitos/${hitoId}`);
        if (resp?.ok || resp?.status === 204) {
            await _recargarHitosEnModal();
            await _cargarProyectos();
        } else mostrarAlerta('No se pudo eliminar el hito.', 'danger');
    }

    async function _recargarHitosEnModal() {
        const resp = await ApiCliente.obtener('/api/proyectos');
        if (!resp?.ok) return;
        const proyectos = await resp.json();
        const proyecto = proyectos.find(p => p.id === _proyectoActualId);
        _renderizarHitos(proyecto?.hitos || []);
    }

    function abrirCambioEstado(proyectoId, estadoActual) {
        _proyectoActualId = proyectoId;
        const selector = document.getElementById('selectorEstadoProyecto');
        if (selector) selector.value = estadoActual;
        ocultarAlertaModal('alertaModalEstado');
        new bootstrap.Modal(document.getElementById('modalCambioEstadoProyecto')).show();
    }

    async function guardarEstado() {
        if (!_proyectoActualId) return;
        const estado = document.getElementById('selectorEstadoProyecto').value;
        const resp = await ApiCliente.parche(`/api/proyectos/${_proyectoActualId}/estado`, { estado });
        if (resp?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalCambioEstadoProyecto'))?.hide();
            await _cargarProyectos();
            mostrarAlerta('Estado actualizado.');
        } else {
            mostrarAlertaModal('alertaModalEstado', 'No se pudo actualizar el estado.');
        }
    }

    async function abrirCreacion() {
        ocultarAlertaModal('alertaModalNuevoProyecto');
        document.getElementById('formNuevoProyecto')?.reset();
        await _cargarRadicacionesSelector();
        new bootstrap.Modal(document.getElementById('modalNuevoProyecto')).show();
    }

    async function _cargarRadicacionesSelector() {
        const selector = document.getElementById('selectorRadicacionProyecto');
        if (!selector) return;
        const resp = await ApiCliente.obtener('/api/radicaciones');
        if (!resp?.ok) return;
        const radicaciones = await resp.json();
        selector.innerHTML = '<option value="">Sin radicación asociada</option>'
            + radicaciones.map(r => `<option value="${r.id}">${r.numeroRadicado} — ${r.nombreEmpresa || ''}</option>`).join('');
    }

    async function guardarNuevoProyecto() {
        ocultarAlertaModal('alertaModalNuevoProyecto');
        const nombre = document.getElementById('campoNombreProyecto').value.trim();
        const fechaFin = document.getElementById('campoFechaFinProyecto').value || null;
        const descripcion = document.getElementById('campoDescripcionProyecto').value.trim() || null;
        const monto = parseFloat(document.getElementById('campoMontoProyecto').value) || null;
        const radicacionId = document.getElementById('selectorRadicacionProyecto').value
            ? parseInt(document.getElementById('selectorRadicacionProyecto').value) : null;
        if (!nombre) { mostrarAlertaModal('alertaModalNuevoProyecto', 'El nombre del proyecto es obligatorio.'); return; }
        const resp = await ApiCliente.crear('/api/proyectos', {
            nombre, descripcion, montoInversion: monto,
            fechaEstimadaFin: fechaFin, radicacionId, responsableId: null
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalNuevoProyecto', err?.mensaje || 'Error al crear el proyecto.');
            return;
        }
        bootstrap.Modal.getInstance(document.getElementById('modalNuevoProyecto'))?.hide();
        await _cargarProyectos();
        mostrarAlerta('Proyecto creado correctamente.');
    }

    function _badgeEstado(estado) {
        const cfg = {
            PLANIFICADO:  ['bg-secondary-subtle text-secondary', 'Planificado'],
            EN_EJECUCION: ['bg-primary-subtle text-primary',    'En ejecución'],
            PAUSADO:      ['bg-warning-subtle text-warning',    'Pausado'],
            FINALIZADO:   ['bg-success-subtle text-success',    'Finalizado'],
            CANCELADO:    ['bg-danger-subtle text-danger',      'Cancelado'],
        };
        const [cls, label] = cfg[estado] ?? ['bg-light text-muted', estado];
        return `<span class="badge ${cls}">${label}</span>`;
    }

    return { cargar, abrirHitos, agregarHito, marcarHitoCumplido, eliminarHito, abrirCambioEstado, guardarEstado, abrirCreacion, guardarNuevoProyecto };
})();
