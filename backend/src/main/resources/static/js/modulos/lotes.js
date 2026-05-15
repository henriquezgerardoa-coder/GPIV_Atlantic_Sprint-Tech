const ModuloLotes = (() => {
    let lotes    = [];
    let empresas = [];
    let modoEdicion = false;
    let idEdicion   = null;

    const ETIQUETA_ESTADO_ASIGNACION = {
        RESERVADO: 'Reservado',
        PREADJUDICADO: 'Preadjudicado',
        ADJUDICADO_RADICADA: 'Adjudicado-radicada'
    };

    const ETIQUETA_ZONA = {
        PARQUE_VIEJO: 'Parque Viejo',
        PARQUE_NUEVO: 'Parque Nuevo'
    };

    function formatearZona(zona) {
        return ETIQUETA_ZONA[zona] || zona || '-';
    }

    async function cargar() {
        const [respLotes, respEmpresas] = await Promise.all([
            ApiCliente.obtener('/api/lotes'),
            ApiCliente.obtener('/api/empresas')
        ]);
        if (!respLotes?.ok) { mostrarAlerta('Error al cargar lotes.', 'danger'); return; }
        lotes    = await respLotes.json();
        empresas = respEmpresas?.ok ? await respEmpresas.json() : [];
        poblarSelectorEmpresa();
        poblarFiltroSuperficie();
        renderizarTabla();
    }

    function poblarSelectorEmpresa() {
        const selector = document.getElementById('selectorEmpresaLote');
        if (!selector) return;
        selector.innerHTML = '<option value="">-- Seleccione empresa --</option>' +
            empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    function poblarFiltroSuperficie() {
        const filtro = document.getElementById('filtroSuperficieLote');
        if (!filtro) return;
        const superficies = [...new Set(lotes.map(l => l.superficieMetrosCuadrados))].sort((a, b) => a - b);
        const valorActual = filtro.value;
        filtro.innerHTML = '<option value="">Todas</option>' +
            superficies.map(s => `<option value="${s}">${s.toLocaleString('es-AR')} m²</option>`).join('');
        filtro.value = valorActual;
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaLotes');
        if (!cuerpo) return;

        const filtroZona       = document.getElementById('filtroZonaLote')?.value || '';
        const filtroSuperficie = document.getElementById('filtroSuperficieLote')?.value || '';
        const filtroEstado     = document.getElementById('filtroEstadoLote')?.value || '';

        let filtrados = lotes;
        if (filtroZona)       filtrados = filtrados.filter(l => l.zona === filtroZona);
        if (filtroSuperficie) filtrados = filtrados.filter(l => l.superficieMetrosCuadrados === parseFloat(filtroSuperficie));
        if (filtroEstado)     filtrados = filtrados.filter(l => filtroEstado === 'ocupado' ? l.ocupado : !l.ocupado);

        const badge = document.getElementById('badgeTotalLotesFiltrados');
        if (badge) badge.textContent = `${filtrados.length} lotes`;

        if (filtrados.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin lotes para los filtros seleccionados</td></tr>';
            return;
        }

        const puedeEditar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
        cuerpo.innerHTML = filtrados.map(l => `
            <tr>
                <td class="text-muted">${l.id}</td>
                <td class="fw-semibold">${l.codigo}</td>
                <td><span class="badge text-bg-light border">${formatearZona(l.zona)}</span></td>
                <td>${l.nombreEmpresa || 'Sin asignar'}</td>
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
                    </button>` : ''}
                </td>
            </tr>`).join('');
    }

    function filtrar() { renderizarTabla(); }

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
        document.getElementById('tituloModalLote').textContent        = 'Editar lote';
        document.getElementById('campoCodigoLote').value             = lote.codigo;
        document.getElementById('campoSuperficieLote').value         = lote.superficieMetrosCuadrados;
        document.getElementById('campoOcupadoLote').checked          = lote.ocupado;
        document.getElementById('selectorEmpresaLote').value         = lote.empresaId ?? '';
        document.getElementById('selectorEstadoAsignacionLote').value = lote.estadoAsignacion || '';
        document.getElementById('campoExpedienteReferenciaLote').value = lote.numeroExpedienteReferencia || '';
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
        if (error) {
            error.classList.add('d-none');
            error.textContent = '';
        }
        if (contenido) contenido.classList.add('d-none');

        bootstrap.Modal.getOrCreateInstance(modalEl).show();

        try {
            const respuesta = await ApiCliente.obtener(`/api/lotes/${id}`);
            if (!respuesta?.ok) {
                throw new Error('No se pudo cargar el detalle del lote.');
            }
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

        document.getElementById('detLoteCodigo').textContent = detalle?.codigo || '-';
        document.getElementById('detLoteZona').textContent = formatearZona(detalle?.zona);
        document.getElementById('detLoteSuperficie').textContent = `${(detalle?.superficieMetrosCuadrados || 0).toLocaleString('es-AR')} m²`;
        document.getElementById('detLoteOcupado').textContent = detalle?.ocupado ? 'Ocupado' : 'Libre';

        document.getElementById('detLoteEmpresa').textContent = detalle?.nombreEmpresa || 'Sin asignar';
        document.getElementById('detLoteEmpresaCuit').textContent = detalle?.cuitEmpresa || '-';
        document.getElementById('detLoteFechaAsignacion').textContent = detalle?.fechaAsignacion || '-';
        document.getElementById('detLoteEstadoAsignacion').textContent = textoEstado;
        document.getElementById('detLoteExpediente').textContent = detalle?.numeroExpedienteReferencia || '-';
    }

    async function guardar() {
        const empresaSeleccionada = document.getElementById('selectorEmpresaLote').value;
        const empresaId = empresaSeleccionada ? parseInt(empresaSeleccionada, 10) : null;
        const superficie = parseFloat(document.getElementById('campoSuperficieLote').value);

        const datos = {
            codigo:                   document.getElementById('campoCodigoLote').value.trim(),
            superficieMetrosCuadrados: superficie,
            ocupado:                  document.getElementById('campoOcupadoLote').checked,
            empresaId,
            estadoAsignacion:         document.getElementById('selectorEstadoAsignacionLote').value || null,
            numeroExpedienteReferencia: document.getElementById('campoExpedienteReferenciaLote').value.trim() || null
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

