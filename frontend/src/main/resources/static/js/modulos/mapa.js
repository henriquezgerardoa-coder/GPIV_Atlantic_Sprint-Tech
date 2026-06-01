const ModuloMapa = (() => {
    let _mapa = null;
    let _capaGeoJSON = null;

    async function cargar() {
        if (_mapa) {
            _mapa.invalidateSize();
            return;
        }

        _mapa = L.map('contenedorMapa').setView([-40.838, -62.963], 14);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        }).addTo(_mapa);

        const [respGeo, respLotes] = await Promise.all([
            fetch('/data/parque-industrial.geojson'),
            ApiCliente.obtener('/api/lotes?tamanio=500')
        ]);

        if (!respGeo.ok) {
            mostrarAlerta('No se pudo cargar el mapa del parque.', 'danger');
            return;
        }

        const geojson = await respGeo.json();
        const lotesData = respLotes?.ok ? await respLotes.json() : [];
        const lotes = lotesData.contenido ?? lotesData;

        const lotePorEmpresa = {};
        lotes.forEach(l => {
            if (l.nombreEmpresa) {
                lotePorEmpresa[l.nombreEmpresa.toLowerCase().trim()] = l;
            }
        });

        _capaGeoJSON = L.geoJSON(geojson, {
            style: feature => _estiloFeature(feature, lotePorEmpresa),
            onEachFeature: (feature, layer) => _configurarInteracciones(feature, layer, lotePorEmpresa)
        }).addTo(_mapa);

        _mapa.fitBounds(_capaGeoJSON.getBounds(), { padding: [20, 20] });
    }

    function _nombreEmpresa(props) {
        const claves = Object.keys(props || {});
        return claves.length > 0 ? claves[0] : null;
    }

    function _estiloFeature(feature, lotePorEmpresa) {
        const nombre = _nombreEmpresa(feature.properties);
        if (!nombre) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#ced4da', fillOpacity: 0.45 };
        }

        const lote = lotePorEmpresa[nombre.toLowerCase().trim()];
        if (!lote) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }

        switch (lote.estadoAsignacion) {
            case 'ADJUDICADO':
                return { color: '#146c43', weight: 1.5, fillColor: '#198754', fillOpacity: 0.55 };
            case 'PREADJUDICADO':
                return { color: '#997404', weight: 1.5, fillColor: '#ffc107', fillOpacity: 0.55 };
            case 'DESADJUDICADO':
                return { color: '#b02a37', weight: 1.5, fillColor: '#dc3545', fillOpacity: 0.55 };
            default:
                return { color: '#084298', weight: 1.5, fillColor: '#0d6efd', fillOpacity: 0.50 };
        }
    }

    function _configurarInteracciones(feature, layer, lotePorEmpresa) {
        const nombre = _nombreEmpresa(feature.properties);

        layer.on('mouseover', () => {
            layer.setStyle({ weight: 3, fillOpacity: 0.80 });
            layer.bringToFront();
        });
        layer.on('mouseout', () => _capaGeoJSON.resetStyle(layer));

        if (!nombre) {
            layer.bindPopup('<span class="text-muted small"><i class="bi bi-dash-circle me-1"></i>Lote sin asignar</span>');
            return;
        }

        const lote = lotePorEmpresa[nombre.toLowerCase().trim()];
        let html = `<strong class="d-block mb-1">${nombre}</strong>`;

        if (lote) {
            const estadoLabel = { ADJUDICADO: 'Adjudicado', PREADJUDICADO: 'Preadjudicado', DESADJUDICADO: 'Desadjudicado' };
            const estadoTexto = estadoLabel[lote.estadoAsignacion] ?? (lote.estadoAsignacion || '—');
            html += `
                <small class="text-muted d-block">Código: <strong>${lote.codigo}</strong></small>
                <small class="text-muted d-block">Superficie: ${(lote.superficieMetrosCuadrados ?? 0).toLocaleString('es-AR')} m²</small>
                <small class="text-muted d-block">Estado: ${estadoTexto}</small>
                <small class="text-muted d-block">Zona: ${lote.zona === 'PARQUE_VIEJO' ? 'Parque Viejo' : lote.zona === 'PARQUE_NUEVO' ? 'Parque Nuevo' : '—'}</small>`;
            if (lote.empresaId) {
                html += `<a class="btn btn-sm btn-outline-primary mt-2 w-100" href="#" onclick="navegarA('empresas'); return false;">
                    <i class="bi bi-building me-1"></i>Ver empresa
                </a>`;
            }
        } else {
            html += `<small class="text-muted">Sin datos en el sistema</small>`;
        }

        layer.bindPopup(html, { maxWidth: 220 });
    }

    return { cargar };
})();
