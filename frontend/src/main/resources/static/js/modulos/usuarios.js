const ModuloUsuarios = (() => {
    let usuarios    = [];
    let empresas    = [];
    let modoEdicion = false;
    let idEdicion   = null;

    const COLOR_ROL = {
        ADMINISTRADOR: 'bg-danger',
        DIRECTIVO:     'bg-warning text-dark',
        SECRETARIO:    'bg-info text-dark',
        EMPRESA:       'bg-secondary'
    };

    async function cargar() {
        const [respUsuarios, respEmpresas] = await Promise.all([
            ApiCliente.obtener('/api/usuarios'),
            ApiCliente.obtener('/api/empresas')
        ]);
        if (!respUsuarios?.ok) { mostrarAlerta('Error al cargar usuarios.', 'danger'); return; }
        usuarios = await respUsuarios.json();
        empresas = respEmpresas?.ok ? await respEmpresas.json() : [];
        poblarSelectorEmpresas();
        renderizarTabla();
    }

    function poblarSelectorEmpresas() {
        const selector = document.getElementById('selectorEmpresaUsuario');
        if (!selector) return;
        const valorActual = selector.value;
        selector.innerHTML = '<option value="">-- Seleccione empresa --</option>' +
            empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
        selector.value = valorActual;
    }

    function renderizarTabla() {
        const cuerpo = document.getElementById('cuerpoTablaUsuarios');
        if (!cuerpo) return;

        if (usuarios.length === 0) {
            cuerpo.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin usuarios registrados</td></tr>';
            return;
        }

        cuerpo.innerHTML = usuarios.map(u => {
            const rolesHtml = [...u.roles]
                .map(r => `<span class="badge ${COLOR_ROL[r] || 'bg-secondary'} me-1">${r}</span>`)
                .join('');
            return `
            <tr>
                <td class="text-muted">${u.id}</td>
                <td class="fw-semibold">${u.nombreUsuario}</td>
                <td>${u.nombreCompleto}</td>
                <td>${rolesHtml}</td>
                <td class="text-center">
                    <span class="badge rounded-pill ${u.activo ? 'bg-success' : 'bg-secondary'}">
                        ${u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary me-1" title="Editar"
                            onclick="ModuloUsuarios.abrirEdicion(${u.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-warning me-1" title="Restablecer contraseña"
                            onclick="ModuloUsuarios.abrirRestablecerClave(${u.id}, '${u.nombreUsuario}')">
                        <i class="bi bi-key"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" title="Eliminar"
                            onclick="ModuloUsuarios.confirmarEliminacion(${u.id}, '${u.nombreUsuario}')">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>`;
        }).join('');
    }

    function abrirCreacion() {
        modoEdicion = false;
        idEdicion   = null;
        document.getElementById('tituloModalUsuario').textContent   = 'Nuevo usuario';
        document.getElementById('formularioUsuario').reset();
        document.getElementById('campoNombreUsuario').disabled      = false;
        document.getElementById('grupoClaveUsuario').classList.remove('d-none');
        document.querySelectorAll('.check-rol-usuario').forEach(cb => { cb.checked = false; });
        document.getElementById('selectorEmpresaUsuario').value = '';
        ocultarAlertaModal('alertaModalUsuario');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    }

    function abrirEdicion(id) {
        const usuario = usuarios.find(u => u.id === id);
        if (!usuario) return;
        modoEdicion = true;
        idEdicion   = id;
        document.getElementById('tituloModalUsuario').textContent   = 'Editar usuario';
        document.getElementById('campoNombreUsuario').value         = usuario.nombreUsuario;
        document.getElementById('campoNombreUsuario').disabled      = true;
        document.getElementById('campoNombreCompleto').value        = usuario.nombreCompleto;
        document.getElementById('campoActivoUsuario').checked       = usuario.activo;
        document.getElementById('grupoClaveUsuario').classList.add('d-none');
        document.querySelectorAll('.check-rol-usuario').forEach(cb => {
            cb.checked = [...usuario.roles].includes(cb.value);
        });
        document.getElementById('selectorEmpresaUsuario').value = usuario.empresaId || '';
        ocultarAlertaModal('alertaModalUsuario');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    }

    async function guardar() {
        const rolesSeleccionados = [...document.querySelectorAll('.check-rol-usuario:checked')]
            .map(cb => cb.value);
        const empresaId = document.getElementById('selectorEmpresaUsuario').value;

        if (rolesSeleccionados.length === 0) {
            mostrarAlertaModal('alertaModalUsuario', 'Debe seleccionar al menos un rol.');
            return;
        }
        if (rolesSeleccionados.includes('EMPRESA') && !empresaId) {
            mostrarAlertaModal('alertaModalUsuario', 'Debe seleccionar una empresa para el rol EMPRESA.');
            return;
        }

        let respuesta;
        if (modoEdicion) {
            const datos = {
                nombreCompleto: document.getElementById('campoNombreCompleto').value.trim(),
                activo:         document.getElementById('campoActivoUsuario').checked,
                roles:          rolesSeleccionados,
                empresaId:      empresaId ? parseInt(empresaId) : null
            };
            if (!datos.nombreCompleto) {
                mostrarAlertaModal('alertaModalUsuario', 'El nombre completo es obligatorio.');
                return;
            }
            respuesta = await ApiCliente.actualizar(`/api/usuarios/${idEdicion}`, datos);
        } else {
            const datos = {
                nombreUsuario:  document.getElementById('campoNombreUsuario').value.trim(),
                nombreCompleto: document.getElementById('campoNombreCompleto').value.trim(),
                clave:          document.getElementById('campoClaveUsuario').value,
                activo:         document.getElementById('campoActivoUsuario').checked,
                roles:          rolesSeleccionados,
                empresaId:      empresaId ? parseInt(empresaId) : null
            };
            if (!datos.nombreUsuario || !datos.nombreCompleto || !datos.clave) {
                mostrarAlertaModal('alertaModalUsuario', 'Todos los campos son obligatorios.');
                return;
            }
            respuesta = await ApiCliente.crear('/api/usuarios', datos);
        }

        if (respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalUsuario'))?.hide();
            await cargar();
            mostrarAlerta(modoEdicion ? 'Usuario actualizado correctamente.' : 'Usuario creado correctamente.');
        } else {
            const error = await respuesta?.json().catch(() => ({}));
            mostrarAlertaModal('alertaModalUsuario', error?.message || 'Error al guardar el usuario.');
        }
    }

    function abrirRestablecerClave(id, nombre) {
        document.getElementById('nombreUsuarioRestablecerClave').textContent = nombre;
        document.getElementById('formularioRestablecerClave').reset();
        document.getElementById('idUsuarioRestablecerClave').value = id;
        ocultarAlertaModal('alertaModalRestablecerClave');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalRestablecerClave')).show();
    }

    async function restablecerClave() {
        const id           = document.getElementById('idUsuarioRestablecerClave').value;
        const claveNueva   = document.getElementById('campoClavaNuevaRest').value;
        const claveConfirm = document.getElementById('campoClaveConfirmRest').value;

        if (!claveNueva || claveNueva !== claveConfirm) {
            mostrarAlertaModal('alertaModalRestablecerClave', 'Las contraseñas no coinciden o están vacías.');
            return;
        }

        const respuesta = await ApiCliente.parche(`/api/usuarios/${id}/clave`, { claveNueva });
        if (respuesta?.status === 204 || respuesta?.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalRestablecerClave'))?.hide();
            mostrarAlerta('Contraseña restablecida correctamente.');
        } else {
            mostrarAlertaModal('alertaModalRestablecerClave', 'Error al restablecer la contraseña.');
        }
    }

    function confirmarEliminacion(id, nombre) {
        document.getElementById('mensajeConfirmacionEliminar').textContent =
            `¿Está seguro que desea eliminar el usuario "${nombre}"?`;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarId   = id;
        document.getElementById('btnConfirmarEliminar').dataset.eliminarTipo = 'usuario';
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalConfirmacion')).show();
    }

    async function eliminar(id) {
        const respuesta = await ApiCliente.eliminar(`/api/usuarios/${id}`);
        if (respuesta?.status === 204 || respuesta?.ok) {
            await cargar();
            mostrarAlerta('Usuario eliminado correctamente.');
        } else {
            mostrarAlerta('No se pudo eliminar el usuario.', 'danger');
        }
    }

    return { cargar, abrirCreacion, abrirEdicion, guardar, abrirRestablecerClave, restablecerClave, confirmarEliminacion, eliminar };
})();

