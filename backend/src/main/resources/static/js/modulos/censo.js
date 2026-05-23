const ModuloCenso = (() => {
    let _empresaId = null;
    let _esGestor = false;

    async function cargar() {
        _esGestor = !Autenticacion.tieneAcceso(['EMPRESA']) ||
            Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO']);

        const bloqueSelector = document.getElementById('bloqueSelectorEmpresaCenso');
        const contenido = document.getElementById('contenidoCenso');

        if (_esGestor) {
            bloqueSelector?.classList.remove('d-none');
            contenido?.classList.add('d-none');
            await _cargarSelectorEmpresas();
        } else {
            bloqueSelector?.classList.add('d-none');
            const sesion = Autenticacion.obtenerSesion();
            _empresaId = sesion.empresaId;
            if (_empresaId) {
                contenido?.classList.remove('d-none');
                _aplicarVisibilidadRol();
                await Promise.all([_cargarDeclaraciones(), _cargarPersonal(), _cargarVehiculos()]);
            }
        }
    }

    function _aplicarVisibilidadRol() {
        // EMPRESA gestiona sus datos; ADMIN/DIRECTIVO solo lectura
        document.getElementById('bloqueFormDeclaracion')?.classList.toggle('d-none', _esGestor);
        document.getElementById('bloqueFormPersonal')?.classList.toggle('d-none', _esGestor);
        document.getElementById('bloqueFormVehiculo')?.classList.toggle('d-none', _esGestor);
        // Para personal: ADMIN/DIRECTIVO ven solo el conteo
        document.getElementById('resumenPersonalGestor')?.classList.toggle('d-none', !_esGestor);
        document.getElementById('bloqueListaPersonal')?.classList.toggle('d-none', _esGestor);
        // Columna Acción en flota vehicular
        const cabecera = document.getElementById('cabeceraTablaVehiculos');
        if (cabecera) {
            const thAccion = cabecera.querySelector('th:last-child');
            if (thAccion) thAccion.classList.toggle('d-none', _esGestor);
        }
    }

    async function _cargarSelectorEmpresas() {
        const selector = document.getElementById('selectorEmpresaCenso');
        if (!selector) return;
        const resp = await ApiCliente.obtener('/api/empresas');
        if (!resp?.ok) return;
        const empresas = await resp.json();
        selector.innerHTML = '<option value="">Seleccionar empresa...</option>'
            + empresas.map(e => `<option value="${e.id}">${e.nombre}</option>`).join('');
    }

    async function seleccionarEmpresa() {
        const selector = document.getElementById('selectorEmpresaCenso');
        _empresaId = selector?.value ? parseInt(selector.value) : null;
        const contenido = document.getElementById('contenidoCenso');
        if (!_empresaId) { contenido?.classList.add('d-none'); return; }
        contenido?.classList.remove('d-none');
        _aplicarVisibilidadRol();
        await Promise.all([_cargarDeclaraciones(), _cargarPersonal(), _cargarVehiculos()]);
    }

    async function _cargarDeclaraciones() {
        if (!_empresaId) return;
        const resp = await ApiCliente.obtener(`/api/empresas/${_empresaId}/censo`);
        const tbody = document.getElementById('cuerpoTablaCenso');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="5" class="text-danger text-center py-3">Error al cargar declaraciones</td></tr>'; return; }
        const datos = await resp.json();
        if (!datos.length) { tbody.innerHTML = '<tr><td colspan="5" class="text-muted text-center py-3">Sin declaraciones</td></tr>'; return; }
        tbody.innerHTML = datos.map(d => `
            <tr>
                <td class="ps-3 fw-semibold">${d.anioPeriodo}</td>
                <td>${d.fechaDeclaracion ? d.fechaDeclaracion.substring(0, 10) : '-'}</td>
                <td>${d.cantidadPersonalRegistrado}</td>
                <td>${d.cantidadPersonasNoRegistrado}</td>
                <td>${d.totalEmpleados}</td>
            </tr>`).join('');
    }

    async function declararCenso() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaDeclaracionCenso');
        const periodo = parseInt(document.getElementById('campoPeriodoCenso').value);
        const registrado = parseInt(document.getElementById('campoCantRegistrado').value) || 0;
        const noRegistrado = parseInt(document.getElementById('campoCantNoRegistrado').value) || 0;
        const observacion = document.getElementById('campoObservacionCenso').value.trim() || null;
        if (!periodo || periodo < 2000) {
            mostrarAlertaModal('alertaDeclaracionCenso', 'Ingresá un año de período válido.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/censo`, {
            anioPeriodo: periodo,
            cantidadPersonalRegistrado: registrado,
            cantidadPersonasNoRegistrado: noRegistrado,
            observacion
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaDeclaracionCenso', err?.mensaje || 'Error al guardar la declaración.');
            return;
        }
        document.getElementById('formDeclaracionCenso').reset();
        await _cargarDeclaraciones();
        mostrarAlerta('Declaración guardada correctamente.');
    }

    async function _cargarPersonal() {
        if (!_empresaId) return;
        const resp = await ApiCliente.obtener(`/api/empresas/${_empresaId}/censo/personal`);

        if (_esGestor) {
            const resumen = document.getElementById('resumenPersonalGestor');
            if (!resumen) return;
            if (!resp?.ok) { resumen.innerHTML = '<p class="text-danger small">Error al cargar personal.</p>'; return; }
            const datos = await resp.json();
            resumen.innerHTML = `
                <div class="card border-0 bg-light">
                    <div class="card-body text-center py-4">
                        <i class="bi bi-people-fill text-primary fs-1 mb-2 d-block"></i>
                        <div class="fs-2 fw-bold">${datos.length}</div>
                        <small class="text-muted">empleado${datos.length !== 1 ? 's' : ''} registrado${datos.length !== 1 ? 's' : ''} en nómina</small>
                    </div>
                </div>`;
            return;
        }

        const tbody = document.getElementById('cuerpoTablaPersonal');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center py-3">Error al cargar personal</td></tr>'; return; }
        const datos = await resp.json();
        if (!datos.length) { tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center py-3">Sin personal registrado</td></tr>'; return; }
        tbody.innerHTML = datos.map(p => `
            <tr>
                <td class="ps-3">${p.cuit}</td>
                <td>${p.nombreCompleto}</td>
                <td>${p.fechaIngreso ? p.fechaIngreso.substring(0, 10) : '-'}</td>
                <td class="text-center">
                    <button class="btn btn-outline-danger btn-sm" onclick="ModuloCenso.eliminarPersonal(${p.id})">
                        <i class="bi bi-trash3"></i>
                    </button>
                </td>
            </tr>`).join('');
    }

    async function agregarPersonal() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaPersonalCenso');
        const cuit = document.getElementById('campoCuitPersonal').value.trim();
        const nombre = document.getElementById('campoNombrePersonal').value.trim();
        const fecha = document.getElementById('campoFechaIngresoPersonal').value;
        if (!cuit || !nombre || !fecha) {
            mostrarAlertaModal('alertaPersonalCenso', 'Completá todos los campos obligatorios.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/censo/personal`, {
            cuit, nombreCompleto: nombre, fechaIngreso: fecha
        });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaPersonalCenso', err?.mensaje || 'Error al agregar el empleado.');
            return;
        }
        document.getElementById('formPersonalCenso').reset();
        await _cargarPersonal();
        mostrarAlerta('Empleado agregado.');
    }

    async function eliminarPersonal(id) {
        const resp = await ApiCliente.eliminar(`/api/empresas/${_empresaId}/censo/personal/${id}`);
        if (resp?.ok) { await _cargarPersonal(); mostrarAlerta('Empleado eliminado.'); }
        else mostrarAlerta('No se pudo eliminar el empleado.', 'danger');
    }

    async function _cargarVehiculos() {
        if (!_empresaId) return;
        const resp = await ApiCliente.obtener(`/api/empresas/${_empresaId}/censo/vehiculos`);
        const tbody = document.getElementById('cuerpoTablaVehiculos');
        if (!tbody) return;
        if (!resp?.ok) { tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center py-3">Error al cargar vehículos</td></tr>'; return; }
        const datos = await resp.json();
        const cols = _esGestor ? 3 : 4;
        if (!datos.length) { tbody.innerHTML = `<tr><td colspan="${cols}" class="text-muted text-center py-3">Sin vehículos registrados</td></tr>`; return; }
        tbody.innerHTML = datos.map(v => `
            <tr>
                <td class="ps-3 fw-semibold">${v.patente}</td>
                <td>${v.marcaModelo}</td>
                <td class="text-center">
                    <span class="badge ${v.patenteValida ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'}">
                        ${v.patenteValida ? 'Válida' : 'Formato no estándar'}
                    </span>
                </td>
                ${_esGestor ? '' : `
                <td class="text-center">
                    <button class="btn btn-outline-danger btn-sm" onclick="ModuloCenso.eliminarVehiculo(${v.id})">
                        <i class="bi bi-trash3"></i>
                    </button>
                </td>`}
            </tr>`).join('');
    }

    async function agregarVehiculo() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaVehiculoCenso');
        const patente = document.getElementById('campoPatenteVehiculo').value.trim();
        const marcaModelo = document.getElementById('campoMarcaModeloVehiculo').value.trim();
        if (!patente || !marcaModelo) {
            mostrarAlertaModal('alertaVehiculoCenso', 'Completá todos los campos obligatorios.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/censo/vehiculos`, { patente, marcaModelo });
        if (!resp?.ok) {
            const err = await resp?.json().catch(() => ({}));
            mostrarAlertaModal('alertaVehiculoCenso', err?.mensaje || 'Error al agregar el vehículo.');
            return;
        }
        document.getElementById('formVehiculoCenso').reset();
        await _cargarVehiculos();
        mostrarAlerta('Vehículo agregado.');
    }

    async function eliminarVehiculo(id) {
        const resp = await ApiCliente.eliminar(`/api/empresas/${_empresaId}/censo/vehiculos/${id}`);
        if (resp?.ok) { await _cargarVehiculos(); mostrarAlerta('Vehículo eliminado.'); }
        else mostrarAlerta('No se pudo eliminar el vehículo.', 'danger');
    }

    return { cargar, seleccionarEmpresa, declararCenso, agregarPersonal, eliminarPersonal, agregarVehiculo, eliminarVehiculo };
})();
