/**
 * ModuloEstadisticas — Informes y estadísticas del parque industrial.
 * Visible para ADMINISTRADOR y DIRECTIVO (R-14: lectura de informes).
 */
const ModuloEstadisticas = (() => {

    let graficoLotes = null;
    let datosResumen = null;

    const ETIQUETA_ESTADO_LOTE = {
        PREADJUDICADO: { texto: 'Preadjudicado', color: '#ffc107' },
        ADJUDICADO:    { texto: 'Adjudicado',    color: '#198754' },
        DESADJUDICADO: { texto: 'Desadjudicado', color: '#dc3545' },
        libre:         { texto: 'Libre',          color: '#0dcaf0' }
    };

    const ETIQUETA_ZONA = {
        PARQUE_VIEJO: 'Parque Viejo',
        PARQUE_NUEVO: 'Parque Nuevo'
    };

    async function cargar() {
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/resumen');
            if (!resp || !resp.ok) {
                mostrarAlerta('No se pudieron cargar las estadísticas.', 'danger');
                return;
            }
            datosResumen = await resp.json();
            renderizar(datosResumen);
            generarGrafico();
        } catch (err) {
            console.error('Error al cargar estadísticas:', err);
            mostrarAlerta('Error al cargar estadísticas.', 'danger');
        }
    }

    function renderizar(datos) {
        setTexto('statInfoEmpresas',         datos.totalEmpresas);
        setTexto('statInfoLotes',            datos.totalLotes);
        setTexto('statInfoOcupados',         datos.lotesOcupados);
        setTexto('statInfoDisponibles',      datos.lotesDisponibles);
        setTexto('statInfoRadicaciones',     datos.totalRadicaciones);
        setTexto('statInfoPendientes',       datos.radicacionesPendientes);
        setTexto('statInfoEnRevision',       datos.radicacionesEnRevision);
        setTexto('statInfoAprobadas',        datos.radicacionesAprobadas);
        setTexto('statInfoRechazadas',       datos.radicacionesRechazadas);
        setTexto('statInfoPreadjudicados',   datos.lotesPreadjudicados ?? '–');
        setTexto('statInfoAdjudicados',      datos.lotesAdjudicados ?? '–');
        setTexto('statInfoDesadjudicados',   datos.lotesDesadjudicados ?? '–');

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

    function generarGrafico() {
        const canvas = document.getElementById('graficoEstadosLotes');
        if (!canvas || typeof Chart === 'undefined') return;

        const datos = datosResumen;
        const libre = datos ? (datos.lotesDisponibles - (datos.lotesPreadjudicados ?? 0) - (datos.lotesDesadjudicados ?? 0)) : 0;
        const libre2 = Math.max(0, datos ? (datos.lotesDisponibles) : 0);

        const etiquetas = ['Preadjudicados', 'Adjudicados', 'Desadjudicados', 'Libres'];
        const valores   = [
            datos?.lotesPreadjudicados ?? 0,
            datos?.lotesAdjudicados    ?? 0,
            datos?.lotesDesadjudicados ?? 0,
            libre2
        ];
        const colores = ['#ffc107', '#198754', '#dc3545', '#0dcaf0'];

        if (graficoLotes) {
            graficoLotes.data.datasets[0].data = valores;
            graficoLotes.update();
            return;
        }

        graficoLotes = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: etiquetas,
                datasets: [{ data: valores, backgroundColor: colores, borderWidth: 2 }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'bottom', labels: { font: { size: 12 } } },
                    tooltip: {
                        callbacks: {
                            label: ctx => ` ${ctx.label}: ${ctx.parsed} lote(s)`
                        }
                    }
                }
            }
        });
    }

    // ── Informe por Empresa ──────────────────────────────────────────────────────

    async function abrirInformeEmpresas() {
        const modalEl = document.getElementById('modalInformeEmpresas');
        if (!modalEl) return;
        const cuerpo = document.getElementById('cuerpoModalInformeEmpresas');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/empresas');
            if (!resp?.ok) throw new Error('No se pudo obtener el informe.');
            const datos = await resp.json();
            if (cuerpo) cuerpo.innerHTML = renderizarTablaEmpresas(datos);
        } catch (e) {
            if (cuerpo) cuerpo.innerHTML = `<div class="alert alert-danger">${e?.message || 'Error al cargar informe.'}</div>`;
        }
    }

    function renderizarTablaEmpresas(empresas) {
        if (!empresas?.length) return '<p class="text-muted text-center py-3">Sin datos disponibles.</p>';

        const filas = empresas.map(emp => {
            const lotes = (emp.lotes || []).map(l => {
                const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion] || { texto: l.estadoAsignacion || '-', color: '#6c757d' };
                return `<span class="badge me-1" style="background-color:${est.color}">${l.codigo}</span>`;
            }).join('') || '<span class="text-muted small">Sin lotes</span>';

            return `<tr>
                <td class="fw-semibold">${emp.nombre || '-'}</td>
                <td class="small text-muted">${emp.cuit || '-'}</td>
                <td class="small">${emp.actividadEconomica || '-'}</td>
                <td>${lotes}</td>
                <td class="text-center">
                    ${emp.estadoUltimaRadicacion
                        ? `<span class="badge bg-secondary">${emp.estadoUltimaRadicacion}</span>`
                        : '<span class="text-muted small">Sin radicación</span>'}
                </td>
                <td class="text-center small">${emp.cantidadEmpleados ?? 0}</td>
            </tr>`;
        }).join('');

        return `<div class="table-responsive">
            <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Empresa</th>
                        <th>CUIT</th>
                        <th>Actividad</th>
                        <th>Lotes</th>
                        <th class="text-center">Estado expediente</th>
                        <th class="text-center">Empleados</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    function imprimirInformeEmpresas() {
        _imprimirModal('modalInformeEmpresas');
    }

    // ── Informe por Lote ────────────────────────────────────────────────────────

    let lotesInformeCache = [];
    let filtroActivoLotes = 'todos';

    async function abrirInformeLotes() {
        const modalEl = document.getElementById('modalInformeLotes');
        if (!modalEl) return;
        const cuerpo = document.getElementById('cuerpoModalInformeLotes');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/lotes');
            if (!resp?.ok) throw new Error('No se pudo obtener el informe.');
            lotesInformeCache = await resp.json();
            filtroActivoLotes = 'todos';
            if (cuerpo) cuerpo.innerHTML = renderizarInformeLotes(lotesInformeCache, filtroActivoLotes);
        } catch (e) {
            if (cuerpo) cuerpo.innerHTML = `<div class="alert alert-danger">${e?.message || 'Error al cargar informe.'}</div>`;
        }
    }

    function filtrarInformeLotes(filtro) {
        filtroActivoLotes = filtro;
        const cuerpo = document.getElementById('cuerpoModalInformeLotes');
        if (cuerpo) cuerpo.innerHTML = renderizarInformeLotes(lotesInformeCache, filtroActivoLotes);
    }

    function renderizarInformeLotes(lotes, filtro) {
        const filtros = [
            { clave: 'todos',         etiqueta: 'Todos',           color: 'secondary' },
            { clave: 'libre',         etiqueta: 'Libres',          color: 'info'      },
            { clave: 'ocupado',       etiqueta: 'Ocupados',        color: 'dark'      },
            { clave: 'PREADJUDICADO', etiqueta: 'Preadjudicados',  color: 'warning'   },
            { clave: 'ADJUDICADO',    etiqueta: 'Adjudicados',     color: 'success'   },
            { clave: 'DESADJUDICADO', etiqueta: 'Desadjudicados',  color: 'danger'    },
        ];

        const filtrados = lotes.filter(l => {
            if (filtro === 'todos')         return true;
            if (filtro === 'libre')         return !l.ocupado;
            if (filtro === 'ocupado')       return l.ocupado;
            return l.estadoAsignacion === filtro;
        });

        const botones = filtros.map(f => {
            const activo = filtro === f.clave ? 'active' : '';
            const cnt = lotes.filter(l => {
                if (f.clave === 'todos')   return true;
                if (f.clave === 'libre')   return !l.ocupado;
                if (f.clave === 'ocupado') return l.ocupado;
                return l.estadoAsignacion === f.clave;
            }).length;
            return `<button class="btn btn-sm btn-outline-${f.color} ${activo} me-1 mb-1"
                            onclick="ModuloEstadisticas.filtrarInformeLotes('${f.clave}')">
                        ${f.etiqueta} <span class="badge bg-${f.color} text-${f.color === 'warning' ? 'dark' : 'white'} ms-1">${cnt}</span>
                    </button>`;
        }).join('');

        if (!filtrados.length) {
            return `<div class="mb-3">${botones}</div><p class="text-muted text-center py-3">Sin lotes para el filtro seleccionado.</p>`;
        }

        const filas = filtrados.map(l => {
            const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion];
            const badgeEstado = est
                ? `<span class="badge" style="background-color:${est.color}">${est.texto}</span>`
                : '<span class="text-muted small">Sin estado</span>';
            return `<tr>
                <td class="fw-semibold">${l.codigo}</td>
                <td>${l.zona ? ETIQUETA_ZONA[l.zona] || l.zona : '-'}</td>
                <td>${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${l.ocupado ? 'bg-danger' : 'bg-success'}">
                        ${l.ocupado ? 'Ocupado' : 'Libre'}
                    </span>
                </td>
                <td>${badgeEstado}</td>
                <td>${l.nombreEmpresa ? `<span class="fw-semibold">${l.nombreEmpresa}</span>` : '<span class="text-muted small">Sin asignar</span>'}</td>
                <td class="small text-muted">${l.fechaAsignacion || '-'}</td>
            </tr>`;
        }).join('');

        return `<div class="mb-3">${botones}</div>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Código</th>
                        <th>Zona</th>
                        <th>Superficie</th>
                        <th class="text-center">Ocupación</th>
                        <th>Estado asignación</th>
                        <th>Empresa</th>
                        <th>Fecha asignación</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    function imprimirInformeLotes() {
        _imprimirModal('modalInformeLotes');
    }

    function _imprimirModal(idModal) {
        const modal = document.getElementById(idModal);
        if (!modal) return;
        modal.classList.add('modal-impresion');
        const limpiar = () => {
            modal.classList.remove('modal-impresion');
            window.removeEventListener('afterprint', limpiar);
        };
        window.addEventListener('afterprint', limpiar);
        window.print();
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) el.textContent = valor ?? '–';
    }

    return { cargar, generarGrafico, abrirInformeEmpresas, imprimirInformeEmpresas, abrirInformeLotes, filtrarInformeLotes, imprimirInformeLotes };
})();
