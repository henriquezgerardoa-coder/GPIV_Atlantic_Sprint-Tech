const ModuloCambioRubro = (() => {
    let rubros = [];
    let solicitudes = [];
    let resolucionModal = null;
    let solicitudEnResolucion = null;

    function _esc(val) {
        if (val == null) return '';
        return String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    async function cargar() {
        await Promise.all([cargarRubros(), cargarSolicitudes()]);
        ajustarVistaPorRol();
    }

    function ajustarVistaPorRol() {
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        const esGestor  = !esEmpresa && Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);

        document.getElementById('bloqueFormSolicitudCambioRubro')?.classList.toggle('d-none', !esEmpresa);
        document.getElementById('columnaResolucion')?.classList.toggle('d-none', !esGestor);
        document.getElementById('thResolucion')?.classList.toggle('d-none', !esGestor);
    }

    function toggleDescripcionOtros() {
        const selector = document.getElementById('selectorRubroSolicitado');
        const bloque = document.getElementById('bloqueDescripcionOtros');
        if (!selector || !bloque) return;
        const seleccionado = rubros.find(r => r.id === parseInt(selector.value));
        const esOtros = seleccionado && seleccionado.nombre.toLowerCase() === 'otros';
        bloque.classList.toggle('d-none', !esOtros);
        if (!esOtros) document.getElementById('campoDescripcionOtros').value = '';
    }

    async function cargarRubros() {
        const resp = await ApiCliente.obtener('/api/catalogos/rubros');
        if (!resp?.ok) return;
        rubros = await resp.json();
        const selector = document.getElementById('selectorRubroSolicitado');
        if (!selector) return;
        selector.innerHTML = '<option value="">Seleccionar rubro...</option>'
            + rubros.map(r => `
                <option value="${r.id}">
                    ${_esc(r.nombre)}${r.requierePermisoEspecial ? ' ⚠ (requiere permiso especial)' : ''}
                </option>`).join('');
    }

    async function cargarSolicitudes() {
        const resp = await ApiCliente.obtener('/api/cambios-rubro');
        const cuerpo = document.getElementById('cuerpoCambiosRubro');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="8" class="text-danger">Error al cargar solicitudes</td></tr>';
            return;
        }
        solicitudes = await resp.json();
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);
        const esDirectivo = Autenticacion.tieneAcceso(['DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'SECRETARIO']);

        cuerpo.innerHTML = solicitudes.length
            ? solicitudes.map(s => {
                const rubroSolicitadoTexto = s.nombreRubroSolicitado.toLowerCase() === 'otros' && s.descripcionOtros
                    ? `Otros <span class="text-muted small">(${_esc(s.descripcionOtros)})</span>`
                    : _esc(s.nombreRubroSolicitado);
                return `
                <tr>
                    <td class="ps-3 fw-semibold">${_esc(s.nombreEmpresa)}</td>
                    <td>${s.rubroAnteriorNombre ? _esc(s.rubroAnteriorNombre) : '<span class="text-muted">Sin rubro</span>'}</td>
                    <td>${rubroSolicitadoTexto}</td>
                    <td class="small text-muted" style="max-width:200px">${_esc(s.justificacion)}</td>
                    <td>${badgeEstado(s.estado)}</td>
                    <td class="small text-muted">${formatearFecha(s.fechaSolicitud)}</td>
                    <td class="small text-muted">${_esc(s.resueltoPor) || '-'}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-secondary" title="Ver detalle"
                            onclick="ModuloCambioRubro.verDetalle(${s.id})">
                            <i class="bi bi-eye"></i>
                        </button>
                    </td>
                    ${esGestor ? `
                    <td class="text-center columnaResolucion">
                        ${s.estado === 'PENDIENTE' && !esDirectivo ? `
                        <button class="btn btn-sm btn-outline-primary"
                            title="Resolver solicitud"
                            onclick="ModuloCambioRubro.abrirResolucion(${s.id})">
                            <i class="bi bi-pencil-square"></i>
                        </button>` : '<span class="text-muted small">—</span>'}
                    </td>` : ''}
                </tr>`;
            }).join('')
            : `<tr><td colspan="${esGestor ? 9 : 8}" class="text-muted text-center py-3">Sin solicitudes registradas</td></tr>`;
    }

    async function enviarSolicitud() {
        const alerta = document.getElementById('alertaCambioRubro');
        alerta?.classList.add('d-none');

        const rubroSolicitadoId = parseInt(document.getElementById('selectorRubroSolicitado').value);
        const justificacion = document.getElementById('campoJustificacionRubro').value.trim();
        const descripcionOtros = document.getElementById('campoDescripcionOtros')?.value.trim() || null;
        const seleccionado = rubros.find(r => r.id === rubroSolicitadoId);
        const esOtros = seleccionado && seleccionado.nombre.toLowerCase() === 'otros';

        if (!rubroSolicitadoId) {
            alerta.textContent = 'Seleccione el rubro solicitado.';
            alerta?.classList.remove('d-none');
            return;
        }
        if (esOtros && !descripcionOtros) {
            alerta.textContent = 'Debe especificar cuál es el rubro cuando selecciona "Otros".';
            alerta?.classList.remove('d-none');
            return;
        }
        if (!justificacion) {
            alerta.textContent = 'La justificación es obligatoria.';
            alerta?.classList.remove('d-none');
            return;
        }

        const cuerpoSolicitud = { rubroSolicitadoId, justificacion };
        if (esOtros && descripcionOtros) cuerpoSolicitud.descripcionOtros = descripcionOtros;
        const resp = await ApiCliente.crear('/api/cambios-rubro', cuerpoSolicitud);
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo enviar la solicitud.';
            alerta?.classList.remove('d-none');
            return;
        }
        document.getElementById('formCambioRubro').reset();
        mostrarAlerta('Solicitud de cambio de rubro enviada. Queda pendiente de aprobación.');
        await cargarSolicitudes();
    }

    function abrirResolucion(solicitudId) {
        const s = solicitudes.find(x => x.id === solicitudId);
        if (!s) return;
        solicitudEnResolucion = s;

        document.getElementById('resolCREmpresa').textContent = s.nombreEmpresa;
        document.getElementById('resolCRRubroAnterior').textContent = s.rubroAnteriorNombre || 'Sin rubro';
        document.getElementById('resolCRRubroSolicitado').textContent =
            s.nombreRubroSolicitado.toLowerCase() === 'otros' && s.descripcionOtros
                ? `Otros (${s.descripcionOtros})` : s.nombreRubroSolicitado;
        document.getElementById('resolCRJustificacion').textContent = s.justificacion;

        // Reset formulario
        document.querySelectorAll('input[name="resolCRDecision"]').forEach(r => r.checked = false);
        ['resolCRImpacto', 'resolCRCompatibilidad', 'resolCRViabilidad', 'resolCRCumplimiento']
            .forEach(name => document.querySelectorAll(`input[name="${name}"]`).forEach(r => r.checked = false));
        document.getElementById('resolCRObservaciones').value = '';
        document.getElementById('resolCRMotivoRechazo').value = '';
        document.getElementById('resolCRNuevoRubro').value = '';
        document.getElementById('bloqueResolMotivoRechazo').classList.add('d-none');
        document.getElementById('bloqueResolNuevoRubro').classList.add('d-none');
        document.getElementById('alertaResolucionCR').classList.add('d-none');

        if (!resolucionModal) {
            resolucionModal = new bootstrap.Modal(document.getElementById('modalResolucionCambioRubro'));
        }
        resolucionModal.show();
    }

    function onCambioDecision() {
        const decision = document.querySelector('input[name="resolCRDecision"]:checked')?.value;
        const esOtros = solicitudEnResolucion?.nombreRubroSolicitado?.toLowerCase() === 'otros';
        document.getElementById('bloqueResolMotivoRechazo').classList.toggle('d-none', decision !== 'rechazar');
        document.getElementById('bloqueResolNuevoRubro').classList.toggle('d-none', !(decision === 'aprobar' && esOtros));
    }

    async function confirmarResolucion() {
        const alerta = document.getElementById('alertaResolucionCR');
        alerta.classList.add('d-none');

        const decision = document.querySelector('input[name="resolCRDecision"]:checked')?.value;
        if (!decision) {
            alerta.textContent = 'Seleccione si aprueba o rechaza la solicitud.';
            alerta.classList.remove('d-none');
            return;
        }

        const leerRadio = name => {
            const checked = document.querySelector(`input[name="${name}"]:checked`);
            return checked ? parseInt(checked.value) : null;
        };

        const puntajeImpactoOperativo    = leerRadio('resolCRImpacto');
        const puntajeCompatibilidadParque = leerRadio('resolCRCompatibilidad');
        const puntajeViabilidadTecnica   = leerRadio('resolCRViabilidad');
        const puntajeCumplimientoNormativo = leerRadio('resolCRCumplimiento');

        if (!puntajeImpactoOperativo || !puntajeCompatibilidadParque ||
            !puntajeViabilidadTecnica || !puntajeCumplimientoNormativo) {
            alerta.textContent = 'Complete los 4 puntajes de la rúbrica (1 a 5).';
            alerta.classList.remove('d-none');
            return;
        }

        const cuerpo = {
            aprobada: decision === 'aprobar',
            puntajeImpactoOperativo,
            puntajeCompatibilidadParque,
            puntajeViabilidadTecnica,
            puntajeCumplimientoNormativo,
            observacionesRubrica: document.getElementById('resolCRObservaciones').value.trim() || null
        };

        if (decision === 'rechazar') {
            const motivo = document.getElementById('resolCRMotivoRechazo').value.trim();
            if (!motivo) {
                alerta.textContent = 'El motivo de rechazo es obligatorio.';
                alerta.classList.remove('d-none');
                return;
            }
            cuerpo.motivoRechazo = motivo;
        }

        if (decision === 'aprobar') {
            const nuevoRubro = document.getElementById('resolCRNuevoRubro').value.trim();
            if (nuevoRubro) cuerpo.nombreNuevoRubro = nuevoRubro;
        }

        const btn = document.getElementById('btnConfirmarResolucion');
        btn.disabled = true;

        const resp = await ApiCliente.parche(`/api/cambios-rubro/${solicitudEnResolucion.id}/resolver`, cuerpo);
        btn.disabled = false;

        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo procesar la resolución.';
            alerta.classList.remove('d-none');
            return;
        }

        resolucionModal.hide();
        solicitudEnResolucion = null;
        const mensaje = decision === 'aprobar'
            ? 'Solicitud aprobada. El rubro de la empresa fue actualizado.'
            : 'Solicitud rechazada correctamente.';
        mostrarAlerta(mensaje, decision === 'aprobar' ? 'success' : 'warning');
        await cargarSolicitudes();
    }

    function badgeEstado(estado) {
        const clases = { PENDIENTE: 'warning text-dark', APROBADA: 'success', RECHAZADA: 'danger' };
        const textos = { PENDIENTE: 'Pendiente', APROBADA: 'Aprobada', RECHAZADA: 'Rechazada' };
        return `<span class="badge bg-${clases[estado] || 'secondary'}">${textos[estado] || _esc(estado)}</span>`;
    }

    function formatearFecha(fechaIso) {
        if (!fechaIso) return '-';
        return String(fechaIso).replace('T', ' ').slice(0, 16);
    }

    function verDetalle(id) {
        const s = solicitudes.find(x => x.id === id);
        if (!s) return;

        document.getElementById('detalleCREmpresa').textContent        = s.nombreEmpresa;
        document.getElementById('detalleCREstado').innerHTML           = badgeEstado(s.estado);
        document.getElementById('detalleCRFecha').textContent          = formatearFecha(s.fechaSolicitud);
        document.getElementById('detalleCRRubroAnterior').textContent  = s.rubroAnteriorNombre || 'Sin rubro anterior';
        document.getElementById('detalleCRRubroSolicitado').textContent =
            s.nombreRubroSolicitado.toLowerCase() === 'otros' && s.descripcionOtros
                ? `Otros (${s.descripcionOtros})` : s.nombreRubroSolicitado;
        document.getElementById('detalleCRJustificacion').textContent  = s.justificacion;

        const elSolicitadoPor = document.getElementById('detalleCRSolicitadoPor');
        const bloqueSolicitadoPor = document.getElementById('bloqueSolicitadoPor');
        if (s.solicitadoPor) { elSolicitadoPor.textContent = s.solicitadoPor; bloqueSolicitadoPor.classList.remove('d-none'); }
        else { bloqueSolicitadoPor.classList.add('d-none'); }

        const bloqueResueltoPor = document.getElementById('bloqueResueltoPor');
        const bloqueFechaResolucion = document.getElementById('bloqueFechaResolucion');
        if (s.resueltoPor) {
            document.getElementById('detalleCRResueltoPor').textContent = s.resueltoPor;
            bloqueResueltoPor.classList.remove('d-none');
            document.getElementById('detalleCRFechaResolucion').textContent = formatearFecha(s.fechaResolucion);
            bloqueFechaResolucion.classList.remove('d-none');
        } else {
            bloqueResueltoPor.classList.add('d-none');
            bloqueFechaResolucion.classList.add('d-none');
        }

        const bloqueMotivoRechazo = document.getElementById('bloqueMotivoRechazo');
        if (s.estado === 'RECHAZADA' && s.motivoRechazo) {
            document.getElementById('detalleCRMotivoRechazo').textContent = s.motivoRechazo;
            bloqueMotivoRechazo.classList.remove('d-none');
        } else {
            bloqueMotivoRechazo.classList.add('d-none');
        }

        const tieneRubrica = s.puntajeImpactoOperativo != null;
        const bloqueRubrica = document.getElementById('bloqueRubricaDetalle');
        if (tieneRubrica) {
            document.getElementById('detalleCRPuntajeImpacto').textContent       = `${s.puntajeImpactoOperativo} / 5`;
            document.getElementById('detalleCRPuntajeCompatibilidad').textContent = `${s.puntajeCompatibilidadParque} / 5`;
            document.getElementById('detalleCRPuntajeViabilidad').textContent     = `${s.puntajeViabilidadTecnica} / 5`;
            document.getElementById('detalleCRPuntajeCumplimiento').textContent   = `${s.puntajeCumplimientoNormativo} / 5`;
            const bloqueObs = document.getElementById('bloqueObservacionesRubrica');
            if (s.observacionesRubrica) {
                document.getElementById('detalleCRObservaciones').textContent = s.observacionesRubrica;
                bloqueObs.classList.remove('d-none');
            } else {
                bloqueObs.classList.add('d-none');
            }
            bloqueRubrica.classList.remove('d-none');
        } else {
            bloqueRubrica.classList.add('d-none');
        }

        new bootstrap.Modal(document.getElementById('modalDetalleCambioRubro')).show();
    }

    return { cargar, enviarSolicitud, abrirResolucion, onCambioDecision, confirmarResolucion, toggleDescripcionOtros, verDetalle };
})();
