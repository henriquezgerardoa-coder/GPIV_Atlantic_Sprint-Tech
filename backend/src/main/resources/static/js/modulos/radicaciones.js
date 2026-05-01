const ModuloRadicaciones = (() => {
    let radicaciones = [];

    async function cargar() {
        const params = new URLSearchParams();
        const estado = document.getElementById('filtroEstadoRad')?.value;
        const desde = document.getElementById('filtroDesdeRad')?.value;
        const hasta = document.getElementById('filtroHastaRad')?.value;
        if (estado) params.set('estado', estado);
        if (desde) params.set('desde', desde);
        if (hasta) params.set('hasta', hasta);

        const ruta = '/api/radicaciones' + (params.toString() ? '?' + params.toString() : '');
        const respuesta = await ApiCliente.obtener(ruta);
        if (!respuesta?.ok) {
            mostrarAlerta('No se pudieron cargar las radicaciones.', 'danger');
            return;
        }

        radicaciones = await respuesta.json();
        renderizarTabla();
        poblarSelectorRadicaciones();
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaRadicaciones');
        if (!cuerpo) return;

        if (radicaciones.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">Sin registros</td></tr>';
            return;
        }

        const puedeGestionar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        cuerpo.innerHTML = radicaciones.map(r => `
            <tr>
                <td class="ps-3 fw-semibold">${r.numeroRadicado}</td>
                <td>${r.tipoSolicitud}</td>
                <td><span class="badge bg-secondary">${formatearEstado(r.estado)}</span></td>
                <td>${r.fechaRadicacion || '-'}</td>
                <td>${(r.fechaUltimaActualizacion || '').replace('T', ' ').slice(0, 16)}</td>
                <td class="text-center">
                    ${puedeGestionar ? `<button class="btn btn-sm btn-outline-primary" onclick="ModuloRadicaciones.marcarEnRevision(${r.id})">En revisión</button>` : '<span class="text-muted small">Solo lectura</span>'}
                </td>
            </tr>
        `).join('');
    }

    function poblarSelectorRadicaciones() {
        const selector = document.getElementById('selectorRadicacionDocumento');
        if (!selector) return;
        selector.innerHTML = radicaciones.map(r => `<option value="${r.id}">${r.numeroRadicado}</option>`).join('');
    }

    async function crear() {
        const tipoSolicitud = document.getElementById('campoTipoSolicitudRad').value.trim();
        const descripcion = document.getElementById('campoDescripcionRad').value.trim();
        if (!tipoSolicitud || !descripcion) {
            mostrarErrorDocumento('Complete tipo y descripción.');
            return;
        }

        const respuesta = await ApiCliente.crear('/api/radicaciones', { tipoSolicitud, descripcion });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarErrorDocumento(error?.mensaje || 'No se pudo crear la radicación.');
            return;
        }

        document.getElementById('formRadicacionNueva').reset();
        mostrarAlerta('Radicación creada correctamente.');
        await cargar();
    }

    async function subirDocumento() {
        const id = document.getElementById('selectorRadicacionDocumento').value;
        const tipoDocumento = document.getElementById('selectorTipoDocumentoRad').value;
        const descripcion = document.getElementById('campoDescripcionDocumentoRad').value.trim();
        const archivo = document.getElementById('campoArchivoDocumentoRad').files[0];

        if (!id || !archivo) {
            mostrarErrorDocumento('Seleccione radicación y archivo.');
            return;
        }

        const formData = new FormData();
        formData.append('tipoDocumento', tipoDocumento);
        formData.append('descripcion', descripcion);
        formData.append('archivo', archivo);

        const respuesta = await ApiCliente.subirArchivo(`/api/radicaciones/${id}/documentos`, formData);
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarErrorDocumento(error?.mensaje || 'No se pudo subir el documento.');
            return;
        }

        document.getElementById('formDocumentoRad').reset();
        mostrarAlerta('Documento cargado correctamente.');
        await cargar();
    }

    async function marcarEnRevision(id) {
        const respuesta = await ApiCliente.parche(`/api/radicaciones/${id}/estado`, {
            estado: 'EN_REVISION',
            comentario: 'Cambio de estado desde panel'
        });
        if (!respuesta?.ok) {
            mostrarAlerta('No se pudo actualizar el estado.', 'danger');
            return;
        }
        await cargar();
    }

    function mostrarErrorDocumento(mensaje) {
        const alerta = document.getElementById('alertaDocumentoRad');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function formatearEstado(estado) {
        const m = {
            PENDIENTE: 'Pendiente',
            EN_REVISION: 'En revisión',
            APROBADA: 'Aprobada',
            RECHAZADA: 'Rechazada',
            REQUIERE_INFORMACION_ADICIONAL: 'Requiere información adicional',
            CANCELADA: 'Cancelada'
        };
        return m[estado] || estado;
    }

    return { cargar, crear, subirDocumento, marcarEnRevision };
})();

