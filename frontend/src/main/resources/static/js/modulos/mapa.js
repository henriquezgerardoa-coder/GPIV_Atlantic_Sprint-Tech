const ModuloMapa = (() => {
    let _mapa = null;
    let _capaGeoJSON = null;
    let _lotePorEmpresa = {};
    let _lotePorCodigo = {};

    async function cargar() {
        const puedeEditar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO']);

        if (_mapa) {
            _mapa.invalidateSize();
            return;
        }

        _mapa = L.map('contenedorMapa', {
            rotate: true,
            touchRotate: true,
            rotateControl: { closeOnZeroBearing: false }
        }).setView([-40.838, -62.963], 14);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        }).addTo(_mapa);

        _activarRotacionClickDerecho(_mapa);

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

        _lotePorEmpresa = {};
        _lotePorCodigo = {};
        lotes.forEach(l => {
            if (l.nombreEmpresa) _lotePorEmpresa[l.nombreEmpresa.toLowerCase().trim()] = l;
            if (l.codigo) _lotePorCodigo[l.codigo.toUpperCase()] = l;
        });

        _capaGeoJSON = L.geoJSON(geojson, {
            style: feature => _estiloFeature(feature),
            onEachFeature: (feature, layer) => _configurarInteracciones(feature, layer, puedeEditar)
        }).addTo(_mapa);

        _mapa.fitBounds(_capaGeoJSON.getBounds(), { padding: [20, 20] });
    }

    function _primeraClave(props) {
        const claves = Object.keys(props || {});
        return claves.length > 0 ? claves[0] : null;
    }

    function _resolverLote(nombre) {
        if (!nombre) return null;
        return _lotePorEmpresa[nombre.toLowerCase().trim()]
            ?? _lotePorCodigo[nombre.toUpperCase()];
    }

    function _estiloFeature(feature) {
        const nombre = _primeraClave(feature.properties);
        if (!nombre) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#ced4da', fillOpacity: 0.45 };
        }

        if (feature.properties.lote_fiscal) {
            return { color: '#5a5a5a', weight: 1.5, fillColor: '#adb5bd', fillOpacity: 0.60 };
        }

        const lote = _resolverLote(nombre);
        if (!lote || !lote.ocupado) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }

        if (lote.colorPersonalizado) {
            return { color: '#343a40', weight: 1.5, fillColor: lote.colorPersonalizado, fillOpacity: 0.65 };
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

    function _configurarInteracciones(feature, layer, puedeEditar) {
        const nombre = _primeraClave(feature.properties);
        const props = feature.properties || {};

        layer.on('mouseover', () => {
            layer.setStyle({ weight: 3, fillOpacity: 0.80 });
            layer.bringToFront();
        });
        layer.on('mouseout', () => _capaGeoJSON.resetStyle(layer));

        if (!nombre) {
            layer.bindPopup('<span class="text-muted small"><i class="bi bi-dash-circle me-1"></i>Lote sin asignar</span>');
            return;
        }

        const lote = _resolverLote(nombre);

        const zonaTexto = z => z === 'PARQUE_VIEJO' ? 'Parque Viejo' : z === 'PARQUE_NUEVO' ? 'Parque Nuevo' : '—';
        const estadoLabel = { ADJUDICADO: 'Adjudicado', PREADJUDICADO: 'Preadjudicado', DESADJUDICADO: 'Desadjudicado' };

        const codigoHeader = lote?.codigo ?? nombre;
        let html = `<strong class="d-block mb-1">${codigoHeader}</strong>`;

        if (props.lote_fiscal) {
            const sup = lote?.superficieMetrosCuadrados ?? props.superficie_m2;
            html += `<small class="text-muted d-block"><i class="bi bi-shield-lock me-1"></i>Lote Fiscal</small>`;
            if (sup) html += `<small class="text-muted d-block">Superficie: ${sup.toLocaleString('es-AR')} m²</small>`;
            layer.bindPopup(html, { maxWidth: 240 });
            return;
        }

        if (lote) {
            const sup = (lote.superficieMetrosCuadrados ?? props.superficie_m2 ?? 0).toLocaleString('es-AR');
            html += `<small class="text-muted d-block">Superficie: ${sup} m²</small>`;
            html += `<small class="text-muted d-block">Zona: ${zonaTexto(lote.zona)}</small>`;

            if (lote.ocupado && lote.empresaId) {
                const estadoTexto = estadoLabel[lote.estadoAsignacion] ?? (lote.estadoAsignacion || '—');
                html += `<small class="text-muted d-block">Estado: ${estadoTexto}</small>`;
                html += `<hr class="my-1">`;
                if (lote.nombreEmpresa) html += `<small class="d-block"><strong>${lote.nombreEmpresa}</strong></small>`;
                if (lote.cuitEmpresa)   html += `<small class="text-muted d-block">CUIT: ${lote.cuitEmpresa}</small>`;
                if (lote.rubroEmpresa)  html += `<small class="text-muted d-block">Rubro: ${lote.rubroEmpresa}</small>`;
                if (lote.referenteEmpresa) html += `<small class="text-muted d-block">Referente: ${lote.referenteEmpresa}</small>`;
                if (lote.correoElectronicoEmpresa) html += `<small class="text-muted d-block"><i class="bi bi-envelope me-1"></i>${lote.correoElectronicoEmpresa}</small>`;
                if (lote.telefonoEmpresa) html += `<small class="text-muted d-block"><i class="bi bi-telephone me-1"></i>${lote.telefonoEmpresa}</small>`;
                html += `<button class="btn btn-sm btn-outline-primary mt-2 w-100"
                                 onclick="ModuloEmpresas.verDetalleAdmin(${lote.empresaId})">
                    <i class="bi bi-building me-1"></i>Ver más
                </button>`;
            } else {
                html += `<small class="text-muted d-block">Estado: Disponible</small>`;
            }

            if (puedeEditar) {
                const colorActual = lote.colorPersonalizado ?? '#6c757d';
                html += `<hr class="my-2">
                <div class="d-flex align-items-center gap-2">
                    <label class="small mb-0 text-muted">Color:</label>
                    <input type="color" id="colorLote_${lote.id}" value="${colorActual}"
                           style="width:32px;height:28px;padding:1px;cursor:pointer;border:1px solid #ced4da;border-radius:4px;">
                    <button class="btn btn-sm btn-outline-secondary py-0"
                            onclick="ModuloMapa.guardarColor(${lote.id})">
                        <i class="bi bi-check2"></i> Aplicar
                    </button>
                </div>`;
            }
        } else if (props.superficie_m2) {
            html += `<small class="text-muted d-block">Superficie: ${props.superficie_m2.toLocaleString('es-AR')} m²</small>`;
            html += `<small class="text-muted d-block">Estado: Disponible</small>`;
        } else {
            html += `<small class="text-muted">Sin datos en el sistema</small>`;
        }

        layer.bindPopup(html, { maxWidth: 240 });
    }

    async function guardarColor(loteId) {
        const input = document.getElementById('colorLote_' + loteId);
        if (!input) return;
        const color = input.value;

        const resp = await ApiCliente.parche(`/api/lotes/${loteId}/color`, { color });
        if (!resp?.ok) {
            mostrarAlerta('No se pudo guardar el color.', 'danger');
            return;
        }

        const loteActualizado = await resp.json();
        const codigo = loteActualizado.codigo?.toUpperCase();
        const empresa = loteActualizado.nombreEmpresa?.toLowerCase().trim();

        if (codigo && _lotePorCodigo[codigo]) _lotePorCodigo[codigo].colorPersonalizado = color;
        if (empresa && _lotePorEmpresa[empresa]) _lotePorEmpresa[empresa].colorPersonalizado = color;

        _capaGeoJSON?.setStyle(f => _estiloFeature(f));
        _mapa?.closePopup();
    }

    function _activarRotacionClickDerecho(mapa) {
        let dragStart = null;
        let bearingInicial = 0;
        const contenedor = mapa.getContainer();

        contenedor.addEventListener('mousedown', e => {
            if (e.button !== 2) return;
            e.preventDefault();
            dragStart = { x: e.clientX };
            bearingInicial = mapa.getBearing ? mapa.getBearing() : 0;
            contenedor.style.cursor = 'grabbing';
        });

        contenedor.addEventListener('contextmenu', e => {
            if (dragStart) e.preventDefault();
        });

        document.addEventListener('mousemove', e => {
            if (!dragStart) return;
            const delta = e.clientX - dragStart.x;
            mapa.setBearing(bearingInicial + delta * 0.5);
        });

        document.addEventListener('mouseup', e => {
            if (e.button !== 2 || !dragStart) return;
            dragStart = null;
            contenedor.style.cursor = '';
        });
    }

    return { cargar, guardarColor };
})();