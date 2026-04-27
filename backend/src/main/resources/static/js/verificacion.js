(async () => {
    const estado = document.getElementById('estadoVerificacion');
    const token = new URLSearchParams(window.location.search).get('token');

    if (!token) {
        estado.className = 'alert alert-danger';
        estado.textContent = 'El enlace de verificacion es invalido.';
        return;
    }

    try {
        const respuesta = await fetch('/api/public/verificacion?token=' + encodeURIComponent(token));
        const cuerpo = await respuesta.json().catch(() => ({}));
        if (respuesta.ok) {
            estado.className = 'alert alert-success';
            estado.textContent = cuerpo.mensaje || 'Correo verificado con exito. Ya puedes iniciar sesion.';
            return;
        }
        estado.className = 'alert alert-warning';
        estado.textContent = cuerpo.mensaje || 'No fue posible verificar el correo.';
    } catch (_) {
        estado.className = 'alert alert-danger';
        estado.textContent = 'No se pudo contactar al servidor para verificar el token.';
    }
})();

