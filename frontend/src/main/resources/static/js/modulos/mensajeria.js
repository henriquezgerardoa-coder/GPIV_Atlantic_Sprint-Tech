const ModuloMensajeria = (() => {
    let conversaciones = [];
    let destinatarios = [];
    let conversacionSeleccionadaId = null;
    let formularioInicializado = false;
    const CLAVE_MENSAJERIA_LEIDA = 'gpiv.mensajeria.leidas.v1';

    function esEmpresaExclusivo() {
        return Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);
    }

    function formatearFecha(valor) {
        return valor ? String(valor).replace('T', ' ').slice(0, 16) : '-';
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

    async function actualizarIndicadorNav() {
        const respuesta = await ApiCliente.obtener('/api/mensajeria/conversaciones');
        if (!respuesta?.ok) return;
        conversaciones = await respuesta.json();
        actualizarBadgeNavNoLeidos();
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
            renderizarLista();
            actualizarBadgeNavNoLeidos();
            if (conversacionSeleccionadaId && conversaciones.some(c => c.id === conversacionSeleccionadaId)) {
                await verConversacion(conversacionSeleccionadaId, true);
                return;
            }
            if (conversaciones.length > 0) {
                await verConversacion(conversaciones[0].id, true);
                return;
            }
            limpiarDetalle();
        } catch (error) {
            mostrarAlertaSeccion(`Error de conexión al cargar Mensajería: ${error?.message || 'desconocido'}`);
        }
    }

    function renderizarLista() {
        const lista = document.getElementById('listaConversacionesMensajeria');
        const badge = document.getElementById('badgeTotalConversacionesMensajeria');
        if (!lista || !badge) return;
        badge.textContent = `${conversaciones.length}`;
        if (conversaciones.length === 0) {
            lista.innerHTML = '<div class="list-group-item text-muted small">Sin conversaciones</div>';
            actualizarBadgeNavNoLeidos();
            return;
        }
        const sesionMsj = Autenticacion.obtenerSesion();
        const miNombre = sesionMsj?.nombreUsuario;
        lista.innerHTML = conversaciones.map(conv => {
            const activo = conv.id === conversacionSeleccionadaId ? ' active' : '';
            const nueva = conv.id !== conversacionSeleccionadaId && esNuevaConv(conv);
            const soySolicitante = conv.usuarioIniciadorNombre === miNombre
                || (esEmpresaExclusivo() && conv.empresaNombre && !conv.usuarioIniciadorId);
            const destinatario = conv.consultaPublica
                ? `Consulta pública: ${escaparHtml(conv.contactoNombreEmpresa || 'Sin nombre')}`
                : soySolicitante
                    ? `Para: ${escaparHtml(conv.usuarioResponsableNombreCompleto || conv.usuarioResponsableNombre || '-')}`
                    : `De: ${escaparHtml(conv.usuarioIniciadorNombreCompleto || conv.usuarioIniciadorNombre || conv.empresaNombre || '-')}`;
            const ultimoMensaje = conv.ultimoMensaje
                ? escaparHtml(conv.ultimoMensaje.length > 95 ? conv.ultimoMensaje.slice(0, 95) + '…' : conv.ultimoMensaje)
                : 'Sin mensajes';
            return `
                <button type="button" class="list-group-item list-group-item-action position-relative${activo}${nueva ? ' border-start border-4 border-primary bg-primary bg-opacity-10' : ''}" onclick="ModuloMensajeria.seleccionarConversacion(${conv.id})">
                    ${nueva ? '<span class="badge bg-primary position-absolute top-0 end-0 mt-2 me-2">Nuevo</span>' : ''}
                    <div class="d-flex justify-content-between align-items-start gap-2">
                        <div class="flex-grow-1 text-start">
                            <div class="fw-semibold">${escaparHtml(conv.asunto || 'Sin asunto')}</div>
                            <div class="small text-muted">${destinatario}</div>
                            <div class="small text-muted mt-1">${ultimoMensaje}</div>
                        </div>
                        <div class="text-end flex-shrink-0 small text-muted">
                            <div>${formatearFecha(conv.fechaUltimaActualizacion)}</div>
                            <div class="badge bg-light text-dark mt-1">${conv.totalMensajes ?? 0}</div>
                        </div>
                    </div>
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
        panelDetalle.classList.remove('d-none');

        document.getElementById('tituloConversacionMensajeria').textContent = conversacion?.asunto || '-';
        const sesionDet = Autenticacion.obtenerSesion();
        const miNombreDet = sesionDet?.nombreUsuario;
        const soySolicitanteDet = conversacion?.usuarioIniciadorNombre === miNombreDet
            || (esEmpresaExclusivo() && conversacion?.empresaNombre && !conversacion?.usuarioIniciadorId);
        const otroParticipante = soySolicitanteDet
            ? (conversacion?.usuarioResponsableNombreCompleto || conversacion?.usuarioResponsableNombre || '-')
            : (conversacion?.usuarioIniciadorNombreCompleto || conversacion?.usuarioIniciadorNombre || conversacion?.empresaNombre || '-');
        document.getElementById('subtituloConversacionMensajeria').textContent = conversacion?.consultaPublica
            ? `Consulta pública de: ${conversacion?.contactoNombreEmpresa || '-'} | Correo: ${conversacion?.contactoCorreoElectronico || '-'} | Teléfono: ${conversacion?.contactoTelefono || '-'} | Responsable: ${conversacion?.usuarioResponsableNombreCompleto || conversacion?.usuarioResponsableNombre || '-'} | Última actualización: ${formatearFecha(conversacion?.fechaUltimaActualizacion)}`
            : `Con: ${otroParticipante} | Última actualización: ${formatearFecha(conversacion?.fechaUltimaActualizacion)}`;

        const contenedor = document.getElementById('contenedorMensajesMensajeria');
        const mensajes = conversacion?.mensajes || [];
        const sesion = Autenticacion.obtenerSesion();
        if (!mensajes.length) {
            contenedor.innerHTML = '<div class="text-muted small text-center py-4">Sin mensajes aún.</div>';
        } else {
            contenedor.innerHTML = mensajes.map(msg => {
                const esMio = !msg.emisorExterno && sesion?.nombreUsuario && msg.usuarioEmisorNombre === sesion.nombreUsuario;
                const alineacion = esMio ? 'justify-content-end' : 'justify-content-start';
                const color = esMio ? 'bg-primary text-white' : msg.emisorExterno ? 'bg-warning-subtle border border-warning' : 'bg-white border';
                const nombre = esMio
                    ? 'Yo'
                    : msg.emisorExterno
                        ? `${escaparHtml(msg.emisorExternoNombre || 'Consulta externa')}`
                        : `${escaparHtml(msg.usuarioEmisorNombreCompleto || msg.usuarioEmisorNombre || '-')}`;
                const contenido = escaparHtml(msg.contenido || '').replace(/\n/g, '<br>');
                return `
                    <div class="d-flex ${alineacion} mb-2">
                        <div class="rounded p-2 ${color}" style="max-width: 85%; min-width: 180px;">
                            <div class="small fw-semibold mb-1">${nombre}</div>
                            <div>${contenido}</div>
                            <div class="small mt-1 ${esMio ? 'text-white-50' : 'text-muted'}">${formatearFecha(msg.fechaEnvio)}</div>
                        </div>
                    </div>`;
            }).join('');
        }
        contenedor.scrollTop = contenedor.scrollHeight;
        marcarComoLeida(conversacion);
        ocultarAlertaRespuesta();
        renderizarLista();
        actualizarBadgeNavNoLeidos();
    }

    function limpiarDetalle() {
        const panelVacio = document.getElementById('panelSinConversacionMensajeria');
        const panelDetalle = document.getElementById('detalleConversacionMensajeria');
        panelDetalle?.classList.add('d-none');
        panelVacio?.classList.remove('d-none');
        document.getElementById('tituloConversacionMensajeria').textContent = '-';
        document.getElementById('subtituloConversacionMensajeria').textContent = '-';
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
        try {
            const respuesta = await ApiCliente.crear('/api/mensajeria/conversaciones', {
                usuarioResponsableId: usuarioResponsableId ? parseInt(usuarioResponsableId) : null,
                asunto,
                mensaje
            });
            if (!respuesta?.ok) {
                const error = await respuesta?.json().catch(() => ({}));
                mostrarAlertaModalMensajeria(error?.mensaje || error?.message || `No se pudo crear la conversación (HTTP ${respuesta?.status || 'N/A'}).`);
                return;
            }
            const creada = await respuesta.json();
            bootstrap.Modal.getInstance(document.getElementById('modalNuevaConversacionMensajeria'))?.hide();
            conversacionSeleccionadaId = creada.id;
            await cargar();
        } catch (error) {
            mostrarAlertaModalMensajeria(`Error de conexión al crear la conversación: ${error?.message || 'desconocido'}`);
        }
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
        seleccionarConversacion,
        actualizarIndicadorNav
    };
})();


