const ModuloMonitor = (() => {
    let expedientes = [];

    const ESTADOS_ORDEN = [
        'PENDIENTE',
        'EN_REVISION',
        'REQUIERE_INFORMACION_ADICIONAL',
        'APROBADA',
        'RADICADA',
        'RECHAZADA',
        'CANCELADA'
    ];

    const ESTADOS_FINALES = new Set(['RADICADA', 'RECHAZADA', 'CANCELADA']);

    const CONFIG_ESTADO = {
        PENDIENTE:                       { label: 'Pendiente',                color: 'secondary', icon: 'hourglass-split' },
        EN_REVISION:                     { label: 'En revisión',              color: 'info',      icon: 'search' },
        REQUIERE_INFORMACION_ADICIONAL:  { label: 'Req. información',         color: 'warning',   icon: 'exclamation-circle' },
        APROBADA:                        { label: 'Aprobada',                 color: 'primary',   icon: 'check-circle' },
        RADICADA:                        { label: 'Radicada',                 color: 'success',   icon: 'patch-check-fill' },
        RECHAZADA:                       { label: 'Rechazada',                color: 'danger',    icon: 'x-circle' },
        CANCELADA:                       { label: 'Cancelada',                color: 'dark',      icon: 'slash-circle' }
    };

    async function cargar() {
        const resp = await ApiCliente.obtener('/api/monitor/expedientes');
        const contenedor = document.getElementById('contenedorMonitor');
        if (!contenedor) return;
        if (!resp?.ok) {
            contenedor.innerHTML = '<p class="text-danger">Error al cargar el monitor.</p>';
            return;
        }
        expedientes = await resp.json();
        renderizarResumen();
        renderizarTabla();
    }

    function renderizarResumen() {
        const conteo = {};
        ESTADOS_ORDEN.forEach(e => conteo[e] = 0);
        let totalPlazoVencido = 0;
        let totalSinMovimiento = 0;

        expedientes.forEach(e => {
            if (conteo[e.estado] !== undefined) conteo[e.estado]++;
            if (e.plazoVencido) totalPlazoVencido++;
            if (!ESTADOS_FINALES.has(e.estado) && e.diasSinMovimiento > 30) totalSinMovimiento++;
        });

        // Tarjetas de estado
        const activos = ['PENDIENTE', 'EN_REVISION', 'REQUIERE_INFORMACION_ADICIONAL', 'APROBADA'];
        const finales = ['RADICADA', 'RECHAZADA', 'CANCELADA'];

        document.getElementById('tarjetasEstadoMonitor').innerHTML = `
            <div class="row g-2 mb-2">
                ${activos.map(e => {
                    const cfg = CONFIG_ESTADO[e];
                    return `
                    <div class="col-6 col-md-3">
                        <div class="card border-0 bg-light text-center py-2 h-100 cursor-pointer"
                             onclick="ModuloMonitor.filtrarPorEstado('${e}')" title="Filtrar por ${cfg.label}">
                            <div class="fs-4 fw-bold text-${cfg.color}">${conteo[e]}</div>
                            <div class="small text-muted">${cfg.label}</div>
                        </div>
                    </div>`;
                }).join('')}
            </div>
            <div class="row g-2 mb-3">
                ${finales.map(e => {
                    const cfg = CONFIG_ESTADO[e];
                    return `
                    <div class="col-4">
                        <div class="card border-0 bg-light text-center py-2 h-100 cursor-pointer"
                             onclick="ModuloMonitor.filtrarPorEstado('${e}')" title="Filtrar por ${cfg.label}">
                            <div class="fs-5 fw-bold text-${cfg.color}">${conteo[e]}</div>
                            <div class="small text-muted">${cfg.label}</div>
                        </div>
                    </div>`;
                }).join('')}
            </div>
            ${totalPlazoVencido > 0 ? `
            <div class="alert alert-danger py-2 d-flex align-items-center gap-2 mb-2">
                <i class="bi bi-exclamation-triangle-fill fs-5"></i>
                <strong>${totalPlazoVencido} expediente${totalPlazoVencido > 1 ? 's' : ''} con plazo de obra vencido</strong>
                <button class="btn btn-sm btn-outline-danger ms-auto" onclick="ModuloMonitor.filtrarPlazoVencido()">Ver</button>
            </div>` : ''}
            ${totalSinMovimiento > 0 ? `
            <div class="alert alert-warning py-2 d-flex align-items-center gap-2 mb-0">
                <i class="bi bi-clock-history fs-5"></i>
                <strong>${totalSinMovimiento} expediente${totalSinMovimiento > 1 ? 's' : ''} sin movimiento hace más de 30 días</strong>
                <button class="btn btn-sm btn-outline-warning ms-auto" onclick="ModuloMonitor.filtrarSinMovimiento()">Ver</button>
            </div>` : ''}`;
    }

    function renderizarTabla(filtro) {
        const lista = filtro ? filtro : expedientes;
        const cuerpo = document.getElementById('cuerpoCuerpoMonitor');
        if (!cuerpo) return;

        cuerpo.innerHTML = lista.length
            ? lista.map(e => {
                const cfg = CONFIG_ESTADO[e.estado] || { label: e.estado, color: 'secondary', icon: 'circle' };
                const alertaPlazo = e.plazoVencido
                    ? '<span class="badge bg-danger ms-1" title="Plazo vencido"><i class="bi bi-exclamation-triangle-fill"></i></span>'
                    : '';
                const alertaMovimiento = !ESTADOS_FINALES.has(e.estado) && e.diasSinMovimiento > 30
                    ? '<span class="badge bg-warning text-dark ms-1" title="Sin movimiento +30 días"><i class="bi bi-clock-history"></i></span>'
                    : '';
                return `
                <tr>
                    <td class="ps-3 fw-semibold text-nowrap">${e.numeroRadicado}${alertaPlazo}${alertaMovimiento}</td>
                    <td class="small">${e.nombreEmpresa}</td>
                    <td class="small text-muted">${e.tipoSolicitud || '-'}</td>
                    <td><span class="badge bg-${cfg.color} ${cfg.color === 'warning' ? 'text-dark' : ''}">${cfg.label}</span></td>
                    <td class="small text-muted">${e.fechaRadicacion || '-'}</td>
                    <td class="small text-muted">${e.fechaPlazo || '-'}</td>
                    <td class="small ${e.diasSinMovimiento > 30 && !ESTADOS_FINALES.has(e.estado) ? 'text-warning fw-semibold' : 'text-muted'}">
                        ${ESTADOS_FINALES.has(e.estado) ? '-' : `${e.diasSinMovimiento}d`}
                    </td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-primary" title="Ver línea de tiempo"
                                onclick="ModuloMonitor.verTimeline(${e.id}, '${e.numeroRadicado}', '${e.nombreEmpresa}')">
                            <i class="bi bi-diagram-3"></i>
                        </button>
                    </td>
                </tr>`;
            }).join('')
            : '<tr><td colspan="8" class="text-muted text-center py-3">Sin expedientes que mostrar</td></tr>';
    }

    function filtrarPorEstado(estado) {
        renderizarTabla(expedientes.filter(e => e.estado === estado));
        document.getElementById('btnLimpiarFiltroMonitor').classList.remove('d-none');
    }

    function filtrarPlazoVencido() {
        renderizarTabla(expedientes.filter(e => e.plazoVencido));
        document.getElementById('btnLimpiarFiltroMonitor').classList.remove('d-none');
    }

    function filtrarSinMovimiento() {
        renderizarTabla(expedientes.filter(e => !ESTADOS_FINALES.has(e.estado) && e.diasSinMovimiento > 30));
        document.getElementById('btnLimpiarFiltroMonitor').classList.remove('d-none');
    }

    function limpiarFiltro() {
        renderizarTabla();
        document.getElementById('btnLimpiarFiltroMonitor').classList.add('d-none');
    }

    async function verTimeline(id, numeroRadicado, empresa) {
        document.getElementById('tituloTimeline').textContent = numeroRadicado;
        document.getElementById('subtituloTimeline').textContent = empresa;
        document.getElementById('cuerpoTimeline').innerHTML =
            '<p class="text-muted text-center py-3 small"><i class="bi bi-hourglass-split me-2"></i>Cargando...</p>';

        const modal = new bootstrap.Modal(document.getElementById('modalTimeline'));
        modal.show();

        const resp = await ApiCliente.obtener(`/api/radicaciones/${id}/historial`);
        const contenedor = document.getElementById('cuerpoTimeline');
        if (!resp?.ok) {
            contenedor.innerHTML = '<p class="text-danger small">No se pudo cargar el historial.</p>';
            return;
        }
        const historial = await resp.json();
        if (!historial.length) {
            contenedor.innerHTML = '<p class="text-muted small text-center py-2">Sin eventos registrados.</p>';
            return;
        }

        // Ordenar cronológico (más antiguo primero)
        const ordenado = [...historial].sort((a, b) => new Date(a.fechaEvento) - new Date(b.fechaEvento));
        const ultimo = ordenado.length - 1;

        contenedor.innerHTML = ordenado.map((ev, idx) => {
            const cfg = CONFIG_ESTADO[ev.estado] || { label: ev.estado, color: 'secondary', icon: 'circle' };
            const esFinal = ESTADOS_FINALES.has(ev.estado);
            const esUltimo = idx === ultimo;
            const fecha = String(ev.fechaEvento).replace('T', ' ').slice(0, 16);
            return `
            <div class="d-flex gap-3 ${esUltimo ? '' : 'mb-1'}">
                <div class="d-flex flex-column align-items-center" style="min-width:36px">
                    <div class="rounded-circle d-flex align-items-center justify-content-center text-white bg-${cfg.color} ${cfg.color === 'warning' ? 'text-dark' : ''}"
                         style="width:34px;height:34px;flex-shrink:0">
                        <i class="bi bi-${cfg.icon} small"></i>
                    </div>
                    ${!esUltimo ? `<div style="width:2px;flex-grow:1;background:#dee2e6;min-height:28px"></div>` : ''}
                </div>
                <div class="flex-grow-1 pb-3 ${!esUltimo ? 'border-bottom' : ''}">
                    <div class="d-flex justify-content-between align-items-start flex-wrap gap-1">
                        <span class="badge bg-${cfg.color} ${cfg.color === 'warning' ? 'text-dark' : ''}">${cfg.label}</span>
                        <small class="text-muted">${fecha}</small>
                    </div>
                    ${ev.comentario ? `<p class="small mb-1 mt-1">${ev.comentario}</p>` : ''}
                    <small class="text-muted"><i class="bi bi-person me-1"></i>${ev.usuario}</small>
                </div>
            </div>`;
        }).join('');

        // Mostrar indicador de estado actual
        const expediente = expedientes.find(e => e.id === id);
        if (expediente) {
            const cfgActual = CONFIG_ESTADO[expediente.estado] || { label: expediente.estado, color: 'secondary' };
            document.getElementById('estadoActualTimeline').innerHTML =
                `<span class="badge bg-${cfgActual.color} ${cfgActual.color === 'warning' ? 'text-dark' : ''} me-2">Estado actual: ${cfgActual.label}</span>
                 <small class="text-muted">${historial.length} evento${historial.length !== 1 ? 's' : ''} registrado${historial.length !== 1 ? 's' : ''}</small>`;
        }
    }

    return { cargar, verTimeline, filtrarPorEstado, filtrarPlazoVencido, filtrarSinMovimiento, limpiarFiltro };
})();
