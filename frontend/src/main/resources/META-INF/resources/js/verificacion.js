(async () => {
    const estado = document.getElementById('estadoVerificacion');
    const titulo = document.getElementById('tituloVerificacion');
    const ayuda = document.getElementById('ayudaVerificacion');
    const accion = document.getElementById('accionVerificacion');
    const icono = document.getElementById('iconoVerificacion');
    const token = new URLSearchParams(window.location.search).get('token');

    function actualizarVista(tipo, mensaje, opciones = {}) {
        const { tituloTexto, textoAccion, hrefAccion, iconoHtml } = opciones;
        estado.className = 'alert alert-' + tipo;
        estado.textContent = mensaje;
        if (tituloTexto) {
            titulo.textContent = tituloTexto;
        }
        if (textoAccion) {
            accion.textContent = textoAccion;
        }
        if (hrefAccion) {
            accion.href = hrefAccion;
        }
        if (iconoHtml) {
            icono.innerHTML = iconoHtml;
        }
        icono.classList.remove('estado-exito', 'estado-alerta', 'estado-error');
        if (tipo === 'success') {
            icono.classList.add('estado-exito');
        } else if (tipo === 'warning') {
            icono.classList.add('estado-alerta');
        } else if (tipo === 'danger') {
            icono.classList.add('estado-error');
        }
    }

    if (!token) {
        actualizarVista('danger', 'El enlace de verificacion es invalido.', {
            tituloTexto: 'No se pudo validar el enlace',
            textoAccion: 'Volver al registro',
            hrefAccion: '/registro.html',
            iconoHtml: '<i class="bi bi-x-circle-fill"></i>'
        });
        ayuda.textContent = 'Solicita un nuevo correo de verificacion desde la pantalla de registro.';
        return;
    }

    try {
        const respuesta = await fetch('/api/public/verificacion?token=' + encodeURIComponent(token));
        const cuerpo = await respuesta.json().catch(() => ({}));
        if (respuesta.ok) {
            actualizarVista('success', cuerpo.mensaje || 'Correo verificado con exito. Ya puedes iniciar sesion.', {
                tituloTexto: 'Correo verificado',
                textoAccion: 'Ir a iniciar sesion',
                hrefAccion: '/ingreso.html?verificacion=ok',
                iconoHtml: '<i class="bi bi-check-circle-fill"></i>'
            });
            ayuda.textContent = 'Tu cuenta ya esta activa. Continúa al ingreso para acceder al sistema.';
            return;
        }
        const mensaje = cuerpo.mensaje || cuerpo.message || 'No fue posible verificar el correo.';
        const esExpirado = respuesta.status === 410;
        actualizarVista(esExpirado ? 'warning' : 'danger', mensaje, {
            tituloTexto: esExpirado ? 'El enlace expiro' : 'No se pudo verificar el correo',
            textoAccion: 'Solicitar nuevo correo',
            hrefAccion: '/registro.html',
            iconoHtml: '<i class="bi bi-envelope-exclamation-fill"></i>'
        });
        ayuda.textContent = esExpirado
            ? 'Puedes reenviar la verificacion desde la pantalla de registro usando tu correo.'
            : 'Si el problema persiste, vuelve a solicitar el enlace o contacta a soporte.';
    } catch (_) {
        actualizarVista('danger', 'No se pudo contactar al servidor para verificar el token.', {
            tituloTexto: 'Conexion no disponible',
            textoAccion: 'Intentar mas tarde',
            hrefAccion: '/ingreso.html',
            iconoHtml: '<i class="bi bi-wifi-off"></i>'
        });
        ayuda.textContent = 'Verifica tu conexion y vuelve a intentarlo en unos minutos.';
    }
})();
