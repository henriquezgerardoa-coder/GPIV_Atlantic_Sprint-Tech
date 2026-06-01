const ModuloMonitor = (() => {
    let _todos = [];
    let _filtroEstado = null;

    function _esc(val) {
        if (val == null) return '';
        return String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    async function cargar() {
        _filtroEstado = null;
        document.getElementById('btnLimpiarFiltroMonitor')?.classList.add('d-none');
        await _cargarExpedientes();
    }

    async function _cargarExpedientes() {
        const resp = await ApiCliente.obtener('/api/monitor/expedientes');
        const tbody = document.getElementById('cuerpoCuerpoMonitor');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="8" class="text-danger text-center py-3">Error al cargar expedientes</td></tr>'; return; }
        _todos = await resp.json();
        _actualizarTarjetasEstado(_todos);
        _renderizarTabla(_filtroEstado ? _todos.filter(e => e.estado === _filtroEstado) : _todos);
    }

    function _actualizarTarjetasEstado(expedientes) {
        const contenedor = document.getElementById('tarjetasEstadoMonitor');
        if (!contenedor) return;
        const estados = {};
        let conAlerta = 0;
        expedientes.forEach(e => {
            estados[e.estado] = (estados[e.estado] || 0) + 1;
            if (e.plazoVencido) conAlerta++;
        });

        const cfgEstados = {
            PENDIENTE:          ['bg-secondary-subtle text-secondary', 'Pendiente'],
            EN_REVISION:        ['bg-primary-subtle text-primary',     'En revisión'],
            APROBADO:           ['bg-success-subtle text-success',     'Aprobado'],
            RECHAZADO:          ['bg-danger-subtle text-danger',       'Rechazado'],
            EN_OBRA:            ['bg-warning-subtle text-warning',     'En obra'],
            HABILITADO:         ['bg-success-subtle text-success',     'Habilitado'],
            REQUIERE_SUBSANACION: ['bg-warning-subtle text-warning',   'Subsanación'],
        };

        let html = '<div class="row g-2 mb-2">';
        if (conAlerta > 0) {
            html += `<div class="col-auto">
                <div class="card border-danger border-opacity-50 bg-danger-subtle" style="cursor:pointer"
                     onclick="ModuloMonitor.filtrarPorVencidos()">
                    <div class="card-body py-2 px-3 d-flex align-items-center gap-2">
                        <i class="bi bi-exclamation-triangle-fill text-danger"></i>
                        <div><div class="fw-bold text-danger">${conAlerta}</div>
                        <small class="text-danger">Plazo vencido</small></div>
                    </div>
                </div>
            </div>`;
        }
        Object.entries(estados).forEach(([estado, cantidad]) => {
            const [cls, label] = cfgEstados[estado] ?? ['bg-light text-muted', estado];
            html += `<div class="col-auto">
                <div class="card border-0 ${cls.replace('text-', 'border-').replace('bg-', 'bg-')}"
                     style="cursor:pointer" onclick="ModuloMonitor.filtrarPorEstado('${estado}')">
                    <div class="card-body py-2 px-3">
                        <div class="fw-bold">${cantidad}</div>
                        <small>${_esc(label)}</small>
                    </div>
                </div>
            </div>`;
        });
        html += '</div>';
        contenedor.innerHTML = html;
    }

    function _renderizarTabla(expedientes) {
        const tbody = document.getElementById('cuerpoCuerpoMonitor');
        if (!tbody) return;
        if (!expedientes.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-muted text-center py-3">Sin expedientes</td></tr>';
            return;
        }
        tbody.innerHTML = expedientes.map(e => {
            const badgeEstado = _badgeEstado(e.estado);
            const alerta = e.plazoVencido
                ? '<span class="badge bg-danger ms-1" title="Plazo vencido"><i class="bi bi-exclamation-triangle-fill"></i></span>'
                : '';
            const diasCls = e.diasSinMovimiento > 30 ? 'text-danger fw-bold'
                : e.diasSinMovimiento > 14 ? 'text-warning fw-semibold' : 'text-muted';
            return `
            <tr>
                <td class="ps-3 fw-semibold">${_esc(e.numeroRadicado) || e.id}${alerta}</td>
                <td>${_esc(e.nombreEmpresa) || '-'}</td>
                <td><small class="text-muted">${_esc(e.tipoSolicitud) || '-'}</small></td>
                <td>${badgeEstado}</td>
                <td><small>${e.fechaRadicacion ? e.fechaRadicacion.substring(0, 10) : '-'}</small></td>
                <td><small ${e.plazoVencido ? 'class="text-danger fw-semibold"' : ''}>${e.fechaPlazo ? e.fechaPlazo.substring(0, 10) : '-'}</small></td>
                <td class="${diasCls}"><small>${e.diasSinMovimiento}d</small></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary" title="Ver timeline"
                            onclick="ModuloMonitor.abrirTimeline(${e.id})">
                        <i class="bi bi-diagram-3"></i>
                    </button>
                </td>
            </tr>`;
        }).join('');
    }

    function filtrarPorEstado(estado) {
        _filtroEstado = estado;
        document.getElementById('btnLimpiarFiltroMonitor')?.classList.remove('d-none');
        _renderizarTabla(_todos.filter(e => e.estado === estado));
    }

    function filtrarPorVencidos() {
        _filtroEstado = '__VENCIDOS__';
        document.getElementById('btnLimpiarFiltroMonitor')?.classList.remove('d-none');
        _renderizarTabla(_todos.filter(e => e.plazoVencido));
    }

    function limpiarFiltro() {
        _filtroEstado = null;
        document.getElementById('btnLimpiarFiltroMonitor')?.classList.add('d-none');
        _renderizarTabla(_todos);
    }

    async function abrirTimeline(expedienteId) {
        const expediente = _todos.find(e => e.id === expedienteId);
        if (!expediente) return;

        document.getElementById('tituloTimeline').textContent = expediente.numeroRadicado || `Exp. #${expedienteId}`;
        document.getElementById('subtituloTimeline').textContent = expediente.nombreEmpresa || '';

        const badgeActual = _badgeEstado(expediente.estado);
        document.getElementById('estadoActualTimeline').innerHTML =
            `<span class="fw-semibold me-2">Estado actual:</span>${badgeActual}
             ${expediente.plazoVencido ? '<span class="badge bg-danger ms-2"><i class="bi bi-exclamation-triangle-fill me-1"></i>Plazo vencido</span>' : ''}`;

        const cuerpo = document.getElementById('cuerpoTimeline');
        if (cuerpo) {
            const eventos = [];
            if (expediente.fechaRadicacion) eventos.push({ fecha: expediente.fechaRadicacion, texto: 'Radicación presentada', tipo: 'info' });
            if (expediente.fechaUltimaActualizacion) eventos.push({ fecha: expediente.fechaUltimaActualizacion, texto: `Última actualización — ${_esc(expediente.estado)}`, tipo: 'primary' });
            if (expediente.fechaAprobacion) eventos.push({ fecha: expediente.fechaAprobacion, texto: 'Aprobado / Habilitado', tipo: 'success' });
            if (expediente.fechaPlazo) eventos.push({ fecha: expediente.fechaPlazo, texto: 'Plazo estimado de obra', tipo: expediente.plazoVencido ? 'danger' : 'warning' });

            eventos.sort((a, b) => a.fecha.localeCompare(b.fecha));

            cuerpo.innerHTML = eventos.length
                ? '<ul class="timeline-list list-unstyled">'
                    + eventos.map(ev => `
                    <li class="d-flex gap-3 mb-3">
                        <div class="flex-shrink-0">
                            <span class="badge rounded-pill bg-${ev.tipo}-subtle text-${ev.tipo} border border-${ev.tipo} border-opacity-25"
                                  style="width:10px;height:10px;display:inline-block;padding:0"></span>
                        </div>
                        <div>
                            <div class="fw-semibold small">${ev.texto}</div>
                            <small class="text-muted">${ev.fecha.substring(0, 10)}</small>
                        </div>
                    </li>`).join('')
                    + '</ul>'
                : '<p class="text-muted small">Sin eventos registrados.</p>';
        }

        new bootstrap.Modal(document.getElementById('modalTimeline')).show();
    }

    function _badgeEstado(estado) {
        const cfg = {
            PENDIENTE:            ['bg-secondary-subtle text-secondary', 'Pendiente'],
            EN_REVISION:          ['bg-primary-subtle text-primary',     'En revisión'],
            APROBADO:             ['bg-success-subtle text-success',     'Aprobado'],
            RECHAZADO:            ['bg-danger-subtle text-danger',       'Rechazado'],
            EN_OBRA:              ['bg-warning-subtle text-warning',     'En obra'],
            HABILITADO:           ['bg-success-subtle text-success',     'Habilitado'],
            REQUIERE_SUBSANACION: ['bg-warning-subtle text-warning',     'Subsanación'],
        };
        const [cls, label] = cfg[estado] ?? ['bg-light text-muted', estado];
        return `<span class="badge ${cls}">${_esc(label)}</span>`;
    }

    return { cargar, filtrarPorEstado, filtrarPorVencidos, limpiarFiltro, abrirTimeline };
})();
