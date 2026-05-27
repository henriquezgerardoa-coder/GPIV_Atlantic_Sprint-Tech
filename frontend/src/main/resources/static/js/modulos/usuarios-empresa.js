const ModuloUsuariosEmpresa = (() => {

    async function cargar() {
        const resp = await ApiCliente.obtener('/api/usuarios/mi-empresa');
        const cuerpo = document.getElementById('cuerpoTablaUsuariosEmpresa');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Error al cargar usuarios.</td></tr>';
            return;
        }
        const usuarios = await resp.json();
        if (usuarios.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3"><i class="bi bi-inbox me-2"></i>Sin usuarios registrados</td></tr>';
            return;
        }
        cuerpo.innerHTML = usuarios.map(u => `
            <tr>
                <td class="fw-semibold">${u.nombreUsuario}</td>
                <td>${u.nombreCompleto}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${u.activo ? 'bg-success' : 'bg-secondary'}">
                        ${u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td class="text-muted small">${u.autorizadoPor || '—'}</td>
            </tr>`).join('');
    }

    function abrirCreacion() {
        document.getElementById('formularioUsuarioEmpresa').reset();
        ocultarAlertaModal('alertaModalUsuarioEmpresa');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuarioEmpresa')).show();
    }

    async function guardar() {
        const nombreUsuario   = document.getElementById('campoNombreUsuarioEmpresa').value.trim();
        const nombreCompleto  = document.getElementById('campoNombreCompletoEmpresa').value.trim();
        const clave           = document.getElementById('campoClaveEmpresa').value;

        if (!nombreUsuario || !nombreCompleto || !clave) {
            mostrarAlertaModal('alertaModalUsuarioEmpresa', 'Todos los campos son obligatorios.');
            return;
        }

        const respuesta = await ApiCliente.crear('/api/usuarios/empresa', {
            nombreUsuario,
            nombreCompleto,
            clave,
            activo: true,
            roles: ['EMPRESA']
        });

        if (respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalUsuarioEmpresa'))?.hide();
            await cargar();
            mostrarAlerta('Usuario agregado correctamente.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalUsuarioEmpresa', error?.message || 'Error al agregar el usuario.');
        }
    }

    return { cargar, abrirCreacion, guardar };
})();