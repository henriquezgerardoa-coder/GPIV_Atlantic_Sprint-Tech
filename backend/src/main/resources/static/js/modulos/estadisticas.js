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

    return { cargar };
})();

