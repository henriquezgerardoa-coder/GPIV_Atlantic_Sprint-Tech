const ModuloMapa = (() => {
    let _mapa = null;
    let _capaGeoJSON = null;
    let _lotePorEmpresa = {};
    let _lotePorCodigo = {};
    let _soloDisponibles = false;
    let _lotesPropio = new Set();

    async function cargar() {
        _soloDisponibles = Autenticacion.tieneAcceso(['EMPRESA'])
            && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        const puedeEditar = !_soloDisponibles && Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO']);

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

        let lotesPromise;
        if (_soloDisponibles) {
            lotesPromise = Promise.all([
                ApiCliente.obtener('/api/lotes/disponibles'),
                ApiCliente.obtener('/api/lotes')
            ]).then(async ([respDisp, respPropios]) => {
                const disponibles = respDisp?.ok ? await respDisp.json() : [];
                const propios = respPropios?.ok ? await respPropios.json() : [];
                _lotesPropio = new Set(propios.map(l => l.id));
                const propiosIds = _lotesPropio;
                return [...propios, ...(disponibles.filter(l => !propiosIds.has(l.id)))];
            });
        } else {
            lotesPromise = ApiCliente.obtener('/api/lotes?tamanio=500').then(r => r?.ok ? r.json() : []);
        }

        const [respGeo, lotesRaw] = await Promise.all([
            fetch('/data/parque-industrial.geojson'),
            lotesPromise
        ]);

        if (!respGeo.ok) {
            mostrarAlerta('No se pudo cargar el mapa del parque.', 'danger');
            return;
        }

        const geojson = await respGeo.json();
        const lotes = (lotesRaw?.contenido ?? lotesRaw) || [];

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

    function _badgeEtapaMapa(etapa) {
        const cfg = {
            DISPONIBLE:             ['bg-secondary-subtle text-secondary', 'Disponible'],
            PROYECTO_EN_EVALUACION: ['bg-warning-subtle text-warning',    'Proyecto en evaluación'],
            ADJUDICADO_PRECARIO:    ['bg-warning text-dark',              'Adjudicado precario'],
            EN_CONSTRUCCION:        ['',                                   'En construcción'],
            OPERATIVO:              ['bg-success-subtle text-success',     'Operativo / Radicado'],
            ESCRITURADO:            ['bg-success-subtle text-success',     'Operativo / Radicado'],
            REVERTIDO:              ['bg-danger-subtle text-danger',       'Desadjudicado'],
        };
        const [cls, label] = cfg[etapa] ?? ['bg-secondary-subtle text-muted', etapa ?? '—'];
        const style = etapa === 'EN_CONSTRUCCION'
            ? 'background-color:#fd7e14;color:#fff;'
            : '';
        return `<span class="badge ${cls}" style="${style}">${label}</span>`;
    }

    function _estiloFeature(feature) {
        const nombre = _primeraClave(feature.properties);
        if (!nombre) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#ced4da', fillOpacity: 0.45 };
        }

        // Lote Fiscal (reserva del Estado): relleno gris-plata institucional con borde discontinuo
        if (feature.properties.lote_fiscal) {
            return {
                color: '#5a5a5a', weight: 2.5,
                fillColor: '#b8bfc7', fillOpacity: 0.70,
                dashArray: '7 4',
            };
        }

        const lote = _resolverLote(nombre);

        // Vista empresa: lotes disponibles (gris) + propios (azul), resto invisible
        if (_soloDisponibles) {
            if (!lote) return { opacity: 0, fillOpacity: 0 };
            if (_lotesPropio.has(lote.id))
                return { color: '#003f7f', weight: 2.5, fillColor: '#0d6efd', fillOpacity: 0.65 };
            if (lote.ocupado) return { opacity: 0, fillOpacity: 0 };
            return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }

        // Lote ENREPAVI: usa color de etapa pero con borde azul-marino distintivo
        if (feature.properties.lote_enrepavi) {
            const estiloEtapa = _estiloEtapa(lote);
            return {
                ...estiloEtapa,
                color: '#00205b', weight: 3,
                dashArray: null,
            };
        }

        if (!lote) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }

        const etapaFinal = lote.etapa === 'OPERATIVO' || lote.etapa === 'ESCRITURADO';
        if (lote.colorPersonalizado && !etapaFinal) {
            return { color: '#343a40', weight: 1.5, fillColor: lote.colorPersonalizado, fillOpacity: 0.65 };
        }

        return _estiloEtapa(lote);
    }

    function _estiloEtapa(lote) {
        if (!lote) {
            return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }
        switch (lote.etapa) {
            case 'PROYECTO_EN_EVALUACION':
                return { color: '#997404', weight: 1.5, fillColor: '#fff3cd', fillOpacity: 0.75 };
            case 'ADJUDICADO_PRECARIO':
                return { color: '#997404', weight: 2,   fillColor: '#ffc107', fillOpacity: 0.65 };
            case 'EN_CONSTRUCCION':
                return { color: '#a44a00', weight: 2,   fillColor: '#fd7e14', fillOpacity: 0.65 };
            case 'OPERATIVO':
            case 'ESCRITURADO':
                return { color: '#146c43', weight: 1.5, fillColor: '#198754', fillOpacity: 0.60 };
            case 'REVERTIDO':
                return { color: '#b02a37', weight: 1.5, fillColor: '#dc3545', fillOpacity: 0.55 };
            default:
                return { color: '#6c757d', weight: 1.5, fillColor: '#dee2e6', fillOpacity: 0.55 };
        }
    }

    function _configurarInteracciones(feature, layer, puedeEditar) {
        const nombre = _primeraClave(feature.properties);
        const props = feature.properties || {};
        const lote = _resolverLote(nombre);

        // Vista empresa: sin interacción en lotes que no son disponibles ni propios
        if (_soloDisponibles && (!lote || (lote.ocupado && !_lotesPropio.has(lote.id)))) return;

        layer.on('mouseover', () => {
            layer.setStyle({ weight: 3, fillOpacity: 0.80 });
            layer.bringToFront();
        });
        layer.on('mouseout', () => _capaGeoJSON.resetStyle(layer));

        if (!nombre) {
            layer.bindPopup('<span class="text-muted small"><i class="bi bi-dash-circle me-1"></i>Lote sin asignar</span>');
            return;
        }

        const zonaTexto = z => z === 'PARQUE_VIEJO' ? 'Parque Viejo' : z === 'PARQUE_NUEVO' ? 'Parque Nuevo' : '—';

        const codigoHeader = lote?.codigo ?? nombre;
        let html = `<strong class="d-block mb-1">${codigoHeader}</strong>`;

        // Badges de tipo especial (no hacen early-return — siguen participando del ciclo de vida)
        if (props.lote_fiscal) {
            html += `<span class="badge me-1" style="background:#5a5a5a;color:#fff">
                <i class="bi bi-shield-lock me-1"></i>Lote Fiscal</span>`;
        }
        if (props.lote_enrepavi) {
            html += `<span class="badge me-1" style="background:#00205b;color:#fff">
                <i class="bi bi-buildings me-1"></i>Propiedad ENREPAVI</span>`;
        }

        if (lote) {
            const sup = (lote.superficieMetrosCuadrados ?? props.superficie_m2 ?? 0).toLocaleString('es-AR');
            html += `<small class="text-muted d-block mt-1">Superficie: ${sup} m²</small>`;
            html += `<small class="text-muted d-block">Zona: ${zonaTexto(lote.zona)}</small>`;

            const etapaEtiqueta = lote.etapaEtiqueta ?? lote.etapa ?? '—';
            const badgeEtapa = _badgeEtapaMapa(lote.etapa);
            html += `<small class="d-block mt-1">Etapa: ${badgeEtapa}</small>`;

            if (lote.etapaVencida) {
                html += `<small class="text-danger d-block fw-semibold"><i class="bi bi-exclamation-triangle me-1"></i>Plazo vencido (${lote.diasEnEtapaActual} días)</small>`;
            }

            if (lote.ocupado && lote.empresaId) {
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
            }

            html += `<button class="btn btn-sm btn-outline-secondary mt-1 w-100"
                             onclick="ModuloCicloLote.abrirTimeline(${lote.id})">
                <i class="bi bi-clock-history me-1"></i>Línea de tiempo
            </button>`;

            if (puedeEditar) {
                html += `<button class="btn btn-sm btn-outline-success mt-1 w-100"
                                 onclick="ModuloCicloLote.abrirTransicion(${lote.id}, '${lote.etapa ?? ''}')">
                    <i class="bi bi-arrow-right-circle me-1"></i>Avanzar etapa
                </button>`;
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
            html += `<small class="text-muted d-block mt-1">Superficie: ${props.superficie_m2.toLocaleString('es-AR')} m²</small>`;
            html += `<small class="text-muted d-block">Etapa: Disponible</small>`;
        } else {
            html += `<small class="text-muted d-block mt-1">Sin datos en el sistema</small>`;
        }

        layer.bindPopup(html, { maxWidth: 280 });
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
        let pitchInicial = 0;
        const contenedor = mapa.getContainer();

        contenedor.addEventListener('mousedown', e => {
            if (e.button !== 2) return;
            e.preventDefault();
            dragStart = { x: e.clientX, y: e.clientY };
            bearingInicial = mapa.getBearing ? mapa.getBearing() : 0;
            pitchInicial   = mapa.getPitch  ? mapa.getPitch()   : 0;
            contenedor.style.cursor = 'grabbing';
        });

        contenedor.addEventListener('contextmenu', e => {
            if (dragStart) e.preventDefault();
        });

        document.addEventListener('mousemove', e => {
            if (!dragStart) return;
            const dx = e.clientX - dragStart.x;
            const dy = e.clientY - dragStart.y;
            if (mapa.setBearing) mapa.setBearing(bearingInicial + dx * 1.0);
            if (mapa.setPitch) {
                const pitch = Math.min(60, Math.max(0, pitchInicial - dy * 0.6));
                mapa.setPitch(pitch);
            }
        });

        document.addEventListener('mouseup', e => {
            if (e.button !== 2 || !dragStart) return;
            dragStart = null;
            contenedor.style.cursor = '';
        });
    }

    return { cargar, guardarColor };
})();