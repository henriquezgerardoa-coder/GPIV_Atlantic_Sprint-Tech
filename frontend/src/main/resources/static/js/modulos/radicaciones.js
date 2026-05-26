const ModuloRadicaciones = (() => {
    let radicaciones = [];
    let rubrosRadicacion = [];
    let historialRadicacionSeleccionadaId = null;
    let historialEventos = [];
    let radicacionDetalleAdmin = null;
    let lotesDisponibles = [];
    let filtroEstadoHistorial = '';
    let soloUltimos10Historial = false;
    let estadosSeleccionadosListado = new Set();
    let formularioInicializado = false;
    let temporizadorBorrador = null;
    const CLAVE_BORRADOR_RADICACION = 'gpiv.radicaciones.borrador.v1';
    const CLAVE_FILTROS_HISTORIAL_RADICACION = 'gpiv.radicaciones.filtros.historial.v1';
    const CLAVE_INDICADOR_RESTAURACION_HISTORIAL = 'gpiv.radicaciones.indicador.restauracion.v1';
    const CLAVE_FECHA_RESTAURACION_HISTORIAL = 'gpiv.radicaciones.indicador.restauracion.fecha.v1';
    const ESTADOS_FILTRO_HISTORIAL_VALIDOS = new Set([
        'PENDIENTE',
        'EN_REVISION',
        'APROBADA',
        'RADICADA',
        'RECHAZADA',
        'REQUIERE_INFORMACION_ADICIONAL',
        'CANCELADA'
    ]);
    const CAMPOS_FORMULARIO_RADICACION = [
        'campoTipoSolicitudRad', 'campoDescripcionRad', 'campoUsoEstimativoRad', 'activarRelevamientoPedidoLotes',
        'relCorreo', 'relRazonSocial', 'relCuit', 'relIngresosBrutos', 'relActividadPrincipal', 'relActividadSecundaria',
        'relTipoEmpresa', 'relObjetoProyecto', 'relDireccion', 'relPersonaReferente', 'relTelefono', 'relCorreoElectronico',
        'relRubro', 'relRubroOtro', 'relDescripcionServicio', 'relEmplazamiento', 'relPersonalJerarquico', 'relPersonalProduccion',
        'relPersonalAdministrativo', 'relTiempoRadicacion', 'relNecesidadM2', 'relSupTrabajo', 'relSupDeposito',
        'relSupExpansion', 'relSupEstacionamiento', 'relTienePlanos', 'relPersonalAOcupar', 'relMateriasPrimas',
        'relDestinoProduccion', 'relTension', 'relPotenciaKw', 'relAguaLtsMes', 'relRequiereGas', 'relTipoResiduos',
        'relTratamientoPlanta', 'relBalanzaPublica', 'relComedorUnitario', 'relSalonCoworking',
        'adjRadTipoDocumento', 'adjRadDescripcion'
    ];

    async function cargarRubrosParaFormulario() {
        const resp = await ApiCliente.obtener('/api/catalogos/rubros');
        if (!resp?.ok) return;
        rubrosRadicacion = await resp.json();
        const selector = document.getElementById('relRubro');
        if (!selector) return;
        const valorActual = selector.value;
        selector.innerHTML = '<option value="">Seleccionar rubro...</option>'
            + rubrosRadicacion.map(r =>
                `<option value="${r.nombre}"${r.requierePermisoEspecial ? ' data-especial="true"' : ''}>${r.nombre}${r.requierePermisoEspecial ? ' ⚠ (requiere permiso especial)' : ''}</option>`
            ).join('');
        if (valorActual) selector.value = valorActual;
        toggleRubroOtros();
    }

    function toggleRubroOtros() {
        const selector = document.getElementById('relRubro');
        const bloque = document.getElementById('bloqueRelRubroOtro');
        if (!selector || !bloque) return;
        const esOtros = selector.value.toLowerCase() === 'otros';
        bloque.classList.toggle('d-none', !esOtros);
        if (!esOtros) {
            const campo = document.getElementById('relRubroOtro');
            if (campo) campo.value = '';
        }
    }

    async function cargar() {
        inicializarFormularioRadicacion();
        await cargarRubrosParaFormulario();
        ajustarVistaRadicacionesPorRol();
        const params = new URLSearchParams();
        const estado = document.getElementById('filtroEstadoRad')?.value;
        const desde = document.getElementById('filtroDesdeRad')?.value;
        const hasta = document.getElementById('filtroHastaRad')?.value;
        if (estado) params.set('estado', estado);
        if (desde) params.set('desde', desde);
        if (hasta) params.set('hasta', hasta);

        params.set('tamanio', '200');
        const ruta = '/api/radicaciones?' + params.toString();
        const respuesta = await ApiCliente.obtener(ruta);
        if (!respuesta?.ok) {
            mostrarAlerta('No se pudieron cargar las radicaciones.', 'danger');
            return;
        }

        const datos = await respuesta.json();
        const listado = datos.contenido ?? datos;
        actualizarContadorRadicaciones(datos.totalElementos ?? listado.length);
        radicaciones = aplicarFiltrosListado(listado, estado);
        renderizarTabla();
        poblarSelectorRadicaciones();
        if (historialRadicacionSeleccionadaId && radicaciones.some(r => r.id === historialRadicacionSeleccionadaId)) {
            await verHistorial(historialRadicacionSeleccionadaId, null, true, true);
        } else {
            ocultarBloqueHistorialExpediente();
        }
    }

    function actualizarContadorRadicaciones(total) {
        const badge = document.getElementById('badgeTotalRadicaciones');
        if (!badge) return;
        badge.textContent = `${total} expediente${total !== 1 ? 's' : ''}`;
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaRadicaciones');
        if (!cuerpo) return;

        if (radicaciones.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="7" class="text-center py-3 text-muted">Sin registros</td></tr>';
            return;
        }

        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']) && !Autenticacion.tieneAcceso(['EMPRESA']);
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        cuerpo.innerHTML = radicaciones.map(r => `
            <tr role="button" title="Seleccionar expediente" onclick="ModuloRadicaciones.seleccionarExpediente(${r.id}, '${r.numeroRadicado}')">
                <td class="ps-3 fw-semibold">${r.numeroRadicado}</td>
                <td>${r.tipoSolicitud}</td>
                <td>${r.usoEstimativo || '-'}</td>
                <td><span class="badge bg-secondary">${formatearEstado(r.estado)}</span></td>
                <td>${r.fechaRadicacion || '-'}</td>
                <td>${(r.fechaUltimaActualizacion || '').replace('T', ' ').slice(0, 16)}</td>
                <td class="text-center">${(esGestor || esEmpresa) ? `<button class="btn btn-sm btn-outline-dark" onclick="event.stopPropagation(); ModuloRadicaciones.verDetalleAdmin(${r.id})">Detalle</button>` : '<span class="text-muted small">-</span>'}</td>
            </tr>
        `).join('');
    }


    function seleccionarExpediente(id, numeroRadicado) {
        mostrarBloqueHistorialExpediente();
        verHistorial(id, numeroRadicado, true, false);
    }

    function mostrarBloqueHistorialExpediente() {
        document.getElementById('bloqueHistorialExpedienteRad')?.classList.remove('d-none');
    }

    function ocultarBloqueHistorialExpediente() {
        document.getElementById('bloqueHistorialExpedienteRad')?.classList.add('d-none');
    }

    const TRANSICIONES_VALIDAS_RAD = {
        PENDIENTE:                       ['EN_REVISION', 'RECHAZADA', 'CANCELADA'],
        EN_REVISION:                     ['APROBADA', 'RECHAZADA', 'REQUIERE_INFORMACION_ADICIONAL', 'CANCELADA'],
        REQUIERE_INFORMACION_ADICIONAL:  ['EN_REVISION', 'RECHAZADA', 'CANCELADA'],
        APROBADA:                        ['RADICADA'],
        RADICADA:                        [],
        RECHAZADA:                       [],
        CANCELADA:                       []
    };
    const NOMBRES_ESTADO_RAD = {
        EN_REVISION:                    'En revisión',
        APROBADA:                       'Aprobada',
        RADICADA:                       'Radicada',
        RECHAZADA:                      'Rechazada',
        REQUIERE_INFORMACION_ADICIONAL: 'Requiere información adicional',
        CANCELADA:                      'Cancelada'
    };

    function opcionesEstadoGestion(estadoActual) {
        const siguientes = TRANSICIONES_VALIDAS_RAD[estadoActual] || [];
        if (siguientes.length === 0) {
            return '<option value="" disabled>Sin cambios de estado posibles</option>';
        }
        return siguientes
            .map(v => `<option value="${v}">${NOMBRES_ESTADO_RAD[v] || v}</option>`)
            .join('');
    }

    function poblarSelectorRadicaciones() {
        const selector = document.getElementById('selectorRadicacionDocumento');
        if (!selector) return;
        selector.innerHTML = radicaciones.map(r => `<option value="${r.id}">${r.numeroRadicado}</option>`).join('');
    }

    async function verHistorial(id, numeroRadicado = null, silencioso = false, restauradoAutomaticamente = false) {
        const cambioExpediente = historialRadicacionSeleccionadaId !== id;
        historialRadicacionSeleccionadaId = id;
        guardarFiltrosHistorialSesion();
        if (!restauradoAutomaticamente) {
            ocultarIndicadorRestauracionHistorial();
        }
        if (cambioExpediente && !silencioso) {
            resetearFiltrosHistorial();
        }
        renderizarCargandoHistorial();

        const respuesta = await ApiCliente.obtener(`/api/radicaciones/${id}/historial`);
        if (!respuesta?.ok) {
            renderizarErrorHistorial();
            if (!silencioso) {
                mostrarAlerta('No se pudo cargar el historial del expediente.', 'danger');
            }
            return;
        }

        historialEventos = await respuesta.json();
        aplicarFiltrosHistorialYRenderizar();

        const numeroRadicadoResuelto = numeroRadicado || obtenerNumeroRadicadoPorId(id);
        const titulo = document.getElementById('tituloHistorialRadDetalle');
        if (titulo) {
            titulo.textContent = numeroRadicadoResuelto ? `Expediente ${numeroRadicadoResuelto}` : `Expediente #${id}`;
        }
        if (restauradoAutomaticamente) {
            mostrarIndicadorRestauracionHistorial(id, numeroRadicadoResuelto);
        }
    }

    function obtenerNumeroRadicadoPorId(id) {
        return radicaciones.find(r => r.id === id)?.numeroRadicado || null;
    }

    function renderizarCargandoHistorial() {
        const cuerpo = document.getElementById('cuerpoTablaHistorialRadicacion');
        if (!cuerpo) return;
        cuerpo.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Cargando historial...</td></tr>';
    }

    function renderizarErrorHistorial() {
        const cuerpo = document.getElementById('cuerpoTablaHistorialRadicacion');
        if (!cuerpo) return;
        cuerpo.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-danger">Error al cargar historial</td></tr>';
    }

    function renderizarTablaHistorial(historial) {
        const cuerpo = document.getElementById('cuerpoTablaHistorialRadicacion');
        if (!cuerpo) return;

        if (!historial || historial.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Sin eventos registrados</td></tr>';
            return;
        }

        cuerpo.innerHTML = historial.map(e => `
            <tr>
                <td class="ps-3">${formatearFechaEvento(e.fechaEvento)}</td>
                <td>${e.estadoAnterior ? formatearEstado(e.estadoAnterior) : '<span class="text-muted">-</span>'}</td>
                <td>${formatearEstado(e.estado)}</td>
                <td>${e.comentario || '-'}</td>
                <td>${e.usuario || '-'}</td>
            </tr>
        `).join('');
    }

    function aplicarFiltrosHistorialYRenderizar() {
        let eventos = [...historialEventos];
        if (filtroEstadoHistorial) {
            eventos = eventos.filter(e => e.estado === filtroEstadoHistorial);
        }
        if (soloUltimos10Historial) {
            eventos = eventos.slice(0, 10);
        }
        renderizarTablaHistorial(eventos);
        actualizarTextoTituloHistorial();
    }

    function resetearFiltrosHistorial() {
        filtroEstadoHistorial = '';
        soloUltimos10Historial = false;
        const selector = document.getElementById('filtroEstadoHistorialRad');
        if (selector) selector.value = '';
        const boton = document.getElementById('btnUltimos10HistorialRad');
        if (boton) boton.textContent = 'Últimos 10';
        guardarFiltrosHistorialSesion();
    }

    function actualizarTextoTituloHistorial() {
        const subtitulo = document.getElementById('subtituloFiltrosHistorialRad');
        if (!subtitulo) return;
        const filtros = [];
        if (filtroEstadoHistorial) {
            filtros.push(`Estado: ${formatearEstado(filtroEstadoHistorial)}`);
        }
        if (soloUltimos10Historial) {
            filtros.push('Mostrando últimos 10 eventos');
        }
        subtitulo.textContent = filtros.length > 0 ? filtros.join(' | ') : 'Sin filtros';
    }

    function formatearFechaEvento(fechaEvento) {
        if (!fechaEvento) return '-';
        return String(fechaEvento).replace('T', ' ').slice(0, 16);
    }

    function ajustarVistaRadicacionesPorRol() {
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']) && !esEmpresa;
        const navTabs = document.getElementById('tabsRadicaciones');

        if (esGestor) {
            if (navTabs) navTabs.classList.remove('d-none');
            document.getElementById('tab-rad-a')?.closest('.nav-item')?.classList.add('d-none');
            document.getElementById('btnNuevaSolicitudRad')?.classList.add('d-none');
            document.getElementById('btnNuevaSolicitudRadListado')?.classList.add('d-none');
            const puedeAdjuntar = Autenticacion.tieneAcceso(['EMPRESA', 'SECRETARIO']);
            document.getElementById('tab-rad-d')?.closest('.nav-item')?.classList.toggle('d-none', !puedeAdjuntar);
            const tabB = document.getElementById('tab-rad-b');
            if (tabB && window.bootstrap?.Tab) window.bootstrap.Tab.getOrCreateInstance(tabB).show();
            return;
        }

        // EMPRESA: ocultar toda la barra de tabs, mostrar panel-b directamente.
        if (navTabs) navTabs.classList.add('d-none');
        document.getElementById('btnNuevaSolicitudRad')?.classList.remove('d-none');
        document.getElementById('btnNuevaSolicitudRadPanelA')?.classList.add('d-none');
        document.getElementById('btnNuevaSolicitudRadListado')?.classList.add('d-none');
        const panelB = document.getElementById('panel-rad-b');
        if (panelB) {
            panelB.classList.add('show', 'active');
            panelB.classList.remove('fade');
        }
    }

    function abrirModalNuevaSolicitud() {
        bootstrap.Modal.getOrCreateInstance(
            document.getElementById('modalNuevaSolicitudRadicacion')
        ).show();
    }

    function toggleAdjuntoRadicacion() {
        const activo = document.getElementById('activarAdjuntoRadicacion')?.checked;
        document.getElementById('bloqueAdjuntoRadicacion')?.classList.toggle('d-none', !activo);
        if (!activo) {
            document.getElementById('adjRadArchivo').value = '';
            document.getElementById('adjRadDescripcion').value = '';
        }
    }

    async function crear() {
        ocultarErrorRadicacion();
        const tipoSolicitud = document.getElementById('campoTipoSolicitudRad').value.trim();
        const descripcion = document.getElementById('campoDescripcionRad').value.trim();
        const usoEstimativo = document.getElementById('campoUsoEstimativoRad').value.trim();
        const usarRelevamiento = document.getElementById('activarRelevamientoPedidoLotes')?.checked;
        if (!tipoSolicitud || !descripcion) {
            mostrarErrorRadicacion('Complete tipo y descripción.');
            return;
        }

        const relevamientoPedidoLotes = usarRelevamiento ? construirRelevamientoPedidoLotes() : null;
        if (usarRelevamiento && !relevamientoPedidoLotes) {
            return;
        }

        const respuesta = await ApiCliente.crear('/api/radicaciones', {
            tipoSolicitud,
            descripcion,
            usoEstimativo,
            relevamientoPedidoLotes
        });
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarErrorRadicacion(error?.mensaje || 'No se pudo crear la radicación.');
            return;
        }

        const radicacionCreada = await respuesta.json().catch(() => null);

        const adjuntoActivo = document.getElementById('activarAdjuntoRadicacion')?.checked;
        const archivoAdjunto = document.getElementById('adjRadArchivo')?.files[0];
        if (adjuntoActivo && archivoAdjunto && radicacionCreada?.id) {
            const formData = new FormData();
            formData.append('tipoDocumento', document.getElementById('adjRadTipoDocumento').value);
            formData.append('descripcion', document.getElementById('adjRadDescripcion').value.trim());
            formData.append('archivo', archivoAdjunto);
            await ApiCliente.subirArchivo(`/api/radicaciones/${radicacionCreada.id}/documentos`, formData);
        }

        document.getElementById('formRadicacionNueva').reset();
        document.getElementById('activarAdjuntoRadicacion').checked = false;
        document.getElementById('bloqueAdjuntoRadicacion')?.classList.add('d-none');
        actualizarVisibilidadRelevamiento();
        limpiarBorradorPersistido();
        bootstrap.Modal.getInstance(document.getElementById('modalNuevaSolicitudRadicacion'))?.hide();
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

    async function subirDocumentoDesdeModal() {
        if (!radicacionDetalleAdmin?.id) return;
        const alerta = document.getElementById('alertaDocRadModal');
        const tipoDocumento = document.getElementById('tipoDocRadModal').value;
        const descripcion = document.getElementById('descripcionDocRadModal').value.trim();
        const archivo = document.getElementById('archivoDocRadModal').files[0];

        alerta?.classList.add('d-none');
        if (!archivo) {
            alerta.textContent = 'Seleccione un archivo.';
            alerta?.classList.remove('d-none');
            return;
        }

        const formData = new FormData();
        formData.append('tipoDocumento', tipoDocumento);
        formData.append('descripcion', descripcion);
        formData.append('archivo', archivo);

        const respuesta = await ApiCliente.subirArchivo(`/api/radicaciones/${radicacionDetalleAdmin.id}/documentos`, formData);
        if (!respuesta?.ok) {
            const error = await respuesta?.json().catch(() => ({}));
            alerta.textContent = error?.mensaje || 'No se pudo subir el documento.';
            alerta?.classList.remove('d-none');
            return;
        }

        document.getElementById('descripcionDocRadModal').value = '';
        document.getElementById('archivoDocRadModal').value = '';

        // Recargar la lista de documentos en el modal
        const respDocs = await ApiCliente.obtener(`/api/radicaciones/${radicacionDetalleAdmin.id}/documentos`);
        const documentos = respDocs?.ok ? await respDocs.json() : [];
        const cuerpoDocs = document.getElementById('cuerpoDocumentosDetalleRadAdmin');
        if (cuerpoDocs) {
            cuerpoDocs.innerHTML = documentos?.length
                ? documentos.map(d => `
                    <tr>
                        <td>${d.tipoDocumento || '-'}</td>
                        <td>${d.nombreArchivo || '-'}</td>
                        <td>${formatearFechaEvento(d.fechaSubida)}</td>
                        <td class="text-end">
                            <a href="/api/radicaciones/${radicacionDetalleAdmin.id}/documentos/${d.id}" target="_blank" class="btn btn-sm btn-link text-decoration-none p-0">
                                <i class="bi bi-file-earmark-pdf"></i> Ver
                            </a>
                        </td>
                    </tr>`).join('')
                : '<tr><td colspan="4" class="text-muted">Sin documentos</td></tr>';
        }
        mostrarAlerta('Documento cargado correctamente.');
    }

    async function cambiarEstado(id) {
        const selector = document.getElementById(`estadoRad-${id}`);
        const estado = selector?.value;
        if (!estado) {
            mostrarAlerta('Seleccione un estado.', 'danger');
            return;
        }

        let comentario;
        if (estado === 'RECHAZADA') {
            const motivo = window.prompt('Ingrese el motivo de rechazo (obligatorio):');
            if (motivo === null) {
                return;
            }
            if (!motivo.trim()) {
                mostrarAlerta('El motivo de rechazo es obligatorio para rechazar una solicitud.', 'danger');
                return;
            }
            comentario = motivo.trim();
        } else {
            const confirmado = window.confirm(`Confirma el cambio de estado a "${formatearEstado(estado)}"?`);
            if (!confirmado) {
                return;
            }
            comentario = `Cambio de estado a ${formatearEstado(estado)} desde el panel de administración`;
        }

        const respuesta = await ApiCliente.parche(`/api/radicaciones/${id}/estado`, { estado, comentario });
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(err?.mensaje || err?.message || 'No se pudo actualizar el estado.', 'danger');
            return;
        }
        mostrarAlerta('Estado actualizado correctamente.');
        await cargar();
    }

    async function verDetalleAdmin(id) {
        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalDetalleRadicacionAdmin'));
        const estadoCarga = document.getElementById('estadoCargaDetalleRadAdmin');
        const error = document.getElementById('errorDetalleRadAdmin');
        const contenido = document.getElementById('contenidoDetalleRadAdmin');
        radicacionDetalleAdmin = null;
        estadoCarga?.classList.remove('d-none');
        error?.classList.add('d-none');
        contenido?.classList.add('d-none');
        modal.show();

        try {
            const [respDetalle, respDocs, respHist, respLoteAsignado, respLotesDisp, respRelevamiento, respRubrica] = await Promise.all([
                ApiCliente.obtener(`/api/radicaciones/${id}`),
                ApiCliente.obtener(`/api/radicaciones/${id}/documentos`),
                ApiCliente.obtener(`/api/radicaciones/${id}/historial`),
                ApiCliente.obtener(`/api/radicaciones/${id}/lote`),
                ApiCliente.obtener('/api/lotes'),
                ApiCliente.obtener(`/api/radicaciones/${id}/relevamiento`),
                ApiCliente.obtener(`/api/radicaciones/${id}/rubrica`)
            ]);
            estadoCarga?.classList.add('d-none');
            if (!respDetalle?.ok) {
                const err = await respDetalle?.json().catch(() => ({}));
                error.textContent = err?.mensaje || err?.message || 'No se pudo cargar el detalle del expediente.';
                error?.classList.remove('d-none');
                return;
            }

            const detalle = await respDetalle.json();
            const documentos = respDocs?.ok ? await respDocs.json() : [];
            const historial = respHist?.ok ? await respHist.json() : [];
            const loteAsignado = (respLoteAsignado?.ok && respLoteAsignado.status !== 204)
                ? await respLoteAsignado.json() : null;
            const respLotesJson = respLotesDisp?.ok ? await respLotesDisp.json() : [];
            const todosLosLotes = Array.isArray(respLotesJson) ? respLotesJson : (respLotesJson.contenido || []);
            lotesDisponibles = todosLosLotes.filter(l => !l.estadoAsignacion);
            const relevamiento = (respRelevamiento?.ok && respRelevamiento.status !== 204)
                ? await respRelevamiento.json().catch(() => null) : null;
            const tieneActa = respRubrica?.ok === true;
            radicacionDetalleAdmin = detalle;
            renderizarDetalleAdmin(detalle, documentos, historial, loteAsignado, relevamiento, tieneActa);
            contenido?.classList.remove('d-none');
        } catch (e) {
            estadoCarga?.classList.add('d-none');
            console.error('Error al cargar detalle de radicación:', e);
            error.textContent = 'Error al cargar el detalle del expediente.';
            error?.classList.remove('d-none');
        }
    }

    function renderizarDetalleAdmin(detalle, documentos, historial, loteAsignado, relevamiento = null, tieneActa = false) {
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        const esGestor = !esEmpresa && Autenticacion.tieneAcceso(['SECRETARIO']);

        setTexto('detRadNumero', detalle?.numeroRadicado || '-');
        setTexto('detRadSolicitante', detalle?.nombreEmpresa || '-');
        setTexto('detRadTipo', detalle?.tipoSolicitud || '-');
        setTexto('detRadEstado', formatearEstado(detalle?.estado));
        setTexto('detRadFechaCreacion', detalle?.fechaRadicacion || '-');
        setTexto('detRadFechaModificacion', formatearFechaEvento(detalle?.fechaUltimaActualizacion));
        setTexto('detRadUsoEstimativo', detalle?.usoEstimativo || '-');
        setTexto('detRadRelevamiento', detalle?.tieneRelevamientoPedidoLotes ? 'Sí' : 'No');
        setTexto('detRadDescripcion', detalle?.descripcion || '-');

        const wrapAprobacion = document.getElementById('wrapperDetRadFechaAprobacion');
        if (wrapAprobacion) {
            wrapAprobacion.classList.toggle('d-none', !detalle?.fechaAprobacion);
            setTexto('detRadFechaAprobacion', detalle?.fechaAprobacion || '-');
        }
        setTexto('detRadTiempoSolicitado', detalle?.tiempoSolicitadoMeses ? `${detalle.tiempoSolicitadoMeses} meses` : '-');
        const wrapTiempoObra = document.getElementById('wrapperDetRadTiempoObra');
        if (wrapTiempoObra) {
            wrapTiempoObra.classList.toggle('d-none', !detalle?.tiempoEstimadoObraMeses);
            setTexto('detRadTiempoObra', detalle?.tiempoEstimadoObraMeses ? `${detalle.tiempoEstimadoObraMeses} meses` : '-');
        }
        const wrapPlazo = document.getElementById('wrapperDetRadFechaPlazo');
        if (wrapPlazo) {
            wrapPlazo.classList.toggle('d-none', !detalle?.fechaPlazo);
            setTexto('detRadFechaPlazo', detalle?.fechaPlazo || '-');
        }

        const tieneTransiciones = (TRANSICIONES_VALIDAS_RAD[detalle?.estado] || []).length > 0;
        document.getElementById('bloqueCambiarEstadoRadAdmin')?.classList.toggle('d-none', !esGestor || !tieneTransiciones);
        document.getElementById('bloqueObservacionRadAdmin')?.classList.toggle('d-none', !esGestor);
        document.getElementById('bloqueAdjuntarDocRadModal')?.classList.toggle('d-none', !esEmpresa);

        const obs = document.getElementById('campoObservacionesRadAdmin');
        if (obs) obs.value = '';
        // Limpiar campos de plazo y documento de estado antes de toggle para no interferir con su visibilidad
        const campoFecha = document.getElementById('campoPlazoFecha');
        if (campoFecha) campoFecha.value = '';
        const campoMeses = document.getElementById('campoPlazoMeses');
        if (campoMeses) campoMeses.value = '';
        const display = document.getElementById('campoPlazoCalculadoDisplay');
        if (display) display.textContent = '-';
        const archivoEstado = document.getElementById('archivoDocumentoEstadoRad');
        if (archivoEstado) archivoEstado.value = '';

        const selector = document.getElementById('selectorNuevoEstadoRadAdmin');
        if (selector) {
            selector.innerHTML = opcionesEstadoGestion(detalle?.estado);
            selector.onchange = () => {
                actualizarRequisitoObservacion(selector.value);
                toggleCamposPlazo(selector.value);
            };
            actualizarRequisitoObservacion(selector.value);
            toggleCamposPlazo(selector.value);
        }

        // Sección de lote
        renderizarSeccionLote(loteAsignado, detalle?.superficieSolicitadaM2);

        // Acta de rúbrica
        renderizarSeccionActaRubrica(detalle?.id, tieneActa, detalle?.estado);

        // Relevamiento
        renderizarRelevamiento(relevamiento, detalle?.tieneRelevamientoPedidoLotes);

        const cuerpoDocs = document.getElementById('cuerpoDocumentosDetalleRadAdmin');
        if (cuerpoDocs) {
            cuerpoDocs.innerHTML = documentos?.length
                ? documentos.map(d => `
                    <tr>
                        <td>${d.tipoDocumento || '-'}</td>
                        <td>${d.nombreArchivo || '-'}</td>
                        <td>${formatearFechaEvento(d.fechaSubida)}</td>
                        <td class="text-end">
                            <a href="/api/radicaciones/${detalle.id}/documentos/${d.id}" target="_blank" class="btn btn-sm btn-link text-decoration-none p-0">
                                <i class="bi bi-file-earmark-pdf"></i> Ver
                            </a>
                        </td>
                    </tr>`).join('')
                : '<tr><td colspan="4" class="text-muted">Sin documentos</td></tr>';
        }

        const cuerpoHist = document.getElementById('cuerpoHistorialDetalleRadAdmin');
        if (cuerpoHist) {
            cuerpoHist.innerHTML = historial?.length
                ? historial.map(h => `
                    <tr>
                        <td>${formatearFechaEvento(h.fechaEvento)}</td>
                        <td>${h.estadoAnterior ? formatearEstado(h.estadoAnterior) : '<span class="text-muted">-</span>'}</td>
                        <td>${formatearEstado(h.estado)}</td>
                        <td>${h.comentario || '-'}</td>
                        <td>${h.usuario || '-'}</td>
                    </tr>`).join('')
                : '<tr><td colspan="5" class="text-muted">Sin historial</td></tr>';
        }
    }

    function renderizarRelevamiento(rel, tieneRelevamiento) {
        const bloque = document.getElementById('bloqueRelevamientoDetalleRad');
        const contenido = document.getElementById('contenidoRelevamientoDetalleRad');
        if (!bloque || !contenido) return;
        if (!tieneRelevamiento) { bloque.classList.add('d-none'); return; }
        bloque.classList.remove('d-none');
        if (!rel) {
            contenido.innerHTML = '<div class="col-12 text-muted small">No se pudieron cargar los datos del relevamiento.</div>';
            return;
        }

        const campo = (etiqueta, valor) => valor != null && valor !== ''
            ? `<div class="col-md-4"><span class="text-muted">${etiqueta}:</span> <span class="fw-semibold">${valor}</span></div>`
            : '';
        const bool = v => v === true ? 'Sí' : v === false ? 'No' : '-';

        contenido.innerHTML = [
            campo('Razón social', rel.razonSocialEmpresa),
            campo('CUIT', rel.cuit),
            campo('Correo', rel.correo),
            campo('Ingresos brutos', rel.ingresosBrutos),
            campo('Actividad principal', rel.actividadPrincipal),
            campo('Actividad secundaria', rel.actividadSecundaria),
            campo('Tipo empresa', rel.tipoEmpresa),
            campo('Objeto del proyecto', rel.objetoProyecto),
            campo('Dirección', rel.direccion),
            campo('Referente', rel.personaReferente),
            campo('Teléfono', rel.telefono),
            campo('Correo contacto', rel.correoElectronico),
            campo('Rubro', rel.rubro),
            campo('Detalle rubro', rel.rubroOtro),
            campo('Descripción servicio/bien', rel.descripcionServicioBienOfrecido),
            campo('Emplazamiento', rel.emplazamientoActual),
            campo('Personal jerárquico', rel.personalJerarquico),
            campo('Personal producción', rel.personalProduccion),
            campo('Personal administrativo', rel.personalAdministrativo),
            campo('Tiempo radicación', rel.tiempoRadicacionMeses != null ? `${rel.tiempoRadicacionMeses} meses` : null),
            campo('Necesidad m²', rel.necesidadMetrosCuadrados != null ? `${rel.necesidadMetrosCuadrados} m²` : null),
            campo('Personal a ocupar', rel.personalAOcupar),
            campo('Sup. trabajo', rel.superficieCubiertaTrabajo != null ? `${rel.superficieCubiertaTrabajo} m²` : null),
            campo('Sup. depósito', rel.superficieCubiertaDeposito != null ? `${rel.superficieCubiertaDeposito} m²` : null),
            campo('Sup. expansión', rel.superficieFuturaExpansion != null ? `${rel.superficieFuturaExpansion} m²` : null),
            campo('Sup. estacionamiento', rel.superficieEstacionamiento != null ? `${rel.superficieEstacionamiento} m²` : null),
            campo('Tiene planos', bool(rel.tienePlanos)),
            campo('Tensión', rel.tensionAlimentacion),
            campo('Potencia (kW)', rel.potenciaInstaladaKw),
            campo('Agua (lts/mes)', rel.aguaLtsMes),
            campo('Requiere gas', bool(rel.requiereGas)),
            campo('Tratamiento en planta', bool(rel.tratamientoEnPlanta)),
            campo('Balanza pública', bool(rel.necesitaBalanzaPublica)),
            campo('Comedor unitario', bool(rel.necesitaComedorUnitario)),
            campo('SUM/coworking', bool(rel.necesitaSalonCoworking)),
            rel.materiasPrimas ? `<div class="col-12"><span class="text-muted">Materias primas:</span> <span class="fw-semibold">${rel.materiasPrimas}</span></div>` : '',
            rel.destinoProduccion ? `<div class="col-12"><span class="text-muted">Destino producción:</span> <span class="fw-semibold">${rel.destinoProduccion}</span></div>` : '',
            rel.tipoResiduosEfluentes ? `<div class="col-12"><span class="text-muted">Residuos/efluentes:</span> <span class="fw-semibold">${rel.tipoResiduosEfluentes}</span></div>` : '',
        ].join('');
    }

    function renderizarSeccionLote(loteAsignado, necesidadM2) {
        const esAdmin = Autenticacion.tieneAcceso(['SECRETARIO']);
        const bloqueAsignacion = document.getElementById('bloqueAsignacionLoteRad');
        if (!bloqueAsignacion) return;

        const elLoteAsignado = document.getElementById('loteAsignadoRad');
        const elSinLote = document.getElementById('sinLoteAsignadoRad');
        const formAsignacion = document.getElementById('formAsignacionLoteRad');
        const btnMostrar = document.getElementById('btnMostrarFormLoteRad');
        const alerta = document.getElementById('alertaAsignacionLoteRad');
        const wrapperSuperficie = document.getElementById('wrapperDetRadSuperficieSolicitada');
        const spanSuperficie = document.getElementById('detRadSuperficieSolicitada');

        bloqueAsignacion.classList.remove('d-none');

        // Superficie solicitada en el encabezado del detalle
        if (wrapperSuperficie) {
            wrapperSuperficie.classList.toggle('d-none', !necesidadM2);
            if (spanSuperficie && necesidadM2) {
                spanSuperficie.textContent = `${necesidadM2.toLocaleString('es-AR')} m²`;
            }
        }

        // Limpiar estado anterior
        formAsignacion?.classList.add('d-none');
        btnMostrar?.classList.remove('d-none');
        alerta?.classList.add('d-none');

        if (loteAsignado) {
            elLoteAsignado?.classList.remove('d-none');
            elSinLote?.classList.add('d-none');
            setTexto('detRadLoteCodigo', loteAsignado.codigo || '-');
            setTexto('detRadLoteSuperficie', loteAsignado.superficieMetrosCuadrados
                ? `${loteAsignado.superficieMetrosCuadrados.toLocaleString('es-AR')} m²` : '-');
        } else {
            elLoteAsignado?.classList.add('d-none');
            elSinLote?.classList.remove('d-none');
            if (btnMostrar) btnMostrar.classList.toggle('d-none', !esAdmin);
            // Poblar selector: ordenar por superficie y marcar los que cumplen el requerimiento
            const selectorLote = document.getElementById('selectorLoteDisponibleRad');
            if (selectorLote) {
                const ordenados = [...lotesDisponibles].sort((a, b) => (a.superficieMetrosCuadrados || 0) - (b.superficieMetrosCuadrados || 0));
                selectorLote.innerHTML = '<option value="">Seleccionar lote...</option>'
                    + ordenados.map(l => {
                        const sup = l.superficieMetrosCuadrados || 0;
                        const cumple = necesidadM2 && sup >= necesidadM2;
                        const etiqueta = cumple ? ' ✓' : '';
                        return `<option value="${l.id}">${l.codigo} — ${sup.toLocaleString('es-AR')} m²${etiqueta}</option>`;
                    }).join('');
            }
        }
    }

    async function registrarObservacion() {
        if (!radicacionDetalleAdmin?.id) return;
        const alerta = document.getElementById('alertaObservacionRadAdmin');
        const campo = document.getElementById('campoObservacionRadAdmin');
        const comentario = campo?.value?.trim();

        alerta?.classList.add('d-none');

        if (!comentario) {
            if (alerta) { alerta.textContent = 'Ingrese una observación antes de guardar.'; alerta.classList.remove('d-none'); }
            return;
        }

        const respuesta = await ApiCliente.crear(`/api/radicaciones/${radicacionDetalleAdmin.id}/observaciones`, { comentario });
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            if (alerta) { alerta.textContent = err?.mensaje || err?.message || 'No se pudo guardar la observación.'; alerta.classList.remove('d-none'); }
            return;
        }

        if (campo) campo.value = '';
        mostrarAlerta('Observación registrada correctamente.');

        // Refrescar historial en el modal
        const respHist = await ApiCliente.obtener(`/api/radicaciones/${radicacionDetalleAdmin.id}/historial`);
        if (respHist?.ok) {
            const historial = await respHist.json();
            const cuerpo = document.getElementById('cuerpoHistorialDetalleRadAdmin');
            if (cuerpo) {
                cuerpo.innerHTML = historial?.length
                    ? historial.map(h => `
                        <tr>
                            <td>${formatearFechaEvento(h.fechaEvento)}</td>
                            <td>${h.estadoAnterior ? formatearEstado(h.estadoAnterior) : '<span class="text-muted">-</span>'}</td>
                            <td>${formatearEstado(h.estado)}</td>
                            <td>${h.comentario || '-'}</td>
                            <td>${h.usuario || '-'}</td>
                        </tr>`).join('')
                    : '<tr><td colspan="5" class="text-muted">Sin historial</td></tr>';
            }
        }
    }

    function renderizarSeccionActaRubrica(id, tieneActa, estadoRadicacion) {
        const esAdmin = Autenticacion.tieneAcceso(['SECRETARIO']) && !Autenticacion.tieneAcceso(['EMPRESA']);
        const esGestorConVista = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']) && !Autenticacion.tieneAcceso(['EMPRESA']);
        const estadosPermitidos = new Set(['APROBADA', 'RADICADA']);
        const bloque = document.getElementById('bloqueActaRubricaRad');
        if (!bloque) return;

        const mostrar = esGestorConVista && estadosPermitidos.has(estadoRadicacion);
        bloque.classList.toggle('d-none', !mostrar);
        if (!mostrar) return;

        document.getElementById('alertaActaRubricaRad')?.classList.add('d-none');

        const textoEstado = document.getElementById('textoEstadoActaRubrica');
        const btnVer = document.getElementById('btnVerActaRubrica');
        const formSubir = document.getElementById('formSubirActaRubrica');

        if (tieneActa) {
            if (textoEstado) textoEstado.textContent = 'Acta cargada';
            if (btnVer) {
                btnVer.href = `/api/radicaciones/${id}/rubrica`;
                btnVer.classList.remove('d-none');
            }
        } else {
            if (textoEstado) textoEstado.textContent = 'Sin acta cargada';
            btnVer?.classList.add('d-none');
        }

        if (formSubir) formSubir.classList.toggle('d-none', !esAdmin);
        const inputArchivo = document.getElementById('archivoActaRubricaRad');
        if (inputArchivo) inputArchivo.value = '';
    }

    async function subirActaRubrica() {
        if (!radicacionDetalleAdmin?.id) return;
        const alerta = document.getElementById('alertaActaRubricaRad');
        const archivo = document.getElementById('archivoActaRubricaRad')?.files[0];

        alerta?.classList.add('d-none');

        if (!archivo) {
            if (alerta) { alerta.textContent = 'Seleccione un archivo PDF.'; alerta.classList.remove('d-none'); }
            return;
        }
        if (!archivo.name.toLowerCase().endsWith('.pdf')) {
            if (alerta) { alerta.textContent = 'Solo se permiten archivos PDF.'; alerta.classList.remove('d-none'); }
            return;
        }
        if (archivo.size > 10 * 1024 * 1024) {
            if (alerta) { alerta.textContent = 'El archivo supera el límite de 10 MB.'; alerta.classList.remove('d-none'); }
            return;
        }

        const formData = new FormData();
        formData.append('archivo', archivo);

        const respuesta = await ApiCliente.subirArchivo(`/api/radicaciones/${radicacionDetalleAdmin.id}/rubrica`, formData);
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            if (alerta) {
                alerta.textContent = err?.mensaje || err?.message || 'No se pudo subir el acta.';
                alerta.classList.remove('d-none');
            }
            return;
        }

        renderizarSeccionActaRubrica(radicacionDetalleAdmin.id, true, radicacionDetalleAdmin.estado);
        mostrarAlerta('Acta de rúbrica cargada correctamente.');
    }

    function mostrarFormAsignacionLote() {
        document.getElementById('formAsignacionLoteRad')?.classList.remove('d-none');
        document.getElementById('btnMostrarFormLoteRad')?.classList.add('d-none');
        document.getElementById('alertaAsignacionLoteRad')?.classList.add('d-none');
    }

    async function confirmarAsignacionLote() {
        if (!radicacionDetalleAdmin?.id) return;
        const selector = document.getElementById('selectorLoteDisponibleRad');
        const loteId = selector?.value;
        const alerta = document.getElementById('alertaAsignacionLoteRad');

        if (!loteId) {
            alerta.textContent = 'Seleccione un lote.';
            alerta?.classList.remove('d-none');
            return;
        }
        alerta?.classList.add('d-none');

        const respuesta = await ApiCliente.parche(`/api/radicaciones/${radicacionDetalleAdmin.id}/lote`, { loteId: parseInt(loteId, 10) });
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo reservar el lote.';
            alerta?.classList.remove('d-none');
            return;
        }

        mostrarAlerta('Lote reservado para la solicitud correctamente.');
        await cargar();
        await verDetalleAdmin(radicacionDetalleAdmin.id);
    }

    function toggleCamposPlazo(estado) {
        const esAprobada = estado === 'APROBADA';
        ['wrapperCampoPlazoFecha', 'wrapperCampoPlazoMeses', 'wrapperCampoPlazoCalculado'].forEach(id => {
            document.getElementById(id)?.classList.toggle('d-none', !esAprobada);
        });
        if (esAprobada) {
            const campoFecha = document.getElementById('campoPlazoFecha');
            if (campoFecha && !campoFecha.value) {
                campoFecha.value = new Date().toISOString().split('T')[0];
            }
            const campoMeses = document.getElementById('campoPlazoMeses');
            if (campoMeses && !campoMeses.value && radicacionDetalleAdmin?.tiempoSolicitadoMeses) {
                campoMeses.value = radicacionDetalleAdmin.tiempoSolicitadoMeses;
            }
            calcularFechaPlazo();
        }

        const estadoActual = radicacionDetalleAdmin?.estado;
        const requiereDoc = (estado === 'APROBADA' && estadoActual !== 'APROBADA')
                         || (estado === 'RADICADA'  && estadoActual !== 'RADICADA');
        const wrapperDoc = document.getElementById('wrapperDocumentoEstadoRad');
        if (wrapperDoc) wrapperDoc.classList.toggle('d-none', !requiereDoc);
        if (!requiereDoc) {
            const arch = document.getElementById('archivoDocumentoEstadoRad');
            if (arch) arch.value = '';
        }
        const labelDoc = document.getElementById('labelDocumentoEstadoRad');
        const ayudaDoc = document.getElementById('ayudaDocumentoEstadoRad');
        if (labelDoc && ayudaDoc) {
            if (estado === 'APROBADA') {
                labelDoc.innerHTML = 'Rúbrica de aprobación (PDF) <span class="text-danger">*</span>';
                ayudaDoc.textContent = 'Suba el acta de rúbrica firmada por el Directorio.';
            } else if (estado === 'RADICADA') {
                labelDoc.innerHTML = 'Certificación de radicación (PDF) <span class="text-danger">*</span>';
                ayudaDoc.textContent = 'Suba el certificado de radicación correspondiente.';
            }
        }
    }

    function calcularFechaPlazo() {
        const fechaVal = document.getElementById('campoPlazoFecha')?.value;
        const mesesVal = parseInt(document.getElementById('campoPlazoMeses')?.value, 10);
        const display = document.getElementById('campoPlazoCalculadoDisplay');
        if (!display) return;
        if (fechaVal && !isNaN(mesesVal) && mesesVal > 0) {
            const d = new Date(fechaVal + 'T00:00:00');
            d.setMonth(d.getMonth() + mesesVal);
            display.textContent = d.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });
        } else {
            display.textContent = '-';
        }
    }

    function actualizarRequisitoObservacion(estado) {
        const label = document.getElementById('labelObservacionesRadAdmin');
        const campo = document.getElementById('campoObservacionesRadAdmin');
        if (!label || !campo) return;
        if (estado === 'RECHAZADA') {
            label.textContent = 'Motivo de rechazo';
            const asterisco = document.createElement('span');
            asterisco.className = 'text-danger fw-bold';
            asterisco.textContent = ' *';
            label.appendChild(asterisco);
            campo.placeholder = 'Ingrese el motivo de rechazo (obligatorio)';
            campo.required = true;
        } else {
            label.textContent = 'Observaciones';
            campo.placeholder = 'Ingrese observaciones para el cambio';
            campo.required = false;
        }
    }

    async function confirmarCambioEstadoDetalleAdmin() {
        if (!radicacionDetalleAdmin?.id) {
            return;
        }
        const selector = document.getElementById('selectorNuevoEstadoRadAdmin');
        const observaciones = document.getElementById('campoObservacionesRadAdmin')?.value?.trim() || '';
        const nuevoEstado = selector?.value;
        if (!nuevoEstado) {
            mostrarAlerta('Seleccione un nuevo estado.', 'danger');
            return;
        }
        const requiereObservacion = nuevoEstado === 'RECHAZADA' || nuevoEstado === 'CANCELADA';
        if (requiereObservacion && !observaciones) {
            mostrarAlerta('Debe ingresar observaciones para el estado seleccionado.', 'danger');
            return;
        }

        const fechaAprobacion = document.getElementById('campoPlazoFecha')?.value || null;
        const mesesVal = parseInt(document.getElementById('campoPlazoMeses')?.value, 10);
        const tiempoEstimadoObraMeses = !isNaN(mesesVal) && mesesVal > 0 ? mesesVal : null;

        if (nuevoEstado === 'APROBADA') {
            if (!fechaAprobacion) {
                mostrarAlerta('Debe indicar la fecha de radicación para aprobar el expediente.', 'danger');
                return;
            }
            if (!tiempoEstimadoObraMeses) {
                mostrarAlerta('Debe indicar los meses de plazo para aprobar el expediente.', 'danger');
                return;
            }
        }

        const requiereDocumento = nuevoEstado === 'APROBADA' || nuevoEstado === 'RADICADA';
        const archivoDocEstado = document.getElementById('archivoDocumentoEstadoRad')?.files[0];
        if (requiereDocumento && !archivoDocEstado) {
            const etiqueta = nuevoEstado === 'APROBADA' ? 'rúbrica de aprobación' : 'certificación de radicación';
            mostrarAlerta(`Debe adjuntar el PDF de ${etiqueta} para continuar.`, 'danger');
            return;
        }
        if (requiereDocumento && archivoDocEstado && !archivoDocEstado.name.toLowerCase().endsWith('.pdf')) {
            mostrarAlerta('Solo se permiten archivos PDF para este documento.', 'danger');
            return;
        }

        let fechaPlazo = null;
        if (fechaAprobacion && tiempoEstimadoObraMeses) {
            const d = new Date(fechaAprobacion + 'T00:00:00');
            d.setMonth(d.getMonth() + tiempoEstimadoObraMeses);
            fechaPlazo = d.toISOString().split('T')[0];
        }

        const confirmado = window.confirm(`Se cambiará el expediente ${radicacionDetalleAdmin.numeroRadicado} a ${formatearEstado(nuevoEstado)}. ¿Desea continuar?`);
        if (!confirmado) {
            return;
        }

        const comentario = observaciones || `Cambio de estado a ${formatearEstado(nuevoEstado)} por administración`;
        const payload = { estado: nuevoEstado, comentario };
        if (fechaAprobacion) payload.fechaAprobacion = fechaAprobacion;
        if (fechaPlazo) payload.fechaPlazo = fechaPlazo;
        if (tiempoEstimadoObraMeses) payload.tiempoEstimadoObraMeses = tiempoEstimadoObraMeses;
        const respuesta = await ApiCliente.parche(`/api/radicaciones/${radicacionDetalleAdmin.id}/estado`, payload);
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            mostrarAlerta(err?.mensaje || err?.message || 'No se pudo actualizar el estado del expediente.', 'danger');
            return;
        }

        if (requiereDocumento && archivoDocEstado) {
            const formData = new FormData();
            if (nuevoEstado === 'APROBADA') {
                formData.append('archivo', archivoDocEstado);
                const respDoc = await ApiCliente.subirArchivo(`/api/radicaciones/${radicacionDetalleAdmin.id}/rubrica`, formData);
                if (!respDoc?.ok) {
                    const err = await respDoc?.json().catch(() => ({}));
                    mostrarAlerta(`Estado actualizado, pero no se pudo adjuntar la rúbrica: ${err?.mensaje || 'intente subirla manualmente'}`, 'warning');
                    bootstrap.Modal.getInstance(document.getElementById('modalDetalleRadicacionAdmin'))?.hide();
                    await cargar();
                    return;
                }
            } else {
                formData.append('tipoDocumento', 'CERTIFICACION_RADICACION');
                formData.append('descripcion', 'Certificación de radicación');
                formData.append('archivo', archivoDocEstado);
                const respDoc = await ApiCliente.subirArchivo(`/api/radicaciones/${radicacionDetalleAdmin.id}/documentos`, formData);
                if (!respDoc?.ok) {
                    const err = await respDoc?.json().catch(() => ({}));
                    mostrarAlerta(`Estado actualizado, pero no se pudo adjuntar la certificación: ${err?.mensaje || 'intente subirla manualmente'}`, 'warning');
                    bootstrap.Modal.getInstance(document.getElementById('modalDetalleRadicacionAdmin'))?.hide();
                    await cargar();
                    return;
                }
            }
        }

        mostrarAlerta('Estado del expediente actualizado correctamente.');
        bootstrap.Modal.getInstance(document.getElementById('modalDetalleRadicacionAdmin'))?.hide();
        await cargar();
    }

    function aplicarFiltrosListado(listado, estadoSimple) {
        let filtrado = [...(listado || [])];
        if (estadoSimple) {
            filtrado = filtrado.filter(r => r.estado === estadoSimple);
        }
        if (estadosSeleccionadosListado.size > 0) {
            filtrado = filtrado.filter(r => estadosSeleccionadosListado.has(r.estado));
        }
        return filtrado;
    }

    function inicializarFiltrosMultiplesEstado() {
        const checks = document.querySelectorAll('.filtro-estado-rad-multi');
        if (!checks?.length) {
            return;
        }
        checks.forEach(chk => {
            chk.addEventListener('change', () => {
                const seleccionados = [...checks].filter(x => x.checked).map(x => x.value);
                estadosSeleccionadosListado = new Set(seleccionados);
                cargar();
            });
        });
    }

    function setTexto(id, texto) {
        const el = document.getElementById(id);
        if (el) el.textContent = texto || '-';
    }

    function irADocumentacion(idRadicacion) {
        const trigger = document.querySelector('#tab-rad-d');
        if (trigger && window.bootstrap?.Tab) {
            window.bootstrap.Tab.getOrCreateInstance(trigger).show();
        }
        const selector = document.getElementById('selectorRadicacionDocumento');
        if (selector) {
            selector.value = String(idRadicacion);
        }
        mostrarAlerta('La solicitud requiere información adicional. Adjunte documentación y vuelva a enviar.', 'warning');
    }

    function mostrarErrorDocumento(mensaje) {
        const alerta = document.getElementById('alertaDocumentoRad');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function mostrarErrorRadicacion(mensaje) {
        const alerta = document.getElementById('alertaRadicacionNueva');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function ocultarErrorRadicacion() {
        const alerta = document.getElementById('alertaRadicacionNueva');
        if (!alerta) return;
        alerta.classList.add('d-none');
        alerta.textContent = '';
    }

    function construirRelevamientoPedidoLotes() {
        const relevamiento = {
            correo: leerTexto('relCorreo'),
            razonSocialEmpresa: leerTexto('relRazonSocial'),
            cuit: leerTexto('relCuit'),
            ingresosBrutos: leerTexto('relIngresosBrutos'),
            actividadPrincipal: leerTexto('relActividadPrincipal'),
            actividadSecundaria: leerTexto('relActividadSecundaria'),
            tipoEmpresa: leerTexto('relTipoEmpresa'),
            objetoProyecto: leerTexto('relObjetoProyecto'),
            direccion: leerTexto('relDireccion'),
            personaReferente: leerTexto('relPersonaReferente'),
            telefono: leerTexto('relTelefono'),
            correoElectronico: leerTexto('relCorreoElectronico'),
            rubro: leerTexto('relRubro'),
            rubroOtro: leerTexto('relRubroOtro'),
            descripcionServicioBienOfrecido: leerTexto('relDescripcionServicio'),
            emplazamientoActual: leerTexto('relEmplazamiento'),
            personalJerarquico: leerEntero('relPersonalJerarquico'),
            personalProduccion: leerEntero('relPersonalProduccion'),
            personalAdministrativo: leerEntero('relPersonalAdministrativo'),
            tiempoRadicacionMeses: leerEntero('relTiempoRadicacion'),
            necesidadMetrosCuadrados: leerEntero('relNecesidadM2'),
            superficieCubiertaTrabajo: leerDecimal('relSupTrabajo'),
            superficieCubiertaDeposito: leerDecimal('relSupDeposito'),
            superficieFuturaExpansion: leerDecimal('relSupExpansion'),
            superficieEstacionamiento: leerDecimal('relSupEstacionamiento'),
            tienePlanos: leerBooleano('relTienePlanos'),
            personalAOcupar: leerEntero('relPersonalAOcupar'),
            materiasPrimas: leerTexto('relMateriasPrimas'),
            destinoProduccion: leerTexto('relDestinoProduccion'),
            tensionAlimentacion: leerTexto('relTension'),
            potenciaInstaladaKw: leerDecimal('relPotenciaKw'),
            aguaLtsMes: leerDecimal('relAguaLtsMes'),
            requiereGas: leerBooleano('relRequiereGas'),
            tipoResiduosEfluentes: leerTexto('relTipoResiduos'),
            tratamientoEnPlanta: leerBooleano('relTratamientoPlanta'),
            necesitaBalanzaPublica: leerBooleano('relBalanzaPublica'),
            necesitaComedorUnitario: leerBooleano('relComedorUnitario'),
            necesitaSalonCoworking: leerBooleano('relSalonCoworking')
        };

        if (!relevamiento.correo || !relevamiento.razonSocialEmpresa || !relevamiento.cuit || !relevamiento.actividadPrincipal) {
            mostrarErrorRadicacion('Complete los campos obligatorios del relevamiento (correo, razón social, CUIT y actividad principal).');
            return null;
        }
        const cuitDigitos = relevamiento.cuit.replace(/-/g, '').trim();
        if (cuitDigitos.length === 11 && !validarDigitoVerificadorCuit(cuitDigitos)) {
            mostrarErrorRadicacion('El CUIT ingresado no es válido (dígito verificador incorrecto).');
            return null;
        }
        return relevamiento;
    }

    function leerTexto(id) {
        return document.getElementById(id)?.value?.trim() || '';
    }

    function leerEntero(id) {
        const valor = document.getElementById(id)?.value;
        return valor === '' || valor == null ? null : Number.parseInt(valor, 10);
    }

    function leerDecimal(id) {
        const valor = document.getElementById(id)?.value;
        return valor === '' || valor == null ? null : Number.parseFloat(valor);
    }

    function leerBooleano(id) {
        return document.getElementById(id)?.value === 'true';
    }

    function validarDigitoVerificadorCuit(cuit11) {
        const pesos = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
        const suma = pesos.reduce((acc, p, i) => acc + p * Number(cuit11[i]), 0);
        const resto = suma % 11;
        if (resto === 1) return false;
        const digitoEsperado = resto === 0 ? 0 : 11 - resto;
        return Number(cuit11[10]) === digitoEsperado;
    }

    function inicializarFormularioRadicacion() {
        if (formularioInicializado) {
            return;
        }
        formularioInicializado = true;

        const formulario = document.getElementById('formRadicacionNueva');
        if (!formulario) {
            return;
        }

        formulario.querySelectorAll('input, textarea, select').forEach(campo => {
            campo.addEventListener('input', programarGuardadoBorrador);
            campo.addEventListener('change', programarGuardadoBorrador);
        });

        const activarRelevamiento = document.getElementById('activarRelevamientoPedidoLotes');
        if (activarRelevamiento) {
            activarRelevamiento.addEventListener('change', () => {
                actualizarVisibilidadRelevamiento();
                programarGuardadoBorrador();
            });
        }

        const filtroHistorial = document.getElementById('filtroEstadoHistorialRad');
        restaurarFiltrosHistorialSesion();
        if (filtroHistorial) {
            filtroHistorial.value = filtroEstadoHistorial;
        }
        const btnUltimos10 = document.getElementById('btnUltimos10HistorialRad');
        if (btnUltimos10) {
            btnUltimos10.textContent = soloUltimos10Historial ? 'Ver todos' : 'Últimos 10';
        }

        if (filtroHistorial) {
            filtroHistorial.addEventListener('change', (e) => {
                filtroEstadoHistorial = e.target.value;
                guardarFiltrosHistorialSesion();
                aplicarFiltrosHistorialYRenderizar();
            });
        }

        if (btnUltimos10) {
            btnUltimos10.addEventListener('click', () => {
                soloUltimos10Historial = !soloUltimos10Historial;
                btnUltimos10.textContent = soloUltimos10Historial ? 'Ver todos' : 'Últimos 10';
                guardarFiltrosHistorialSesion();
                aplicarFiltrosHistorialYRenderizar();
            });
        }

        const restaurado = restaurarBorrador();
        inicializarFiltrosMultiplesEstado();
        actualizarVisibilidadRelevamiento();
        if (!restaurado) {
            actualizarEstadoBorrador('Borrador no guardado');
        }
        actualizarTextoTituloHistorial();
    }

    function actualizarVisibilidadRelevamiento() {
        const usarRelevamiento = document.getElementById('activarRelevamientoPedidoLotes')?.checked;
        const bloque = document.getElementById('bloqueRelevamientoPedidoLotes');
        if (!bloque) return;
        bloque.classList.toggle('d-none', !usarRelevamiento);
    }

    function programarGuardadoBorrador() {
        if (temporizadorBorrador) {
            clearTimeout(temporizadorBorrador);
        }
        temporizadorBorrador = setTimeout(guardarBorradorAhora, 500);
    }

    function guardarBorradorAhora() {
        const datos = {};
        CAMPOS_FORMULARIO_RADICACION.forEach(id => {
            const campo = document.getElementById(id);
            if (!campo) {
                return;
            }
            if (campo.type === 'checkbox') {
                datos[id] = campo.checked;
            } else {
                datos[id] = campo.value;
            }
        });

        localStorage.setItem(CLAVE_BORRADOR_RADICACION, JSON.stringify({
            fecha: new Date().toISOString(),
            datos
        }));
        actualizarEstadoBorrador('Borrador guardado');
    }

    function restaurarBorrador() {
        const bruto = localStorage.getItem(CLAVE_BORRADOR_RADICACION);
        if (!bruto) {
            return false;
        }
        try {
            const guardado = JSON.parse(bruto);
            const datos = guardado?.datos || {};
            Object.entries(datos).forEach(([id, valor]) => {
                const campo = document.getElementById(id);
                if (!campo) {
                    return;
                }
                if (campo.type === 'checkbox') {
                    campo.checked = Boolean(valor);
                } else if (valor != null) {
                    campo.value = valor;
                }
            });
            const fecha = guardado?.fecha ? formatearFechaBorrador(guardado.fecha) : null;
            actualizarEstadoBorrador(fecha ? `Borrador restaurado (${fecha})` : 'Borrador restaurado');
            return true;
        } catch (_e) {
            actualizarEstadoBorrador('Borrador inválido, vuelva a guardarlo');
            return false;
        }
    }

    function descartarBorrador() {
        localStorage.removeItem(CLAVE_BORRADOR_RADICACION);
        actualizarEstadoBorrador('Borrador descartado');
    }

    function limpiarBorradorPersistido() {
        localStorage.removeItem(CLAVE_BORRADOR_RADICACION);
        actualizarEstadoBorrador('Borrador limpiado tras envío');
    }

    function actualizarEstadoBorrador(texto) {
        const estado = document.getElementById('estadoBorradorRadicacion');
        if (!estado) return;
        estado.textContent = texto;
    }

    function formatearFechaBorrador(fechaIso) {
        const fecha = new Date(fechaIso);
        if (Number.isNaN(fecha.getTime())) {
            return null;
        }
        return fecha.toLocaleString();
    }

    function guardarFiltrosHistorialSesion() {
        try {
            sessionStorage.setItem(CLAVE_FILTROS_HISTORIAL_RADICACION, JSON.stringify({
                filtroEstadoHistorial,
                soloUltimos10Historial,
                historialRadicacionSeleccionadaId
            }));
        } catch (_e) {
            // No bloquear la UI si el almacenamiento no está disponible.
        }
    }

    function restaurarFiltrosHistorialSesion() {
        try {
            const bruto = sessionStorage.getItem(CLAVE_FILTROS_HISTORIAL_RADICACION);
            if (!bruto) {
                return;
            }
            const guardado = JSON.parse(bruto);
            const estado = guardado?.filtroEstadoHistorial || '';
            filtroEstadoHistorial = ESTADOS_FILTRO_HISTORIAL_VALIDOS.has(estado) ? estado : '';
            soloUltimos10Historial = Boolean(guardado?.soloUltimos10Historial);
            const seleccionado = guardado?.historialRadicacionSeleccionadaId;
            const seleccionadoNumero = Number.parseInt(seleccionado, 10);
            historialRadicacionSeleccionadaId = Number.isNaN(seleccionadoNumero) ? null : seleccionadoNumero;
        } catch (_e) {
            filtroEstadoHistorial = '';
            soloUltimos10Historial = false;
            historialRadicacionSeleccionadaId = null;
            try {
                sessionStorage.removeItem(CLAVE_FILTROS_HISTORIAL_RADICACION);
            } catch (_ignore) {
                // Sin acción.
            }
        }
    }

    function mostrarIndicadorRestauracionHistorial(idExpediente, numeroRadicado) {
        const indicador = document.getElementById('indicadorRestauracionHistorialRad');
        if (!indicador) {
            return;
        }
        const fechaIso = new Date().toISOString();
        const etiquetaExpediente = construirEtiquetaExpediente(idExpediente, numeroRadicado);
        actualizarTextoIndicadorRestauracion(etiquetaExpediente);
        actualizarTooltipIndicadorRestauracion(indicador, fechaIso, idExpediente, numeroRadicado);
        try {
            if (sessionStorage.getItem(CLAVE_INDICADOR_RESTAURACION_HISTORIAL) === '1') {
                return;
            }
            sessionStorage.setItem(CLAVE_INDICADOR_RESTAURACION_HISTORIAL, '1');
            sessionStorage.setItem(CLAVE_FECHA_RESTAURACION_HISTORIAL, fechaIso);
            indicador.classList.remove('d-none');
            setTimeout(() => indicador.classList.add('d-none'), 4000);
        } catch (_e) {
            indicador.classList.remove('d-none');
            setTimeout(() => indicador.classList.add('d-none'), 4000);
        }
    }

    function ocultarIndicadorRestauracionHistorial() {
        const indicador = document.getElementById('indicadorRestauracionHistorialRad');
        if (!indicador) {
            return;
        }
        indicador.classList.add('d-none');
    }

    function actualizarTooltipIndicadorRestauracion(indicador, fechaIso, idExpediente, numeroRadicado) {
        const fechaFormateada = formatearFechaBorrador(fechaIso);
        const etiquetaExpediente = construirEtiquetaExpediente(idExpediente, numeroRadicado);
        const textoTooltip = fechaFormateada
            ? `${etiquetaExpediente} | Última restauración: ${fechaFormateada}`
            : 'Última restauración automática del expediente';
        indicador.title = textoTooltip;
        indicador.setAttribute('aria-label', textoTooltip);
    }

    function actualizarTextoIndicadorRestauracion(etiquetaExpediente) {
        const texto = document.getElementById('textoIndicadorRestauracionHistorialRad');
        if (!texto) {
            return;
        }
        texto.textContent = `Expediente recuperado de la sesión · ${etiquetaExpediente}`;
    }

    function construirEtiquetaExpediente(idExpediente, numeroRadicado) {
        return numeroRadicado
            ? `Expediente ${numeroRadicado}`
            : (idExpediente ? `Expediente #${idExpediente}` : 'Expediente restaurado');
    }

    function formatearEstado(estado) {
        const m = { PENDIENTE: 'Pendiente', ...NOMBRES_ESTADO_RAD };
        return m[estado] || estado;
    }

    return {
        cargar,
        crear,
        subirDocumento,
        cambiarEstado,
        irADocumentacion,
        seleccionarExpediente,
        verHistorial,
        verDetalleAdmin,
        confirmarCambioEstadoDetalleAdmin,
        toggleCamposPlazo,
        calcularFechaPlazo,
        mostrarFormAsignacionLote,
        confirmarAsignacionLote,
        guardarBorradorAhora,
        descartarBorrador,
        abrirModalNuevaSolicitud,
        subirDocumentoDesdeModal,
        toggleRubroOtros,
        toggleAdjuntoRadicacion,
        subirActaRubrica,
        registrarObservacion
    };
})();

