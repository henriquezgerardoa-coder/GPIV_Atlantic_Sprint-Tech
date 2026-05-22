const ModuloCenso = (() => {
    let empresaIdActual = null;
    let anioActual = new Date().getFullYear();

    async function cargar() {
        const campoPeriodo = document.getElementById('campoPeriodoCenso');
        if (campoPeriodo && !campoPeriodo.value) campoPeriodo.value = anioActual;

        const sesion = Autenticacion.obtenerSesion();
        const esEmpresa = Autenticacion.tieneAcceso(['EMPRESA']) && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);

        if (esEmpresa) {
            empresaIdActual = sesion.empresaId;
            document.getElementById('bloqueSelectorEmpresaCenso')?.classList.add('d-none');
            if (empresaIdActual) {
                await cargarDatosCenso(empresaIdActual);
            } else {
                mostrarMensaje('No tiene empresa asociada.', 'warning');
            }
        } else {
            document.getElementById('bloqueSelectorEmpresaCenso')?.classList.remove('d-none');
            await poblarSelectorEmpresas();
        }
    }

    async function poblarSelectorEmpresas() {
        const resp = await ApiCliente.obtener('/api/empresas');
        if (!resp?.ok) return;
        const empresas = await resp.json();
        const selector = document.getElementById('selectorEmpresaCenso');
        if (!selector) return;
        selector.innerHTML = '<option value="">Seleccionar empresa...</option>'
            + empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    async function seleccionarEmpresa() {
        const selector = document.getElementById('selectorEmpresaCenso');
        const id = parseInt(selector?.value);
        if (!id) return;
        empresaIdActual = id;
        await cargarDatosCenso(id);
    }

    async function cargarDatosCenso(empresaId) {
        ocultarMensaje();
        document.getElementById('contenidoCenso')?.classList.remove('d-none');
        await Promise.all([
            cargarDeclaraciones(empresaId),
            cargarPersonal(empresaId),
            cargarVehiculos(empresaId)
        ]);
    }

    // ── Declaraciones ──────────────────────────────────────────────────────

    async function cargarDeclaraciones(empresaId) {
        const resp = await ApiCliente.obtener(`/api/empresas/${empresaId}/censo`);
        const cuerpo = document.getElementById('cuerpoTablaCenso');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="5" class="text-danger">Error al cargar declaraciones</td></tr>';
            return;
        }
        const censos = await resp.json();
        cuerpo.innerHTML = censos.length
            ? censos.map(c => `
                <tr>
                    <td class="ps-3 fw-semibold">${c.anioPeriodo}</td>
                    <td>${c.fechaDeclaracion || '-'}</td>
                    <td>${c.cantidadPersonalRegistrado}</td>
                    <td>${c.cantidadPersonasNoRegistrado}</td>
                    <td class="fw-semibold">${c.totalEmpleados}</td>
                </tr>`).join('')
            : '<tr><td colspan="5" class="text-muted text-center py-3">Sin declaraciones registradas</td></tr>';
    }

    async function declararCenso() {
        if (!empresaIdActual) return;
        const alerta = document.getElementById('alertaDeclaracionCenso');
        alerta?.classList.add('d-none');

        const anioPeriodo = parseInt(document.getElementById('campoPeriodoCenso').value);
        const cantRegistrado = parseInt(document.getElementById('campoCantRegistrado').value) || 0;
        const cantNoRegistrado = parseInt(document.getElementById('campoCantNoRegistrado').value) || 0;
        const observacion = document.getElementById('campoObservacionCenso').value.trim();

        if (!anioPeriodo) {
            alerta.textContent = 'Ingrese el año de período.';
            alerta?.classList.remove('d-none');
            return;
        }

        const resp = await ApiCliente.crear(`/api/empresas/${empresaIdActual}/censo`, {
            anioPeriodo, cantidadPersonalRegistrado: cantRegistrado,
            cantidadPersonasNoRegistrado: cantNoRegistrado, observacion
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo guardar la declaración.';
            alerta?.classList.remove('d-none');
            return;
        }
        document.getElementById('formDeclaracionCenso').reset();
        document.getElementById('campoPeriodoCenso').value = anioActual;
        mostrarAlerta('Declaración de censo guardada.');
        await cargarDeclaraciones(empresaIdActual);
    }

    // ── Personal ───────────────────────────────────────────────────────────

    async function cargarPersonal(empresaId) {
        const resp = await ApiCliente.obtener(`/api/empresas/${empresaId}/censo/personal`);
        const cuerpo = document.getElementById('cuerpoTablaPersonal');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="4" class="text-danger">Error al cargar personal</td></tr>';
            return;
        }
        const personal = await resp.json();
        const puedeEditar = !Autenticacion.tieneAcceso(['DIRECTIVO'])
            || Autenticacion.tieneAcceso(['ADMINISTRADOR']);
        cuerpo.innerHTML = personal.length
            ? personal.map(p => `
                <tr>
                    <td class="ps-3">${p.cuit}</td>
                    <td>${p.nombreCompleto}</td>
                    <td>${p.fechaIngreso || '-'}</td>
                    <td class="text-center">
                        ${puedeEditar
                            ? `<button class="btn btn-sm btn-outline-danger"
                                onclick="ModuloCenso.eliminarPersonal(${p.id})">
                                <i class="bi bi-trash"></i>
                               </button>`
                            : '<span class="text-muted">-</span>'}
                    </td>
                </tr>`).join('')
            : '<tr><td colspan="4" class="text-muted text-center py-3">Sin personal registrado</td></tr>';
    }

    async function agregarPersonal() {
        if (!empresaIdActual) return;
        const alerta = document.getElementById('alertaPersonalCenso');
        alerta?.classList.add('d-none');

        const cuit = document.getElementById('campoCuitPersonal').value.trim();
        const nombreCompleto = document.getElementById('campoNombrePersonal').value.trim();
        const fechaIngreso = document.getElementById('campoFechaIngresoPersonal').value;

        if (!cuit || !nombreCompleto || !fechaIngreso) {
            alerta.textContent = 'Complete todos los campos del empleado.';
            alerta?.classList.remove('d-none');
            return;
        }

        const resp = await ApiCliente.crear(
            `/api/empresas/${empresaIdActual}/censo/personal`,
            { cuit, nombreCompleto, fechaIngreso }
        );
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo agregar el empleado.';
            alerta?.classList.remove('d-none');
            return;
        }
        document.getElementById('formPersonalCenso').reset();
        mostrarAlerta('Empleado agregado al registro.');
        await cargarPersonal(empresaIdActual);
    }

    async function eliminarPersonal(personalId) {
        if (!empresaIdActual) return;
        if (!confirm('¿Confirma la eliminación del empleado?')) return;
        const resp = await ApiCliente.eliminar(
            `/api/empresas/${empresaIdActual}/censo/personal/${personalId}`
        );
        if (!resp?.ok) {
            mostrarAlerta('No se pudo eliminar el empleado.', 'danger');
            return;
        }
        mostrarAlerta('Empleado eliminado del registro.');
        await cargarPersonal(empresaIdActual);
    }

    // ── Vehículos ──────────────────────────────────────────────────────────

    async function cargarVehiculos(empresaId) {
        const resp = await ApiCliente.obtener(`/api/empresas/${empresaId}/censo/vehiculos`);
        const cuerpo = document.getElementById('cuerpoTablaVehiculos');
        if (!cuerpo) return;
        if (!resp?.ok) {
            cuerpo.innerHTML = '<tr><td colspan="4" class="text-danger">Error al cargar vehículos</td></tr>';
            return;
        }
        const vehiculos = await resp.json();
        const puedeEditar = !Autenticacion.tieneAcceso(['DIRECTIVO'])
            || Autenticacion.tieneAcceso(['ADMINISTRADOR']);
        cuerpo.innerHTML = vehiculos.length
            ? vehiculos.map(v => `
                <tr>
                    <td class="ps-3 fw-semibold">${v.patente}</td>
                    <td>${v.marcaModelo}</td>
                    <td class="text-center">
                        ${v.patenteValida
                            ? '<span class="badge bg-success">Válida</span>'
                            : '<span class="badge bg-warning text-dark">Revisar</span>'}
                    </td>
                    <td class="text-center">
                        ${puedeEditar
                            ? `<button class="btn btn-sm btn-outline-danger"
                                onclick="ModuloCenso.eliminarVehiculo(${v.id})">
                                <i class="bi bi-trash"></i>
                               </button>`
                            : '<span class="text-muted">-</span>'}
                    </td>
                </tr>`).join('')
            : '<tr><td colspan="4" class="text-muted text-center py-3">Sin vehículos registrados</td></tr>';
    }

    async function agregarVehiculo() {
        if (!empresaIdActual) return;
        const alerta = document.getElementById('alertaVehiculoCenso');
        alerta?.classList.add('d-none');

        const patente = document.getElementById('campoPatenteVehiculo').value.trim();
        const marcaModelo = document.getElementById('campoMarcaModeloVehiculo').value.trim();

        if (!patente || !marcaModelo) {
            alerta.textContent = 'Complete la patente y la marca/modelo.';
            alerta?.classList.remove('d-none');
            return;
        }

        const resp = await ApiCliente.crear(
            `/api/empresas/${empresaIdActual}/censo/vehiculos`,
            { patente, marcaModelo }
        );
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            alerta.textContent = err?.mensaje || err?.message || 'No se pudo agregar el vehículo.';
            alerta?.classList.remove('d-none');
            return;
        }
        document.getElementById('formVehiculoCenso').reset();
        mostrarAlerta('Vehículo agregado a la flota.');
        await cargarVehiculos(empresaIdActual);
    }

    async function eliminarVehiculo(vehiculoId) {
        if (!empresaIdActual) return;
        if (!confirm('¿Confirma la eliminación del vehículo?')) return;
        const resp = await ApiCliente.eliminar(
            `/api/empresas/${empresaIdActual}/censo/vehiculos/${vehiculoId}`
        );
        if (!resp?.ok) {
            mostrarAlerta('No se pudo eliminar el vehículo.', 'danger');
            return;
        }
        mostrarAlerta('Vehículo eliminado de la flota.');
        await cargarVehiculos(empresaIdActual);
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    function mostrarMensaje(texto, tipo = 'info') {
        const el = document.getElementById('mensajeCenso');
        if (!el) return;
        el.className = `alert alert-${tipo}`;
        el.textContent = texto;
        el.classList.remove('d-none');
        document.getElementById('contenidoCenso')?.classList.add('d-none');
    }

    function ocultarMensaje() {
        document.getElementById('mensajeCenso')?.classList.add('d-none');
    }

    return {
        cargar,
        seleccionarEmpresa,
        declararCenso,
        agregarPersonal,
        eliminarPersonal,
        agregarVehiculo,
        eliminarVehiculo
    };
})();
