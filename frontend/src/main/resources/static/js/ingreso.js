function irAplicacion() {
    if (window.top && window.top !== window) {
        window.top.location.href = "/app.html";
        return;
    }
    window.location.href = "/app.html";
}

// Redirige si ya hay sesión activa
if (sessionStorage.getItem("credencial")) {
    irAplicacion();
}

const mensajeContexto = document.getElementById("mensajeContexto");
const campoNombreUsuario = document.getElementById("nombreUsuario");

function mostrarMensajeContexto(texto, tipo) {
    if (!texto) {
        mensajeContexto.className = "alert d-none";
        mensajeContexto.textContent = "";
        return;
    }
    mensajeContexto.className = "alert alert-" + tipo;
    mensajeContexto.textContent = texto;
    mensajeContexto.classList.remove("d-none");
}

function inicializarContextoIngreso() {
    const parametros = new URLSearchParams(window.location.search);
    const usuario = parametros.get("usuario");
    const correo = parametros.get("correo");
    const verificacion = parametros.get("verificacion");
    const registro = parametros.get("registro");

    if (usuario && !campoNombreUsuario.value.trim()) {
        campoNombreUsuario.value = usuario;
    } else if (correo && !campoNombreUsuario.value.trim()) {
        campoNombreUsuario.value = correo;
    }

    if (verificacion === "ok") {
        mostrarMensajeContexto(
            "Correo verificado con exito. Ya puedes iniciar sesion.",
            "success",
        );
    } else if (registro === "ok") {
        mostrarMensajeContexto(
            "Registro completado. Ya puedes iniciar sesion con tu usuario y contrasena.",
            "info",
        );
    }

    if ([usuario, correo, verificacion, registro].some(Boolean)) {
        const nuevaRuta = window.location.pathname;
        window.history.replaceState({}, document.title, nuevaRuta);
    }
}

inicializarContextoIngreso();

document
    .getElementById("formularioIngreso")
    .addEventListener("submit", async (evento) => {
        evento.preventDefault();

        const nombreUsuario = campoNombreUsuario.value.trim();
        const clave = document.getElementById("claveAcceso").value;
        const btnIngresar = document.getElementById("btnIngresar");
        const mensajeError = document.getElementById("mensajeError");
        const textoError = document.getElementById("textoError");

        if (!nombreUsuario || !clave) return;

        btnIngresar.disabled = true;
        btnIngresar.innerHTML =
            '<span class="spinner-border spinner-border-sm me-2"></span>Verificando...';
        mensajeError.classList.add("d-none");

        const credencial = Credenciales.codificarBasicAuth(
            nombreUsuario,
            clave,
        );

        try {
            const respuesta = await fetch("/api/yo", {
                headers: { Authorization: "Basic " + credencial },
            });

            if (respuesta.ok) {
                const usuario = await respuesta.json();
                sessionStorage.setItem("credencial", credencial);
                sessionStorage.setItem("usuario", JSON.stringify(usuario));
                irAplicacion();
            } else {
                const detalle = await respuesta.json().catch(() => ({}));
                if (respuesta.status === 401) {
                    textoError.textContent =
                        "Usuario/correo o contrasena incorrectos.";
                } else if (respuesta.status === 429) {
                    textoError.textContent =
                        detalle.mensaje ||
                        "Demasiados intentos fallidos. Intente mas tarde.";
                } else {
                    textoError.textContent =
                        detalle.mensaje ||
                        "Error al iniciar sesion. Intente nuevamente.";
                }
                mensajeError.classList.remove("d-none");
            }
        } catch (_) {
            textoError.textContent = "No se pudo conectar con el servidor.";
            mensajeError.classList.remove("d-none");
        } finally {
            btnIngresar.disabled = false;
            btnIngresar.innerHTML =
                '<i class="bi bi-box-arrow-in-right me-2"></i>Ingresar';
        }
    });
