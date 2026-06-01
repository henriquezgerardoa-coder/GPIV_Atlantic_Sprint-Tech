// ApiCliente — fetch con Basic Auth; redirige al login ante 401.
const ApiCliente = (() => {
    function obtenerCabeceras(incluirTipoContenido = true) {
        const credencial = sessionStorage.getItem("credencial") || "";
        const cabeceras = { Authorization: "Basic " + credencial };
        if (incluirTipoContenido)
            cabeceras["Content-Type"] = "application/json";
        return cabeceras;
    }

    async function solicitar(ruta, opciones = {}) {
        const cabeceras = opciones.headers || obtenerCabeceras();
        const respuesta = await fetch(ruta, {
            ...opciones,
            headers: cabeceras,
        });
        if (respuesta.status === 401) {
            sessionStorage.clear();
            window.location.href = "/index.html";
            return null;
        }
        return respuesta;
    }

    return {
        obtener: (ruta) => solicitar(ruta, { method: "GET" }),
        crear: (ruta, datos) =>
            solicitar(ruta, { method: "POST", body: JSON.stringify(datos) }),
        actualizar: (ruta, datos) =>
            solicitar(ruta, { method: "PUT", body: JSON.stringify(datos) }),
        parche: (ruta, datos) =>
            solicitar(ruta, { method: "PATCH", body: JSON.stringify(datos) }),
        eliminar: (ruta) => solicitar(ruta, { method: "DELETE" }),
        subirArchivo: (ruta, formData) =>
            solicitar(ruta, {
                method: "POST",
                headers: obtenerCabeceras(false),
                body: formData,
            }),
    };
})();
