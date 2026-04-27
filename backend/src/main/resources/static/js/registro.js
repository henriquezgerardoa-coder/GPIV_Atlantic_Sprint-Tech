const formularioRegistro = document.getElementById('formularioRegistro');
const formularioReenvio = document.getElementById('formularioReenvio');
const mensajeRegistro = document.getElementById('mensajeRegistro');

function mostrarMensaje(texto, tipo) {
    mensajeRegistro.className = 'alert alert-' + tipo;
    mensajeRegistro.textContent = texto;
    mensajeRegistro.classList.remove('d-none');
}

async function manejarRespuesta(respuesta) {
    const cuerpo = await respuesta.json().catch(() => ({}));
    return cuerpo.mensaje || 'No se pudo completar la operacion';
}

formularioRegistro.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const correoElectronico = document.getElementById('correoElectronico').value.trim();
    const clave = document.getElementById('clave').value;
    const confirmacionClave = document.getElementById('confirmacionClave').value;
    const btnRegistrar = document.getElementById('btnRegistrar');

    if (!correoElectronico || !clave || !confirmacionClave) {
        return;
    }

    btnRegistrar.disabled = true;
    btnRegistrar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Registrando...';

    try {
        const respuesta = await fetch('/api/public/registro', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correoElectronico, clave, confirmacionClave })
        });

        const mensaje = await manejarRespuesta(respuesta);
        if (respuesta.ok) {
            formularioRegistro.reset();
            document.getElementById('correoReenvio').value = correoElectronico;
            mostrarMensaje(mensaje, 'success');
            return;
        }
        mostrarMensaje(mensaje, 'danger');
    } catch (_) {
        mostrarMensaje('No se pudo conectar con el servidor.', 'danger');
    } finally {
        btnRegistrar.disabled = false;
        btnRegistrar.innerHTML = '<i class="bi bi-person-plus me-2"></i>Registrarme';
    }
});

formularioReenvio.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const correoElectronico = document.getElementById('correoReenvio').value.trim();
    const btnReenviar = document.getElementById('btnReenviar');
    if (!correoElectronico) {
        return;
    }

    btnReenviar.disabled = true;

    try {
        const respuesta = await fetch('/api/public/verificacion/reenviar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correoElectronico })
        });
        const mensaje = await manejarRespuesta(respuesta);
        mostrarMensaje(mensaje, respuesta.ok ? 'success' : 'warning');
    } catch (_) {
        mostrarMensaje('No se pudo reenviar el correo en este momento.', 'danger');
    } finally {
        btnReenviar.disabled = false;
    }
});

