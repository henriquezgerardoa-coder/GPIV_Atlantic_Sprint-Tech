const Credenciales = (() => {
    function convertirBytesABase64(bytes) {
        if (typeof btoa === "function") {
            let binario = "";
            bytes.forEach((byte) => {
                binario += String.fromCharCode(byte);
            });
            return btoa(binario);
        }
        if (typeof Buffer !== "undefined") {
            return Buffer.from(bytes).toString("base64");
        }
        throw new Error("No hay un codificador Base64 disponible.");
    }

    function codificarBasicAuth(identificador, clave) {
        const valor = `${identificador}:${clave}`;
        const bytes = new TextEncoder().encode(valor);
        return convertirBytesABase64(bytes);
    }

    return {
        codificarBasicAuth,
    };
})();

if (typeof window !== "undefined") {
    window.Credenciales = Credenciales;
}
