const ModuloRadicaciones = (() => {
    let radicaciones = [];
    let historialRadicacionSeleccionadaId = null;
    let historialEventos = [];
    let radicacionDetalleAdmin = null;
    let lotesDisponibles = [];
    let filtroEstadoHistorial = '';
    let soloUltimos10Historial = false;
    let estadosSeleccionadosListado = new Set();
    let formularioInicializado = false;
    let temporizadorBorrador = null;
    let evaluacionActual = null;
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
        'relTipoEmpresa', 'relObjetoProyecto',
        'relTiempoRadicacion', 'relNecesidadM2', 'relSupTrabajo', 'relSupDeposito',
        'relSupExpansion', 'relSupEstacionamiento', 'relTienePlanos', 'relPersonalAOcupar', 'relMateriasPrimas',
        'relDestinoProduccion', 'relTension', 'relPotenciaKw', 'relAguaLtsMes', 'relRequiereGas', 'relTipoResiduos',
        'relTratamientoPlanta', 'relBalanzaPublica', 'relComedorUnitario', 'relSalonCoworking',
        'adjRadTipoDocumento', 'adjRadDescripcion'
    ];

    async function cargar() {
        inicializarFormularioRadicacion();
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

    function filtrarTexto() {
        renderizarTabla();
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaRadicaciones');
        if (!cuerpo) return;

        const textoBusq = (document.getElementById('filtroBusquedaRad')?.value || '').trim().toLowerCase();
        const lista = textoBusq
            ? radicaciones.filter(r =>
                (r.nombreEmpresa || r.razonSocialEmpresa || '').toLowerCase().includes(textoBusq) ||
                (r.numeroRadicado || '').toLowerCase().includes(textoBusq) ||
                (r.cuitEmpresa || '').toLowerCase().includes(textoBusq))
            : radicaciones;

        if (lista.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="8" class="text-center py-3 text-muted">Sin registros</td></tr>';
            return;
        }

        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']) && !Autenticacion.tieneAcceso(['EMPRESA']);
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        cuerpo.innerHTML = lista.map(r => `
            <tr role="button" title="Seleccionar expediente" onclick="ModuloRadicaciones.seleccionarExpediente(${r.id}, '${r.numeroRadicado}')">
                <td class="ps-3 fw-semibold">${r.numeroRadicado}</td>
                <td>${r.nombreEmpresa || r.razonSocialEmpresa || '-'}</td>
                <td>${r.tipoSolicitud}</td>
                <td>${r.usoEstimativo || '-'}</td>
                <td><span class="badge bg-secondary">${formatearEstado(r.estado)}</span></td>
                <td>${r.fechaRadicacion || '-'}</td>
                <td>${formatearFechaEvento(r.fechaUltimaActualizacion)}</td>
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
        REQUIERE_INFORMACION_ADICIONAL:  ['PENDIENTE', 'RECHAZADA', 'CANCELADA'],
        APROBADA:                        ['RADICADA', 'DESADJUDICACION', 'CANCELADA'],
        RADICADA:                        [],
        RECHAZADA:                       [],
        CANCELADA:                       [],
        DESADJUDICACION:                 []
    };
    const NOMBRES_ESTADO_RAD = {
        EN_REVISION:                    'En revisión',
        APROBADA:                       'Aprobada',
        RADICADA:                       'Radicada',
        RECHAZADA:                      'Rechazada',
        REQUIERE_INFORMACION_ADICIONAL: 'Requiere información adicional',
        CANCELADA:                      'Cancelada',
        DESADJUDICACION:                'Desadjudicación'
    };

    function opcionesEstadoGestion(estadoActual, proyectoEstado) {
        let siguientes = TRANSICIONES_VALIDAS_RAD[estadoActual] || [];
        if (siguientes.length === 0) {
            return '<option value="" disabled>Sin cambios de estado posibles</option>';
        }
        return siguientes.map(v => {
            const bloqueada = v === 'RADICADA' && proyectoEstado !== 'COMPLETADO';
            const etiqueta = NOMBRES_ESTADO_RAD[v] || v;
            if (bloqueada) {
                return `<option value="${v}" disabled title="El proyecto debe estar Completado">${etiqueta} (proyecto pendiente)</option>`;
            }
            return `<option value="${v}">${etiqueta}</option>`;
        }).join('');
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
            const puedeAdjuntar = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
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
            const [respDetalle, respDocs, respHist, respLoteAsignado, respRelevamiento, respRubrica, respEvaluacion] = await Promise.all([
                ApiCliente.obtener(`/api/radicaciones/${id}`),
                ApiCliente.obtener(`/api/radicaciones/${id}/documentos`),
                ApiCliente.obtener(`/api/radicaciones/${id}/historial`),
                ApiCliente.obtener(`/api/radicaciones/${id}/lote`),
                ApiCliente.obtener(`/api/radicaciones/${id}/relevamiento`),
                ApiCliente.obtener(`/api/radicaciones/${id}/rubrica`),
                ApiCliente.obtener(`/api/radicaciones/${id}/evaluacion`)
            ]);
            estadoCarga?.classList.add('d-none');
            if (!respDetalle?.ok) {
                const err = await respDetalle?.json().catch(() => ({}));
                error.textContent = err?.mensaje || err?.message || 'No se pudo cargar el detalle del expediente.';
                error?.classList.remove('d-none');
                return;
            }

            let detalle = await respDetalle.json();
            const documentos = respDocs?.ok ? await respDocs.json() : [];
            const historial = respHist?.ok ? await respHist.json() : [];
            let loteAsignado = (respLoteAsignado?.ok && respLoteAsignado.status !== 204)
                ? await respLoteAsignado.json().catch(() => null) : null;
            // Fallback: si el endpoint de lote falló pero el detalle indica que hay lote asignado
            if (!loteAsignado && detalle.loteId) {
                loteAsignado = { id: detalle.loteId, codigo: detalle.codigoLote, superficieMetrosCuadrados: null };
            }

            // Auto-transición PENDIENTE → EN_REVISION al visualizar el detalle (Secretario/Admin)
            const esEmpresaVisor = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO', 'TECNICO']);
            if (detalle.estado === 'PENDIENTE' && !esEmpresaVisor) {
                const respTransicion = await ApiCliente.parche(`/api/radicaciones/${id}/estado`, {
                    estado: 'EN_REVISION',
                    comentario: 'Expediente tomado en revisión'
                });
                if (respTransicion?.ok) {
                    detalle = await respTransicion.json();
                    verHistorial(id, null, true);
                }
            }

            // Lotes solo se necesitan cuando no hay lote asignado y el usuario puede asignar uno
            lotesDisponibles = [];
            if (!loteAsignado && Autenticacion.tieneAcceso(['SECRETARIO'])) {
                const respLotes = await ApiCliente.obtener('/api/lotes');
                const arr = respLotes?.ok ? await respLotes.json() : [];
                lotesDisponibles = (Array.isArray(arr) ? arr : (arr.contenido || [])).filter(l => !l.estadoAsignacion);
            }
            const relevamiento = (respRelevamiento?.ok && respRelevamiento.status !== 204)
                ? await respRelevamiento.json().catch(() => null) : null;
            const tieneActa = respRubrica?.ok === true;
            const evaluacion = respEvaluacion?.ok ? await respEvaluacion.json().catch(() => null) : null;
            radicacionDetalleAdmin = detalle;
            renderizarDetalleAdmin(detalle, documentos, historial, loteAsignado, relevamiento, tieneActa, evaluacion);
            contenido?.classList.remove('d-none');
        } catch (e) {
            estadoCarga?.classList.add('d-none');
            console.error('Error al cargar detalle de radicación:', e);
            error.textContent = 'Error al cargar el detalle del expediente.';
            error?.classList.remove('d-none');
        }
    }

    function renderizarDetalleAdmin(detalle, documentos, historial, loteAsignado, relevamiento = null, tieneActa = false, evaluacion = null) {
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
        const wrapResolucion = document.getElementById('wrapperDetRadNumeroResolucion');
        if (wrapResolucion) {
            wrapResolucion.classList.toggle('d-none', !detalle?.numeroResolucion);
            setTexto('detRadNumeroResolucion', detalle?.numeroResolucion || '-');
        }
        const wrapResueltoPor = document.getElementById('wrapperDetRadResueltoPor');
        if (wrapResueltoPor) {
            wrapResueltoPor.classList.toggle('d-none', !detalle?.resueltoPor);
            setTexto('detRadResueltoPor', detalle?.resueltoPor || '-');
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
        const campoNroRes = document.getElementById('campoNumeroResolucionEstadoRad');
        if (campoNroRes) campoNroRes.value = '';
        document.getElementById('wrapperNumeroResolucionEstadoRad')?.classList.add('d-none');

        const selector = document.getElementById('selectorNuevoEstadoRadAdmin');
        if (selector) {
            selector.innerHTML = opcionesEstadoGestion(detalle?.estado, detalle?.proyectoEstado);
            selector.onchange = () => {
                actualizarRequisitoObservacion(selector.value);
                toggleCamposPlazo(selector.value);
            };
            // Saltar opciones deshabilitadas al inicializar
            if (selector.options[selector.selectedIndex]?.disabled) {
                const habilitada = [...selector.options].find(o => !o.disabled && o.value);
                if (habilitada) selector.value = habilitada.value;
            }
            actualizarRequisitoObservacion(selector.value);
            toggleCamposPlazo(selector.value);
        }

        // Sección de lote
        renderizarSeccionLote(loteAsignado, detalle?.superficieSolicitadaM2);

        // Acta de rúbrica
        renderizarSeccionActaRubrica(detalle?.id, tieneActa, detalle?.estado);

        // Evaluación por etapas (HU-03)
        renderizarEvaluacion(evaluacion, detalle?.estado);

        // Relevamiento
        renderizarRelevamiento(relevamiento, detalle?.tieneRelevamientoPedidoLotes, detalle, historial, documentos);

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

    function renderizarRelevamiento(rel, tieneRelevamiento, detalle, historial, documentos) {
        const bloque = document.getElementById('bloqueRelevamientoDetalleRad');
        const contenido = document.getElementById('contenidoRelevamientoDetalleRad');
        if (!bloque || !contenido) return;
        bloque.classList.remove('d-none');

        const campo = (etiqueta, valor) => valor != null && valor !== ''
            ? `<div class="col-md-4"><span class="text-muted">${etiqueta}:</span> <span class="fw-semibold">${valor}</span></div>`
            : '';
        const bool = v => v === true ? 'Sí' : v === false ? 'No' : '-';

        // Empresa que solicita (datos del sistema)
        const razonSocial = detalle?.razonSocialEmpresa || detalle?.nombreEmpresa || '-';
        const seccionEmpresa = `
            <div class="col-12 mb-1">
                <strong class="small">Empresa que solicita:</strong> <span class="fw-semibold">${razonSocial}</span>
            </div>
            ${campo('CUIT', detalle?.cuitEmpresa)}
            ${campo('Actividad económica', detalle?.actividadEconomicaEmpresa)}
            ${campo('Dirección', detalle?.direccionEmpresa)}
            ${campo('Correo electrónico', detalle?.correoElectronicoEmpresa)}
            ${campo('Teléfono', detalle?.telefonoEmpresa)}
            <div class="col-12"><hr class="my-2"></div>
        `;

        // Tipo y destino de la solicitud (datos del expediente)
        const seccionSolicitud = `
            ${campo('1. Tipo de solicitud', detalle?.tipoSolicitud)}
            ${campo('2. Destino del Lote', detalle?.usoEstimativo)}
        `;

        // Campos del relevamiento
        let seccionRelevamiento = '';
        if (tieneRelevamiento && rel) {
            seccionRelevamiento = [
                campo('3. Tiempo de radicación', rel.tiempoRadicacionMeses != null ? `${rel.tiempoRadicacionMeses} meses` : null),
                campo('4. Necesidad de m²', rel.necesidadMetrosCuadrados != null ? `${rel.necesidadMetrosCuadrados} m²` : null),
                campo('5. Personal a ocupar', rel.personalAOcupar),
                campo('6. Sup. trabajo (m²)', rel.superficieCubiertaTrabajo != null ? `${rel.superficieCubiertaTrabajo} m²` : null),
                campo('7. Sup. depósito (m²)', rel.superficieCubiertaDeposito != null ? `${rel.superficieCubiertaDeposito} m²` : null),
                campo('8. Sup. expansión (m²)', rel.superficieFuturaExpansion != null ? `${rel.superficieFuturaExpansion} m²` : null),
                campo('9. Sup. estacionamiento (m²)', rel.superficieEstacionamiento != null ? `${rel.superficieEstacionamiento} m²` : null),
                campo('10. Tiene planos', bool(rel.tienePlanos)),
                campo('11. Tensión alimentación', rel.tensionAlimentacion),
                campo('12. Potencia instalada simultánea (kW)', rel.potenciaInstaladaKw),
                campo('13. Agua (lts/mes)', rel.aguaLtsMes),
                campo('14. Gas', bool(rel.requiereGas)),
                campo('15. Tratamiento en planta', bool(rel.tratamientoEnPlanta)),
                campo('16. Necesidad balanza pública', bool(rel.necesitaBalanzaPublica)),
                campo('17. Necesidad comedor unitario', bool(rel.necesitaComedorUnitario)),
                campo('18. Necesidad SUM/coworking', bool(rel.necesitaSalonCoworking)),
                rel.materiasPrimas ? `<div class="col-12"><span class="text-muted">19. Materias primas:</span> <span class="fw-semibold">${rel.materiasPrimas}</span></div>` : '',
                rel.destinoProduccion ? `<div class="col-12"><span class="text-muted">20. Destino de la producción:</span> <span class="fw-semibold">${rel.destinoProduccion}</span></div>` : '',
                rel.tipoResiduosEfluentes ? `<div class="col-12"><span class="text-muted">21. Residuos/Efluentes:</span> <span class="fw-semibold">${rel.tipoResiduosEfluentes}</span></div>` : '',
                campo('Tipo empresa', rel.tipoEmpresa),
                campo('Objeto del proyecto', rel.objetoProyecto),
            ].join('');
        } else if (tieneRelevamiento && !rel) {
            seccionRelevamiento = '<div class="col-12 text-muted small">No se pudieron cargar los datos del relevamiento.</div>';
        }

        // Adjunta documentación
        const tieneDocumentos = documentos && documentos.length > 0;
        const seccionDoc = campo('22. Adjunta documentación', tieneDocumentos ? 'Sí' : 'No');

        // Usuario que realiza la solicitud (último elemento del historial = evento de creación)
        const eventoCreacion = historial && historial.length > 0 ? historial[historial.length - 1] : null;
        const usuarioSolicitud = eventoCreacion?.usuario || '-';
        const seccionUsuario = campo('23. Usuario que realiza la solicitud', usuarioSolicitud);

        contenido.innerHTML = seccionEmpresa + seccionSolicitud + seccionRelevamiento + seccionDoc + seccionUsuario;
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

        // Superficie solicitada dentro de la tarjeta de lote asignado
        const spanSupInLote = document.getElementById('detRadSupSolicitadaEnLote');
        if (spanSupInLote) {
            spanSupInLote.textContent = necesidadM2 ? `${necesidadM2.toLocaleString('es-AR')} m²` : '-';
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

            const compartidos = radicacionDetalleAdmin?.otrasEmpresasEnLote ?? [];
            const banner = document.getElementById('bannerLoteCompartido');
            if (banner) {
                if (compartidos.length > 0) {
                    const filas = compartidos.map(e => `
                        <tr>
                            <td>${e.empresaNombre || '-'}</td>
                            <td class="text-muted small">${e.numeroRadicado || '-'}</td>
                            <td><span class="badge bg-secondary">${e.estadoRadicacion || '-'}</span></td>
                        </tr>`).join('');
                    banner.innerHTML = `
                        <div class="alert alert-warning py-2 mb-0">
                            <div class="fw-semibold mb-1">
                                <i class="bi bi-people-fill me-1"></i>
                                Este lote también está asignado a otra${compartidos.length > 1 ? 's' : ''} solicitud${compartidos.length > 1 ? 'es' : ''}:
                            </div>
                            <table class="table table-sm table-borderless mb-0">
                                <thead><tr>
                                    <th class="small">Empresa</th>
                                    <th class="small">N° Radicado</th>
                                    <th class="small">Estado</th>
                                </tr></thead>
                                <tbody>${filas}</tbody>
                            </table>
                        </div>`;
                } else {
                    banner.innerHTML = '';
                }
            }
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
        cargar(); // actualiza la lista en segundo plano sin bloquear
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

        const estadosResolucion = ['APROBADA', 'RADICADA', 'RECHAZADA', 'CANCELADA', 'DESADJUDICACION'];
        const wrapperNroRes = document.getElementById('wrapperNumeroResolucionEstadoRad');
        if (wrapperNroRes) wrapperNroRes.classList.toggle('d-none', !estadosResolucion.includes(estado));

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
        const etiquetas = {
            RECHAZADA: 'Motivo de rechazo',
            CANCELADA: 'Motivo de cancelación',
            DESADJUDICACION: 'Motivo de desadjudicación'
        };
        const placeholders = {
            RECHAZADA: 'Ingrese el motivo de rechazo (obligatorio)',
            CANCELADA: 'Ingrese el motivo de cancelación (obligatorio)',
            DESADJUDICACION: 'Ingrese el motivo de desadjudicación (obligatorio)'
        };
        if (etiquetas[estado]) {
            label.textContent = etiquetas[estado];
            const asterisco = document.createElement('span');
            asterisco.className = 'text-danger fw-bold';
            asterisco.textContent = ' *';
            label.appendChild(asterisco);
            campo.placeholder = placeholders[estado];
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
        const requiereObservacion = nuevoEstado === 'RECHAZADA' || nuevoEstado === 'CANCELADA' || nuevoEstado === 'DESADJUDICACION';
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

        const comentario = observaciones || `Cambio de estado a ${formatearEstado(nuevoEstado)} por administración`;
        const payload = { estado: nuevoEstado, comentario };
        if (fechaAprobacion) payload.fechaAprobacion = fechaAprobacion;
        if (fechaPlazo) payload.fechaPlazo = fechaPlazo;
        if (tiempoEstimadoObraMeses) payload.tiempoEstimadoObraMeses = tiempoEstimadoObraMeses;
        const nroResolucion = document.getElementById('campoNumeroResolucionEstadoRad')?.value?.trim();
        if (nroResolucion) payload.numeroResolucion = nroResolucion;
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
        } else if (estadosSeleccionadosListado.size > 0) {
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
                const dropdown = document.getElementById('filtroEstadoRad');
                if (dropdown) dropdown.value = '';
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
            tipoEmpresa: leerTexto('relTipoEmpresa'),
            objetoProyecto: leerTexto('relObjetoProyecto'),
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

    // ── Evaluación por etapas (HU-03) ──────────────────────────────────────

    function renderizarEvaluacion(evaluacion, estadoRadicacion) {
        const esGestor = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO'])
            && !Autenticacion.tieneAcceso(['EMPRESA']);
        const bloque = document.getElementById('bloqueEvaluacionEtapasRad');
        if (!bloque || !esGestor) return;

        bloque.classList.remove('d-none');
        evaluacionActual = evaluacion;

        // Limpiar alertas
        [1, 2, 3].forEach(n => document.getElementById(`alertaEvalEtapa${n}`)?.classList.add('d-none'));

        if (evaluacion) {
            setRadioEval('evalE1C1', evaluacion.etapa1Criterio1);
            setRadioEval('evalE1C2', evaluacion.etapa1Criterio2);
            setRadioEval('evalE1C3', evaluacion.etapa1Criterio3);
            setValorEval('evalObsEtapa1', evaluacion.etapa1Observaciones);
            actualizarBadgeEtapa('badgeEtapa1Rad', evaluacion.etapa1Completa);

            setRadioEval('evalE2C1', evaluacion.etapa2Criterio1);
            setRadioEval('evalE2C2', evaluacion.etapa2Criterio2);
            setRadioEval('evalE2C3', evaluacion.etapa2Criterio3);
            setValorEval('evalObsEtapa2', evaluacion.etapa2Observaciones);
            actualizarBadgeEtapa('badgeEtapa2Rad', evaluacion.etapa2Completa);

            setRadioEval('evalE3C1', evaluacion.etapa3Criterio1);
            setRadioEval('evalE3C2', evaluacion.etapa3Criterio2);
            setRadioEval('evalE3C3', evaluacion.etapa3Criterio3);
            setValorEval('evalObsEtapa3', evaluacion.etapa3Observaciones);
            actualizarBadgeEtapa('badgeEtapa3Rad', evaluacion.etapa3Completa);

            const spanTotal = document.getElementById('puntuacionTotalEval');
            if (spanTotal) spanTotal.textContent = evaluacion.puntuacionTotal != null ? evaluacion.puntuacionTotal : '–';
        } else {
            ['evalE1C1','evalE1C2','evalE1C3','evalE2C1','evalE2C2','evalE2C3','evalE3C1','evalE3C2','evalE3C3']
                .forEach(name => document.querySelectorAll(`input[name="${name}"]`).forEach(r => { r.checked = false; }));
            ['evalObsEtapa1','evalObsEtapa2','evalObsEtapa3']
                .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
            ['badgeEtapa1Rad','badgeEtapa2Rad','badgeEtapa3Rad'].forEach(id => actualizarBadgeEtapa(id, false));
            const spanTotal = document.getElementById('puntuacionTotalEval');
            if (spanTotal) spanTotal.textContent = '–';
        }
    }

    function actualizarBadgeEtapa(id, completa) {
        const badge = document.getElementById(id);
        if (!badge) return;
        badge.className = completa ? 'badge bg-success' : 'badge bg-secondary';
    }

    function setRadioEval(name, value) {
        if (value == null) return;
        const radio = document.querySelector(`input[name="${name}"][value="${value}"]`);
        if (radio) radio.checked = true;
    }

    function setValorEval(id, valor) {
        const el = document.getElementById(id);
        if (el) el.value = valor || '';
    }

    function leerRadioEval(name) {
        const checked = document.querySelector(`input[name="${name}"]:checked`);
        return checked ? parseInt(checked.value, 10) : null;
    }

    async function guardarEtapaEvaluacion(numero) {
        if (!radicacionDetalleAdmin?.id) return;
        const alertaEl = document.getElementById(`alertaEvalEtapa${numero}`);
        alertaEl?.classList.add('d-none');

        const criterio1 = leerRadioEval(`evalE${numero}C1`);
        const criterio2 = leerRadioEval(`evalE${numero}C2`);
        const criterio3 = leerRadioEval(`evalE${numero}C3`);
        const observaciones = document.getElementById(`evalObsEtapa${numero}`)?.value?.trim() || '';

        if (!criterio1 || !criterio2 || !criterio3) {
            if (alertaEl) { alertaEl.textContent = 'Debe puntuar los tres criterios antes de guardar.'; alertaEl.classList.remove('d-none'); }
            return;
        }

        const respuesta = await ApiCliente.actualizar(
            `/api/radicaciones/${radicacionDetalleAdmin.id}/evaluacion/etapa/${numero}`,
            { criterio1, criterio2, criterio3, observaciones }
        );
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            if (alertaEl) { alertaEl.textContent = err?.mensaje || err?.message || 'No se pudo guardar la etapa.'; alertaEl.classList.remove('d-none'); }
            return;
        }

        const evaluacion = await respuesta.json().catch(() => null);
        renderizarEvaluacion(evaluacion, radicacionDetalleAdmin.estado);
        mostrarAlerta(`Etapa ${numero} guardada correctamente.`);
    }

    return {
        cargar, filtrarTexto,
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
        toggleAdjuntoRadicacion,
        subirActaRubrica,
        registrarObservacion,
        guardarEtapaEvaluacion
    };
})();

