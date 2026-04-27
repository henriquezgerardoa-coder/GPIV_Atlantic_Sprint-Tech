// Redirige si ya hay sesión activa
if (sessionStorage.getItem('credencial')) {
    window.location.href = '/app.html';
}

document.getElementById('formularioIngreso').addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const nombreUsuario   = document.getElementById('nombreUsuario').value.trim();
    const clave           = document.getElementById('claveAcceso').value;
    const btnIngresar     = document.getElementById('btnIngresar');
    const mensajeError    = document.getElementById('mensajeError');
    const textoError      = document.getElementById('textoError');

    if (!nombreUsuario || !clave) return;

    btnIngresar.disabled = true;
    btnIngresar.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Verificando...';
    mensajeError.classList.add('d-none');

    const credencial = btoa(`${nombreUsuario}:${clave}`);

    try {
        const respuesta = await fetch('/api/yo', {
            headers: { 'Authorization': 'Basic ' + credencial }
        });

        if (respuesta.ok) {
            const usuario = await respuesta.json();
            sessionStorage.setItem('credencial', credencial);
            sessionStorage.setItem('usuario', JSON.stringify(usuario));
            window.location.href = '/app.html';
        } else {
            const detalle = await respuesta.json().catch(() => ({}));
            if (respuesta.status === 401) {
                textoError.textContent = 'Usuario/correo o contrasena incorrectos.';
            } else if (respuesta.status === 429) {
                textoError.textContent = detalle.mensaje || 'Demasiados intentos fallidos. Intente mas tarde.';
            } else {
                textoError.textContent = detalle.mensaje || 'Error al iniciar sesion. Intente nuevamente.';
            }
            mensajeError.classList.remove('d-none');
        }
    } catch (_) {
        textoError.textContent = 'No se pudo conectar con el servidor.';
        mensajeError.classList.remove('d-none');
    } finally {
        btnIngresar.disabled = false;
        btnIngresar.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i>Ingresar';
    }
});

