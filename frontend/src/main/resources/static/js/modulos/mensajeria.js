const ModuloMensajeria = (() => {
    let conversaciones = [];
    let destinatarios = [];
    let borradores = [];
    let conversacionSeleccionadaId = null;
    let tabActiva = 'ENTRADA';
    let formularioInicializado = false;
    const CLAVE_MENSAJERIA_LEIDA = 'gpiv.mensajeria.leidas.v1';

    function esEmpresaExclusivo() {
        return Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
    }

    function formatearFecha(valor) {
        return valor ? String(valor).replace('T', ' ').slice(0, 16) : '-';
    }

    function formatearFechaRelativa(valor) {
        if (!valor) return '-';
        const fecha = new Date(String(valor).replace(' ', 'T'));
        if (isNaN(fecha)) return String(valor).slice(0, 16);
        const hoy = new Date();
        const ayer = new Date(hoy); ayer.setDate(hoy.getDate() - 1);
        if (fecha.toDateString() === hoy.toDateString())
            return fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' });
        if (fecha.toDateString() === ayer.toDateString()) return 'Ayer';
        return fecha.toLocaleDateString('es-AR', {
            day: '2-digit', month: '2-digit',
            ...(fecha.getFullYear() !== hoy.getFullYear() ? { year: '2-digit' } : {})
        });
    }

    function generarIniciales(nombre) {
        if (!nombre) return '?';
        return nombre.trim().split(/\s+/).slice(0, 2).map(p => (p[0] || '').toUpperCase()).join('');
    }

    function colorAvatar(seed) {
        const paleta = ['#4e73df','#1cc88a','#36b9cc','#f6c23e','#e74a3b','#6f42c1','#20c997','#fd7e14'];
        let h = 0;
        for (let i = 0; i < (seed || '').length; i++) h = (h * 31 + (seed || '').charCodeAt(i)) % paleta.length;
        return paleta[h];
    }

    function escaparHtml(valor) {
        return String(valor ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function mostrarAlertaSeccion(mensaje, tipo = 'danger') {
        const alerta = document.getElementById('alertaMensajeria');
        if (!alerta) return;
        alerta.className = `alert alert-${tipo} alerta-modal mb-3`;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function ocultarAlertaSeccion() {
        document.getElementById('alertaMensajeria')?.classList.add('d-none');
    }

    function mostrarAlertaModalMensajeria(mensaje) {
        const alerta = document.getElementById('alertaModalMensajeria');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function ocultarAlertaModalMensajeria() {
        document.getElementById('alertaModalMensajeria')?.classList.add('d-none');
    }

    function mostrarAlertaRespuesta(mensaje) {
        const alerta = document.getElementById('alertaRespuestaMensajeria');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    }

    function ocultarAlertaRespuesta() {
        document.getElementById('alertaRespuestaMensajeria')?.classList.add('d-none');
    }

    function claveLeidosUsuario() {
        return `${CLAVE_MENSAJERIA_LEIDA}.${Autenticacion.obtenerSesion()?.nombreUsuario || 'anon'}`;
    }

    function obtenerLeidos() {
        try {
            return JSON.parse(localStorage.getItem(claveLeidosUsuario()) || '{}') || {};
        } catch (_) {
            return {};
        }
    }

    function guardarLeidos(mapa) {
        localStorage.setItem(claveLeidosUsuario(), JSON.stringify(mapa));
    }

    function marcarComoLeida(conversacion) {
        if (!conversacion?.id) return;
        const leidos = obtenerLeidos();
        leidos[conversacion.id] = conversacion.fechaUltimaActualizacion || new Date().toISOString();
        guardarLeidos(leidos);
    }

    function esNuevaConv(conv) {
        const leidos = obtenerLeidos();
        const ultimaLectura = leidos[conv.id];
        return !ultimaLectura || String(ultimaLectura) < String(conv.fechaUltimaActualizacion || '');
    }

    function actualizarBadgeNavNoLeidos() {
        const totalNoLeidas = conversaciones.filter(conv => conv.id !== conversacionSeleccionadaId && esNuevaConv(conv)).length;
        ['badgeMensajeriaNav', 'badgeMensajeriaNavMovil'].forEach(id => {
            const badge = document.getElementById(id);
            if (!badge) return;
            badge.textContent = `${totalNoLeidas}`;
            badge.classList.toggle('d-none', totalNoLeidas <= 0);
        });
    }

    function actualizarBadgesTabs() {
        const entrada = conversaciones.filter(c => c.bandeja !== 'SALIDA').length;
        const salida = conversaciones.filter(c => c.bandeja === 'SALIDA').length;
        const badgeE = document.getElementById('badgeMensajeriaEntrada');
        const badgeS = document.getElementById('badgeMensajeriaSalida');
        const badgeB = document.getElementById('badgeMensajeriaBorradores');
        if (badgeE) badgeE.textContent = `${entrada}`;
        if (badgeS) badgeS.textContent = `${salida}`;
        if (badgeB) badgeB.textContent = `${borradores.length}`;
    }

    function seleccionarTab(tab) {
        tabActiva = tab;
        [['tabMsjEntrada','ENTRADA'], ['tabMsjSalida','SALIDA'], ['tabMsjBorradores','BORRADORES']].forEach(([id, t]) => {
            document.getElementById(id)?.classList.toggle('msj-tab-activo', t === tab);
        });
        const listaConv = document.getElementById('listaConversacionesMensajeria');
        const listaBorr = document.getElementById('listaBorradoresMensajeria');
        if (tab === 'BORRADORES') {
            listaConv?.classList.add('d-none');
            listaBorr?.classList.remove('d-none');
            limpiarDetalle();
            renderizarBorradores();
        } else {
            listaConv?.classList.remove('d-none');
            listaBorr?.classList.add('d-none');
            renderizarLista();
            const primera = conversaciones.find(c => tab === 'SALIDA' ? c.bandeja === 'SALIDA' : c.bandeja !== 'SALIDA');
            if (primera) verConversacion(primera.id, true); else limpiarDetalle();
        }
    }

    async function actualizarIndicadorNav() {
        const respuesta = await ApiCliente.obtener('/api/mensajeria/conversaciones');
        if (!respuesta?.ok) return;
        conversaciones = await respuesta.json();
        actualizarBadgeNavNoLeidos();
        actualizarBadgesTabs();
    }

    function inicializarFormularioRespuesta() {
        if (formularioInicializado) return;
        const formulario = document.getElementById('formRespuestaMensajeria');
        formulario?.addEventListener('submit', async (event) => {
            event.preventDefault();
            await enviarRespuesta();
        });
        formularioInicializado = true;
    }

    function ajustarVistaPorRol() {
        document.getElementById('btnNuevaConversacionMensajeria')?.classList.remove('d-none');
    }

    async function cargar() {
        try {
            inicializarFormularioRespuesta();
            ajustarVistaPorRol();
            ocultarAlertaSeccion();
            const respuesta = await ApiCliente.obtener('/api/mensajeria/conversaciones');
            if (!respuesta?.ok) {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaSeccion(error?.mensaje || error?.message || 'No se pudieron cargar las conversaciones.');
                return;
            }
            conversaciones = await respuesta.json();
            const respBorr = await ApiCliente.obtener('/api/mensajeria/borradores');
            if (respBorr?.ok) borradores = await respBorr.json();
            actualizarBadgesTabs();
            renderizarLista();
            actualizarBadgeNavNoLeidos();
            if (tabActiva === 'BORRADORES') {
                renderizarBorradores();
                return;
            }
            if (conversacionSeleccionadaId && conversaciones.some(c => c.id === conversacionSeleccionadaId)) {
                await verConversacion(conversacionSeleccionadaId, true);
                return;
            }
            const primera = conversaciones.find(c => tabActiva === 'SALIDA' ? c.bandeja === 'SALIDA' : c.bandeja !== 'SALIDA');
            if (primera) {
                await verConversacion(primera.id, true);
                return;
            }
            limpiarDetalle();
        } catch (error) {
            mostrarAlertaSeccion(`Error de conexión al cargar Mensajería: ${error?.message || 'desconocido'}`);
        }
    }

    function renderizarLista() {
        const lista = document.getElementById('listaConversacionesMensajeria');
        if (!lista) return;
        const filtradas = tabActiva === 'SALIDA'
            ? conversaciones.filter(c => c.bandeja === 'SALIDA')
            : conversaciones.filter(c => c.bandeja !== 'SALIDA');
        if (filtradas.length === 0) {
            lista.innerHTML = '<div class="px-3 py-4 text-center text-muted small">Sin conversaciones</div>';
            actualizarBadgeNavNoLeidos();
            return;
        }
        const sesion = Autenticacion.obtenerSesion();
        const miNombre = sesion?.nombreUsuario;
        lista.innerHTML = filtradas.map(conv => {
            const activo = conv.id === conversacionSeleccionadaId;
            const nueva = !activo && esNuevaConv(conv);
            const soySolicitante = conv.usuarioIniciadorNombre === miNombre
                || (esEmpresaExclusivo() && conv.empresaNombre && !conv.usuarioIniciadorId);
            const otroNombre = conv.consultaPublica
                ? (conv.contactoNombreEmpresa || 'Consulta externa')
                : soySolicitante
                    ? (conv.usuarioResponsableNombreCompleto || conv.usuarioResponsableNombre || '-')
                    : (conv.usuarioIniciadorNombreCompleto || conv.usuarioIniciadorNombre || conv.empresaNombre || '-');
            const etiqueta = conv.consultaPublica
                ? `<span class="badge bg-warning text-dark" style="font-size:.65rem;">Consulta pública</span>`
                : `<span style="font-size:.75rem;">${soySolicitante ? 'Para' : 'De'}: ${escaparHtml(otroNombre)}</span>`;
            const ultimoMsg = conv.ultimoMensaje
                ? escaparHtml(conv.ultimoMensaje.length > 55 ? conv.ultimoMensaje.slice(0, 55) + '…' : conv.ultimoMensaje)
                : '<em>Sin mensajes</em>';
            const color = colorAvatar(otroNombre);
            const clases = ['msj-conv-item', activo ? 'msj-activo' : '', nueva ? 'msj-no-leida' : ''].filter(Boolean).join(' ');
            return `
                <button type="button" class="${clases}" onclick="ModuloMensajeria.seleccionarConversacion(${conv.id})">
                    <div class="msj-avatar" style="background:${color};">${escaparHtml(generarIniciales(otroNombre))}</div>
                    <div class="flex-grow-1 overflow-hidden">
                        <div class="d-flex justify-content-between align-items-baseline gap-1">
                            <span class="msj-asunto text-truncate" style="font-size:.85rem;">${escaparHtml(conv.asunto || 'Sin asunto')}</span>
                            <span class="text-muted flex-shrink-0" style="font-size:.68rem;">${formatearFechaRelativa(conv.fechaUltimaActualizacion)}</span>
                        </div>
                        <div class="text-muted text-truncate mt-1">${etiqueta}</div>
                        <div class="text-muted text-truncate" style="font-size:.75rem;">${ultimoMsg}</div>
                    </div>
                    ${nueva ? '<span class="position-absolute top-0 end-0 mt-2 me-2 badge rounded-pill bg-primary" style="font-size:.6rem;">Nuevo</span>' : ''}
                </button>`;
        }).join('');
        actualizarBadgeNavNoLeidos();
    }

    async function seleccionarConversacion(id) {
        await verConversacion(id, false);
    }

    async function verConversacion(id, silencioso = false) {
        conversacionSeleccionadaId = id;
        renderizarLista();
        const respuesta = await ApiCliente.obtener(`/api/mensajeria/conversaciones/${id}`);
        if (!respuesta?.ok) {
            if (!silencioso) mostrarAlertaSeccion('No se pudo cargar la conversación.');
            limpiarDetalle();
            return;
        }
        const conversacion = await respuesta.json();
        renderizarDetalle(conversacion);
    }

    function renderizarDetalle(conversacion) {
        const panelVacio = document.getElementById('panelSinConversacionMensajeria');
        const panelDetalle = document.getElementById('detalleConversacionMensajeria');
        if (!panelVacio || !panelDetalle) return;

        panelVacio.classList.add('d-none');
        panelVacio.classList.remove('flex-grow-1');
        panelDetalle.classList.remove('d-none');

        const sesion = Autenticacion.obtenerSesion();
        const miNombre = sesion?.nombreUsuario;
        const soySolicitante = conversacion?.usuarioIniciadorNombre === miNombre
            || (esEmpresaExclusivo() && conversacion?.empresaNombre && !conversacion?.usuarioIniciadorId);
        const otroParticipante = conversacion?.consultaPublica
            ? (conversacion?.contactoNombreEmpresa || 'Consulta externa')
            : soySolicitante
                ? (conversacion?.usuarioResponsableNombreCompleto || conversacion?.usuarioResponsableNombre || '-')
                : (conversacion?.usuarioIniciadorNombreCompleto || conversacion?.usuarioIniciadorNombre || conversacion?.empresaNombre || '-');

        document.getElementById('tituloConversacionMensajeria').textContent = conversacion?.asunto || '-';

        const subtitulo = document.getElementById('subtituloConversacionMensajeria');
        if (conversacion?.consultaPublica) {
            subtitulo.innerHTML = `
                <span class="badge bg-warning text-dark me-2">Consulta pública</span>
                <span>${escaparHtml(conversacion.contactoNombreEmpresa || '-')}</span>
                ${conversacion.contactoCorreoElectronico ? `<span class="ms-2"><i class="bi bi-envelope me-1"></i>${escaparHtml(conversacion.contactoCorreoElectronico)}</span>` : ''}
                ${conversacion.contactoTelefono ? `<span class="ms-2"><i class="bi bi-telephone me-1"></i>${escaparHtml(conversacion.contactoTelefono)}</span>` : ''}
                <span class="ms-2 text-muted">Responsable: ${escaparHtml(conversacion.usuarioResponsableNombreCompleto || conversacion.usuarioResponsableNombre || '-')}</span>`;
        } else {
            const colorOtro = colorAvatar(otroParticipante);
            subtitulo.innerHTML = `
                <span class="d-inline-flex align-items-center gap-2">
                    <span class="msj-avatar-sm" style="background:${colorOtro};">${escaparHtml(generarIniciales(otroParticipante))}</span>
                    <span>${soySolicitante ? 'Para' : 'De'}: <strong>${escaparHtml(otroParticipante)}</strong></span>
                    <span class="text-muted ms-2" style="font-size:.75rem;">${formatearFechaRelativa(conversacion?.fechaUltimaActualizacion)}</span>
                </span>`;
        }

        const contenedor = document.getElementById('contenedorMensajesMensajeria');
        const mensajes = conversacion?.mensajes || [];
        if (!mensajes.length) {
            contenedor.innerHTML = '<div class="text-muted small text-center py-5">Sin mensajes aún.</div>';
        } else {
            contenedor.innerHTML = mensajes.map(msg => {
                const esMio = !msg.emisorExterno && miNombre && msg.usuarioEmisorNombre === miNombre;
                const nombreMsg = esMio ? 'Yo'
                    : msg.emisorExterno ? (msg.emisorExternoNombre || 'Externo')
                    : (msg.usuarioEmisorNombreCompleto || msg.usuarioEmisorNombre || '-');
                const iniciales = generarIniciales(esMio ? (sesion?.nombreCompleto || miNombre || 'Yo') : nombreMsg);
                const colorMsg = esMio ? '#0d6efd' : colorAvatar(nombreMsg);
                const contenidoHtml = escaparHtml(msg.contenido || '').replace(/\n/g, '<br>');
                const horaMsg = `<span style="font-size:.68rem;" class="text-muted">${formatearFechaRelativa(msg.fechaEnvio)}</span>`;
                if (esMio) {
                    return `
                        <div class="d-flex justify-content-end align-items-end gap-2 mb-3">
                            <div style="max-width:68%;">
                                <div class="msj-burbuja-salida">${contenidoHtml}</div>
                                <div class="text-end mt-1">${horaMsg}</div>
                            </div>
                            <div class="msj-avatar-sm" style="background:${colorMsg};">${escaparHtml(iniciales)}</div>
                        </div>`;
                } else {
                    const clsBurbuja = msg.emisorExterno ? 'msj-burbuja-externo' : 'msj-burbuja-entrada';
                    return `
                        <div class="d-flex justify-content-start align-items-end gap-2 mb-3">
                            <div class="msj-avatar-sm" style="background:${colorMsg};">${escaparHtml(iniciales)}</div>
                            <div style="max-width:68%;">
                                <div class="mb-1" style="font-size:.72rem;color:#6c757d;font-weight:600;">${escaparHtml(nombreMsg)}</div>
                                <div class="${clsBurbuja}">${contenidoHtml}</div>
                                <div class="mt-1">${horaMsg}</div>
                            </div>
                        </div>`;
                }
            }).join('');
        }
        contenedor.scrollTop = contenedor.scrollHeight;
        marcarComoLeida(conversacion);
        ocultarAlertaRespuesta();
        renderizarLista();
        actualizarBadgeNavNoLeidos();
    }

    function limpiarDetalle() {
        conversacionSeleccionadaId = null;
        const panelVacio = document.getElementById('panelSinConversacionMensajeria');
        const panelDetalle = document.getElementById('detalleConversacionMensajeria');
        panelDetalle?.classList.add('d-none');
        if (panelVacio) { panelVacio.classList.remove('d-none'); panelVacio.classList.add('flex-grow-1'); }
        const titulo = document.getElementById('tituloConversacionMensajeria');
        const subtitulo = document.getElementById('subtituloConversacionMensajeria');
        if (titulo) titulo.textContent = '-';
        if (subtitulo) subtitulo.innerHTML = '';
        const contenedor = document.getElementById('contenedorMensajesMensajeria');
        if (contenedor) contenedor.innerHTML = '';
    }

    async function cargarDestinatarios() {
        const selector = document.getElementById('selectorDestinatarioMensajeria');
        if (!selector) return;
        selector.innerHTML = '<option value="">Cargando destinatarios...</option>';
        const respuesta = await ApiCliente.obtener('/api/mensajeria/destinatarios');
        if (!respuesta?.ok) {
            selector.innerHTML = '<option value="">No se pudieron cargar los destinatarios</option>';
            return;
        }
        destinatarios = await respuesta.json();
        if (!destinatarios.length) {
            selector.innerHTML = '<option value="">No hay destinatarios disponibles</option>';
            return;
        }
        selector.innerHTML = '<option value="">-- Seleccione destinatario --</option>' + destinatarios.map(d => {
            const roles = (d.roles || []).join(', ');
            return `<option value="${d.id}">${escaparHtml(d.nombreCompleto || d.nombreUsuario || 'Usuario')} (${escaparHtml(roles)})</option>`;
        }).join('');
    }

    async function abrirNuevaConversacion() {
        ocultarAlertaModalMensajeria();
        const formulario = document.getElementById('formNuevaConversacionMensajeria');
        formulario?.reset();
        const campoId = document.getElementById('campoIdBorradorActivo');
        if (campoId) campoId.value = '';
        await cargarDestinatarios();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevaConversacionMensajeria')).show();
    }

    async function guardarNuevaConversacion() {
        ocultarAlertaModalMensajeria();
        const usuarioResponsableId = document.getElementById('selectorDestinatarioMensajeria')?.value;
        const asunto = document.getElementById('campoAsuntoMensajeria')?.value.trim();
        const mensaje = document.getElementById('campoMensajeInicialMensajeria')?.value.trim();
        const destinatarioId = parseInt(usuarioResponsableId);
        if (!destinatarioId) {
            mostrarAlertaModalMensajeria('Debe seleccionar un destinatario.');
            return;
        }
        if (!asunto || !mensaje) {
            mostrarAlertaModalMensajeria('Complete asunto y mensaje.');
            return;
        }
        const borradorActivoId = document.getElementById('campoIdBorradorActivo')?.value
            ? parseInt(document.getElementById('campoIdBorradorActivo').value) : null;
        try {
            const respuesta = await ApiCliente.crear('/api/mensajeria/conversaciones', {
                usuarioResponsableId: destinatarioId,
                asunto,
                mensaje
            });
            if (!respuesta?.ok) {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaModalMensajeria(error?.mensaje || error?.message || `No se pudo crear la conversación (HTTP ${respuesta?.status || 'N/A'}).`);
                return;
            }
            if (borradorActivoId) {
                await ApiCliente.eliminar(`/api/mensajeria/borradores/${borradorActivoId}`);
            }
            const creada = await respuesta.json();
            bootstrap.Modal.getInstance(document.getElementById('modalNuevaConversacionMensajeria'))?.hide();
            tabActiva = 'SALIDA';
            conversacionSeleccionadaId = creada.id;
            await cargar();
        } catch (error) {
            mostrarAlertaModalMensajeria(`Error de conexión al crear la conversación: ${error?.message || 'desconocido'}`);
        }
    }

    async function guardarComoBorrador() {
        ocultarAlertaModalMensajeria();
        const usuarioResponsableId = document.getElementById('selectorDestinatarioMensajeria')?.value;
        const asunto = document.getElementById('campoAsuntoMensajeria')?.value.trim();
        const contenido = document.getElementById('campoMensajeInicialMensajeria')?.value.trim();
        const borradorActivoId = document.getElementById('campoIdBorradorActivo')?.value
            ? parseInt(document.getElementById('campoIdBorradorActivo').value) : null;
        const url = borradorActivoId
            ? `/api/mensajeria/borradores/${borradorActivoId}`
            : '/api/mensajeria/borradores';
        const metodo = borradorActivoId ? ApiCliente.actualizar : ApiCliente.crear;
        try {
            const respuesta = await metodo(url, {
                destinatarioId: usuarioResponsableId ? parseInt(usuarioResponsableId) : null,
                asunto: asunto || null,
                contenido: contenido || null
            });
            if (!respuesta?.ok) {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaModalMensajeria(error?.mensaje || error?.message || 'No se pudo guardar el borrador.');
                return;
            }
            bootstrap.Modal.getInstance(document.getElementById('modalNuevaConversacionMensajeria'))?.hide();
            tabActiva = 'BORRADORES';
            await cargar();
        } catch (error) {
            mostrarAlertaModalMensajeria(`Error al guardar borrador: ${error?.message || 'desconocido'}`);
        }
    }

    function renderizarBorradores() {
        const lista = document.getElementById('listaBorradoresMensajeria');
        if (!lista) return;
        if (borradores.length === 0) {
            lista.innerHTML = '<div class="px-3 py-4 text-center text-muted small">Sin borradores guardados</div>';
            return;
        }
        lista.innerHTML = borradores.map(b => {
            const color = colorAvatar(b.destinatarioNombre || '?');
            const iniciales = generarIniciales(b.destinatarioNombre || '?');
            return `
                <div class="msj-conv-item" style="cursor:default;">
                    <div class="msj-avatar" style="background:${color};opacity:.55;">${escaparHtml(iniciales)}</div>
                    <div class="flex-grow-1 overflow-hidden">
                        <div class="d-flex justify-content-between align-items-baseline gap-1">
                            <span class="msj-asunto text-truncate text-muted" style="font-size:.85rem;">${escaparHtml(b.asunto || '(Sin asunto)')}</span>
                            <span class="text-muted flex-shrink-0" style="font-size:.68rem;">${formatearFechaRelativa(b.fechaUltimaModificacion)}</span>
                        </div>
                        <div class="text-muted text-truncate" style="font-size:.75rem;">
                            Para: ${b.destinatarioNombre ? escaparHtml(b.destinatarioNombre) : '<em>sin destinatario</em>'}
                        </div>
                        <div class="text-muted text-truncate" style="font-size:.73rem;">${escaparHtml((b.contenido || '').slice(0, 55))}${(b.contenido || '').length > 55 ? '…' : ''}</div>
                        <div class="d-flex gap-2 mt-2">
                            <button class="btn btn-sm btn-outline-secondary py-0 px-2" style="font-size:.75rem;"
                                onclick="ModuloMensajeria.editarBorrador(${b.id})">
                                <i class="bi bi-pencil me-1"></i>Editar
                            </button>
                            <button class="btn btn-sm btn-primary py-0 px-2" style="font-size:.75rem;"
                                onclick="ModuloMensajeria.enviarBorrador(${b.id})">
                                <i class="bi bi-send me-1"></i>Enviar
                            </button>
                            <button class="btn btn-sm btn-outline-danger py-0 px-2 ms-auto" style="font-size:.75rem;"
                                onclick="ModuloMensajeria.eliminarBorrador(${b.id})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>`;
        }).join('');
    }

    async function editarBorrador(id) {
        const borrador = borradores.find(b => b.id === id);
        if (!borrador) return;
        await cargarDestinatarios();
        const selector = document.getElementById('selectorDestinatarioMensajeria');
        const asuntoEl = document.getElementById('campoAsuntoMensajeria');
        const mensajeEl = document.getElementById('campoMensajeInicialMensajeria');
        if (selector && borrador.destinatarioId) selector.value = borrador.destinatarioId;
        if (asuntoEl) asuntoEl.value = borrador.asunto || '';
        if (mensajeEl) mensajeEl.value = borrador.contenido || '';
        document.getElementById('campoIdBorradorActivo')?.setAttribute('value', id);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevaConversacionMensajeria')).show();
    }

    async function enviarBorrador(id) {
        if (!confirm('¿Enviar este borrador como nueva conversación?')) return;
        const respuesta = await ApiCliente.crear(`/api/mensajeria/borradores/${id}/enviar`, {});
        if (!respuesta?.ok) {
            const err = await respuesta?.json().catch(() => ({}));
            mostrarAlertaSeccion(err?.mensaje || err?.message || 'No se pudo enviar el borrador.');
            return;
        }
        const conv = await respuesta.json();
        tabActiva = 'SALIDA';
        conversacionSeleccionadaId = conv.id;
        await cargar();
    }

    async function eliminarBorrador(id) {
        if (!confirm('¿Eliminar este borrador?')) return;
        const respuesta = await ApiCliente.eliminar(`/api/mensajeria/borradores/${id}`);
        if (!respuesta?.ok) {
            mostrarAlertaSeccion('No se pudo eliminar el borrador.');
            return;
        }
        borradores = borradores.filter(b => b.id !== id);
        actualizarBadgesTabs();
        renderizarBorradores();
    }

    async function enviarRespuesta() {
        ocultarAlertaRespuesta();
        if (!conversacionSeleccionadaId) {
            mostrarAlertaRespuesta('Seleccione una conversación primero.');
            return;
        }
        const mensaje = document.getElementById('campoRespuestaMensajeria')?.value.trim();
        if (!mensaje) {
            mostrarAlertaRespuesta('Escriba un mensaje antes de enviar.');
            return;
        }
        try {
            const respuesta = await ApiCliente.crear(`/api/mensajeria/conversaciones/${conversacionSeleccionadaId}/mensajes`, { mensaje });
            if (!respuesta?.ok) {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaRespuesta(error?.mensaje || error?.message || `No se pudo enviar la respuesta (HTTP ${respuesta?.status || 'N/A'}).`);
                return;
            }
            document.getElementById('campoRespuestaMensajeria').value = '';
            await cargar();
        } catch (error) {
            mostrarAlertaRespuesta(`Error de conexión al enviar respuesta: ${error?.message || 'desconocido'}`);
        }
    }

    return {
        cargar,
        abrirNuevaConversacion,
        guardarNuevaConversacion,
        guardarComoBorrador,
        seleccionarConversacion,
        seleccionarTab,
        editarBorrador,
        enviarBorrador,
        eliminarBorrador,
        actualizarIndicadorNav
    };
})();


