const ModuloLotes = (() => {
    let lotes    = [];
    let empresas = [];
    let modoEdicion = false;
    let idEdicion   = null;

    const ETIQUETA_ESTADO_ASIGNACION = {
        DISPONIBLE:                'Libre',
        PREADJUDICADO_A_SOLICITUD: 'Preadjudicado a solicitud',
        ASIGNADO_A_EMPRESA:        'Asignado a empresa',
        ADJUDICADO_RADICADA:       'Adjudicado - Radicada'
    };

    const ETIQUETA_ZONA = {
        PARQUE_VIEJO: 'Parque Viejo',
        PARQUE_NUEVO: 'Parque Nuevo'
    };

    async function cargar() {
        const [respLotes, respEmpresas] = await Promise.all([
            ApiCliente.obtener('/api/lotes?tamanio=500'),
            ApiCliente.obtener('/api/empresas')
        ]);
        if (!respLotes?.ok) { mostrarAlerta('Error al cargar lotes.', 'danger'); return; }
        const lotesData = await respLotes.json();
        lotes    = lotesData.contenido ?? lotesData;
        empresas = respEmpresas?.ok ? await respEmpresas.json() : [];
        poblarSelectorEmpresa();
        resetearFiltros();
        renderizarTabla();
    }

    function poblarSelectorEmpresa() {
        const selector = document.getElementById('selectorEmpresaLote');
        if (!selector) return;
        selector.innerHTML = '<option value="" selected>Sin asignar</option>' +
            empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    function resetearFiltros() {
        ['filtroZonaLote', 'filtroSuperficieLote', 'filtroEstadoOcupacionLote'].forEach(grupoId => {
            const grupo = document.getElementById(grupoId);
            if (!grupo) return;
            grupo.querySelectorAll('button').forEach((btn, idx) => {
                btn.classList.toggle('active', idx === 0);
            });
        });
    }

    function leerFiltroActivo(grupoId, attr) {
        const grupo = document.getElementById(grupoId);
        if (!grupo) return '';
        const activo = grupo.querySelector('button.active');
        return activo ? (activo.dataset[attr] ?? '') : '';
    }

    function filtrar(boton) {
        if (boton) {
            boton.closest('.btn-group').querySelectorAll('button').forEach(b => b.classList.remove('active'));
            boton.classList.add('active');
        }
        renderizarTabla();
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaLotes');
        if (!cuerpo) return;

        const zona      = leerFiltroActivo('filtroZonaLote', 'zona');
        const sup       = leerFiltroActivo('filtroSuperficieLote', 'sup');
        const ocupacion = leerFiltroActivo('filtroEstadoOcupacionLote', 'ocupacion');

        let filtrados = lotes.filter(l => {
            if (zona && l.zona !== zona) return false;
            if (sup && l.superficieMetrosCuadrados !== parseFloat(sup)) return false;
            if (ocupacion === 'libre'   &&  l.ocupado) return false;
            if (ocupacion === 'ocupado' && !l.ocupado) return false;
            return true;
        });

        const badge = document.getElementById('badgeLotesFiltrados');
        if (badge) {
            const hayFiltro = zona || sup || ocupacion;
            badge.textContent = hayFiltro ? `${filtrados.length} de ${lotes.length}` : `Total: ${lotes.length}`;
        }

        if (filtrados.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin lotes para los filtros seleccionados</td></tr>';
            return;
        }

        const puedeEditar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        cuerpo.innerHTML = filtrados.map(l => `
            <tr>
                <td class="text-muted">${l.id}</td>
                <td class="fw-semibold">${l.codigo}</td>
                <td>${l.zona ? `<span class="badge text-bg-light border">${ETIQUETA_ZONA[l.zona] ?? l.zona}</span>` : '<span class="text-muted">—</span>'}</td>
                <td>${l.empresaId
                    ? `<span class="fw-semibold">${l.nombreEmpresa}</span>`
                    : '<span class="text-muted fst-italic"><i class="bi bi-dash-circle me-1"></i>Sin asignar</span>'
                }</td>
                <td>${l.superficieMetrosCuadrados.toLocaleString('es-AR')} m²</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${l.ocupado ? 'bg-danger' : 'bg-success'}">
                        <i class="bi bi-circle-fill me-1" style="font-size:.55rem;"></i>
                        ${l.ocupado ? 'Ocupado' : 'Libre'}
                    </span>
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-secondary me-1" title="Detalle"
                            onclick="ModuloLotes.abrirDetalle(${l.id})">
                        <i class="bi bi-eye"></i>
                    </button>
                    ${puedeEditar ? `
                    <button class="btn btn-sm btn-outline-primary me-1" title="Editar"
                            onclick="ModuloLotes.abrirEdicion(${l.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" title="Eliminar"
                            onclick="ModuloLotes.confirmarEliminacion(${l.id}, '${l.codigo.replace(/'/g, "\\'")}')">
                        <i class="bi bi-trash"></i>
                    </button>` : '<span class="text-muted small">Sin permisos</span>'}
                </td>
            </tr>`).join('');
    }

    function abrirCreacion() {
        modoEdicion = false;
        idEdicion   = null;
        document.getElementById('tituloModalLote').textContent = 'Nuevo lote';
        document.getElementById('formularioLote').reset();
        ocultarAlertaModal('alertaModalLote');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalLote')).show();
    }

    function abrirEdicion(id) {
        const lote = lotes.find(l => l.id === id);
        if (!lote) return;
        modoEdicion = true;
        idEdicion   = id;
        document.getElementById('tituloModalLote').textContent              = 'Editar lote';
        document.getElementById('campoCodigoLote').value                    = lote.codigo;
        document.getElementById('campoSuperficieLote').value                = lote.superficieMetrosCuadrados;
        document.getElementById('campoOcupadoLote').checked                 = lote.ocupado;
        document.getElementById('selectorEmpresaLote').value                = lote.empresaId ?? '';
        document.getElementById('selectorEstadoAsignacionLote').value       = lote.estadoAsignacion || '';
        document.getElementById('campoExpedienteReferenciaLote').value      = lote.numeroExpedienteReferencia || '';
        document.getElementById('selectorZonaLote').value                   = lote.zona || '';
        ocultarAlertaModal('alertaModalLote');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalLote')).show();
    }

    async function abrirDetalle(id) {
        const modalEl = document.getElementById('modalDetalleLote');
        if (!modalEl) return;

        const estadoCarga = document.getElementById('estadoCargaDetalleLote');
        const error = document.getElementById('errorDetalleLote');
        const contenido = document.getElementById('contenidoDetalleLote');

        if (estadoCarga) estadoCarga.classList.remove('d-none');
        if (error) { error.classList.add('d-none'); error.textContent = ''; }
        if (contenido) contenido.classList.add('d-none');

        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            const respuesta = await ApiCliente.obtener(`/api/lotes/${id}`);
            if (!respuesta?.ok) throw new Error('No se pudo cargar el detalle del lote.');
            const detalle = await respuesta.json();
            renderizarDetalle(detalle);
            if (contenido) contenido.classList.remove('d-none');
        } catch (e) {
            if (error) {
                error.textContent = e?.message || 'No se pudo cargar el detalle del lote.';
                error.classList.remove('d-none');
            }
        } finally {
            if (estadoCarga) estadoCarga.classList.add('d-none');
        }
    }

    function renderizarDetalle(detalle) {
        const estado = detalle?.estadoAsignacion || '';
        const textoEstado = ETIQUETA_ESTADO_ASIGNACION[estado] || 'Sin estado';

        document.getElementById('detLoteCodigo').textContent     = detalle?.codigo || '-';
        document.getElementById('detLoteSuperficie').textContent = `${(detalle?.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²`;
        document.getElementById('detLoteOcupado').textContent    = detalle?.ocupado ? 'Ocupado' : 'Libre';
        document.getElementById('detLoteZona').textContent       = (detalle?.zona && ETIQUETA_ZONA[detalle.zona]) || '-';

        document.getElementById('detLoteEmpresa').innerHTML = detalle?.empresaId
            ? `<span class="fw-semibold">${detalle.nombreEmpresa}</span>`
            : '<span class="text-muted fst-italic"><i class="bi bi-dash-circle me-1"></i>Sin asignar</span>';
        document.getElementById('detLoteEmpresaCuit').textContent      = detalle?.cuitEmpresa || '-';
        document.getElementById('detLoteFechaAsignacion').textContent  = detalle?.fechaAsignacion || '-';
        document.getElementById('detLoteEstadoAsignacion').textContent = textoEstado;
        document.getElementById('detLoteExpediente').textContent       = detalle?.numeroExpedienteReferencia || '-';
    }

    async function guardar() {
        const empresaSeleccionada = document.getElementById('selectorEmpresaLote').value;
        const empresaId = empresaSeleccionada ? parseInt(empresaSeleccionada, 10) : null;
        const superficie = parseFloat(document.getElementById('campoSuperficieLote').value);

        const datos = {
            codigo:                     document.getElementById('campoCodigoLote').value.trim(),
            superficieMetrosCuadrados:  superficie,
            ocupado:                    document.getElementById('campoOcupadoLote').checked,
            empresaId,
            estadoAsignacion:           document.getElementById('selectorEstadoAsignacionLote').value || null,
            numeroExpedienteReferencia: document.getElementById('campoExpedienteReferenciaLote').value.trim() || null,
            zona:                       document.getElementById('selectorZonaLote').value || null
        };

        if (!datos.codigo || isNaN(superficie) || superficie <= 0) {
            mostrarAlertaModal('alertaModalLote', 'Complete codigo y superficie correctamente.');
            return;
        }

        const respuesta = modoEdicion
            ? await ApiCliente.actualizar(`/api/lotes/${idEdicion}`, datos)
            : await ApiCliente.crear('/api/lotes', datos);

        if (respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalLote'))?.hide();
            await cargar();
            mostrarAlerta(modoEdicion ? 'Lote actualizado correctamente.' : 'Lote creado correctamente.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalLote', error?.message || 'Error al guardar el lote.');
        }
    }

    function confirmarEliminacion(id, codigo) {
        document.getElementById('mensajeConfirmacionEliminar').textContent =
            `¿Está seguro que desea eliminar el lote "${codigo}"?`;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarId   = id;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarTipo = 'lote';
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConfirmacion')).show();
    }

    async function eliminar(id) {
        const respuesta = await ApiCliente.eliminar(`/api/lotes/${id}`);
        if (respuesta?.status === 204 || respuesta?.ok) {
            await cargar();
            mostrarAlerta('Lote eliminado correctamente.');
        } else {
            mostrarAlerta('No se pudo eliminar el lote.', 'danger');
        }
    }

    return { cargar, filtrar, abrirCreacion, abrirEdicion, abrirDetalle, guardar, confirmarEliminacion, eliminar };
})();