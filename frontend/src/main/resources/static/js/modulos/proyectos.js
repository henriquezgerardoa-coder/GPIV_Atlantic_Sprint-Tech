const ModuloProyectos = (() => {
    let _proyectoActualId = null;
    let _proyectos = [];
    let _esTecnico = false;
    let _puedeGestionarHitos = false;
    let _puedeGestionarEstado = false;
    let _puedeCrearProyecto = false;

    function _esc(val) {
        if (val == null) return '';
        return String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    async function cargar() {
        _esTecnico = Autenticacion.tieneAcceso(['TECNICO'])
            && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        _puedeGestionarHitos = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO', 'TECNICO']);
        _puedeGestionarEstado = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO', 'TECNICO']);
        _puedeCrearProyecto = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO']);

        document.getElementById('btnNuevoProyecto')?.classList.toggle('d-none', !_puedeCrearProyecto);

        await Promise.all([_cargarProyectos(), _verificarAlertas()]);
    }

    async function _cargarProyectos() {
        const resp = await ApiCliente.obtener('/api/proyectos');
        const tbody = document.getElementById('cuerpoTablaProyectos');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="7" class="text-danger text-center py-3">Error al cargar proyectos</td></tr>'; return; }
        const datos = await resp.json();
        if (!datos.length) { tbody.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-3">Sin proyectos registrados</td></tr>'; return; }
        _proyectos = datos;
        _renderizarHitosProximos(datos);
        tbody.innerHTML = datos.map(p => {
            const estadoBadge = _badgeEstado(p.estado);
            const avance = p.avanceFisico ?? 0;
            const colorBarra = avance >= 100 ? 'bg-success' : avance >= 50 ? 'bg-primary' : 'bg-warning';
            return `
            <tr>
                <td class="ps-3">
                    <div class="fw-semibold">${_esc(p.nombre)}</div>
                    ${p.descripcion ? `<small class="text-muted">${_esc(p.descripcion.substring(0, 60))}${p.descripcion.length > 60 ? '…' : ''}</small>` : ''}
                </td>
                <td>${_esc(p.nombreEmpresa) || '-'}</td>
                <td>${estadoBadge}</td>
                <td>
                    <div class="progress" style="height:8px">
                        <div class="progress-bar ${colorBarra}" style="width:${avance}%"></div>
                    </div>
                    <small class="text-muted">${avance}%</small>
                    ${p.tieneHitosVencidos ? ' <i class="bi bi-exclamation-triangle-fill text-danger" title="Hitos vencidos"></i>' : ''}
                </td>
                <td>${p.fechaEstimadaFin ? p.fechaEstimadaFin.substring(0, 10) : '-'}</td>
                <td>${_esc(p.responsableSeguimiento) || '-'}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary me-1" title="Ver detalle e hitos"
                            onclick="ModuloProyectos.abrirHitos(${p.id})">
                        <i class="bi bi-list-check"></i>
                    </button>
                    ${_puedeGestionarEstado ? `<button class="btn btn-sm btn-outline-secondary" title="Cambiar estado"
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
                <strong>${_esc(a.descripcion)}</strong>
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
        document.getElementById('bloqueFormNuevoHito')?.classList.toggle('d-none', !_puedeGestionarHitos);
        _renderizarDetalleProyecto(proyecto);
        _renderizarHitos(proyecto.hitos || []);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalHitosProyecto')).show();
    }

    function _renderizarDetalleProyecto(p) {
        const contenedor = document.getElementById('detalleProyectoModal');
        if (!contenedor) return;
        const fmtFecha = f => f ? f.substring(0, 10) : '—';
        const fmtMonto = m => m != null
            ? new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(m)
            : '—';
        contenedor.innerHTML = `
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Estado:</span>
                <span class="ms-1">${_badgeEstado(p.estado)}</span>
                ${p.tieneHitosVencidos ? '<span class="badge bg-danger ms-1"><i class="bi bi-exclamation-triangle-fill me-1"></i>Hitos vencidos</span>' : ''}
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Avance físico:</span>
                <span class="ms-1 fw-semibold">${p.avanceFisico ?? 0}%</span>
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Responsable:</span>
                <span class="ms-1">${_esc(p.responsableSeguimiento) || '—'}</span>
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Inicio real:</span>
                <span class="ms-1">${fmtFecha(p.fechaInicioReal)}</span>
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Fin estimado:</span>
                <span class="ms-1">${fmtFecha(p.fechaEstimadaFin)}</span>
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Creación:</span>
                <span class="ms-1">${p.fechaCreacion ? p.fechaCreacion.substring(0, 10) : '—'}</span>
            </div>
            <div class="col-sm-6 col-md-4">
                <span class="text-muted">Monto inversión:</span>
                <span class="ms-1">${fmtMonto(p.montoInversion)}</span>
            </div>
            ${p.descripcion ? `<div class="col-12"><span class="text-muted">Descripción:</span> <span class="ms-1">${_esc(p.descripcion)}</span></div>` : ''}`;
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
                    <span class="${h.cumplido ? 'text-decoration-line-through text-muted' : ''}">${_esc(h.descripcion)}</span>
                    ${h.fechaVencimiento ? `<small class="d-block text-muted">Plazo: ${h.fechaVencimiento.substring(0, 10)}</small>` : ''}
                </div>
                ${_puedeGestionarHitos && !h.cumplido ? `
                <button class="btn btn-sm btn-outline-success" onclick="ModuloProyectos.marcarHitoCumplido(${h.id})" title="Marcar cumplido">
                    <i class="bi bi-check-lg"></i>
                </button>` : ''}
                ${_puedeGestionarHitos && !h.cumplido ? `
                <button class="btn btn-sm btn-outline-danger" onclick="ModuloProyectos.eliminarHito(${h.id})" title="Eliminar">
                    <i class="bi bi-trash3"></i>
                </button>` : ''}
            </li>`).join('')
            + '</ul>';
    }

    async function agregarHito() {
        if (!_puedeGestionarHitos) {
            mostrarAlertaModal('alertaModalHitos', 'No tienes permisos para gestionar hitos.');
            return;
        }
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

    async function marcarHitoCumplido(hitoId, proyectoId) {
        if (!_puedeGestionarHitos) {
            mostrarAlerta('No tienes permisos para gestionar hitos.', 'danger');
            return;
        }
        const pid = proyectoId ?? _proyectoActualId;
        if (!pid) return;
        const resp = await ApiCliente.parche(`/api/proyectos/${pid}/hitos/${hitoId}/cumplido`, {});
        if (resp?.ok) {
            if (_proyectoActualId === pid) await _recargarHitosEnModal();
            await _cargarProyectos();
        } else mostrarAlerta('No se pudo marcar el hito.', 'danger');
    }

    async function eliminarHito(hitoId) {
        if (!_puedeGestionarHitos) {
            mostrarAlerta('No tienes permisos para gestionar hitos.', 'danger');
            return;
        }
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
        if (!_puedeGestionarEstado) {
            mostrarAlerta('No tienes permisos para cambiar el estado del proyecto.', 'danger');
            return;
        }
        _proyectoActualId = proyectoId;
        const selector = document.getElementById('selectorEstadoProyecto');
        if (selector) selector.value = estadoActual;
        ocultarAlertaModal('alertaModalEstado');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalCambioEstadoProyecto')).show();
    }

    async function guardarEstado() {
        if (!_puedeGestionarEstado) {
            mostrarAlertaModal('alertaModalEstado', 'No tienes permisos para cambiar el estado del proyecto.');
            return;
        }
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
        if (!_puedeCrearProyecto) {
            mostrarAlerta('No tienes permisos para crear proyectos.', 'danger');
            return;
        }
        ocultarAlertaModal('alertaModalNuevoProyecto');
        document.getElementById('formNuevoProyecto')?.reset();
        await _cargarRadicacionesSelector();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevoProyecto')).show();
    }

    async function _cargarRadicacionesSelector() {
        const selector = document.getElementById('selectorRadicacionProyecto');
        if (!selector) return;
        const resp = await ApiCliente.obtener('/api/radicaciones');
        if (!resp?.ok) return;
        const radicaciones = await resp.json();
        selector.innerHTML = '<option value="">Sin radicación asociada</option>'
            + radicaciones.map(r => `<option value="${r.id}">${_esc(r.numeroRadicado)} — ${_esc(r.nombreEmpresa) || ''}</option>`).join('');
    }

    async function guardarNuevoProyecto() {
        if (!_puedeCrearProyecto) {
            mostrarAlertaModal('alertaModalNuevoProyecto', 'No tienes permisos para crear proyectos.');
            return;
        }
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

    function _renderizarHitosProximos(proyectos) {
        const panel  = document.getElementById('bloqueHitosProximos');
        const lista  = document.getElementById('listaHitosProximos');
        const badge  = document.getElementById('badgeHitosProximos');
        if (!panel || !lista) return;

        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        const limite = new Date(hoy);
        limite.setDate(limite.getDate() + 30);

        const proximos = [];
        proyectos.forEach(p => {
            (p.hitos || []).forEach(h => {
                if (h.cumplido || !h.fechaVencimiento) return;
                const fv = new Date(h.fechaVencimiento);
                if (fv <= limite) {
                    proximos.push({
                        hitoId:        h.id,
                        proyectoId:    p.id,
                        nombreProyecto: p.nombre,
                        nombreEmpresa:  p.nombreEmpresa || '—',
                        descripcion:    h.descripcion,
                        fechaVencimiento: h.fechaVencimiento,
                        vencido:        h.vencido,
                        fv
                    });
                }
            });
        });

        proximos.sort((a, b) => a.fv - b.fv);

        if (!proximos.length) {
            panel.classList.add('d-none');
            return;
        }

        if (badge) badge.textContent = proximos.length;
        panel.classList.remove('d-none');
        lista.innerHTML = proximos.map(h => {
            const diffDias = Math.ceil((h.fv - hoy) / 86400000);
            let tiempoLabel;
            if (h.vencido || diffDias < 0) {
                tiempoLabel = `<span class="badge bg-danger">Vencido hace ${Math.abs(diffDias)} día${Math.abs(diffDias) !== 1 ? 's' : ''}</span>`;
            } else if (diffDias === 0) {
                tiempoLabel = '<span class="badge bg-danger">Vence hoy</span>';
            } else if (diffDias <= 7) {
                tiempoLabel = `<span class="badge bg-warning text-dark">${diffDias} día${diffDias !== 1 ? 's' : ''}</span>`;
            } else {
                tiempoLabel = `<span class="badge bg-secondary">${diffDias} días</span>`;
            }
            const btnVerProyecto = `<button class="btn btn-sm btn-outline-primary me-1" title="Ver proyecto"
                        onclick="ModuloProyectos.abrirHitos(${h.proyectoId})">
                    <i class="bi bi-list-check"></i>
                </button>`;
            const accion = btnVerProyecto + (_puedeGestionarHitos ? `
                <button class="btn btn-sm btn-outline-success" title="Marcar cumplido"
                        onclick="ModuloProyectos.marcarHitoCumplido(${h.hitoId}, ${h.proyectoId})">
                    <i class="bi bi-check-lg"></i>
                </button>` : '');
            return `
            <tr>
                <td class="ps-3 fw-semibold">${_esc(h.nombreProyecto)}</td>
                <td class="text-muted small">${_esc(h.nombreEmpresa)}</td>
                <td>${_esc(h.descripcion)}</td>
                <td class="small">${h.fechaVencimiento}</td>
                <td>${tiempoLabel}</td>
                <td class="text-center">${accion}</td>
            </tr>`;
        }).join('');
    }

    function _badgeEstado(estado) {
        const cfg = {
            INICIADO:     ['bg-secondary-subtle text-secondary', 'Iniciado'],
            EN_EJECUCION: ['bg-primary-subtle text-primary',    'En ejecución'],
            DETENIDO:     ['bg-warning-subtle text-warning',    'Detenido'],
            COMPLETADO:   ['bg-success-subtle text-success',    'Completado'],
            CANCELADO:    ['bg-danger-subtle text-danger',      'Cancelado'],
        };
        const [cls, label] = cfg[estado] ?? ['bg-light text-muted', estado];
        return `<span class="badge ${cls}">${_esc(label)}</span>`;
    }

    return { cargar, abrirHitos, agregarHito, marcarHitoCumplido, eliminarHito, abrirCambioEstado, guardarEstado, abrirCreacion, guardarNuevoProyecto };
})();
