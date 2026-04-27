const formularioRegistro = document.getElementById('formularioRegistro');
const formularioReenvio = document.getElementById('formularioReenvio');
const mensajeRegistro = document.getElementById('mensajeRegistro');
const campoCorreo = document.getElementById('correoElectronico');
const campoClave = document.getElementById('clave');
const campoConfirmacion = document.getElementById('confirmacionClave');
const campoCorreoReenvio = document.getElementById('correoReenvio');
const errorCorreoReenvio = document.getElementById('errorCorreoReenvio');
const botonToggleClave = document.getElementById('toggleClave');
const botonToggleConfirmacion = document.getElementById('toggleConfirmacion');
const reqLongitud = document.getElementById('req-longitud');
const reqMinuscula = document.getElementById('req-minuscula');
const reqMayuscula = document.getElementById('req-mayuscula');
const reqNumero = document.getElementById('req-numero');
const textoFortaleza = document.getElementById('textoFortaleza');
const barraFortaleza = document.getElementById('barraFortaleza');

const regexCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const regexClave = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/;

function mostrarMensaje(texto, tipo) {
    mensajeRegistro.className = 'alert alert-' + tipo;
    mensajeRegistro.textContent = texto;
    mensajeRegistro.classList.remove('d-none');
}

function limpiarMensaje() {
    mensajeRegistro.classList.add('d-none');
}

async function manejarRespuesta(respuesta) {
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (cuerpo.mensaje) {
        return cuerpo.mensaje;
    }
    if (respuesta.status === 409) {
        return 'Ese correo ya se encuentra registrado.';
    }
    if (respuesta.status === 429) {
        return 'Demasiados intentos. Intenta nuevamente en unos minutos.';
    }
    return 'No se pudo completar la operacion';
}

function marcarCampo(campo, esValido) {
    campo.classList.toggle('is-valid', esValido);
    campo.classList.toggle('is-invalid', !esValido);
}

function marcarRequisito(elemento, cumple) {
    elemento.classList.toggle('cumplido', cumple);
}

function actualizarFortalezaClave(valor) {
    let puntaje = 0;
    const cumpleLongitud = valor.length >= 8 && valor.length <= 72;
    const cumpleMinuscula = /[a-z]/.test(valor);
    const cumpleMayuscula = /[A-Z]/.test(valor);
    const cumpleNumero = /\d/.test(valor);

    if (cumpleLongitud) puntaje++;
    if (cumpleMinuscula) puntaje++;
    if (cumpleMayuscula) puntaje++;
    if (cumpleNumero) puntaje++;

    marcarRequisito(reqLongitud, cumpleLongitud);
    marcarRequisito(reqMinuscula, cumpleMinuscula);
    marcarRequisito(reqMayuscula, cumpleMayuscula);
    marcarRequisito(reqNumero, cumpleNumero);

    if (!valor) {
        textoFortaleza.textContent = 'Baja';
        barraFortaleza.style.width = '20%';
        barraFortaleza.className = 'progress-bar bg-danger';
        barraFortaleza.setAttribute('aria-valuenow', '20');
        return;
    }

    if (puntaje <= 2) {
        textoFortaleza.textContent = 'Baja';
        barraFortaleza.style.width = '35%';
        barraFortaleza.className = 'progress-bar bg-danger';
        barraFortaleza.setAttribute('aria-valuenow', '35');
    } else if (puntaje === 3) {
        textoFortaleza.textContent = 'Media';
        barraFortaleza.style.width = '65%';
        barraFortaleza.className = 'progress-bar bg-warning';
        barraFortaleza.setAttribute('aria-valuenow', '65');
    } else {
        textoFortaleza.textContent = 'Alta';
        barraFortaleza.style.width = '100%';
        barraFortaleza.className = 'progress-bar bg-success';
        barraFortaleza.setAttribute('aria-valuenow', '100');
    }
}

function validarCorreo() {
    const valor = campoCorreo.value.trim();
    const valido = regexCorreo.test(valor);
    marcarCampo(campoCorreo, valido);
    return valido;
}

function validarClave() {
    const valor = campoClave.value;
    actualizarFortalezaClave(valor);
    const valido = regexClave.test(valor);
    marcarCampo(campoClave, valido);
    return valido;
}

function validarConfirmacion() {
    const valido = campoConfirmacion.value.length > 0 && campoConfirmacion.value === campoClave.value;
    marcarCampo(campoConfirmacion, valido);
    return valido;
}

function alternarVisibilidad(campo, boton) {
    const esPassword = campo.type === 'password';
    campo.type = esPassword ? 'text' : 'password';
    boton.innerHTML = `<i class="bi ${esPassword ? 'bi-eye-slash' : 'bi-eye'}"></i>`;
}

campoCorreo.addEventListener('blur', validarCorreo);
campoClave.addEventListener('input', () => {
    validarClave();
    if (campoConfirmacion.value) {
        validarConfirmacion();
    }
});
campoConfirmacion.addEventListener('input', validarConfirmacion);
campoCorreoReenvio.addEventListener('input', () => {
    campoCorreoReenvio.classList.remove('is-invalid');
    errorCorreoReenvio.classList.add('d-none');
});

botonToggleClave.addEventListener('click', () => alternarVisibilidad(campoClave, botonToggleClave));
botonToggleConfirmacion.addEventListener('click', () => alternarVisibilidad(campoConfirmacion, botonToggleConfirmacion));

actualizarFortalezaClave('');

formularioRegistro.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const correoElectronico = campoCorreo.value.trim();
    const clave = campoClave.value;
    const confirmacionClave = campoConfirmacion.value;
    const btnRegistrar = document.getElementById('btnRegistrar');

    const correoValido = validarCorreo();
    const claveValida = validarClave();
    const confirmacionValida = validarConfirmacion();

    if (!correoValido || !claveValida || !confirmacionValida) {
        mostrarMensaje('Revisa los campos marcados para continuar.', 'warning');
        return;
    }

    limpiarMensaje();
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
            [campoCorreo, campoClave, campoConfirmacion].forEach((campo) => {
                campo.classList.remove('is-valid', 'is-invalid');
            });
            campoCorreoReenvio.value = correoElectronico;
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

    const correoElectronico = campoCorreoReenvio.value.trim();
    const btnReenviar = document.getElementById('btnReenviar');
    if (!regexCorreo.test(correoElectronico)) {
        campoCorreoReenvio.classList.add('is-invalid');
        errorCorreoReenvio.classList.remove('d-none');
        mostrarMensaje('Ingresa un correo valido para reenviar la verificacion.', 'warning');
        return;
    }

    campoCorreoReenvio.classList.remove('is-invalid');
    errorCorreoReenvio.classList.add('d-none');
    limpiarMensaje();
    btnReenviar.disabled = true;
    btnReenviar.textContent = 'Enviando...';

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
        btnReenviar.textContent = 'Reenviar';
    }
});

