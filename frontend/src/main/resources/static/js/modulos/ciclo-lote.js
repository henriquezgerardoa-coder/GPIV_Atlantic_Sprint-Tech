'use strict';

const ETAPAS_ORDEN = [
    'DISPONIBLE',
    'PROYECTO_EN_EVALUACION',
    'ADJUDICADO_PRECARIO',
    'EN_CONSTRUCCION',
    'OPERATIVO',
    'ESCRITURADO',
];

const ETAPAS_ETIQUETAS = {
    DISPONIBLE:             'Disponible',
    PROYECTO_EN_EVALUACION: 'Proyecto en evaluación',
    ADJUDICADO_PRECARIO:    'Adjudicado precario',
    EN_CONSTRUCCION:        'En construcción',
    OPERATIVO:              'Operativo',
    ESCRITURADO:            'Escriturado',
    REVERTIDO:              'Desadjudicado',
};

const TRANSICIONES_VALIDAS = {
    DISPONIBLE:             ['PROYECTO_EN_EVALUACION'],
    PROYECTO_EN_EVALUACION: ['ADJUDICADO_PRECARIO', 'REVERTIDO', 'DISPONIBLE'],
    ADJUDICADO_PRECARIO:    ['EN_CONSTRUCCION', 'REVERTIDO'],
    EN_CONSTRUCCION:        ['OPERATIVO', 'REVERTIDO'],
    OPERATIVO:              ['ESCRITURADO', 'REVERTIDO'],
    ESCRITURADO:            [],
    REVERTIDO:              ['DISPONIBLE'],
};

let _loteIdActivo = null;

