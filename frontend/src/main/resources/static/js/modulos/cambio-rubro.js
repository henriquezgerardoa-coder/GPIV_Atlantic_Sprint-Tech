const ModuloCambioRubro = (() => {
    let rubros = [];
    let solicitudes = [];

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
                    ${r.nombre}${r.requierePermisoEspecial ? ' ⚠ (requiere permiso especial)' : ''}
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

        cuerpo.innerHTML = solicitudes.length
            ? solicitudes.map(s => {
                const rubroSolicitadoTexto = s.nombreRubroSolicitado.toLowerCase() === 'otros' && s.descripcionOtros
                    ? `Otros <span class="text-muted small">(${s.descripcionOtros})</span>`
                    : s.nombreRubroSolicitado;
                return `
                <tr>
                    <td class="ps-3 fw-semibold">${s.nombreEmpresa}</td>
                    <td>${s.rubroAnteriorNombre || '<span class="text-muted">Sin rubro</span>'}</td>
                    <td>${rubroSolicitadoTexto}</td>
                    <td class="small text-muted" style="max-width:200px">${s.justificacion}</td>
                    <td>${badgeEstado(s.estado)}</td>
                    <td class="small text-muted">${formatearFecha(s.fechaSolicitud)}</td>
                    <td class="small text-muted">${s.resueltoPor || '-'}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-secondary" title="Ver detalle"
                            onclick="ModuloCambioRubro.verDetalle(${s.id})">
                            <i class="bi bi-eye"></i>
                        </button>
                    </td>
                    ${esGestor ? `
                    <td class="text-center columnaResolucion">
                        ${s.estado === 'PENDIENTE' ? `
                        <button class="btn btn-sm btn-success me-1"
                            onclick="ModuloCambioRubro.aprobar(${s.id}, ${!!s.descripcionOtros})">
                            <i class="bi bi-check-lg"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger"
                            onclick="ModuloCambioRubro.rechazar(${s.id})">
                            <i class="bi bi-x-lg"></i>
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

    async function aprobar(solicitudId, esOtros) {
        if (!confirm('¿Confirma la aprobación del cambio de rubro? El rubro de la empresa será actualizado.')) return;
        const rubrica = solicitarRubrica();
        if (!rubrica) return;
        const cuerpo = { aprobada: true, ...rubrica };
        if (esOtros) {
            const nombreNuevoRubro = prompt(
                'La empresa solicitó el rubro "Otros".\n' +
                'Ingrese el nombre oficial del nuevo rubro para crearlo en el sistema\n' +
                '(deje vacío para dejarlo como "Otros"):'
            );
            if (nombreNuevoRubro === null) return;
            if (nombreNuevoRubro.trim()) cuerpo.nombreNuevoRubro = nombreNuevoRubro.trim();
        }
        const resp = await ApiCliente.parche(`/api/cambios-rubro/${solicitudId}/resolver`, cuerpo);
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlerta(err?.mensaje || err?.message || 'No se pudo aprobar la solicitud.', 'danger');
            return;
        }
        mostrarAlerta('Solicitud aprobada. El rubro de la empresa fue actualizado.');
        await cargarSolicitudes();
    }

    async function rechazar(solicitudId) {
        const motivo = prompt('Ingrese el motivo de rechazo (obligatorio):');
        if (motivo === null) return;
        if (!motivo.trim()) {
            mostrarAlerta('El motivo de rechazo es obligatorio.', 'danger');
            return;
        }
        const rubrica = solicitarRubrica();
        if (!rubrica) return;
        const resp = await ApiCliente.parche(`/api/cambios-rubro/${solicitudId}/resolver`, {
            aprobada: false,
            motivoRechazo: motivo.trim(),
            ...rubrica
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlerta(err?.mensaje || err?.message || 'No se pudo rechazar la solicitud.', 'danger');
            return;
        }
        mostrarAlerta('Solicitud rechazada.');
        await cargarSolicitudes();
    }

    function badgeEstado(estado) {
        const clases = { PENDIENTE: 'warning text-dark', APROBADA: 'success', RECHAZADA: 'danger' };
        const textos = { PENDIENTE: 'Pendiente', APROBADA: 'Aprobada', RECHAZADA: 'Rechazada' };
        return `<span class="badge bg-${clases[estado] || 'secondary'}">${textos[estado] || estado}</span>`;
    }

    function formatearFecha(fechaIso) {
        if (!fechaIso) return '-';
        return String(fechaIso).replace('T', ' ').slice(0, 16);
    }

    function solicitarRubrica() {
        const puntajeImpactoOperativo = solicitarPuntaje('impacto operativo');
        if (puntajeImpactoOperativo === null) return null;
        const puntajeCompatibilidadParque = solicitarPuntaje('compatibilidad del parque');
        if (puntajeCompatibilidadParque === null) return null;
        const puntajeViabilidadTecnica = solicitarPuntaje('viabilidad tecnica');
        if (puntajeViabilidadTecnica === null) return null;
        const puntajeCumplimientoNormativo = solicitarPuntaje('cumplimiento normativo');
        if (puntajeCumplimientoNormativo === null) return null;

        const observaciones = prompt('Observaciones de rúbrica (opcional):');
        if (observaciones === null) return null;

        return {
            puntajeImpactoOperativo,
            puntajeCompatibilidadParque,
            puntajeViabilidadTecnica,
            puntajeCumplimientoNormativo,
            observacionesRubrica: observaciones.trim() || null
        };
    }

    function solicitarPuntaje(etiqueta) {
        const valor = prompt(`Ingrese puntaje de ${etiqueta} (1 a 5):`);
        if (valor === null) return null;
        const numero = parseInt(valor, 10);
        if (!Number.isInteger(numero) || numero < 1 || numero > 5) {
            mostrarAlerta(`El puntaje de ${etiqueta} debe ser un número entre 1 y 5.`, 'danger');
            return null;
        }
        return numero;
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

    return { cargar, enviarSolicitud, aprobar, rechazar, toggleDescripcionOtros, verDetalle };
})();
