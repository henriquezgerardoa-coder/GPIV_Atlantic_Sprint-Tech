const ModuloDashboard = (() => {
    let graficoRubro = null;
    let graficoZona  = null;

    const COLORES_RUBRO = [
        '#0d6efd','#198754','#ffc107','#dc3545','#0dcaf0',
        '#6f42c1','#fd7e14','#20c997','#6610f2','#d63384'
    ];

    async function cargar() {
        const resp = await ApiCliente.obtener('/api/estadisticas/gerencial');
        const contenedor = document.getElementById('contenedorDashboard');
        if (!contenedor) return;
        if (!resp?.ok) {
            contenedor.innerHTML = '<p class="text-danger">Error al cargar el dashboard.</p>';
            return;
        }
        const datos = await resp.json();
        renderizarKpis(datos);
        renderizarGraficoRubro(datos.distribucionRubro);
        renderizarGraficoZona(datos.distribucionZona);
        renderizarTablaZona(datos.distribucionZona);
        document.getElementById('fechaActualizacionDashboard').textContent =
            new Date().toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });
    }

    function renderizarKpis(d) {
        const fila1 = document.getElementById('kpisFila1');
        const fila2 = document.getElementById('kpisFila2');
        if (!fila1 || !fila2) return;

        fila1.innerHTML = `
            ${kpiCard('Tasa de ocupación', formatPct(d.tasaOcupacion), 'bi-pie-chart-fill', 'primary',
                `${d.lotesOcupados} de ${d.totalLotes} lotes ocupados`)}
            ${kpiCard('Empleos actuales', d.empleosActuales.toLocaleString('es-AR'), 'bi-people-fill', 'success',
                'Personal registrado en empresas')}
            ${kpiCard('Empleos proyectados', d.empleosProyectados.toLocaleString('es-AR'), 'bi-graph-up-arrow', 'info',
                'Previstos en expedientes aprobados')}
            ${kpiCard('Empresas activas', d.totalEmpresas.toLocaleString('es-AR'), 'bi-building', 'warning',
                `Distribuidas en ${d.distribucionRubro.length} rubros`)}`;

        fila2.innerHTML = `
            ${kpiCard('Expedientes activos', d.radicacionesActivas.toLocaleString('es-AR'), 'bi-file-earmark-text', 'secondary',
                'En trámite (no cerrados)')}
            ${kpiCard('Proyectos en curso', d.proyectosActivos.toLocaleString('es-AR'), 'bi-building-gear', 'primary',
                'No finalizados ni cancelados')}
            ${kpiCard('Superficie ocupada', formatM2(d.superficieOcupadaM2), 'bi-grid-3x3-gap-fill', 'success',
                `de ${formatM2(d.superficieTotalM2)} totales`)}
            ${d.proyectosConHitosVencidos > 0
                ? kpiCard('Hitos vencidos', d.proyectosConHitosVencidos.toLocaleString('es-AR'), 'bi-exclamation-triangle-fill', 'danger',
                    'Proyectos con hitos sin cumplir')
                : kpiCard('Hitos al día', '✓', 'bi-patch-check-fill', 'success',
                    'Sin hitos vencidos pendientes')}`;
    }

    function kpiCard(titulo, valor, icono, color, subtexto) {
        return `
        <div class="col-6 col-md-3">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-body text-center py-3">
                    <i class="bi bi-${icono} fs-3 text-${color} mb-1 d-block"></i>
                    <div class="fs-4 fw-bold">${valor}</div>
                    <div class="small fw-semibold text-muted">${titulo}</div>
                    <div class="small text-muted mt-1">${subtexto}</div>
                </div>
            </div>
        </div>`;
    }

    function renderizarGraficoRubro(distribucion) {
        const canvas = document.getElementById('graficoRubroDashboard');
        if (!canvas || !distribucion?.length) return;
        if (graficoRubro) { graficoRubro.destroy(); graficoRubro = null; }

        graficoRubro = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: distribucion.map(r => r.nombre),
                datasets: [{
                    data: distribucion.map(r => r.empresas),
                    backgroundColor: COLORES_RUBRO.slice(0, distribucion.length),
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right', labels: { font: { size: 12 }, boxWidth: 14 } },
                    tooltip: {
                        callbacks: {
                            label: ctx => ` ${ctx.label}: ${ctx.raw} empresa${ctx.raw !== 1 ? 's' : ''}`
                        }
                    }
                }
            }
        });
    }

    function renderizarGraficoZona(distribucion) {
        const canvas = document.getElementById('graficoZonaDashboard');
        if (!canvas || !distribucion?.length) return;
        if (graficoZona) { graficoZona.destroy(); graficoZona = null; }

        const labels = distribucion.map(z => z.zona.replace('_', ' '));
        graficoZona = new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Ocupados',
                        data: distribucion.map(z => z.ocupados),
                        backgroundColor: '#198754',
                        borderRadius: 4
                    },
                    {
                        label: 'Libres',
                        data: distribucion.map(z => z.libres),
                        backgroundColor: '#dee2e6',
                        borderRadius: 4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: { stacked: true, beginAtZero: true, ticks: { stepSize: 1 } }
                },
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            footer: items => {
                                const zona = distribucion[items[0].dataIndex];
                                return `Tasa: ${formatPct(zona.tasaOcupacion)}`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderizarTablaZona(distribucion) {
        const tabla = document.getElementById('tablaZonaDashboard');
        if (!tabla || !distribucion?.length) return;
        tabla.innerHTML = distribucion.map(z => `
            <tr>
                <td class="ps-3 fw-semibold">${z.zona.replace('_', ' ')}</td>
                <td class="text-end">${z.total}</td>
                <td class="text-end">${z.ocupados}</td>
                <td class="text-end">${z.libres}</td>
                <td class="text-end">${formatM2(z.superficieM2)}</td>
                <td class="text-end">
                    <div class="d-flex align-items-center gap-2 justify-content-end">
                        <div class="progress flex-grow-1" style="height:8px;min-width:60px">
                            <div class="progress-bar bg-success" style="width:${z.tasaOcupacion}%"></div>
                        </div>
                        <span class="small">${formatPct(z.tasaOcupacion)}</span>
                    </div>
                </td>
            </tr>`).join('');
    }

    function formatPct(v) {
        return (Math.round((v ?? 0) * 10) / 10).toLocaleString('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
    }

    function formatM2(v) {
        const n = v ?? 0;
        return n >= 10000
            ? (n / 10000).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ha'
            : n.toLocaleString('es-AR', { maximumFractionDigits: 0 }) + ' m²';
    }

    return { cargar };
})();
