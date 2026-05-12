/**
 * Autenticacion — Gestiona sesión, roles y redirecciones.
 */
const Autenticacion = (() => {

    function obtenerSesion() {
        const datos = sessionStorage.getItem('usuario');
        return datos ? JSON.parse(datos) : null;
    }

    function cerrarSesion() {
        sessionStorage.clear();
        window.location.href = '/index.html';
    }

    function esAdministrador() {
        return tieneRol('ADMINISTRADOR');
    }

    function tieneRol(rol) {
        return obtenerSesion()?.roles?.includes(rol) ?? false;
    }

    function tieneAcceso(rolesPermitidos) {
        const sesion = obtenerSesion();
        if (!sesion?.roles) return false;
        return rolesPermitidos.some(r => sesion.roles.includes(r));
    }

    function verificarYRedirigir() {
        if (!sessionStorage.getItem('credencial')) {
            window.location.href = '/ingreso.html';
            return false;
        }
        return true;
    }

    return { obtenerSesion, cerrarSesion, esAdministrador, tieneRol, tieneAcceso, verificarYRedirigir };
})();
