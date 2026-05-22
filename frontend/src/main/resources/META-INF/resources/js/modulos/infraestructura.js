const ModuloInfraestructura = (() => {
    let servicios = [];
    let servicioActivo = null;

    const CFG = {
        OPERATIVO:     { label: 'Operativo',     color: 'success', icono: 'check-circle-fill',        bg: 'bg-success' },
        MANTENIMIENTO: { label: 'Mantenimiento', color: 'warning', icono: 'tools',                    bg: 'bg-warning' },
        FALLA_CRITICA: { label: 'Falla crítica', color: 'danger',  icono: 'exclamation-octagon-fill', bg: 'bg-danger'  }
    };

    async function cargar() {
        const resp = await ApiCliente.obtener('/api/infraestructura');
        const contenedor = document.getElementById('gridServicios');
        if (!contenedor) return;
        if (!resp?.ok) {
            contenedor.innerHTML = '<p class="text-danger col-12">Error al cargar los servicios.</p>';
            return;
        }
        servicios = await resp.json();
        ajustarVistaPorRol();
        renderizarGrid();
        actualizarResumen();
    }

    function ajustarVistaPorRol() {
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);
        document.getElementById('btnNuevoServicio')?.classList.toggle('d-none', !esGestor);
    }

    function renderizarGrid() {
        const contenedor = document.getElementById('gridServicios');
        if (!contenedor) return;
        if (!servicios.length) {
            contenedor.innerHTML = '<p class="text-muted col-12 py-3">Sin servicios registrados.</p>';
            return;
        }
        contenedor.innerHTML = servicios.map(s => {
            const cfg = CFG[s.estadoActual] || CFG.OPERATIVO;
            const fecha = s.fechaUltimaActualizacion
                ? String(s.fechaUltimaActualizacion).replace('T', ' ').slice(0, 16)
                : null;
            return `
            <div class="col-sm-6 col-lg-4">
                <div class="card border-0 shadow-sm h-100 ${s.estadoActual === 'FALLA_CRITICA' ? 'border-danger border' : ''}">
                    <div class="card-body">
                        <div class="d-flex align-items-center justify-content-between mb-2">
                            <span class="badge ${cfg.bg} ${cfg.color === 'warning' ? 'text-dark' : ''}">
                                <i class="bi bi-${cfg.icono} me-1"></i>${cfg.label}
                            </span>
                            <small class="text-muted">#${s.id}</small>
                        </div>
                        <h6 class="fw-bold mb-1">${s.nombre}</h6>
                        ${s.descripcionTecnica ? `<p class="small text-muted mb-2" style="font-size:0.78rem">${s.descripcionTecnica}</p>` : ''}
                        ${s.ultimoComentario
                            ? `<div class="alert alert-${cfg.color} py-1 px-2 small mb-2" style="font-size:0.78rem">
                                   <i class="bi bi-chat-left-text me-1"></i>${s.ultimoComentario}
                               </div>`
                            : ''}
                        <div class="mt-auto d-flex justify-content-between align-items-center">
                            <small class="text-muted">${fecha ? `Actualizado ${fecha}` : ''}</small>
                            <button class="btn btn-sm btn-outline-secondary" title="Ver historial y gestionar"
                                    onclick="ModuloInfraestructura.abrirDetalle(${s.id})">
                                <i class="bi bi-clock-history"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>`;
        }).join('');
    }

    function actualizarResumen() {
        const totales = { OPERATIVO: 0, MANTENIMIENTO: 0, FALLA_CRITICA: 0 };
        servicios.forEach(s => { if (totales[s.estadoActual] !== undefined) totales[s.estadoActual]++; });

        const badge = (estado, n) => n > 0
            ? `<span class="badge ${CFG[estado].bg} ${estado === 'MANTENIMIENTO' ? 'text-dark' : ''} me-2">
                   <i class="bi bi-${CFG[estado].icono} me-1"></i>${n} ${CFG[estado].label}
               </span>`
            : '';

        document.getElementById('resumenEstadoServicios').innerHTML =
            badge('OPERATIVO', totales.OPERATIVO) +
            badge('MANTENIMIENTO', totales.MANTENIMIENTO) +
            badge('FALLA_CRITICA', totales.FALLA_CRITICA);
    }

    async function abrirDetalle(servicioId) {
        servicioActivo = servicios.find(s => s.id === servicioId);
        if (!servicioActivo) return;

        const cfg = CFG[servicioActivo.estadoActual] || CFG.OPERATIVO;
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);

        document.getElementById('tituloModalServicio').textContent = servicioActivo.nombre;
        document.getElementById('estadoBadgeServicio').innerHTML =
            `<span class="badge ${cfg.bg} ${cfg.color === 'warning' ? 'text-dark' : ''}">
                <i class="bi bi-${cfg.icono} me-1"></i>${cfg.label}
             </span>`;

        document.getElementById('bloqueActualizarEstado')?.classList.toggle('d-none', !esGestor);
        document.getElementById('selectorNuevoEstado').value = servicioActivo.estadoActual;
        document.getElementById('campoComentarioServicio').value = '';
        document.getElementById('alertaModalServicio')?.classList.add('d-none');

        document.getElementById('historialServicioLista').innerHTML =
            '<p class="text-muted small text-center py-2"><i class="bi bi-hourglass-split me-1"></i>Cargando...</p>';

        new bootstrap.Modal(document.getElementById('modalDetalleServicio')).show();
        await cargarHistorial(servicioId);
    }

    async function cargarHistorial(servicioId) {
        const resp = await ApiCliente.obtener(`/api/infraestructura/${servicioId}/historial`);
        const lista = document.getElementById('historialServicioLista');
        if (!resp?.ok) { lista.innerHTML = '<p class="text-danger small">Error al cargar historial.</p>'; return; }
        const eventos = await resp.json();
        if (!eventos.length) {
            lista.innerHTML = '<p class="text-muted small text-center py-2">Sin eventos registrados.</p>';
            return;
        }
        lista.innerHTML = eventos.map((ev, idx) => {
            const cfg = CFG[ev.estado] || CFG.OPERATIVO;
            const esUltimo = idx === eventos.length - 1;
            const fecha = String(ev.fechaEvento).replace('T', ' ').slice(0, 16);
            return `
            <div class="d-flex gap-3 ${esUltimo ? '' : 'mb-1'}">
                <div class="d-flex flex-column align-items-center" style="min-width:32px">
                    <div class="rounded-circle d-flex align-items-center justify-content-center text-white ${cfg.bg} ${cfg.color === 'warning' ? 'text-dark' : ''}"
                         style="width:30px;height:30px;flex-shrink:0">
                        <i class="bi bi-${cfg.icono}" style="font-size:0.75rem"></i>
                    </div>
                    ${!esUltimo ? `<div style="width:2px;flex-grow:1;background:#dee2e6;min-height:24px"></div>` : ''}
                </div>
                <div class="flex-grow-1 pb-2 ${!esUltimo ? 'border-bottom' : ''}">
                    <div class="d-flex justify-content-between align-items-start flex-wrap gap-1">
                        <span class="badge ${cfg.bg} ${cfg.color === 'warning' ? 'text-dark' : ''} small">${cfg.label}</span>
                        <small class="text-muted">${fecha}</small>
                    </div>
                    ${ev.comentario ? `<p class="small mb-1 mt-1">${ev.comentario}</p>` : ''}
                    <small class="text-muted"><i class="bi bi-person me-1"></i>${ev.usuario}</small>
                </div>
            </div>`;
        }).join('');
    }

    async function guardarEstado() {
        const nuevoEstado = document.getElementById('selectorNuevoEstado').value;
        const comentario = document.getElementById('campoComentarioServicio').value.trim();

        if (nuevoEstado !== 'OPERATIVO' && !comentario) {
            mostrarAlertaModal('alertaModalServicio', 'El comentario es obligatorio para fallas y mantenimientos.');
            return;
        }

        const resp = await ApiCliente.parche(`/api/infraestructura/${servicioActivo.id}/estado`, {
            estado: nuevoEstado, comentario
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalServicio', err?.mensaje || err?.message || 'No se pudo actualizar el estado.');
            return;
        }

        bootstrap.Modal.getInstance(document.getElementById('modalDetalleServicio'))?.hide();
        mostrarAlerta(`Estado de "${servicioActivo.nombre}" actualizado.`);
        await cargar();
    }

    function abrirCreacion() {
        document.getElementById('formNuevoServicio')?.reset();
        document.getElementById('alertaModalNuevoServicio')?.classList.add('d-none');
        new bootstrap.Modal(document.getElementById('modalNuevoServicio')).show();
    }

    async function guardarNuevoServicio() {
        const nombre = document.getElementById('campoNombreServicio')?.value.trim();
        const descripcionTecnica = document.getElementById('campoDescripcionServicio')?.value.trim() || null;

        if (!nombre) {
            mostrarAlertaModal('alertaModalNuevoServicio', 'El nombre del servicio es obligatorio.');
            return;
        }

        const resp = await ApiCliente.crear('/api/infraestructura', { nombre, descripcionTecnica });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalNuevoServicio', err?.mensaje || err?.message || 'No se pudo crear el servicio.');
            return;
        }
        bootstrap.Modal.getInstance(document.getElementById('modalNuevoServicio'))?.hide();
        mostrarAlerta('Servicio registrado correctamente.');
        await cargar();
    }

    return { cargar, abrirDetalle, guardarEstado, abrirCreacion, guardarNuevoServicio };
})();
