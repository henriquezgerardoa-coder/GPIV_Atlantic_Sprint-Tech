const ModuloDashboard = (() => {
    let _graficoRubro = null;
    let _graficoZona = null;

    async function cargar() {
        document.getElementById('fechaActualizacionDashboard').textContent =
            'Actualizado: ' + new Date().toLocaleString('es-AR');

        const resp = await ApiCliente.obtener('/api/estadisticas/gerencial');
        if (!resp?.ok) {
            document.getElementById('kpisFila1').innerHTML =
                '<div class="col-12 text-danger text-center py-4 small">Error al cargar indicadores</div>';
            return;
        }
        const datos = await resp.json();
        _renderizarKPIs(datos);
        _renderizarGraficoRubro(datos.distribucionRubro || []);
        _renderizarGraficoZona(datos.distribucionZona || []);
        _renderizarTablaZonas(datos.distribucionZona || []);
    }

    function _renderizarKPIs(d) {
        const fila1 = document.getElementById('kpisFila1');
        const fila2 = document.getElementById('kpisFila2');
        if (fila1) fila1.innerHTML = [
            _kpi('bi-buildings-fill',        'text-primary',   d.totalEmpresas,          'Empresas radicadas'),
            _kpi('bi-grid-3x3-gap-fill',     'text-secondary', d.totalLotes,              'Total lotes'),
            _kpi('bi-check-square-fill',     'text-success',   d.lotesOcupados,           'Lotes ocupados'),
            _kpi('bi-square',                'text-muted',     d.lotesLibres,             'Lotes disponibles'),
            _kpi('bi-percent',               'text-info',      (d.tasaOcupacion ?? 0).toFixed(1) + '%', 'Tasa de ocupación'),
        ].join('');
        if (fila2) fila2.innerHTML = [
            _kpi('bi-people-fill',           'text-primary',   d.empleosActuales,         'Empleos actuales'),
            _kpi('bi-person-plus-fill',      'text-success',   d.empleosProyectados,      'Empleos proyectados'),
            _kpi('bi-folder-fill',           'text-warning',   d.radicacionesActivas,     'Radicaciones activas'),
            _kpi('bi-building-gear',         'text-info',      d.proyectosActivos,        'Proyectos activos'),
            _kpi('bi-exclamation-triangle-fill', d.proyectosConHitosVencidos > 0 ? 'text-danger' : 'text-muted',
                                             d.proyectosConHitosVencidos,   'Proyectos con hitos vencidos'),
        ].join('');
    }

    function _kpi(icono, colorCls, valor, etiqueta) {
        return `
        <div class="col-6 col-md-4 col-lg">
            <div class="card border-0 shadow-sm text-center h-100">
                <div class="card-body py-3">
                    <i class="bi ${icono} ${colorCls} fs-3 mb-2 d-block"></i>
                    <div class="fs-4 fw-bold">${valor ?? '-'}</div>
                    <small class="text-muted">${etiqueta}</small>
                </div>
            </div>
        </div>`;
    }

    function _renderizarGraficoRubro(datos) {
        const canvas = document.getElementById('graficoRubroDashboard');
        if (!canvas) return;
        if (_graficoRubro) { _graficoRubro.destroy(); _graficoRubro = null; }
        if (!datos.length) { canvas.parentElement.innerHTML = '<p class="text-muted small text-center">Sin datos de distribución por rubro.</p>'; return; }
        const colores = ['#0d6efd','#198754','#ffc107','#dc3545','#0dcaf0','#6f42c1','#fd7e14','#20c997'];
        _graficoRubro = new Chart(canvas, {
            type: 'pie',
            data: {
                labels: datos.map(r => r.nombre),
                datasets: [{ data: datos.map(r => r.empresas), backgroundColor: colores.slice(0, datos.length) }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } } }
            }
        });
    }

    function _renderizarGraficoZona(datos) {
        const canvas = document.getElementById('graficoZonaDashboard');
        if (!canvas) return;
        if (_graficoZona) { _graficoZona.destroy(); _graficoZona = null; }
        if (!datos.length) { canvas.parentElement.innerHTML = '<p class="text-muted small text-center">Sin datos de distribución por zona.</p>'; return; }
        _graficoZona = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: datos.map(z => z.zona),
                datasets: [
                    { label: 'Ocupados', data: datos.map(z => z.ocupados), backgroundColor: '#0d6efd' },
                    { label: 'Libres',   data: datos.map(z => z.libres),   backgroundColor: '#dee2e6' },
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } } },
                scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true } }
            }
        });
    }

    function _renderizarTablaZonas(datos) {
        const tbody = document.getElementById('tablaZonaDashboard');
        if (!tbody) return;
        if (!datos.length) { tbody.innerHTML = '<tr><td colspan="6" class="text-muted text-center py-3">Sin datos</td></tr>'; return; }
        tbody.innerHTML = datos.map(z => {
            const tasa = (z.tasaOcupacion ?? 0).toFixed(1);
            const colorBarra = z.tasaOcupacion >= 90 ? 'bg-success' : z.tasaOcupacion >= 60 ? 'bg-primary' : 'bg-warning';
            return `
            <tr>
                <td class="ps-3 fw-semibold">${z.zona}</td>
                <td class="text-end">${z.total}</td>
                <td class="text-end">${z.ocupados}</td>
                <td class="text-end">${z.libres}</td>
                <td class="text-end">${z.superficieM2 ? z.superficieM2.toLocaleString('es-AR') + ' m²' : '-'}</td>
                <td class="text-end" style="min-width:160px">
                    <div class="d-flex align-items-center gap-2 justify-content-end">
                        <div class="progress flex-grow-1" style="height:8px">
                            <div class="progress-bar ${colorBarra}" style="width:${tasa}%"></div>
                        </div>
                        <span class="small text-muted" style="min-width:40px">${tasa}%</span>
                    </div>
                </td>
            </tr>`;
        }).join('');
    }

    return { cargar };
})();
