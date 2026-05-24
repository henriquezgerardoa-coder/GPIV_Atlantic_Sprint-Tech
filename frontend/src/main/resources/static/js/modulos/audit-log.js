/**
 * ModuloAuditLog — Registro de auditoría del sistema.
 * Visible solo para ADMINISTRADOR y DIRECTIVO.
 */
const ModuloAuditLog = (() => {

    let logSeleccionado = null;

    async function cargar() {
        const entidad = document.getElementById('filtroAuditEntidad')?.value.trim() || '';
        const usuario = document.getElementById('filtroAuditUsuario')?.value.trim() || '';

        let url = '/api/audit-log';
        if (entidad) url += '?entidad=' + encodeURIComponent(entidad);
        else if (usuario) url += '?usuario=' + encodeURIComponent(usuario);

        mostrarCargando();
        try {
            const resp = await ApiCliente.obtener(url);
            if (!resp?.ok) {
                mostrarError('No se pudo cargar el registro de auditoría.');
                return;
            }
            const datos = await resp.json();
            renderizar(datos);
        } catch (err) {
            console.error('Error al cargar audit-log:', err);
            mostrarError('Error al cargar el registro de auditoría.');
        }
    }

    function renderizar(logs) {
        const tbody = document.getElementById('cuerpoTablaAuditLog');
        if (!tbody) return;

        if (!logs.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">Sin registros</td></tr>';
            return;
        }

        tbody.innerHTML = logs.map((log, i) => `
            <tr>
                <td class="ps-3 text-muted small">${i + 1}</td>
                <td class="small">${formatearFecha(log.fechaHora)}</td>
                <td><span class="fw-semibold">${esc(log.usuarioResponsable)}</span></td>
                <td>${badgeOperacion(log.operacion)}</td>
                <td class="small">${esc(log.entidadAfectada)}<span class="text-muted ms-1">#${esc(log.idReferencia)}</span></td>
                <td class="text-muted small">${esc(log.direccionIp)}</td>
                <td class="text-center">
                    <button class="btn btn-outline-secondary btn-sm py-0"
                            onclick="ModuloAuditLog.verDetalle(${log.id})"
                            title="Ver cambios">
                        <i class="bi bi-eye"></i>
                    </button>
                </td>
            </tr>
        `).join('');

        window._auditLogDatos = logs;
    }

    function verDetalle(id) {
        const logs = window._auditLogDatos || [];
        logSeleccionado = logs.find(l => l.id === id);
        if (!logSeleccionado) return;

        document.getElementById('auditDetalleFecha').textContent        = formatearFecha(logSeleccionado.fechaHora);
        document.getElementById('auditDetalleUsuario').textContent      = logSeleccionado.usuarioResponsable;
        document.getElementById('auditDetalleOperacion').innerHTML      = badgeOperacion(logSeleccionado.operacion);
        document.getElementById('auditDetalleEntidad').textContent      = logSeleccionado.entidadAfectada + ' #' + logSeleccionado.idReferencia;
        document.getElementById('auditDetalleIp').textContent           = logSeleccionado.direccionIp;
        document.getElementById('auditDetalleAnterior').textContent     = formatearJson(logSeleccionado.valorAnterior);
        document.getElementById('auditDetalleNuevo').textContent        = formatearJson(logSeleccionado.valorNuevo);

        new bootstrap.Modal(document.getElementById('modalAuditDetalle')).show();
    }

    function mostrarCargando() {
        const tbody = document.getElementById('cuerpoTablaAuditLog');
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Cargando...</td></tr>';
    }

    function mostrarError(msg) {
        const tbody = document.getElementById('cuerpoTablaAuditLog');
        if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">${msg}</td></tr>`;
    }

    function badgeOperacion(op) {
        const mapa = {
            CREAR:    'success',
            ACTUALIZAR: 'warning',
            ELIMINAR: 'danger',
            CONSULTAR: 'secondary',
        };
        const color = mapa[op] || 'info';
        return `<span class="badge bg-${color}">${op}</span>`;
    }

    function formatearFecha(iso) {
        if (!iso) return '–';
        const d = new Date(iso);
        return d.toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'medium' });
    }

    function formatearJson(valor) {
        if (!valor) return '—';
        try {
            return JSON.stringify(JSON.parse(valor), null, 2);
        } catch {
            return valor;
        }
    }

    function esc(val) {
        if (val == null) return '–';
        return String(val)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    return { cargar, verDetalle };
})();
