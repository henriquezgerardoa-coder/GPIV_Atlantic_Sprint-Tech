/**
 * ModuloEstadisticas — Informes y estadísticas del parque industrial.
 * Visible para ADMINISTRADOR, DIRECTIVO y SECRETARIO.
 */
const ModuloEstadisticas = (() => {

    let graficoLotes = null;
    let datosResumen = null;

    // Cachés de datos por tab
    let _empresasCache = null;
    let _lotesCache = null;
    let _radicacionesCache = null;
    let _serviciosCache = null;
    let lotesInformeCache = [];
    let filtroActivoLotes = 'todos';

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

    const ETIQUETA_ESTADO_RAD = {
        PENDIENTE:                    { texto: 'Pendiente',                    color: 'warning'   },
        EN_REVISION:                  { texto: 'En revisión',                  color: 'info'      },
        APROBADA:                     { texto: 'Aprobada',                     color: 'success'   },
        RADICADA:                     { texto: 'Radicada',                     color: 'primary'   },
        RECHAZADA:                    { texto: 'Rechazada',                    color: 'danger'    },
        REQUIERE_INFORMACION_ADICIONAL: { texto: 'Requiere información',        color: 'secondary' },
        CANCELADA:                    { texto: 'Cancelada',                    color: 'dark'      },
        DESADJUDICACION:              { texto: 'Desadjudicación',              color: 'danger'    },
    };

    // Etiquetas de criterios de evaluación
    const ETIQUETAS_CRITERIOS = {
        etapa1: {
            nombre: 'Empleabilidad / Materia Prima / Gestión Ambiental',
            criterio1: 'Generación de empleo local',
            criterio2: 'Uso de materia prima regional',
            criterio3: 'Gestión ambiental y manejo de residuos',
        },
        etapa2: {
            nombre: 'Financiero / Rentabilidad',
            criterio1: 'Rentabilidad del proyecto',
            criterio2: 'Solidez financiera',
            criterio3: 'Inversión declarada',
        },
        etapa3: {
            nombre: 'Preadjudicación / Cronograma de Obra',
            criterio1: 'Viabilidad técnica',
            criterio2: 'Cronograma de obra',
            criterio3: 'Calidad de la documentación',
        },
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
            // Invalidar cachés al recargar
            _empresasCache = null;
            _lotesCache = null;
            _radicacionesCache = null;
            _serviciosCache = null;
            // Recargar el tab activo si ya cargó datos
            const tabActivo = document.querySelector('#tabsInformes .nav-link.active');
            if (tabActivo) _cargarTabActivo(tabActivo.getAttribute('data-bs-target'));
        } catch (err) {
            console.error('Error al cargar estadísticas:', err);
            mostrarAlerta('Error al cargar estadísticas.', 'danger');
        }
    }

    // ── Wiring de eventos de tab ─────────────────────────────────────────────

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('tabsInformes')?.addEventListener('shown.bs.tab', e => {
            _cargarTabActivo(e.target.getAttribute('data-bs-target'));
        });
    });

    function _cargarTabActivo(target) {
        if (target === '#panel-inf-empresas') _cargarListaTabEmpresas();
        else if (target === '#panel-inf-lotes') _cargarListaTabLotes();
        else if (target === '#panel-inf-radicaciones') _cargarListaTabRadicaciones();
        else if (target === '#panel-inf-servicios') _cargarListaTabServicios();
    }

    // ── Lista inline tab Servicios ───────────────────────────────────────────

    const ETIQUETA_ESTADO_SERVICIO = {
        OPERATIVO:      { texto: 'Operativo',     color: 'success' },
        MANTENIMIENTO:  { texto: 'Mantenimiento', color: 'warning' },
        FALLA_CRITICA:  { texto: 'Falla crítica', color: 'danger'  },
    };

    async function _cargarListaTabServicios() {
        const cuerpo = document.getElementById('listaTabServicios');
        if (!cuerpo) return;
        if (_serviciosCache) { cuerpo.innerHTML = _renderListaServicios(_serviciosCache); return; }
        cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm text-primary"></span><span class="text-muted ms-2 small">Cargando...</span></div>';
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/servicios');
            if (!resp?.ok) throw new Error();
            _serviciosCache = await resp.json();
            cuerpo.innerHTML = _renderListaServicios(_serviciosCache);
        } catch {
            cuerpo.innerHTML = '<div class="alert alert-danger m-3">Error al cargar servicios.</div>';
        }
    }

    function _renderListaServicios(servicios) {
        if (!servicios?.length) return '<p class="text-muted text-center py-3">No hay servicios de infraestructura registrados.</p>';
        return servicios.map((s, idx) => {
            const est = ETIQUETA_ESTADO_SERVICIO[s.estadoActual] ?? { texto: s.estadoActual, color: 'secondary' };
            const total = s.empresas?.length ?? 0;
            const filas = (s.empresas || []).map(e => `<tr>
                <td class="small fw-semibold ps-3">${e.nombreEmpresa || '-'}</td>
                <td class="small text-muted">${e.cuitEmpresa || '-'}</td>
                <td class="small">${e.codigoLote || '-'}</td>
                <td class="small text-end pe-3">${e.consumo || '—'}</td>
            </tr>`).join('');
            const tablaHtml = total > 0
                ? `<div class="table-responsive">
                    <table class="table table-sm table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="ps-3">Empresa</th><th>CUIT</th>
                                <th>Lote</th><th class="text-end pe-3">Consumo estimado</th>
                            </tr>
                        </thead>
                        <tbody>${filas}</tbody>
                    </table>
                   </div>`
                : `<p class="text-muted small text-center py-2 mb-0">Sin empresas registradas para este servicio.</p>`;
            return `<div class="accordion-item border-0 border-bottom">
                <h2 class="accordion-header">
                    <button class="accordion-button ${idx > 0 ? 'collapsed' : ''} py-2 px-3"
                        type="button" data-bs-toggle="collapse"
                        data-bs-target="#colServicio${s.id}" aria-expanded="${idx === 0}">
                        <span class="badge bg-${est.color} me-2">${est.texto}</span>
                        <span class="fw-semibold">${s.nombre}</span>
                        <span class="badge bg-secondary ms-2 rounded-pill">${total}</span>
                        ${s.descripcionTecnica
                            ? `<span class="text-muted small ms-3 d-none d-md-inline">${s.descripcionTecnica}</span>`
                            : ''}
                    </button>
                </h2>
                <div id="colServicio${s.id}" class="accordion-collapse collapse ${idx === 0 ? 'show' : ''}">
                    <div class="accordion-body p-0">${tablaHtml}</div>
                </div>
            </div>`;
        }).join('');
    }

    // ── Lista inline tab Empresas ────────────────────────────────────────────

    async function _cargarListaTabEmpresas() {
        const cuerpo = document.getElementById('listaTabEmpresas');
        if (!cuerpo) return;
        if (_empresasCache) { cuerpo.innerHTML = _renderListaEmpresas(_empresasCache); return; }
        cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm text-primary"></span><span class="text-muted ms-2 small">Cargando...</span></div>';
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/empresas');
            if (!resp?.ok) throw new Error();
            _empresasCache = await resp.json();
            cuerpo.innerHTML = _renderListaEmpresas(_empresasCache);
        } catch {
            cuerpo.innerHTML = '<div class="alert alert-danger m-3">Error al cargar empresas.</div>';
        }
    }

    function _renderListaEmpresas(empresas) {
        if (!empresas?.length) return '<p class="text-muted text-center py-3">Sin datos disponibles.</p>';
        const filas = empresas.map(emp => {
            const lotes = (emp.lotes || []).map(l => {
                const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion] || { texto: l.estadoAsignacion || '-', color: '#6c757d' };
                return `<span class="badge me-1" style="background-color:${est.color}">${l.codigo}</span>`;
            }).join('') || '<span class="text-muted small">Sin lotes</span>';
            const estadoRad = ETIQUETA_ESTADO_RAD[emp.estadoUltimaRadicacion];
            return `<tr>
                <td class="fw-semibold">${emp.nombre || '-'}</td>
                <td class="small text-muted">${emp.cuit || '-'}</td>
                <td class="small">${emp.actividadEconomica || '-'}</td>
                <td>${lotes}</td>
                <td class="text-center">
                    ${estadoRad
                        ? `<span class="badge bg-${estadoRad.color}">${estadoRad.texto}</span>`
                        : '<span class="text-muted small">Sin radicación</span>'}
                </td>
                <td class="text-center small">${emp.cantidadEmpleados ?? 0}</td>
                <td class="text-center">
                    <button class="btn btn-outline-secondary btn-sm py-0" onclick="ModuloEstadisticas.verInformeEmpresa(${emp.id})">
                        <i class="bi bi-eye me-1"></i>Ver
                    </button>
                </td>
            </tr>`;
        }).join('');
        return `<div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Empresa</th><th>CUIT</th><th>Actividad</th>
                        <th>Lotes</th><th class="text-center">Estado expediente</th>
                        <th class="text-center">Empleados</th><th class="text-center">Informe</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    // ── Lista inline tab Lotes ───────────────────────────────────────────────

    async function _cargarListaTabLotes() {
        const cuerpo = document.getElementById('listaTabLotes');
        if (!cuerpo) return;
        if (_lotesCache) { cuerpo.innerHTML = _renderListaLotes(_lotesCache); return; }
        cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm text-primary"></span><span class="text-muted ms-2 small">Cargando...</span></div>';
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/lotes');
            if (!resp?.ok) throw new Error();
            _lotesCache = await resp.json();
            cuerpo.innerHTML = _renderListaLotes(_lotesCache);
        } catch {
            cuerpo.innerHTML = '<div class="alert alert-danger m-3">Error al cargar lotes.</div>';
        }
    }

    function _renderListaLotes(lotes) {
        if (!lotes?.length) return '<p class="text-muted text-center py-3">Sin datos disponibles.</p>';
        const filas = lotes.map(l => {
            const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion];
            const badgeEstado = est
                ? `<span class="badge" style="background-color:${est.color}">${est.texto}</span>`
                : '<span class="text-muted small">Sin estado</span>';
            return `<tr>
                <td class="fw-semibold ps-3">${l.codigo}</td>
                <td>${l.zona ? (ETIQUETA_ZONA[l.zona] || l.zona) : '-'}</td>
                <td>${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${l.ocupado ? 'bg-danger' : 'bg-success'}">${l.ocupado ? 'Ocupado' : 'Libre'}</span>
                </td>
                <td>${badgeEstado}</td>
                <td>${l.nombreEmpresa ? `<span class="fw-semibold">${l.nombreEmpresa}</span>` : '<span class="text-muted small">Sin asignar</span>'}</td>
                <td class="small text-muted">${l.fechaAsignacion || '-'}</td>
                <td class="text-center">
                    <button class="btn btn-outline-secondary btn-sm py-0" onclick="ModuloEstadisticas.verInformeLote(${l.id})">
                        <i class="bi bi-eye me-1"></i>Ver
                    </button>
                </td>
            </tr>`;
        }).join('');
        return `<div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Código</th><th>Zona</th><th>Superficie</th>
                        <th class="text-center">Ocupación</th><th>Estado</th>
                        <th>Empresa</th><th>Fecha asignación</th><th class="text-center">Informe</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    // ── Lista inline tab Radicaciones ────────────────────────────────────────

    async function _cargarListaTabRadicaciones() {
        const cuerpo = document.getElementById('listaTabRadicaciones');
        if (!cuerpo) return;
        if (_radicacionesCache) { cuerpo.innerHTML = _renderListaRadicaciones(_radicacionesCache); return; }
        cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm text-primary"></span><span class="text-muted ms-2 small">Cargando...</span></div>';
        try {
            const resp = await ApiCliente.obtener('/api/estadisticas/informes/radicaciones');
            if (!resp?.ok) throw new Error();
            _radicacionesCache = await resp.json();
            cuerpo.innerHTML = _renderListaRadicaciones(_radicacionesCache);
        } catch {
            cuerpo.innerHTML = '<div class="alert alert-danger m-3">Error al cargar radicaciones.</div>';
        }
    }

    function _renderListaRadicaciones(radicaciones) {
        if (!radicaciones?.length) return '<p class="text-muted text-center py-3">Sin datos disponibles.</p>';
        const filas = radicaciones.map(r => {
            const estadoRad = ETIQUETA_ESTADO_RAD[r.estado] || { texto: r.estado, color: 'secondary' };
            const ptotal = r.puntuacionTotal != null ? `<span class="fw-bold text-primary">${r.puntuacionTotal}/45</span>` : '<span class="text-muted small">Sin evaluar</span>';
            const e1 = r.etapa1Completa ? `<span class="badge bg-success">${r.etapa1Puntuacion}/15</span>` : '<span class="badge bg-light text-muted border">–</span>';
            const e2 = r.etapa2Completa ? `<span class="badge bg-success">${r.etapa2Puntuacion}/15</span>` : '<span class="badge bg-light text-muted border">–</span>';
            const e3 = r.etapa3Completa ? `<span class="badge bg-success">${r.etapa3Puntuacion}/15</span>` : '<span class="badge bg-light text-muted border">–</span>';
            return `<tr>
                <td class="small fw-semibold ps-3">${r.numeroRadicado || '-'}</td>
                <td class="small">${r.nombreEmpresa || '-'}</td>
                <td><span class="badge bg-${estadoRad.color}">${estadoRad.texto}</span></td>
                <td class="small text-muted">${r.fechaRadicacion || '-'}</td>
                <td class="small">${r.codigoLote || '<span class="text-muted">Sin lote</span>'}</td>
                <td class="text-center">${e1}</td>
                <td class="text-center">${e2}</td>
                <td class="text-center">${e3}</td>
                <td class="text-center">${ptotal}</td>
                <td class="text-center">
                    <button class="btn btn-outline-secondary btn-sm py-0" onclick="ModuloEstadisticas.verInformeRadicacion(${r.id})">
                        <i class="bi bi-eye me-1"></i>Ver
                    </button>
                </td>
            </tr>`;
        }).join('');
        return `<div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">N° Radicado</th><th>Empresa</th><th>Estado</th>
                        <th>Fecha</th><th>Lote</th>
                        <th class="text-center">Etapa 1</th>
                        <th class="text-center">Etapa 2</th>
                        <th class="text-center">Etapa 3</th>
                        <th class="text-center">Total</th>
                        <th class="text-center">Informe</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    // ── Informes individuales ────────────────────────────────────────────────

    function verInformeEmpresa(id) {
        if (!_empresasCache) return;
        const emp = _empresasCache.find(e => e.id === id);
        if (!emp) return;
        const cuerpo = document.getElementById('cuerpoModalEmpresaIndividual');
        if (cuerpo) cuerpo.innerHTML = _renderDetalleEmpresa(emp);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInformeEmpresaIndividual')).show();
    }

    function _renderDetalleEmpresa(emp) {
        const lotes = (emp.lotes || []);
        const lotesHtml = lotes.length
            ? lotes.map(l => {
                const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion] || { texto: l.estadoAsignacion || '-', color: '#6c757d' };
                return `<div class="d-flex align-items-center gap-2 border-bottom py-2">
                    <span class="badge" style="background-color:${est.color}">${l.codigo}</span>
                    <span class="small text-muted">${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</span>
                    ${l.zona ? `<span class="small text-muted">${ETIQUETA_ZONA[l.zona] || l.zona}</span>` : ''}
                    ${l.fechaAsignacion ? `<span class="small text-muted ms-auto">${l.fechaAsignacion}</span>` : ''}
                </div>`;
            }).join('')
            : '<p class="text-muted small">Sin lotes asignados.</p>';
        const estadoRad = ETIQUETA_ESTADO_RAD[emp.estadoUltimaRadicacion];
        return `<div class="row g-3">
            <div class="col-md-6">
                <h6 class="text-muted small fw-semibold text-uppercase mb-2">Datos generales</h6>
                <table class="table table-sm table-borderless mb-0">
                    <tr><td class="text-muted small">Nombre</td><td class="fw-semibold">${emp.nombre || '-'}</td></tr>
                    <tr><td class="text-muted small">Razón social</td><td>${emp.razonSocial || '-'}</td></tr>
                    <tr><td class="text-muted small">CUIT</td><td>${emp.cuit || '-'}</td></tr>
                    <tr><td class="text-muted small">Actividad</td><td>${emp.actividadEconomica || '-'}</td></tr>
                    <tr><td class="text-muted small">Dirección</td><td>${emp.direccion || '-'}</td></tr>
                    <tr><td class="text-muted small">Correo</td><td>${emp.correoElectronico || '-'}</td></tr>
                    <tr><td class="text-muted small">Teléfono</td><td>${emp.telefono || '-'}</td></tr>
                    <tr><td class="text-muted small">Empleados</td><td>${emp.cantidadEmpleados ?? 0}</td></tr>
                    <tr><td class="text-muted small">Fecha registro</td><td>${emp.fechaRegistro || '-'}</td></tr>
                </table>
            </div>
            <div class="col-md-6">
                <h6 class="text-muted small fw-semibold text-uppercase mb-2">Estado expediente</h6>
                <div class="mb-2">
                    ${estadoRad
                        ? `<span class="badge bg-${estadoRad.color} fs-6">${estadoRad.texto}</span>`
                        : '<span class="text-muted small">Sin radicación</span>'}
                </div>
                ${emp.fechaUltimaRadicacion ? `<div class="small text-muted mb-1">Última radicación: ${emp.fechaUltimaRadicacion}</div>` : ''}
                ${emp.numeroUltimaRadicacion ? `<div class="small text-muted mb-3">N° radicado: <span class="fw-semibold">${emp.numeroUltimaRadicacion}</span></div>` : ''}
                <h6 class="text-muted small fw-semibold text-uppercase mb-2 mt-3">Lotes asignados</h6>
                ${lotesHtml}
            </div>
        </div>`;
    }

    function verInformeLote(id) {
        if (!_lotesCache) return;
        const lote = _lotesCache.find(l => l.id === id);
        if (!lote) return;
        const cuerpo = document.getElementById('cuerpoModalLoteIndividual');
        if (cuerpo) cuerpo.innerHTML = _renderDetalleLote(lote);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInformeLoteIndividual')).show();
    }

    function _renderDetalleLote(l) {
        const est = ETIQUETA_ESTADO_LOTE[l.estadoAsignacion];
        const badgeEstado = est
            ? `<span class="badge fs-6" style="background-color:${est.color}">${est.texto}</span>`
            : '<span class="text-muted small">Sin estado</span>';
        return `<table class="table table-sm table-borderless mb-0">
            <tr><td class="text-muted small" style="width:40%">Código</td><td class="fw-semibold">${l.codigo}</td></tr>
            <tr><td class="text-muted small">Zona</td><td>${l.zona ? (ETIQUETA_ZONA[l.zona] || l.zona) : '-'}</td></tr>
            <tr><td class="text-muted small">Superficie</td><td>${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</td></tr>
            <tr><td class="text-muted small">Ocupación</td><td><span class="badge rounded-pill ${l.ocupado ? 'bg-danger' : 'bg-success'}">${l.ocupado ? 'Ocupado' : 'Libre'}</span></td></tr>
            <tr><td class="text-muted small">Estado de asignación</td><td>${badgeEstado}</td></tr>
            <tr><td class="text-muted small">Empresa</td><td>${l.nombreEmpresa || '<span class="text-muted">Sin asignar</span>'}</td></tr>
            <tr><td class="text-muted small">CUIT empresa</td><td>${l.cuitEmpresa || '-'}</td></tr>
            <tr><td class="text-muted small">Fecha asignación</td><td>${l.fechaAsignacion || '-'}</td></tr>
            <tr><td class="text-muted small">N° expediente ref.</td><td>${l.numeroExpedienteReferencia || '-'}</td></tr>
            <tr><td class="text-muted small">Fecha plazo radicación</td><td>${l.fechaPlazoRadicacion || '-'}</td></tr>
        </table>`;
    }

    function verInformeRadicacion(id) {
        if (!_radicacionesCache) return;
        const rad = _radicacionesCache.find(r => r.id === id);
        if (!rad) return;
        const cuerpo = document.getElementById('cuerpoModalRadicacionIndividual');
        if (cuerpo) cuerpo.innerHTML = _renderDetalleRadicacion(rad);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInformeRadicacionIndividual')).show();
    }

    function _renderDetalleRadicacion(r) {
        const estadoRad = ETIQUETA_ESTADO_RAD[r.estado] || { texto: r.estado, color: 'secondary' };
        const tieneEval = r.etapa1Completa || r.etapa2Completa || r.etapa3Completa || r.puntuacionTotal != null;

        function bloqueEtapa(n, c1v, c2v, c3v, puntaje, obs, completa) {
            const et = ETIQUETAS_CRITERIOS[`etapa${n}`];
            const pBar = puntaje != null ? `<div class="progress mb-1" style="height:8px;">
                <div class="progress-bar bg-${puntaje >= 12 ? 'success' : puntaje >= 8 ? 'warning' : 'danger'}"
                     style="width:${Math.round(puntaje / 15 * 100)}%"></div>
            </div>` : '';
            const badge = completa
                ? `<span class="badge bg-success">${puntaje}/15</span>`
                : '<span class="badge bg-light text-muted border">Sin completar</span>';
            return `<div class="card border-0 bg-light mb-3">
                <div class="card-body py-2">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="mb-0 small fw-semibold">Etapa ${n} — ${et.nombre}</h6>
                        ${badge}
                    </div>
                    ${pBar}
                    <div class="row g-2 mt-1">
                        <div class="col-4 text-center">
                            <div class="text-muted" style="font-size:0.7rem">${et.criterio1}</div>
                            <div class="fw-bold fs-5">${c1v ?? '–'}</div>
                        </div>
                        <div class="col-4 text-center">
                            <div class="text-muted" style="font-size:0.7rem">${et.criterio2}</div>
                            <div class="fw-bold fs-5">${c2v ?? '–'}</div>
                        </div>
                        <div class="col-4 text-center">
                            <div class="text-muted" style="font-size:0.7rem">${et.criterio3}</div>
                            <div class="fw-bold fs-5">${c3v ?? '–'}</div>
                        </div>
                    </div>
                    ${obs ? `<p class="small text-muted mt-2 mb-0"><i class="bi bi-chat-left-text me-1"></i>${obs}</p>` : ''}
                </div>
            </div>`;
        }

        const resumenEval = r.puntuacionTotal != null
            ? `<div class="alert alert-primary py-2 mb-3">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="fw-semibold">Puntuación total</span>
                    <span class="fs-4 fw-bold">${r.puntuacionTotal} / 45</span>
                </div>
                <div class="progress mt-1" style="height:8px;">
                    <div class="progress-bar bg-primary" style="width:${Math.round(r.puntuacionTotal / 45 * 100)}%"></div>
                </div>
                ${r.evaluador ? `<div class="small text-muted mt-1">Evaluado por: ${r.evaluador}</div>` : ''}
               </div>`
            : `<div class="alert alert-secondary py-2 mb-3 text-muted small">Sin evaluación registrada aún.</div>`;

        return `<div class="row g-3">
            <div class="col-md-5">
                <h6 class="text-muted small fw-semibold text-uppercase mb-2">Datos de la radicación</h6>
                <table class="table table-sm table-borderless mb-0">
                    <tr><td class="text-muted small">N° radicado</td><td class="fw-semibold">${r.numeroRadicado || '-'}</td></tr>
                    <tr><td class="text-muted small">Empresa</td><td>${r.nombreEmpresa || '-'}</td></tr>
                    <tr><td class="text-muted small">CUIT</td><td>${r.cuitEmpresa || '-'}</td></tr>
                    <tr><td class="text-muted small">Actividad</td><td>${r.actividadEconomica || '-'}</td></tr>
                    <tr><td class="text-muted small">Estado</td>
                        <td><span class="badge bg-${estadoRad.color}">${estadoRad.texto}</span></td></tr>
                    <tr><td class="text-muted small">Fecha radicación</td><td>${r.fechaRadicacion || '-'}</td></tr>
                    <tr><td class="text-muted small">Fecha plazo</td><td>${r.fechaPlazo || '-'}</td></tr>
                    <tr><td class="text-muted small">Lote asignado</td><td>${r.codigoLote || '<span class="text-muted">Sin lote</span>'}</td></tr>
                    <tr><td class="text-muted small">Tiempo estimado obra</td><td>${r.tiempoEstimadoObraMeses != null ? r.tiempoEstimadoObraMeses + ' meses' : '-'}</td></tr>
                    <tr><td class="text-muted small">Empleados previstos</td><td>${r.empleadosPrevistos ?? '-'}</td></tr>
                </table>
            </div>
            <div class="col-md-7">
                <h6 class="text-muted small fw-semibold text-uppercase mb-2">Evaluación por etapas</h6>
                ${resumenEval}
                ${bloqueEtapa(1, r.etapa1Criterio1, r.etapa1Criterio2, r.etapa1Criterio3, r.etapa1Puntuacion, r.etapa1Observaciones, r.etapa1Completa)}
                ${bloqueEtapa(2, r.etapa2Criterio1, r.etapa2Criterio2, r.etapa2Criterio3, r.etapa2Puntuacion, r.etapa2Observaciones, r.etapa2Completa)}
                ${bloqueEtapa(3, r.etapa3Criterio1, r.etapa3Criterio2, r.etapa3Criterio3, r.etapa3Puntuacion, r.etapa3Observaciones, r.etapa3Completa)}
            </div>
        </div>`;
    }

    // ── Detalle por estadística (modal genérico) ─────────────────────────────

    const _FILTROS_COMBINADOS = {
        RECHAZADA_CANCELADA: ['RECHAZADA', 'CANCELADA'],
    };

    async function verDetalleEstadistica(tipo, filtro, titulo) {
        const modalEl = document.getElementById('modalDetalleEstadistica');
        if (!modalEl) return;
        document.getElementById('modalDetalleEstadisticaLabel').textContent = titulo;
        const cuerpo = document.getElementById('cuerpoModalDetalleEstadistica');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border spinner-border-sm text-primary"></span><span class="text-muted ms-2 small">Cargando...</span></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
        try {
            if (tipo === 'radicacion') {
                if (!_radicacionesCache) {
                    const resp = await ApiCliente.obtener('/api/estadisticas/informes/radicaciones');
                    if (!resp?.ok) throw new Error();
                    _radicacionesCache = await resp.json();
                }
                const claves = _FILTROS_COMBINADOS[filtro] || (filtro ? [filtro] : null);
                const lista = claves ? _radicacionesCache.filter(r => claves.includes(r.estado)) : _radicacionesCache;
                if (cuerpo) cuerpo.innerHTML = _renderDetalleRadicaciones(lista);
            } else if (tipo === 'lote') {
                if (!_lotesCache) {
                    const resp = await ApiCliente.obtener('/api/estadisticas/informes/lotes');
                    if (!resp?.ok) throw new Error();
                    _lotesCache = await resp.json();
                }
                const lista = filtro ? _lotesCache.filter(l => l.etapa === filtro) : _lotesCache;
                if (cuerpo) cuerpo.innerHTML = _renderDetalleLotes(lista);
            }
        } catch {
            if (cuerpo) cuerpo.innerHTML = '<div class="alert alert-danger m-3">Error al cargar datos.</div>';
        }
    }

    function _renderDetalleRadicaciones(lista) {
        if (!lista?.length) return '<p class="text-muted text-center py-3">No hay radicaciones para este estado.</p>';
        const filas = lista.map(r => {
            const estadoRad = ETIQUETA_ESTADO_RAD[r.estado] || { texto: r.estado, color: 'secondary' };
            return `<tr>
                <td class="small fw-semibold ps-3">${r.numeroRadicado || '-'}</td>
                <td class="small">${r.nombreEmpresa || '-'}</td>
                <td><span class="badge bg-${estadoRad.color}">${estadoRad.texto}</span></td>
                <td class="small text-muted">${r.fechaRadicacion || '-'}</td>
                <td class="small">${r.codigoLote || '<span class="text-muted">Sin lote</span>'}</td>
                <td class="text-center">
                    <button class="btn btn-outline-secondary btn-sm py-0"
                        onclick="ModuloEstadisticas.verInformeRadicacion(${r.id})">
                        <i class="bi bi-eye me-1"></i>Ver
                    </button>
                </td>
            </tr>`;
        }).join('');
        return `<div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0">
            <thead class="table-light"><tr>
                <th class="ps-3">N° Radicado</th><th>Empresa</th><th>Estado</th>
                <th>Fecha</th><th>Lote</th><th class="text-center">Detalle</th>
            </tr></thead>
            <tbody>${filas}</tbody>
        </table></div>`;
    }

    function _renderDetalleLotes(lista) {
        if (!lista?.length) return '<p class="text-muted text-center py-3">No hay lotes en esta etapa.</p>';
        const filas = lista.map(l => `<tr>
            <td class="fw-semibold ps-3">${l.codigo}</td>
            <td>${l.zona ? (ETIQUETA_ZONA[l.zona] || l.zona) : '-'}</td>
            <td>${(l.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²</td>
            <td>${l.nombreEmpresa || '<span class="text-muted small">Sin asignar</span>'}</td>
            <td class="small text-muted">${l.fechaAsignacion || '-'}</td>
            <td class="text-center">
                <button class="btn btn-outline-secondary btn-sm py-0"
                    onclick="ModuloEstadisticas.verInformeLote(${l.id})">
                    <i class="bi bi-eye me-1"></i>Ver
                </button>
            </td>
        </tr>`).join('');
        return `<div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0">
            <thead class="table-light"><tr>
                <th class="ps-3">Código</th><th>Zona</th><th>Superficie</th>
                <th>Empresa</th><th>Fecha asignación</th><th class="text-center">Detalle</th>
            </tr></thead>
            <tbody>${filas}</tbody>
        </table></div>`;
    }

    // ── Informe completo Radicaciones (modal) ────────────────────────────────

    async function abrirInformeRadicaciones() {
        const modalEl = document.getElementById('modalInformeRadicaciones');
        if (!modalEl) return;
        const cuerpo = document.getElementById('cuerpoModalInformeRadicaciones');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
        try {
            if (!_radicacionesCache) {
                const resp = await ApiCliente.obtener('/api/estadisticas/informes/radicaciones');
                if (!resp?.ok) throw new Error('No se pudo obtener el informe.');
                _radicacionesCache = await resp.json();
            }
            if (cuerpo) cuerpo.innerHTML = _renderTablaRadicacionesCompleta(_radicacionesCache);
        } catch (e) {
            if (cuerpo) cuerpo.innerHTML = `<div class="alert alert-danger">${e?.message || 'Error al cargar informe.'}</div>`;
        }
    }

    function _renderTablaRadicacionesCompleta(radicaciones) {
        if (!radicaciones?.length) return '<p class="text-muted text-center py-3">Sin datos disponibles.</p>';
        const filas = radicaciones.map(r => {
            const estadoRad = ETIQUETA_ESTADO_RAD[r.estado] || { texto: r.estado, color: 'secondary' };
            const e1 = r.etapa1Completa ? `${r.etapa1Puntuacion}/15` : '–';
            const e2 = r.etapa2Completa ? `${r.etapa2Puntuacion}/15` : '–';
            const e3 = r.etapa3Completa ? `${r.etapa3Puntuacion}/15` : '–';
            const total = r.puntuacionTotal != null ? `${r.puntuacionTotal}/45` : '–';
            return `<tr>
                <td class="small fw-semibold">${r.numeroRadicado || '-'}</td>
                <td class="small">${r.nombreEmpresa || '-'}</td>
                <td class="small text-muted">${r.cuitEmpresa || '-'}</td>
                <td><span class="badge bg-${estadoRad.color}">${estadoRad.texto}</span></td>
                <td class="small text-muted">${r.fechaRadicacion || '-'}</td>
                <td class="small">${r.codigoLote || '-'}</td>
                <td class="text-center small">${r.empleadosPrevistos ?? '-'}</td>
                <td class="text-center small">${e1}</td>
                <td class="text-center small">${e2}</td>
                <td class="text-center small">${e3}</td>
                <td class="text-center small fw-semibold">${total}</td>
                <td class="small text-muted">${r.evaluador || '-'}</td>
            </tr>`;
        }).join('');
        return `<div class="table-responsive">
            <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th>N° Radicado</th><th>Empresa</th><th>CUIT</th><th>Estado</th>
                        <th>Fecha</th><th>Lote</th><th class="text-center">Empleados prev.</th>
                        <th class="text-center">Etapa 1</th><th class="text-center">Etapa 2</th>
                        <th class="text-center">Etapa 3</th><th class="text-center">Total eval.</th>
                        <th>Evaluador</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
    }

    function imprimirInformeRadicaciones() {
        _imprimirModal('modalInformeRadicaciones');
    }

    function imprimirInformeEmpresaIndividual() {
        _imprimirModal('modalInformeEmpresaIndividual');
    }

    function imprimirInformeLoteIndividual() {
        _imprimirModal('modalInformeLoteIndividual');
    }

    function imprimirInformeRadicacionIndividual() {
        _imprimirModal('modalInformeRadicacionIndividual');
    }

    // ── Informe por Empresa (modal completo) ─────────────────────────────────

    async function abrirInformeEmpresas() {
        const modalEl = document.getElementById('modalInformeEmpresas');
        if (!modalEl) return;
        const cuerpo = document.getElementById('cuerpoModalInformeEmpresas');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            if (!_empresasCache) {
                const resp = await ApiCliente.obtener('/api/estadisticas/informes/empresas');
                if (!resp?.ok) throw new Error('No se pudo obtener el informe.');
                _empresasCache = await resp.json();
            }
            if (cuerpo) cuerpo.innerHTML = renderizarTablaEmpresas(_empresasCache);
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

    // ── Informe por Lote (modal completo) ────────────────────────────────────

    async function abrirInformeLotes() {
        const modalEl = document.getElementById('modalInformeLotes');
        if (!modalEl) return;
        const cuerpo = document.getElementById('cuerpoModalInformeLotes');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><span class="spinner-border text-primary"></span><p class="text-muted mt-2">Cargando informe...</p></div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            if (!_lotesCache) {
                const resp = await ApiCliente.obtener('/api/estadisticas/informes/lotes');
                if (!resp?.ok) throw new Error('No se pudo obtener el informe.');
                _lotesCache = await resp.json();
            }
            filtroActivoLotes = 'todos';
            lotesInformeCache = _lotesCache;
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
            return `<div class="mb-3 filtros-informe-impresion">${botones}</div><p class="text-muted text-center py-3">Sin lotes para el filtro seleccionado.</p>`;
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

        return `<div class="mb-3 filtros-informe-impresion">${botones}</div>
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

    // ── Estadísticas generales ───────────────────────────────────────────────

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
        setTexto('statInfoProyectoEval',       datos.lotesProyectoEnEvaluacion ?? '–');
        setTexto('statInfoAdjudicadoPrecario', datos.lotesAdjudicadoPrecario   ?? '–');
        setTexto('statInfoEnConstruccion',     datos.lotesEnConstruccion        ?? '–');
        setTexto('statInfoOperativos',         datos.lotesOperativos            ?? '–');
        setTexto('statInfoEscriturados',       datos.lotesEscriturados          ?? '–');
        setTexto('statInfoDesadjudicados',     datos.lotesRevertidos            ?? '–');

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
            { clave: 'DESADJUDICACION',              etiqueta: 'Desadjudicación',           color: 'danger'  },
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

        const etiquetas = [
            'Disponibles',
            'Proyecto en evaluación',
            'Adjudicado precario',
            'En construcción',
            'Operativos',
            'Escriturados',
            'Desadjudicados',
        ];
        const valores = [
            Math.max(0, (datos?.totalLotes ?? 0)
                - (datos?.lotesProyectoEnEvaluacion ?? 0)
                - (datos?.lotesAdjudicadoPrecario   ?? 0)
                - (datos?.lotesEnConstruccion        ?? 0)
                - (datos?.lotesOperativos            ?? 0)
                - (datos?.lotesEscriturados          ?? 0)
                - (datos?.lotesRevertidos            ?? 0)),
            datos?.lotesProyectoEnEvaluacion ?? 0,
            datos?.lotesAdjudicadoPrecario   ?? 0,
            datos?.lotesEnConstruccion        ?? 0,
            datos?.lotesOperativos            ?? 0,
            datos?.lotesEscriturados          ?? 0,
            datos?.lotesRevertidos            ?? 0,
        ];
        const colores = ['#0dcaf0', '#ffc107', '#fd7e14', '#dc3545', '#198754', '#0d6efd', '#adb5bd'];

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

    // ── Utilidades ───────────────────────────────────────────────────────────

    function _imprimirModal(idModal) {
        const modal = document.getElementById(idModal);
        if (!modal) return;

        const fechaLarga = new Date().toLocaleDateString('es-AR', {
            day: 'numeric', month: 'long', year: 'numeric'
        });

        const encabezado = document.createElement('div');
        encabezado.className = 'encabezado-impresion';
        encabezado.id = '_encabezadoImpresionTemp';
        encabezado.innerHTML = `
            <img src="/img/logo.png" alt="Logo ENREPAVI">
            <span class="encabezado-impresion-org">ENREPAVI</span>
            <div class="encabezado-impresion-sep"></div>
            <div class="encabezado-impresion-info">
                <span class="encabezado-impresion-titulo">INFORME REALIZADO</span>
                <span class="encabezado-impresion-dato">Lugar: Viedma</span>
                <span class="encabezado-impresion-dato">Fecha: ${fechaLarga}</span>
            </div>`;

        const cuerpo = modal.querySelector('.modal-body');
        if (cuerpo) cuerpo.insertBefore(encabezado, cuerpo.firstChild);

        modal.classList.add('modal-impresion');

        // El modal está anidado dentro de un contenedor; para que la regla
        // CSS "body > *:not(.modal-impresion)" funcione debe ser hijo directo de body.
        const padreOriginal = modal.parentElement;
        const siguienteHermano = modal.nextSibling;
        document.body.appendChild(modal);

        const limpiar = () => {
            modal.classList.remove('modal-impresion');
            document.getElementById('_encabezadoImpresionTemp')?.remove();
            if (siguienteHermano) {
                padreOriginal.insertBefore(modal, siguienteHermano);
            } else {
                padreOriginal.appendChild(modal);
            }
            window.removeEventListener('afterprint', limpiar);
        };
        window.addEventListener('afterprint', limpiar);
        window.print();
    }

    function setTexto(id, valor) {
        const el = document.getElementById(id);
        if (el) el.textContent = valor ?? '–';
    }

    return {
        cargar,
        generarGrafico,
        verDetalleEstadistica,
        abrirInformeEmpresas,
        imprimirInformeEmpresas,
        abrirInformeLotes,
        filtrarInformeLotes,
        imprimirInformeLotes,
        abrirInformeRadicaciones,
        imprimirInformeRadicaciones,
        verInformeEmpresa,
        verInformeLote,
        verInformeRadicacion,
        imprimirInformeEmpresaIndividual,
        imprimirInformeLoteIndividual,
        imprimirInformeRadicacionIndividual,
    };
})();