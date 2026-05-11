/**
 * ApiCliente — Envuelve fetch añadiendo cabeceras de autenticación Basic Auth.
 * Redirige al login automáticamente ante 401 (sesión expirada/inválida).
 */
const ApiCliente = (() => {

    function obtenerCabeceras() {
        const credencial = sessionStorage.getItem('credencial') || '';
        return {
            'Content-Type': 'application/json',
            'Authorization': 'Basic ' + credencial
        };
    }

    function obtenerCabecerasSinTipoContenido() {
        const credencial = sessionStorage.getItem('credencial') || '';
        return {
            'Authorization': 'Basic ' + credencial
        };
    }

    async function solicitar(ruta, opciones = {}) {
        const cabeceras = opciones.headers || obtenerCabeceras();
        const respuesta = await fetch(ruta, {
            ...opciones,
            headers: cabeceras
        });
        if (respuesta.status === 401) {
            sessionStorage.clear();
            window.location.href = '/ingreso.html';
            return null;
        }
        return respuesta;
    }

    return {
        obtener:    (ruta)        => solicitar(ruta, { method: 'GET' }),
        crear:      (ruta, datos) => solicitar(ruta, { method: 'POST',  body: JSON.stringify(datos) }),
        actualizar: (ruta, datos) => solicitar(ruta, { method: 'PUT',   body: JSON.stringify(datos) }),
        parche:     (ruta, datos) => solicitar(ruta, { method: 'PATCH', body: JSON.stringify(datos) }),
        eliminar:   (ruta)        => solicitar(ruta, { method: 'DELETE' }),
        subirArchivo: (ruta, formData) => solicitar(ruta, {
            method: 'POST',
            headers: obtenerCabecerasSinTipoContenido(),
            body: formData
        })
    };
})();
