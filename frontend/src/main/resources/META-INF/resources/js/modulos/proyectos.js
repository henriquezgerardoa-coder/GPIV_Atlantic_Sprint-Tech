const ModuloProyectos = (() => {
    let proyectos = [];
    let proyectoActivo = null;

    async function cargar() {
        await Promise.all([cargarProyectos(), cargarAlertas()]);
        ajustarVistaPorRol();
    }

    function ajustarVistaPorRol() {
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);
        document.getElementById('btnNuevoProyecto')?.classList.toggle('d-none', !esGestor);
    }

    async function cargarProyectos() {
        const resp = await ApiCliente.obtener('/api/proyectos');
        const cuerpo = document.getElementById('cuerpoTablaProyectos');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-danger">Error al cargar proyectos</td></tr>';
            return;
        }
        proyectos = await resp.json();
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);

        cuerpo.innerHTML = proyectos.length
            ? proyectos.map(p => `
                <tr>
                    <td class="ps-3 fw-semibold">
                        ${p.nombre}
                        ${p.tieneHitosVencidos ? '<span class="badge bg-danger ms-1" title="Tiene hitos vencidos"><i class="bi bi-exclamation-triangle-fill"></i></span>' : ''}
                    </td>
                    <td class="small text-muted">${p.empresaNombre || '<span class="text-muted fst-italic">Sin empresa</span>'}</td>
                    <td>${badgeEstado(p.estado)}</td>
                    <td style="min-width:120px">
                        <div class="progress" style="height:10px" title="${Math.round(p.avanceFisico)}% completado">
                            <div class="progress-bar ${p.avanceFisico >= 100 ? 'bg-success' : 'bg-primary'}"
                                 style="width:${p.avanceFisico}%"></div>
                        </div>
                        <small class="text-muted">${Math.round(p.avanceFisico)}%</small>
                    </td>
                    <td class="small text-muted">${p.fechaEstimadaFin || '-'}</td>
                    <td class="small text-muted">${p.responsableNombre || '-'}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-primary" title="Ver hitos"
                                onclick="ModuloProyectos.verHitos(${p.id})">
                            <i class="bi bi-list-check"></i>
                        </button>
                        ${esGestor ? `
                        <button class="btn btn-sm btn-outline-secondary ms-1" title="Cambiar estado"
                                onclick="ModuloProyectos.abrirCambioEstado(${p.id}, '${p.estado}')">
                            <i class="bi bi-arrow-repeat"></i>
                        </button>` : ''}
                    </td>
                </tr>`).join('')
            : '<tr><td colspan="7" class="text-muted text-center py-3">Sin proyectos registrados</td></tr>';
    }

    async function cargarAlertas() {
        const resp = await ApiCliente.obtener('/api/proyectos/alertas');
        const bloque = document.getElementById('bloqueAlertasHitos');
        const lista = document.getElementById('listaAlertasHitos');
        if (!bloque || !lista) return;
        if (!resp?.ok) return;
        const alertas = await resp.json();
        if (!alertas.length) {
            bloque.classList.add('d-none');
            return;
        }
        bloque.classList.remove('d-none');
        lista.innerHTML = alertas.map(h => `
            <li class="list-group-item list-group-item-danger py-1 px-3 small">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <strong>${h.descripcion}</strong> — venció el ${h.fechaVencimiento}
            </li>`).join('');
    }

    async function verHitos(proyectoId) {
        proyectoActivo = proyectos.find(p => p.id === proyectoId);
        if (!proyectoActivo) return;

        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);

        document.getElementById('tituloModalHitos').textContent = proyectoActivo.nombre;
        document.getElementById('subtituloModalHitos').textContent =
            (proyectoActivo.empresaNombre || '') + ' · ' + traducirEstado(proyectoActivo.estado);

        document.getElementById('bloqueFormNuevoHito')?.classList.toggle('d-none', !esGestor);

        renderizarHitos(proyectoActivo.hitos, esGestor);

        document.getElementById('formNuevoHito')?.reset();
        document.getElementById('alertaModalHitos')?.classList.add('d-none');

        const modal = new bootstrap.Modal(document.getElementById('modalHitosProyecto'));
        modal.show();
    }

    function renderizarHitos(hitos, esGestor) {
        const lista = document.getElementById('listaHitosModal');
        if (!lista) return;
        if (!hitos.length) {
            lista.innerHTML = '<p class="text-muted text-center py-2 small">Sin hitos cargados para este proyecto.</p>';
            return;
        }
        lista.innerHTML = hitos.map(h => `
            <div class="d-flex align-items-center gap-2 border-bottom py-2">
                <span class="flex-grow-1 small">
                    ${h.vencido ? '<span class="badge bg-danger me-1"><i class="bi bi-exclamation-triangle-fill"></i> Vencido</span>' :
                      h.cumplido ? '<span class="badge bg-success me-1"><i class="bi bi-check-lg"></i> Cumplido</span>' :
                      '<span class="badge bg-warning text-dark me-1">Pendiente</span>'}
                    ${h.descripcion}
                    ${h.fechaVencimiento ? `<span class="text-muted ms-1">(hasta ${h.fechaVencimiento})</span>` : ''}
                </span>
                ${esGestor && !h.cumplido ? `
                <button class="btn btn-sm btn-success" title="Marcar cumplido"
                        onclick="ModuloProyectos.marcarCumplido(${proyectoActivo.id}, ${h.id})">
                    <i class="bi bi-check-lg"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" title="Eliminar"
                        onclick="ModuloProyectos.eliminarHito(${proyectoActivo.id}, ${h.id})">
                    <i class="bi bi-trash"></i>
                </button>` : ''}
            </div>`).join('');
    }

    async function agregarHito() {
        const alerta = document.getElementById('alertaModalHitos');
        alerta?.classList.add('d-none');

        const descripcion = document.getElementById('campoDescripcionHito')?.value.trim();
        const fechaVencimiento = document.getElementById('campoFechaHito')?.value || null;

        if (!descripcion) {
            mostrarAlertaModal('alertaModalHitos', 'La descripción del hito es obligatoria.');
            return;
        }

        const resp = await ApiCliente.crear(`/api/proyectos/${proyectoActivo.id}/hitos`, {
            descripcion,
            fechaVencimiento
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalHitos', err?.mensaje || err?.message || 'No se pudo agregar el hito.');
            return;
        }

        document.getElementById('formNuevoHito')?.reset();
        await recargarHitosModal();
        await cargarProyectos();
    }

    async function marcarCumplido(proyectoId, hitoId) {
        const resp = await ApiCliente.parche(`/api/proyectos/${proyectoId}/hitos/${hitoId}/cumplido`, {});
        if (!resp?.ok) {
            mostrarAlerta('No se pudo marcar el hito como cumplido.', 'danger');
            return;
        }
        await recargarHitosModal();
        await Promise.all([cargarProyectos(), cargarAlertas()]);
    }

    async function eliminarHito(proyectoId, hitoId) {
        if (!confirm('¿Eliminar este hito?')) return;
        const resp = await ApiCliente.eliminar(`/api/proyectos/${proyectoId}/hitos/${hitoId}`);
        if (!resp?.ok) {
            mostrarAlerta('No se pudo eliminar el hito.', 'danger');
            return;
        }
        await recargarHitosModal();
        await cargarProyectos();
    }

    async function recargarHitosModal() {
        const resp = await ApiCliente.obtener('/api/proyectos');
        if (!resp?.ok) return;
        proyectos = await resp.json();
        proyectoActivo = proyectos.find(p => p.id === proyectoActivo.id);
        if (!proyectoActivo) return;
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);
        renderizarHitos(proyectoActivo.hitos, esGestor);
    }

    function abrirCambioEstado(proyectoId, estadoActual) {
        const modal = document.getElementById('modalCambioEstadoProyecto');
        if (!modal) return;
        modal.dataset.proyectoId = proyectoId;
        document.getElementById('selectorEstadoProyecto').value = estadoActual;
        document.getElementById('alertaModalEstado')?.classList.add('d-none');
        new bootstrap.Modal(modal).show();
    }

    async function guardarEstado() {
        const modal = document.getElementById('modalCambioEstadoProyecto');
        const proyectoId = parseInt(modal.dataset.proyectoId);
        const estado = document.getElementById('selectorEstadoProyecto').value;
        const resp = await ApiCliente.parche(`/api/proyectos/${proyectoId}/estado`, { estado });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalEstado', err?.mensaje || err?.message || 'No se pudo cambiar el estado.');
            return;
        }
        bootstrap.Modal.getInstance(modal)?.hide();
        mostrarAlerta('Estado del proyecto actualizado.');
        await cargarProyectos();
    }

    function abrirCreacion() {
        document.getElementById('formNuevoProyecto')?.reset();
        document.getElementById('alertaModalNuevoProyecto')?.classList.add('d-none');
        cargarOpcionesRadicaciones();
        new bootstrap.Modal(document.getElementById('modalNuevoProyecto')).show();
    }

    async function cargarOpcionesRadicaciones() {
        const resp = await ApiCliente.obtener('/api/radicaciones');
        const selector = document.getElementById('selectorRadicacionProyecto');
        if (!selector) return;
        if (!resp?.ok) return;
        const lista = await resp.json();
        const aprobadas = lista.filter(r => r.estado === 'APROBADA' || r.estado === 'RADICADA');
        selector.innerHTML = '<option value="">Sin radicación asociada</option>'
            + aprobadas.map(r => `<option value="${r.id}">${r.numeroRadicado} — ${r.nombreEmpresa || ''}</option>`).join('');
    }

    async function guardarNuevoProyecto() {
        const nombre = document.getElementById('campoNombreProyecto')?.value.trim();
        const descripcion = document.getElementById('campoDescripcionProyecto')?.value.trim();
        const montoStr = document.getElementById('campoMontoProyecto')?.value.trim();
        const fechaEstimadaFin = document.getElementById('campoFechaFinProyecto')?.value || null;
        const radicacionId = parseInt(document.getElementById('selectorRadicacionProyecto')?.value) || null;

        if (!nombre) { mostrarAlertaModal('alertaModalNuevoProyecto', 'El nombre es obligatorio.'); return; }
        if (!descripcion) { mostrarAlertaModal('alertaModalNuevoProyecto', 'La descripción es obligatoria.'); return; }

        const montoInversion = montoStr ? parseFloat(montoStr) : null;

        const resp = await ApiCliente.crear('/api/proyectos', {
            nombre, descripcion, montoInversion, fechaEstimadaFin, radicacionId
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalNuevoProyecto', err?.mensaje || err?.message || 'No se pudo crear el proyecto.');
            return;
        }
        bootstrap.Modal.getInstance(document.getElementById('modalNuevoProyecto'))?.hide();
        mostrarAlerta('Proyecto creado correctamente.');
        await cargarProyectos();
    }

    function badgeEstado(estado) {
        const mapa = {
            PLANIFICADO:  'secondary',
            EN_EJECUCION: 'primary',
            PAUSADO:      'warning text-dark',
            FINALIZADO:   'success',
            CANCELADO:    'danger'
        };
        return `<span class="badge bg-${mapa[estado] || 'secondary'}">${traducirEstado(estado)}</span>`;
    }

    function traducirEstado(estado) {
        const textos = {
            PLANIFICADO:  'Planificado',
            EN_EJECUCION: 'En ejecución',
            PAUSADO:      'Pausado',
            FINALIZADO:   'Finalizado',
            CANCELADO:    'Cancelado'
        };
        return textos[estado] || estado;
    }

    return { cargar, verHitos, agregarHito, marcarCumplido, eliminarHito,
             abrirCambioEstado, guardarEstado, abrirCreacion, guardarNuevoProyecto };
})();