window.ModuloCicloLote = {

    async cargarAlertas() {
        const bloque = document.getElementById('alertasCicloLote');
        if (!bloque) return;
        try {
            const resp = await ApiCliente.obtener('/api/lotes/alertas-vencimiento');
            const alertas = resp?.ok ? await resp.json() : [];
            if (!alertas || alertas.length === 0) {
                bloque.innerHTML = '';
                return;
            }
            bloque.innerHTML = `
                <div class="alert alert-danger border-danger-subtle mb-3 shadow-sm" role="alert">
                    <h6 class="alert-heading mb-2">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>Alertas del Sistema — Etapas vencidas
                    </h6>
                    <ul class="list-unstyled mb-0 small">
                        ${alertas.map(a => `
                            <li class="mb-1">
                                <i class="bi bi-clock-fill text-danger me-1"></i>
                                <strong>${a.codigoLote}</strong> – ${a.nombreEmpresa ?? '—'}:
                                excedió el plazo en etapa <em>${a.etapaEtiqueta}</em>
                                (${a.diasVencida} día${a.diasVencida !== 1 ? 's' : ''} de atraso).
                            </li>`).join('')}
                    </ul>
                </div>`;
        } catch {
            bloque.innerHTML = '';
        }
    },

    async abrirTimeline(loteId) {
        _loteIdActivo = loteId;
        const modal = new bootstrap.Modal(document.getElementById('modalTimelineLote'));
        modal.show();
        const cuerpo = document.getElementById('cuerpoTimelineLote');
        if (cuerpo) cuerpo.innerHTML = '<div class="text-center py-4"><div class="spinner-border spinner-border-sm text-primary"></div></div>';
        try {
            const resp = await ApiCliente.obtener(`/api/lotes/${loteId}/historial`);
            const historial = resp?.ok ? await resp.json() : [];
            _renderizarTimeline(loteId, historial);
        } catch (e) {
            if (cuerpo) cuerpo.innerHTML = `<p class="text-danger small p-3">Error al cargar el historial: ${e.message ?? e}</p>`;
        }
    },

    abrirTransicion(loteId, etapaActual) {
        _loteIdActivo = loteId;
        const opciones = (TRANSICIONES_VALIDAS[etapaActual] ?? []);
        const select = document.getElementById('selectNuevaEtapa');
        if (select) {
            select.innerHTML = opciones.length
                ? opciones.map(e => `<option value="${e}">${ETAPAS_ETIQUETAS[e] ?? e}</option>`).join('')
                : '<option value="">Sin transiciones disponibles</option>';
            select.disabled = opciones.length === 0;
        }
        const txtEtapaActual = document.getElementById('textoEtapaActual');
        if (txtEtapaActual) txtEtapaActual.textContent = ETAPAS_ETIQUETAS[etapaActual] ?? etapaActual ?? '—';
        const textarea = document.getElementById('motivoTransicion');
        if (textarea) textarea.value = '';
        document.getElementById('btnConfirmarTransicion').disabled = opciones.length === 0;
        new bootstrap.Modal(document.getElementById('modalTransicionEtapa')).show();
    },

    async confirmarTransicion() {
        if (!_loteIdActivo) return;
        const etapa = document.getElementById('selectNuevaEtapa')?.value;
        const motivo = document.getElementById('motivoTransicion')?.value?.trim() ?? '';
        const btn = document.getElementById('btnConfirmarTransicion');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Guardando...';
        try {
            const respTransicion = await ApiCliente.crear(`/api/lotes/${_loteIdActivo}/transicionar`, { etapa, motivo });
            if (!respTransicion?.ok) throw new Error(await respTransicion?.text() ?? 'Error desconocido');
            bootstrap.Modal.getInstance(document.getElementById('modalTransicionEtapa'))?.hide();
            mostrarAlerta('Etapa actualizada correctamente', 'success');
            if (typeof ModuloMapa !== 'undefined' && ModuloMapa.recargar) ModuloMapa.recargar();
        } catch (e) {
            mostrarAlerta('Error al transicionar: ' + (e.message ?? e), 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check2 me-1"></i>Confirmar';
        }
    },
};

function _renderizarTimeline(loteId, historial) {
    const cuerpo = document.getElementById('cuerpoTimelineLote');
    const tituloEl = document.getElementById('tituloTimelineLote');
    if (!cuerpo) return;

    if (!historial || historial.length === 0) {
        cuerpo.innerHTML = '<p class="text-muted small p-3">Sin historial de etapas registrado.</p>';
        return;
    }

    const codigoLote = historial[0].codigoLote ?? `Lote #${loteId}`;
    if (tituloEl) tituloEl.textContent = `Línea de tiempo — ${codigoLote}`;

    const etapasCompletadas = new Set(historial.map(h => h.etapaNueva));
    const ultimaEtapa = historial[historial.length - 1].etapaNueva;
    const huboReversion = etapasCompletadas.has('REVERTIDO');

    // Línea de tiempo horizontal de las 7 etapas en orden
    const etapasVista = huboReversion
        ? [...ETAPAS_ORDEN, 'REVERTIDO']
        : ETAPAS_ORDEN;

    let pasos = '';
    for (let i = 0; i < etapasVista.length; i++) {
        const e = etapasVista[i];
        const esActual = e === ultimaEtapa;
        const completada = etapasCompletadas.has(e) && !esActual;
        const futura = !etapasCompletadas.has(e) && !esActual;
        const entrada = historial.find(h => h.etapaNueva === e);
        const fecha = entrada ? entrada.fechaCambio.substring(0, 10) : null;

        let iconoHtml;
        if (completada) {
            iconoHtml = `<i class="bi bi-check-circle-fill text-success icono" style="font-size:1.6rem"></i>`;
        } else if (esActual) {
            iconoHtml = `<i class="bi bi-circle-fill text-primary icono parpadeo-azul" style="font-size:1.6rem"></i>`;
        } else {
            iconoHtml = `<i class="bi bi-circle icono text-muted" style="font-size:1.6rem"></i>`;
        }

        pasos += `
            <div class="paso ${futura ? 'futuro' : ''} text-center" style="flex:1 1 0;min-width:80px">
                ${iconoHtml}
                <div class="nombre small mt-1 ${esActual ? 'fw-semibold' : ''}">${ETAPAS_ETIQUETAS[e] ?? e}</div>
                ${fecha ? `<div class="text-muted" style="font-size:0.7rem">${fecha}</div>` : ''}
            </div>`;
        if (i < etapasVista.length - 1) {
            pasos += `<div class="conector ${completada ? 'completado' : ''}" style="flex:0 0 1rem;height:2px;align-self:center;margin-bottom:1.6rem"></div>`;
        }
    }

    // Bitácora de cambios
    let bitacora = '<h6 class="mt-4 mb-2 small text-muted fw-semibold">Bitácora de transiciones</h6><ul class="list-group list-group-flush">';
    for (const h of historial) {
        const anterior = h.etapaAnteriorEtiqueta ?? '—';
        const nueva = h.etapaNuevaEtiqueta ?? h.etapaNueva;
        bitacora += `
            <li class="list-group-item px-0 py-2 small">
                <div class="d-flex justify-content-between">
                    <span>${anterior} → <strong>${nueva}</strong></span>
                    <span class="text-muted">${h.fechaCambio.substring(0, 10)}</span>
                </div>
                <div class="text-muted" style="font-size:0.78rem">
                    Por: ${h.usuarioResponsable}
                    ${h.motivo ? ` · "${h.motivo}"` : ''}
                    ${h.fechaLimitePlazo ? ` · Plazo: ${h.fechaLimitePlazo}` : ''}
                </div>
            </li>`;
    }
    bitacora += '</ul>';

    cuerpo.innerHTML = `
        <div class="timeline-lote mb-3">${pasos}</div>
        ${bitacora}`;
}
