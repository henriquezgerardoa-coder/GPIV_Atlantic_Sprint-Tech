(() => {
    const formulario = document.getElementById("formConsultaPublica");
    const mensaje = document.getElementById("mensajeConsultaPublica");
    const boton = document.getElementById("btnEnviarConsultaPublica");

    if (!formulario || !mensaje || !boton) return;

    function mostrarMensaje(texto, tipo) {
        mensaje.className = `alert alert-${tipo}`;
        mensaje.textContent = texto;
        mensaje.classList.remove("d-none");
    }

    function obtenerDatosFormulario() {
        return {
            nombreOEmpresa: document
                .getElementById("consultaNombreEmpresa")
                ?.value.trim(),
            correoElectronico: document
                .getElementById("consultaCorreo")
                ?.value.trim(),
            telefono:
                document.getElementById("consultaTelefono")?.value.trim() ||
                null,
            asunto: document.getElementById("consultaAsunto")?.value.trim(),
            mensaje: document.getElementById("consultaMensaje")?.value.trim(),
        };
    }

    formulario.addEventListener("submit", async (event) => {
        event.preventDefault();
        mensaje.classList.add("d-none");

        const datos = obtenerDatosFormulario();
        if (
            !datos.nombreOEmpresa ||
            !datos.correoElectronico ||
            !datos.asunto ||
            !datos.mensaje
        ) {
            mostrarMensaje(
                "Complete los campos obligatorios para enviar la consulta.",
                "danger",
            );
            return;
        }

        boton.disabled = true;
        const textoOriginal = boton.textContent;
        boton.textContent = "Enviando...";

        try {
            const respuesta = await fetch("/api/public/consulta", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(datos),
            });

            if (!respuesta.ok) {
                const error = await respuesta.json().catch(() => ({}));
                mostrarMensaje(
                    error?.mensaje ||
                        error?.message ||
                        "No se pudo enviar la consulta. Intente nuevamente.",
                    "danger",
                );
                return;
            }

            formulario.reset();
            mostrarMensaje(
                "Consulta enviada correctamente. Un Admin/Directivo la revisará en breve.",
                "success",
            );
        } catch (_) {
            mostrarMensaje(
                "No se pudo enviar la consulta por un error de conexión.",
                "danger",
            );
        } finally {
            boton.disabled = false;
            boton.textContent = textoOriginal;
        }
    });
})();
