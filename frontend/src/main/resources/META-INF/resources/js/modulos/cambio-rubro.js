const ModuloCambioRubro = (() => {
    let rubros = [];
    let solicitudes = [];

    async function cargar() {
        await Promise.all([cargarRubros(), cargarSolicitudes()]);
        ajustarVistaPorRol();
    }

    function ajustarVistaPorRol() {
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        const esGestor  = !esEmpresa && Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);

        document.getElementById('bloqueFormSolicitudCambioRubro')?.classList.toggle('d-none', !esEmpresa);
        document.getElementById('columnaResolucion')?.classList.toggle('d-none', !esGestor);
        document.getElementById('thResolucion')?.classList.toggle('d-none', !esGestor);
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
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);

        cuerpo.innerHTML = solicitudes.length
            ? solicitudes.map(s => `
                <tr>
                    <td class="ps-3 fw-semibold">${s.nombreEmpresa}</td>
                    <td>${s.rubroAnteriorNombre || '<span class="text-muted">Sin rubro</span>'}</td>
                    <td>${s.nombreRubroSolicitado}</td>
                    <td class="small text-muted" style="max-width:200px">${s.justificacion}</td>
                    <td>${badgeEstado(s.estado)}</td>
                    <td class="small text-muted">${formatearFecha(s.fechaSolicitud)}</td>
                    <td class="small text-muted">${s.resueltoPor || '-'}</td>
                    ${esGestor ? `
                    <td class="text-center columnaResolucion">
                        ${s.estado === 'PENDIENTE' ? `
                        <button class="btn btn-sm btn-success me-1"
                            onclick="ModuloCambioRubro.aprobar(${s.id})">
                            <i class="bi bi-check-lg"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger"
                            onclick="ModuloCambioRubro.rechazar(${s.id})">
                            <i class="bi bi-x-lg"></i>
                        </button>` : '<span class="text-muted small">—</span>'}
                    </td>` : ''}
                </tr>`).join('')
            : `<tr><td colspan="${esGestor ? 8 : 7}" class="text-muted text-center py-3">Sin solicitudes registradas</td></tr>`;
    }

    async function enviarSolicitud() {
        const alerta = document.getElementById('alertaCambioRubro');
        alerta?.classList.add('d-none');

        const rubroSolicitadoId = parseInt(document.getElementById('selectorRubroSolicitado').value);
        const justificacion = document.getElementById('campoJustificacionRubro').value.trim();

        if (!rubroSolicitadoId) {
            alerta.textContent = 'Seleccione el rubro solicitado.';
            alerta?.classList.remove('d-none');
            return;
        }
        if (!justificacion) {
            alerta.textContent = 'La justificación es obligatoria.';
            alerta?.classList.remove('d-none');
            return;
        }

        const resp = await ApiCliente.crear('/api/cambios-rubro', { rubroSolicitadoId, justificacion });
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

    async function aprobar(solicitudId) {
        if (!confirm('¿Confirma la aprobación del cambio de rubro? El rubro de la empresa será actualizado.')) return;
        const resp = await ApiCliente.parche(`/api/cambios-rubro/${solicitudId}/resolver`, {
            aprobada: true
        });
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
        const resp = await ApiCliente.parche(`/api/cambios-rubro/${solicitudId}/resolver`, {
            aprobada: false,
            motivoRechazo: motivo.trim()
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

    return { cargar, enviarSolicitud, aprobar, rechazar };
})();
