const ModuloLotes = (() => {
    let lotes    = [];
    let empresas = [];
    let modoEdicion = false;
    let idEdicion   = null;

    async function cargar() {
        const [respLotes, respEmpresas] = await Promise.all([
            ApiCliente.obtener('/api/lotes'),
            ApiCliente.obtener('/api/empresas')
        ]);
        if (!respLotes?.ok) { mostrarAlerta('Error al cargar lotes.', 'danger'); return; }
        lotes    = await respLotes.json();
        empresas = respEmpresas?.ok ? await respEmpresas.json() : [];
        poblarSelectorEmpresa();
        poblarFiltroEmpresa();
        renderizarTabla();
    }

    function poblarSelectorEmpresa() {
        const selector = document.getElementById('selectorEmpresaLote');
        if (!selector) return;
        selector.innerHTML = '<option value="">-- Seleccione empresa --</option>' +
            empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    function poblarFiltroEmpresa() {
        const filtro = document.getElementById('filtroEmpresaLote');
        if (!filtro) return;
        const valorActual = filtro.value;
        filtro.innerHTML  = '<option value="">Todas las empresas</option>' +
            empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        filtro.value = valorActual;
    }

    function renderizarTabla(filtroId = '') {
        const cuerpo = document.getElementById('cuerpoTablaLotes');
        if (!cuerpo) return;

        const filtrados = filtroId ? lotes.filter(l => String(l.empresaId) === String(filtroId)) : lotes;

        if (filtrados.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin lotes registrados</td></tr>';
            return;
        }

        const puedeEditar = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'OPERADOR']);
        cuerpo.innerHTML = filtrados.map(l => `
            <tr>
                <td class="text-muted">${l.id}</td>
                <td class="fw-semibold">${l.codigo}</td>
                <td>${l.nombreEmpresa}</td>
                <td>${l.superficieMetrosCuadrados.toLocaleString('es-AR')} m²</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${l.ocupado ? 'bg-danger' : 'bg-success'}">
                        <i class="bi bi-circle-fill me-1" style="font-size:.55rem;"></i>
                        ${l.ocupado ? 'Ocupado' : 'Libre'}
                    </span>
                </td>
                <td class="text-center">
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

    function filtrar(empresaId) { renderizarTabla(empresaId); }

    function abrirCreacion() {
        modoEdicion = false;
        idEdicion   = null;
        document.getElementById('tituloModalLote').textContent = 'Nuevo Lote';
        document.getElementById('formularioLote').reset();
        ocultarAlertaModal('alertaModalLote');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalLote')).show();
    }

    function abrirEdicion(id) {
        const lote = lotes.find(l => l.id === id);
        if (!lote) return;
        modoEdicion = true;
        idEdicion   = id;
        document.getElementById('tituloModalLote').textContent        = 'Editar Lote';
        document.getElementById('campoCodigoLote').value             = lote.codigo;
        document.getElementById('campoSuperficieLote').value         = lote.superficieMetrosCuadrados;
        document.getElementById('campoOcupadoLote').checked          = lote.ocupado;
        document.getElementById('selectorEmpresaLote').value         = lote.empresaId;
        ocultarAlertaModal('alertaModalLote');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalLote')).show();
    }

    async function guardar() {
        const empresaId = parseInt(document.getElementById('selectorEmpresaLote').value);
        const superficie = parseFloat(document.getElementById('campoSuperficieLote').value);

        const datos = {
            codigo:                   document.getElementById('campoCodigoLote').value.trim(),
            superficieMetrosCuadrados: superficie,
            ocupado:                  document.getElementById('campoOcupadoLote').checked,
            empresaId
        };

        if (!datos.codigo || isNaN(superficie) || superficie <= 0 || !empresaId) {
            mostrarAlertaModal('alertaModalLote', 'Complete todos los campos obligatorios correctamente.');
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

    return { cargar, filtrar, abrirCreacion, abrirEdicion, guardar, confirmarEliminacion, eliminar };
})();

