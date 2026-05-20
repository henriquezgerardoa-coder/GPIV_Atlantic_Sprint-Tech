/**
 * ModuloEstadisticas — Informes y estadísticas del parque industrial.
 * Visible para ADMINISTRADOR y DIRECTIVO (R-14: lectura de informes).
 */
const ModuloEstadisticas = (() => {

    async function cargar() {
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/resumen');
            if (!resp || !resp.ok) {
                mostrarAlerta('No se pudieron cargar las estadísticas.', 'danger');
                return;
            }
            const datos = await resp.json();
            renderizar(datos);
        } catch (err) {
            console.error('Error al cargar estadísticas:', err);
            mostrarAlerta('Error al cargar estadísticas.', 'danger');
        }
    }

    function renderizar(datos) {
        // Tarjetas de resumen
        setTexto('statInfoEmpresas',    datos.totalEmpresas);
        setTexto('statInfoLotes',       datos.totalLotes);
        setTexto('statInfoOcupados',    datos.lotesOcupados);
        setTexto('statInfoDisponibles', datos.lotesDisponibles);
        setTexto('statInfoRadicaciones',datos.totalRadicaciones);
        setTexto('statInfoPendientes',  datos.radicacionesPendientes);
        setTexto('statInfoEnRevision',  datos.radicacionesEnRevision);
        setTexto('statInfoAprobadas',   datos.radicacionesAprobadas);
        setTexto('statInfoRechazadas',  datos.radicacionesRechazadas);

        // Tabla detallada por estado
        const tbody = document.getElementById('cuerpoTablaEstadosRadicacion');
        if (!tbody) return;
        const mapa = datos.radicacionesPorEstado || {};
        const estados = [
            { clave: 'PENDIENTE',                    etiqueta: 'Pendiente',                color: 'warning' },
            { clave: 'EN_REVISION',                  etiqueta: 'En revisión',              color: 'info'    },
            { clave: 'APROBADA',                     etiqueta: 'Aprobada',                 color: 'success' },
            { clave: 'RADICADA',                     etiqueta: 'Radicada',                 color: 'primary' },
            { clave: 'RECHAZADA',                    etiqueta: 'Rechazada',                color: 'danger'  },
            { clave: 'REQUIERE_INFORMACION_ADICIONAL', etiqueta: 'Requiere información adicional', color: 'secondary' },
            { clave: 'CANCELADA',                    etiqueta: 'Cancelada',                color: 'dark'    },
        ];
        tbody.innerHTML = estados.map(e => {
            const cantidad = mapa[e.clave] ?? 0;
            const pct = datos.totalRadicaciones > 0
                ? Math.round((cantidad / datos.totalRadicaciones) * 100)
                : 0;
            return `<tr>
                <td><span class="badge bg-${e.color}">${e.etiqueta}</span></td>
                <td class="fw-semibold">${cantidad}</td>
                <td>
                    <div class="progress" style="height:10px;min-width:80px;">
                        <div class="progress-bar bg-${e.color}" style="width:${pct}%;"></div>
                    </div>
                </td>
                <td class="text-muted small">${pct}%</td>
            </tr>`;
        }).join('');
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) el.textContent = valor ?? '–';
    }

    // ─── Informe por empresas ──────────────────────────────────────────────────

    async function abrirInformeEmpresas() {
        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInformeEmpresas'));
        const cuerpo = document.getElementById('cuerpoModalInformeEmpresas');
        cuerpo.innerHTML = `<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>`;
        modal.show();

        const resp = await ApiCliente.obtener('/api/estadisticas/informes/empresas');
        if (!resp?.ok) {
            cuerpo.innerHTML = '<div class="alert alert-danger">No se pudo cargar el informe de empresas.</div>';
            return;
        }
        const empresas = await resp.json();
        cuerpo.innerHTML = renderizarInformeEmpresas(empresas);
    }

    function renderizarInformeEmpresas(empresas) {
        if (!empresas?.length) return '<p class="text-muted">Sin empresas registradas.</p>';
        return empresas.map(e => `
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-header bg-light d-flex justify-content-between align-items-start">
                    <div>
                        <strong>${e.nombre || '-'}</strong>
                        <span class="text-muted small ms-2">CUIT: ${e.cuit || '-'}</span>
                    </div>
                    <span class="badge bg-secondary">${e.cantidadEmpleados ?? 0} empleados</span>
                </div>
                <div class="card-body">
                    <div class="row g-2 mb-2">
                        <div class="col-md-4"><span class="text-muted small">Razón social:</span><br><span>${e.razonSocial || '-'}</span></div>
                        <div class="col-md-4"><span class="text-muted small">Actividad:</span><br><span>${e.actividadEconomica || '-'}</span></div>
                        <div class="col-md-4"><span class="text-muted small">Correo:</span><br><span>${e.correoElectronico || '-'}</span></div>
                        <div class="col-md-4"><span class="text-muted small">Teléfono:</span><br><span>${e.telefono || '-'}</span></div>
                        <div class="col-md-4"><span class="text-muted small">Dirección:</span><br><span>${e.direccion || '-'}</span></div>
                        <div class="col-md-4"><span class="text-muted small">Fecha registro:</span><br><span>${(e.fechaRegistro || '').slice(0, 10) || '-'}</span></div>
                    </div>
                    ${e.estadoUltimaRadicacion ? `
                    <div class="mb-2">
                        <span class="text-muted small">Último expediente:</span>
                        <span class="ms-1 fw-semibold">${e.numeroUltimaRadicacion || '-'}</span>
                        <span class="badge bg-secondary ms-1">${e.estadoUltimaRadicacion}</span>
                        <span class="text-muted small ms-1">${e.fechaUltimaRadicacion || ''}</span>
                    </div>` : ''}
                    ${e.lotes?.length ? `
                    <div class="table-responsive mt-2">
                        <table class="table table-sm table-bordered mb-0">
                            <thead class="table-light">
                                <tr><th>Código lote</th><th>Zona</th><th>Superficie (m²)</th><th>Estado asignación</th><th>Fecha asignación</th></tr>
                            </thead>
                            <tbody>
                                ${e.lotes.map(l => `<tr>
                                    <td>${l.codigo || '-'}</td>
                                    <td>${l.zona || '-'}</td>
                                    <td>${l.superficieMetrosCuadrados ?? '-'}</td>
                                    <td>${l.estadoAsignacion || '-'}</td>
                                    <td>${l.fechaAsignacion || '-'}</td>
                                </tr>`).join('')}
                            </tbody>
                        </table>
                    </div>` : '<p class="text-muted small mb-0">Sin lotes asignados.</p>'}
                </div>
            </div>
        `).join('');
    }

    function imprimirInformeEmpresas() {
        const contenido = document.getElementById('cuerpoModalInformeEmpresas')?.innerHTML || '';
        _imprimirContenido('Informe por Empresa — ENREPAVI', contenido);
    }

    // ─── Informe por lotes ─────────────────────────────────────────────────────

    async function abrirInformeLotes() {
        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInformeLotes'));
        const cuerpo = document.getElementById('cuerpoModalInformeLotes');
        cuerpo.innerHTML = `<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>`;
        modal.show();

        const resp = await ApiCliente.obtener('/api/estadisticas/informes/lotes');
        if (!resp?.ok) {
            cuerpo.innerHTML = '<div class="alert alert-danger">No se pudo cargar el informe de lotes.</div>';
            return;
        }
        const lotes = await resp.json();
        cuerpo.innerHTML = renderizarInformeLotes(lotes);
    }

    function renderizarInformeLotes(lotes) {
        if (!lotes?.length) return '<p class="text-muted">Sin lotes registrados.</p>';
        const libres     = lotes.filter(l => !l.ocupado && !l.estadoAsignacion);
        const preAsig    = lotes.filter(l => l.estadoAsignacion === 'PRE_ASIGNADO');
        const asignados  = lotes.filter(l => l.ocupado || (l.estadoAsignacion && l.estadoAsignacion !== 'PRE_ASIGNADO'));

        function tablaLotes(lista, cols) {
            if (!lista.length) return '<p class="text-muted small">Sin registros.</p>';
            return `<div class="table-responsive">
                <table class="table table-sm table-bordered mb-0">
                    <thead class="table-light"><tr>${cols.map(c => `<th>${c.label}</th>`).join('')}</tr></thead>
                    <tbody>${lista.map(l => `<tr>${cols.map(c => `<td>${c.val(l) ?? '-'}</td>`).join('')}</tr>`).join('')}</tbody>
                </table></div>`;
        }

        const colsLibre = [
            { label: 'Código', val: l => l.codigo },
            { label: 'Zona',   val: l => l.zona },
            { label: 'Sup. (m²)', val: l => l.superficieMetrosCuadrados }
        ];
        const colsPreAsig = [
            { label: 'Código', val: l => l.codigo },
            { label: 'Zona',   val: l => l.zona },
            { label: 'Sup. (m²)', val: l => l.superficieMetrosCuadrados },
            { label: 'Empresa', val: l => l.nombreEmpresa },
            { label: 'CUIT',    val: l => l.cuitEmpresa },
            { label: 'N° Expediente', val: l => l.numeroExpedienteReferencia },
            { label: 'Plazo radicación', val: l => l.fechaPlazoRadicacion }
        ];
        const colsAsig = [
            { label: 'Código', val: l => l.codigo },
            { label: 'Zona',   val: l => l.zona },
            { label: 'Sup. (m²)', val: l => l.superficieMetrosCuadrados },
            { label: 'Estado asignación', val: l => l.estadoAsignacion },
            { label: 'Empresa', val: l => l.nombreEmpresa },
            { label: 'CUIT',    val: l => l.cuitEmpresa },
            { label: 'Fecha asignación', val: l => l.fechaAsignacion },
            { label: 'N° Expediente', val: l => l.numeroExpedienteReferencia }
        ];

        return `
            <h6 class="fw-bold mb-2">Lotes libres (${libres.length})</h6>
            ${tablaLotes(libres, colsLibre)}
            <h6 class="fw-bold mb-2 mt-4">Lotes pre-asignados (${preAsig.length})</h6>
            ${tablaLotes(preAsig, colsPreAsig)}
            <h6 class="fw-bold mb-2 mt-4">Lotes asignados (${asignados.length})</h6>
            ${tablaLotes(asignados, colsAsig)}
        `;
    }

    function imprimirInformeLotes() {
        const contenido = document.getElementById('cuerpoModalInformeLotes')?.innerHTML || '';
        _imprimirContenido('Informe por Lote — ENREPAVI', contenido);
    }

    // ─── Utilidad de impresión ─────────────────────────────────────────────────

    function _imprimirContenido(titulo, html) {
        const ventana = window.open('', '_blank');
        ventana.document.write(`<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8">
<title>${titulo}</title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
<style>
  @media print { body { padding: 1.5rem; } }
  body { padding: 1.5rem; font-size: 14px; }
  h2 { font-size: 1.2rem; margin-bottom: 1rem; }
</style>
</head><body>
<h2>${titulo}</h2>
${html}
<script>window.onload = () => { window.print(); window.close(); };<\/script>
</body></html>`);
        ventana.document.close();
    }

    return {
        cargar,
        abrirInformeEmpresas,
        abrirInformeLotes,
        imprimirInformeEmpresas,
        imprimirInformeLotes
    };
})();

