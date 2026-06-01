const ModuloCenso = (() => {
    let _empresaId = null;
    let _esGestor   = false;  // ADMIN, DIRECTIVO, SECRETARIO
    let _soloLectura = false; // ADMIN, DIRECTIVO (no editan)

    // ── Helpers ────────────────────────────────────────────────────────────

    function _formatearCuil(cuil) {
        const d = (cuil || '').replace(/[^0-9]/g, '');
        if (d.length !== 11) return cuil || '—';
        return d.substring(0, 2) + '-' + d.substring(2, 10) + '-' + d.substring(10);
    }

    function _soloDigitosCuil(input) {
        input.value = input.value.replace(/[^0-9]/g, '').slice(0, 11);
    }

    // ── Ciclo de carga ──────────────────────────────────────────────────────

    async function cargar() {
        const esEmpresaSolo = Autenticacion.tieneAcceso(['EMPRESA'])
            && !Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO', 'SECRETARIO']);
        _esGestor    = !esEmpresaSolo;
        _soloLectura = Autenticacion.tieneAcceso(['ADMINISTRADOR', 'DIRECTIVO'])
            && !Autenticacion.tieneAcceso(['SECRETARIO', 'EMPRESA']);

        const bloqueSelector = document.getElementById('bloqueSelectorEmpresaCenso');
        const contenido      = document.getElementById('contenidoCenso');

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
        // Formularios solo para EMPRESA
        document.getElementById('bloqueFormDeclaracion')?.classList.toggle('d-none', _esGestor || !_soloLectura && _esGestor);
        document.getElementById('bloqueFormVehiculo')?.classList.toggle('d-none', _soloLectura || _esGestor);
        // Tab de nómina: oculto para gestores (solo ven conteos en las tarjetas de resumen)
        document.getElementById('tabPersonalCenso')?.classList.toggle('d-none', _esGestor);
        document.getElementById('bloqueFormPersonal')?.classList.toggle('d-none', _esGestor);
        document.getElementById('cabeceraAccionPersonal')?.classList.toggle('d-none', _esGestor);
        // Columna de acciones en vehículos
        const cabeceraVehiculos = document.getElementById('cabeceraTablaVehiculos');
        if (cabeceraVehiculos) {
            cabeceraVehiculos.querySelector('th:last-child')?.classList.toggle('d-none', _soloLectura || _esGestor);
        }
        // Tarjetas de resumen: gestor ve declarados/pendientes, empresa ve total
        document.getElementById('bloqueStatGestor')?.classList.toggle('d-none', !_esGestor);
        document.getElementById('bloqueStatEmpresa')?.classList.toggle('d-none', _esGestor);
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
        await Promise.all([_cargarDeclaraciones(), _cargarResumenGestor(), _cargarPersonal(), _cargarVehiculos()]);
    }

    // ── Resumen gestores (declarados / pendientes) ──────────────────────────

    async function _cargarResumenGestor() {
        if (!_empresaId || !_esGestor) return;
        const resp = await ApiCliente.obtener(`/api/empresas/${_empresaId}/empleados/resumen-declaracion`);
        if (!resp?.ok) return;
        const data = await resp.json();
        const elDec = document.getElementById('statCensoDeclarados');
        const elPen = document.getElementById('statCensoPendientes');
        if (elDec) elDec.textContent = data.declarados ?? '—';
        if (elPen) elPen.textContent = data.pendientes ?? '—';
        const elUlt = document.getElementById('statCensoUltimaDeclaracion');
        if (elUlt) elUlt.textContent = data.ultimaDeclaracion
            ? data.ultimaDeclaracion.substring(0, 10)
            : 'Sin declaraciones';
    }

    // ── Declaraciones históricas ─────────────────────────────────────────────

    async function _cargarDeclaraciones() {
        if (!_empresaId) return;
        const resp  = await ApiCliente.obtener(`/api/empresas/${_empresaId}/censo`);
        const tbody = document.getElementById('cuerpoTablaCenso');
        if (!tbody) return;
        if (!resp?.ok) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center py-3">Error al cargar declaraciones</td></tr>';
            return;
        }
        const datos = await resp.json();
        if (!datos.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center py-3">Sin declaraciones</td></tr>';
            return;
        }
        tbody.innerHTML = datos.map(d => {
            const periodo = d.semestre > 0
                ? `S${d.semestre} ${d.anioPeriodo}`
                : `${d.anioPeriodo}`;
            const badge = d.semestre > 0
                ? `<span class="badge bg-primary-subtle text-primary ms-1">Jurada</span>`
                : '';
            return `<tr>
                <td class="ps-3 fw-semibold">${periodo}${badge}</td>
                <td>${d.fechaDeclaracion ? d.fechaDeclaracion.substring(0, 10) : '-'}</td>
                <td>${d.cantidadPersonalRegistrado}</td>
                <td class="text-muted small">${d.observacion || '-'}</td>
            </tr>`;
        }).join('');
    }

    async function declararCenso() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaDeclaracionCenso');
        const periodo     = parseInt(document.getElementById('campoPeriodoCenso').value);
        const registrado  = parseInt(document.getElementById('campoCantRegistrado').value)   || 0;
        const noRegistrado = parseInt(document.getElementById('campoCantNoRegistrado').value) || 0;
        const observacion = document.getElementById('campoObservacionCenso').value.trim() || null;
        if (!periodo || periodo < 2000) {
            mostrarAlertaModal('alertaDeclaracionCenso', 'Ingresá un año de período válido.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/censo`, {
            anioPeriodo: periodo,
            cantidadPersonalRegistrado: registrado,
            cantidadPersonalNoRegistrado: noRegistrado,
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

    // ── Personal registrado ────────────────────────────────────────────────

    async function _cargarPersonal() {
        if (!_empresaId || _esGestor) return;
        const resp  = await ApiCliente.obtener(`/api/empresas/${_empresaId}/empleados`);
        const tbody = document.getElementById('cuerpoTablaPersonal');
        if (!tbody) return;
        const cols = _esGestor ? 3 : 4;
        if (!resp?.ok) {
            tbody.innerHTML = `<tr><td colspan="${cols}" class="text-danger text-center py-3">Error al cargar personal</td></tr>`;
            return;
        }
        const datos = await resp.json();

        const elTotal = document.getElementById('statCensoPersonalTotal');
        const elPend  = document.getElementById('statCensoPendientesEmpresa');
        const pendientes = datos.filter(p => !p.declarado).length;
        if (elTotal) elTotal.textContent = datos.length;
        if (elPend)  elPend.textContent  = pendientes;

        const btnDeclarar = document.getElementById('btnGenerarDeclaracion');
        if (btnDeclarar) btnDeclarar.disabled = pendientes === 0;

        if (!datos.length) {
            tbody.innerHTML = `<tr><td colspan="${cols}" class="text-muted text-center py-3">Sin personal registrado</td></tr>`;
            return;
        }
        tbody.innerHTML = datos.map(p => {
            const badge = p.declarado
                ? '<span class="badge bg-success-subtle text-success">Declarado</span>'
                : '<span class="badge bg-warning-subtle text-warning">Pendiente</span>';
            return `<tr>
                <td class="ps-3 fw-semibold">${_formatearCuil(p.cuit)}</td>
                <td>${p.nombre}</td>
                <td class="text-center">${badge}</td>
                ${!_esGestor ? `<td class="text-center">
                    <button class="btn btn-outline-danger btn-sm" onclick="ModuloCenso.eliminarPersonal(${p.id})"
                            ${p.declarado ? 'title="Empleado ya declarado"' : ''}>
                        <i class="bi bi-trash3"></i>
                    </button>
                </td>` : ''}
            </tr>`;
        }).join('');
    }

    async function agregarPersonal() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaPersonalCenso');
        const cuil   = document.getElementById('campoCuilPersonal').value.trim();
        const nombre = document.getElementById('campoNombrePersonal').value.trim();
        if (cuil.length !== 11 || !/^[0-9]{11}$/.test(cuil)) {
            mostrarAlertaModal('alertaPersonalCenso', 'El CUIL debe tener exactamente 11 dígitos.');
            return;
        }
        if (!nombre) {
            mostrarAlertaModal('alertaPersonalCenso', 'El nombre es obligatorio.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/empleados`, {
            cuit: cuil, nombre
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
        const resp = await ApiCliente.eliminar(`/api/empresas/${_empresaId}/empleados/${id}`);
        if (resp?.ok) { await _cargarPersonal(); mostrarAlerta('Empleado eliminado.'); }
        else mostrarAlerta('No se pudo eliminar el empleado.', 'danger');
    }

    async function generarDeclaracion() {
        const btn = document.getElementById('btnGenerarDeclaracion');
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Generando...'; }
        try {
            const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/empleados/declarar`, {});
            if (!resp?.ok) {
                const err = await resp?.json().catch(() => ({}));
                mostrarAlerta(err?.mensaje || 'Error al generar la declaración.', 'danger');
                return;
            }
            await Promise.all([_cargarPersonal(), _cargarDeclaraciones()]);
            mostrarAlerta('Declaración jurada generada correctamente.');
        } finally {
            if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-file-earmark-check me-1"></i>Generar Declaración Jurada'; }
        }
    }

    // ── Vehículos ───────────────────────────────────────────────────────────

    async function _cargarVehiculos() {
        if (!_empresaId) return;
        const resp  = await ApiCliente.obtener(`/api/empresas/${_empresaId}/censo/vehiculos`);
        const tbody = document.getElementById('cuerpoTablaVehiculos');
        if (!tbody) return;
        const cols = (_soloLectura || _esGestor) ? 4 : 5;
        if (!resp?.ok) {
            tbody.innerHTML = `<tr><td colspan="${cols}" class="text-danger text-center py-3">Error al cargar vehículos</td></tr>`;
            return;
        }
        const datos = await resp.json();
        const statId = _esGestor ? 'statCensoVehiculos' : 'statCensoVehiculosEmpresa';
        const statEl = document.getElementById(statId);
        if (statEl) statEl.textContent = datos.length;
        if (!datos.length) {
            tbody.innerHTML = `<tr><td colspan="${cols}" class="text-muted text-center py-3">Sin vehículos registrados</td></tr>`;
            return;
        }
        tbody.innerHTML = datos.map(v => `
            <tr>
                <td class="ps-3 fw-semibold">${v.patente}</td>
                <td>${v.marcaModelo}</td>
                <td>${v.tipo}</td>
                <td class="text-center">
                    <span class="badge ${v.patenteValida ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'}">
                        ${v.patenteValida ? 'Válida' : 'Formato no estándar'}
                    </span>
                </td>
                ${(!_soloLectura && !_esGestor) ? `
                <td class="text-center">
                    <button class="btn btn-outline-danger btn-sm" onclick="ModuloCenso.eliminarVehiculo(${v.id})">
                        <i class="bi bi-trash3"></i>
                    </button>
                </td>` : ''}
            </tr>`).join('');
    }

    async function agregarVehiculo() {
        if (!_empresaId) return;
        ocultarAlertaModal('alertaVehiculoCenso');
        const patente    = document.getElementById('campoPatenteVehiculo').value.trim();
        const marcaModelo = document.getElementById('campoMarcaModeloVehiculo').value.trim();
        const tipo       = document.getElementById('campoTipoVehiculo').value.trim();
        if (!patente || !marcaModelo || !tipo) {
            mostrarAlertaModal('alertaVehiculoCenso', 'Completá todos los campos obligatorios.');
            return;
        }
        const resp = await ApiCliente.crear(`/api/empresas/${_empresaId}/censo/vehiculos`, { patente, marcaModelo, tipo });
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

    return {
        cargar, seleccionarEmpresa,
        declararCenso,
        agregarPersonal, eliminarPersonal, generarDeclaracion,
        soloDigitosCuil: _soloDigitosCuil,
        agregarVehiculo, eliminarVehiculo
    };
})();
