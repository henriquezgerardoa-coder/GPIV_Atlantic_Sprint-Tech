-- Seed completo: empresas, usuarios, radicaciones y proyectos de prueba
-- Contraseña de todos los usuarios EMPRESA: empresa12345

-- ============================================================
-- COLUMNA estado_anterior EN HISTORIAL (necesaria para el seed)
-- ============================================================

ALTER TABLE radicacion_historial
    ADD COLUMN IF NOT EXISTS estado_anterior VARCHAR(40);

ALTER TABLE radicacion_historial
    DROP CONSTRAINT IF EXISTS radicacion_historial_estado_anterior_check;

ALTER TABLE radicacion_historial
    ADD CONSTRAINT radicacion_historial_estado_anterior_check
        CHECK (estado_anterior IS NULL OR estado_anterior IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RADICADA',
            'RECHAZADA',
            'REQUIERE_INFORMACION_ADICIONAL',
            'CANCELADA'
        ));

-- ============================================================
-- EMPRESAS SEED
-- ============================================================
INSERT INTO empresas (nombre, razon_social, cuit, direccion, actividad_economica, correo_electronico, telefono, fecha_registro, status, rubro_id)
SELECT v.nombre, v.razon_social, v.cuit, v.direccion, v.actividad_economica, v.correo_electronico, v.telefono, v.fecha_registro, v.status, v.rubro_id
FROM (
VALUES
  ('Daluar Carpintería',
   'Daluar Carpintería SRL',
   '20-23456789-4',
   'Calle Metalúrgica 456, Junín',
   'Fabricación de perfiles de aluminio y carpintería metálica',
   'contacto@daluar.com.ar',
   '0236-4234567',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Metalurgia y siderurgia')),

  ('La Anónima SRL',
   'La Anónima SRL',
   '30-71234567-0',
   'Av. Industrias 1234, Junín',
   'Distribución y logística de productos de consumo masivo',
   'contacto@laanonima.com.ar',
   '0236-4123456',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Logística y almacenamiento')),

  ('Premoldeados Ga-Ma',
   'Premoldeados Ga-Ma S.A.',
   '30-34567890-2',
   'Ruta Provincial 65, Km 12, Junín',
   'Fabricación de premoldeados de hormigón',
   'contacto@gama.com.ar',
   '0236-4345678',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Manufactura y producción')),

  ('Premoldeados Las Dos Riveras',
   'Premoldeados Las Dos Riveras S.A.',
   '30-56789012-8',
   'Calle Obras 234, Junín',
   'Fabricación de premoldeados livianos de hormigón',
   'contacto@dosriveras.com.ar',
   '0236-4567890',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Manufactura y producción')),

  ('Santos S.A.',
   'Santos S.A.',
   '30-45678901-1',
   'Parque Industrial Sur, Av. Eco 890, Junín',
   'Tratamiento y reciclado de residuos industriales no peligrosos',
   'contacto@santos.com.ar',
   '0236-4456789',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Manufactura y producción')),

  ('Crisa SRL',
   'Crisa SRL',
   '30-67890123-5',
   'Parque Industrial Norte, Lote 14, Junín',
   'Fabricación de recipientes y envases plásticos industriales',
   'contacto@crisa.com.ar',
   '0236-4678901',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Química y farmacéutica')),

  ('Bassani S.A.',
   'Bassani S.A.',
   '30-78901234-3',
   'Ruta Nacional 7, Paraje Industrial, Junín',
   'Fabricación de maquinaria agrícola liviana',
   'contacto@bassani.com.ar',
   '0236-4789012',
   NOW(), 'ACTIVA',
   (SELECT id FROM rubros WHERE nombre = 'Manufactura y producción'))
) AS v(nombre, razon_social, cuit, direccion, actividad_economica, correo_electronico, telefono, fecha_registro, status, rubro_id)
WHERE NOT EXISTS (
  SELECT 1 FROM empresas e WHERE e.cuit = v.cuit
);

-- ============================================================
-- USUARIOS EMPRESA
-- ============================================================
INSERT INTO usuarios (nombre_usuario, nombre_completo, clave_acceso_hash, correo_electronico, activo, email_verificado, empresa_id)
SELECT v.nombre_usuario, v.nombre_completo, v.clave_acceso_hash, v.correo_electronico, v.activo, v.email_verificado, v.empresa_id
FROM (
VALUES
  ('anonima',    'La Anónima SRL',               '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@laanonima.com.ar',   true, true, (SELECT id FROM empresas WHERE cuit = '30-71234567-0')),
  ('daluar',     'Daluar Carpintería',            '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@daluar.com.ar',      true, true, (SELECT id FROM empresas WHERE cuit = '20-23456789-4')),
  ('gama',       'Premoldeados Ga-Ma',            '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@gama.com.ar',        true, true, (SELECT id FROM empresas WHERE cuit = '30-34567890-2')),
  ('dosriveras', 'Premoldeados Las Dos Riveras',  '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@dosriveras.com.ar',  true, true, (SELECT id FROM empresas WHERE cuit = '30-56789012-8')),
  ('santos',     'Santos S.A.',                   '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@santos.com.ar',      true, true, (SELECT id FROM empresas WHERE cuit = '30-45678901-1')),
  ('crisa',      'Crisa SRL',                     '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@crisa.com.ar',       true, true, (SELECT id FROM empresas WHERE cuit = '30-67890123-5')),
  ('bassani',    'Bassani S.A.',                  '$2a$10$rr26ki7LGI9USXfTIK9bseZHM9xYl1RORaxHDmE9qSeMCERhr5BB2', 'contacto@bassani.com.ar',     true, true, (SELECT id FROM empresas WHERE cuit = '30-78901234-3'))
) AS v(nombre_usuario, nombre_completo, clave_acceso_hash, correo_electronico, activo, email_verificado, empresa_id)
WHERE NOT EXISTS (
  SELECT 1 FROM usuarios u WHERE u.nombre_usuario = v.nombre_usuario
);

INSERT INTO usuarios_roles (usuario_id, rol)
SELECT id, 'EMPRESA' FROM usuarios
WHERE nombre_usuario IN ('anonima','daluar','gama','dosriveras','santos','crisa','bassani')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur WHERE ur.usuario_id = usuarios.id AND ur.rol = 'EMPRESA'
  );

-- ============================================================
-- RADICACIONES EN ESTADO PENDIENTE
-- ============================================================

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado, fecha_radicacion, fecha_ultima_actualizacion)
SELECT
  'RAD-20260412-AA1B2C3D',
   (SELECT id FROM empresas WHERE cuit = '30-71234567-0'),
   'PEDIDO_LOTES',
   'Solicitud de radicación para instalación de depósito logístico y punto de distribución regional.',
   'Depósito logístico y centro de distribución',
   '{"correo":"contacto@laanonima.com.ar","razonSocialEmpresa":"La Anónima SRL","cuit":"30-71234567-0","ingresosBrutos":"CM-123456","actividadPrincipal":"Distribución y logística de productos de consumo masivo","actividadSecundaria":"Almacenamiento de mercadería","tipoEmpresa":"EXISTENTE","objetoProyecto":"Ampliar la capacidad de distribución regional","direccion":"Av. Industrias 1234, Junín","personaReferente":"Carlos Méndez","telefono":"0236-4123456","correoElectronico":"cmendez@laanonima.com.ar","rubro":"Distribución y Logística","rubroOtro":null,"descripcionServicioBienOfrecido":"Distribución minorista de productos de consumo masivo a comercios de la región","emplazamientoActual":"ALQUILADO","personalJerarquico":3,"personalProduccion":12,"personalAdministrativo":4,"tiempoRadicacionMeses":24,"necesidadMetrosCuadrados":2500,"superficieCubiertaTrabajo":1800.0,"superficieCubiertaDeposito":600.0,"superficieFuturaExpansion":500.0,"superficieEstacionamiento":400.0,"tienePlanos":false,"personalAOcupar":19,"materiasPrimas":"No aplica — actividad de distribución","destinoProduccion":"Distribución a comercios minoristas de la región","tensionAlimentacion":"BAJA","potenciaInstaladaKw":30.0,"aguaLtsMes":2000.0,"requiereGas":false,"tipoResiduosEfluentes":"Residuos sólidos no peligrosos (embalajes, pallets)","tratamientoEnPlanta":false,"necesitaBalanzaPublica":false,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'PENDIENTE', '2026-04-12', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260412-AA1B2C3D'
);

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado, fecha_radicacion, fecha_ultima_actualizacion)
SELECT
  'RAD-20260415-BB3C4D5E',
   (SELECT id FROM empresas WHERE cuit = '20-23456789-4'),
   'PEDIDO_LOTES',
   'Solicitud para instalación de planta de fabricación de perfiles de aluminio y ventanas.',
   'Planta de carpintería metálica y aluminio',
   '{"correo":"contacto@daluar.com.ar","razonSocialEmpresa":"Daluar Carpintería","cuit":"20-23456789-4","ingresosBrutos":"AR-987654","actividadPrincipal":"Fabricación de perfiles de aluminio y carpintería metálica","actividadSecundaria":null,"tipoEmpresa":"NUEVA","objetoProyecto":null,"direccion":"Calle Metalúrgica 456, Junín","personaReferente":"Luis Dalmasso","telefono":"0236-4234567","correoElectronico":"ldalmasso@daluar.com.ar","rubro":"Metalmecánica","rubroOtro":null,"descripcionServicioBienOfrecido":"Perfiles de aluminio extrusado y fabricación de ventanas, puertas y cerramientos","emplazamientoActual":"PROPIO","personalJerarquico":2,"personalProduccion":8,"personalAdministrativo":2,"tiempoRadicacionMeses":36,"necesidadMetrosCuadrados":1800,"superficieCubiertaTrabajo":1200.0,"superficieCubiertaDeposito":300.0,"superficieFuturaExpansion":300.0,"superficieEstacionamiento":200.0,"tienePlanos":true,"personalAOcupar":12,"materiasPrimas":"Lingotes de aluminio, perfiles base, selladores","destinoProduccion":"Venta a constructoras y distribuidores de la zona central","tensionAlimentacion":"MEDIA","potenciaInstaladaKw":120.0,"aguaLtsMes":1500.0,"requiereGas":false,"tipoResiduosEfluentes":"Virutas y recortes de aluminio (material reciclable)","tratamientoEnPlanta":false,"necesitaBalanzaPublica":false,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'PENDIENTE', '2026-04-15', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260415-BB3C4D5E'
);

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado, fecha_radicacion, fecha_ultima_actualizacion)
SELECT
  'RAD-20260418-CC4D5E6F',
   (SELECT id FROM empresas WHERE cuit = '30-34567890-2'),
   'PEDIDO_LOTES',
   'Solicitud de espacio para planta de producción de premoldeados de hormigón: losas, vigas y columnas prefabricadas.',
   'Planta de premoldeados de hormigón',
   '{"correo":"contacto@gama.com.ar","razonSocialEmpresa":"Premoldeados Ga-Ma","cuit":"30-34567890-2","ingresosBrutos":"BU-112233","actividadPrincipal":"Fabricación de premoldeados de hormigón: losas, vigas y columnas prefabricadas","actividadSecundaria":null,"tipoEmpresa":"NUEVA","objetoProyecto":null,"direccion":"Ruta Provincial 65, Km 12, Junín","personaReferente":"Roberto García","telefono":"0236-4345678","correoElectronico":"rgarcia@gama.com.ar","rubro":"Construcción y Materiales","rubroOtro":null,"descripcionServicioBienOfrecido":"Losas pretensadas, columnas y vigas prefabricadas de hormigón armado para construcción civil","emplazamientoActual":"ALQUILADO","personalJerarquico":2,"personalProduccion":15,"personalAdministrativo":3,"tiempoRadicacionMeses":36,"necesidadMetrosCuadrados":3300,"superficieCubiertaTrabajo":2000.0,"superficieCubiertaDeposito":800.0,"superficieFuturaExpansion":1000.0,"superficieEstacionamiento":600.0,"tienePlanos":false,"personalAOcupar":20,"materiasPrimas":"Cemento Portland, áridos (arena y grava), hierro para armado, aditivos químicos","destinoProduccion":"Empresas constructoras y municipios de la provincia de Buenos Aires","tensionAlimentacion":"MEDIA","potenciaInstaladaKw":250.0,"aguaLtsMes":20000.0,"requiereGas":false,"tipoResiduosEfluentes":"Lechada de cemento, residuos inertes","tratamientoEnPlanta":true,"necesitaBalanzaPublica":true,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'PENDIENTE', '2026-04-18', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260418-CC4D5E6F'
);

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado, fecha_radicacion, fecha_ultima_actualizacion)
SELECT
  'RAD-20260421-DD5E6F7A',
   (SELECT id FROM empresas WHERE cuit = '30-45678901-1'),
   'PEDIDO_LOTES',
   'Solicitud para instalación de planta de tratamiento y reciclado de residuos industriales.',
   'Planta de tratamiento de residuos industriales',
   '{"correo":"contacto@santos.com.ar","razonSocialEmpresa":"Santos S.A.","cuit":"30-45678901-1","ingresosBrutos":"RS-556677","actividadPrincipal":"Tratamiento y reciclado de residuos industriales no peligrosos","actividadSecundaria":"Venta de materiales recuperados","tipoEmpresa":"EXISTENTE","objetoProyecto":"Ampliar la planta de tratamiento para atender la creciente demanda regional","direccion":"Parque Industrial Sur, Av. Eco 890, Junín","personaReferente":"Mariana Santos","telefono":"0236-4456789","correoElectronico":"msantos@santos.com.ar","rubro":"Medio Ambiente y Residuos","rubroOtro":null,"descripcionServicioBienOfrecido":"Clasificación, tratamiento y reciclado de plásticos, metales y papel provenientes de la industria","emplazamientoActual":"ALQUILADO","personalJerarquico":4,"personalProduccion":20,"personalAdministrativo":5,"tiempoRadicacionMeses":24,"necesidadMetrosCuadrados":5000,"superficieCubiertaTrabajo":2500.0,"superficieCubiertaDeposito":1500.0,"superficieFuturaExpansion":1500.0,"superficieEstacionamiento":800.0,"tienePlanos":true,"personalAOcupar":29,"materiasPrimas":"Residuos industriales (plásticos, metales, papel, vidrio)","destinoProduccion":"Industrias recicladoras e incineradores autorizados a nivel nacional","tensionAlimentacion":"MEDIA","potenciaInstaladaKw":350.0,"aguaLtsMes":5000.0,"requiereGas":false,"tipoResiduosEfluentes":"Líquidos residuales del proceso de lavado (efluentes tratados), residuos sólidos no aprovechables","tratamientoEnPlanta":true,"necesitaBalanzaPublica":true,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'PENDIENTE', '2026-04-21', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260421-DD5E6F7A'
);

-- ============================================================
-- RADICACIONES EN ESTADO APROBADA
-- ============================================================

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado,
   fecha_radicacion, fecha_aprobacion, fecha_plazo, tiempo_estimado_obra_meses, fecha_ultima_actualizacion)
SELECT
  'RAD-20260301-EE6F7A8B',
   (SELECT id FROM empresas WHERE cuit = '30-56789012-8'),
   'PEDIDO_LOTES',
   'Planta de producción de premoldeados livianos: paneles, cercos y postes de hormigón armado.',
   'Planta de premoldeados livianos',
   '{"correo":"contacto@dosriveras.com.ar","razonSocialEmpresa":"Premoldeados Las Dos Riveras","cuit":"30-56789012-8","ingresosBrutos":"DR-778899","actividadPrincipal":"Fabricación de premoldeados livianos de hormigón","actividadSecundaria":null,"tipoEmpresa":"NUEVA","objetoProyecto":null,"direccion":"Calle Obras 234, Junín","personaReferente":"Daniel Rivera","telefono":"0236-4567890","correoElectronico":"drivera@dosriveras.com.ar","rubro":"Construcción y Materiales","rubroOtro":null,"descripcionServicioBienOfrecido":"Paneles, cercos y postes de hormigón armado para uso rural y urbano","emplazamientoActual":"ALQUILADO","personalJerarquico":2,"personalProduccion":10,"personalAdministrativo":2,"tiempoRadicacionMeses":6,"necesidadMetrosCuadrados":1800,"superficieCubiertaTrabajo":1000.0,"superficieCubiertaDeposito":400.0,"superficieFuturaExpansion":400.0,"superficieEstacionamiento":300.0,"tienePlanos":true,"personalAOcupar":14,"materiasPrimas":"Cemento, áridos, hierro","destinoProduccion":"Agropecuarias y constructores locales","tensionAlimentacion":"BAJA","potenciaInstaladaKw":80.0,"aguaLtsMes":8000.0,"requiereGas":false,"tipoResiduosEfluentes":"Lechada de cemento e inertes","tratamientoEnPlanta":false,"necesitaBalanzaPublica":false,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'APROBADA',
   '2026-03-01', '2026-03-20', '2026-09-20', 6, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260301-EE6F7A8B'
);

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado,
   fecha_radicacion, fecha_aprobacion, fecha_plazo, tiempo_estimado_obra_meses, fecha_ultima_actualizacion)
SELECT
  'RAD-20260310-FF7A8B9C',
   (SELECT id FROM empresas WHERE cuit = '30-67890123-5'),
   'PEDIDO_LOTES',
   'Instalación de planta de fabricación de recipientes y envases plásticos industriales.',
   'Planta de fabricación de envases plásticos',
   '{"correo":"contacto@crisa.com.ar","razonSocialEmpresa":"Crisa SRL","cuit":"30-67890123-5","ingresosBrutos":"CR-445566","actividadPrincipal":"Fabricación de recipientes y envases plásticos industriales","actividadSecundaria":null,"tipoEmpresa":"NUEVA","objetoProyecto":null,"direccion":"Parque Industrial Norte, Lote 14, Junín","personaReferente":"Cristina Allende","telefono":"0236-4678901","correoElectronico":"callende@crisa.com.ar","rubro":"Plásticos y Petroquímica","rubroOtro":null,"descripcionServicioBienOfrecido":"Envases plásticos de tipo PET, HDPE y PP para industrias alimenticia, química y agropecuaria","emplazamientoActual":"PROPIO","personalJerarquico":3,"personalProduccion":18,"personalAdministrativo":4,"tiempoRadicacionMeses":6,"necesidadMetrosCuadrados":2500,"superficieCubiertaTrabajo":1500.0,"superficieCubiertaDeposito":500.0,"superficieFuturaExpansion":600.0,"superficieEstacionamiento":400.0,"tienePlanos":true,"personalAOcupar":25,"materiasPrimas":"Resinas plásticas (PET, HDPE, PP), colorantes, aditivos estabilizadores","destinoProduccion":"Industrias alimenticia, química y agropecuaria a nivel nacional","tensionAlimentacion":"MEDIA","potenciaInstaladaKw":180.0,"aguaLtsMes":3000.0,"requiereGas":false,"tipoResiduosEfluentes":"Recortes y residuos plásticos (material recuperable), aguas de enfriamiento","tratamientoEnPlanta":false,"necesitaBalanzaPublica":false,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'APROBADA',
   '2026-03-10', '2026-03-28', '2026-09-28', 6, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260310-FF7A8B9C'
);

INSERT INTO radicaciones
  (numero_radicado, empresa_id, tipo_solicitud, descripcion, uso_estimativo,
   relevamiento_pedido_lotes, estado,
   fecha_radicacion, fecha_aprobacion, fecha_plazo, tiempo_estimado_obra_meses, fecha_ultima_actualizacion)
SELECT
  'RAD-20260318-GG8B9C0D',
   (SELECT id FROM empresas WHERE cuit = '30-78901234-3'),
   'PEDIDO_LOTES',
   'Planta de manufactura de maquinaria agrícola liviana: rastras, arados y sembradoras.',
   'Planta de maquinaria agrícola',
   '{"correo":"contacto@bassani.com.ar","razonSocialEmpresa":"Bassani S.A.","cuit":"30-78901234-3","ingresosBrutos":"BS-334455","actividadPrincipal":"Fabricación de maquinaria agrícola liviana: rastras, arados y sembradoras","actividadSecundaria":"Mantenimiento y servicio técnico de maquinaria","tipoEmpresa":"EXISTENTE","objetoProyecto":"Ampliar la línea de producción e incorporar nuevos modelos de sembradoras de precisión","direccion":"Ruta Nacional 7, Paraje Industrial, Junín","personaReferente":"Alberto Bassani","telefono":"0236-4789012","correoElectronico":"abassani@bassani.com.ar","rubro":"Maquinaria e Industria Pesada","rubroOtro":null,"descripcionServicioBienOfrecido":"Maquinaria agrícola liviana: rastras de discos, arados de vertedera y sembradoras a grano grueso y fino","emplazamientoActual":"PROPIO","personalJerarquico":5,"personalProduccion":25,"personalAdministrativo":6,"tiempoRadicacionMeses":6,"necesidadMetrosCuadrados":3300,"superficieCubiertaTrabajo":2000.0,"superficieCubiertaDeposito":600.0,"superficieFuturaExpansion":800.0,"superficieEstacionamiento":500.0,"tienePlanos":true,"personalAOcupar":36,"materiasPrimas":"Chapas de acero, perfiles estructurales, componentes mecánicos, pintura industrial","destinoProduccion":"Productores agropecuarios de la región pampeana y NOA","tensionAlimentacion":"MEDIA","potenciaInstaladaKw":300.0,"aguaLtsMes":4000.0,"requiereGas":true,"tipoResiduosEfluentes":"Virutas metálicas, solventes y pinturas (residuos peligrosos controlados)","tratamientoEnPlanta":false,"necesitaBalanzaPublica":false,"necesitaComedorUnitario":true,"necesitaSalonCoworking":false}',
   'APROBADA',
   '2026-03-18', '2026-04-05', '2026-10-05', 6, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM radicaciones r WHERE r.numero_radicado = 'RAD-20260318-GG8B9C0D'
);

-- ============================================================
-- HISTORIAL DE RADICACIONES PENDIENTES
-- ============================================================

INSERT INTO radicacion_historial (radicacion_id, estado_anterior, estado, comentario, usuario, fecha_evento)
SELECT r.id, NULL, 'PENDIENTE', 'Solicitud de radicación creada', u.nombre_usuario, CAST(r.fecha_radicacion AS TIMESTAMP)
FROM radicaciones r
JOIN empresas e ON e.id = r.empresa_id
JOIN usuarios u ON u.empresa_id = e.id
WHERE r.numero_radicado IN (
  'RAD-20260412-AA1B2C3D',
  'RAD-20260415-BB3C4D5E',
  'RAD-20260418-CC4D5E6F',
  'RAD-20260421-DD5E6F7A'
);

-- ============================================================
-- HISTORIAL DE RADICACIONES APROBADAS
-- ============================================================

INSERT INTO radicacion_historial (radicacion_id, estado_anterior, estado, comentario, usuario, fecha_evento)
SELECT r.id, NULL, 'PENDIENTE', 'Solicitud de radicación creada', u.nombre_usuario, CAST(r.fecha_radicacion AS TIMESTAMP)
FROM radicaciones r
JOIN empresas e ON e.id = r.empresa_id
JOIN usuarios u ON u.empresa_id = e.id
WHERE r.numero_radicado IN (
  'RAD-20260301-EE6F7A8B',
  'RAD-20260310-FF7A8B9C',
  'RAD-20260318-GG8B9C0D'
);

INSERT INTO radicacion_historial (radicacion_id, estado_anterior, estado, comentario, usuario, fecha_evento)
SELECT r.id, 'PENDIENTE', 'EN_REVISION', 'Expediente en revisión por equipo técnico', 'admin', CAST(r.fecha_radicacion + 3 AS TIMESTAMP)
FROM radicaciones r
WHERE r.numero_radicado IN (
  'RAD-20260301-EE6F7A8B',
  'RAD-20260310-FF7A8B9C',
  'RAD-20260318-GG8B9C0D'
);

INSERT INTO radicacion_historial (radicacion_id, estado_anterior, estado, comentario, usuario, fecha_evento)
SELECT r.id, 'EN_REVISION', 'APROBADA',
  'Aprobado en sesión del Directorio. Acta de rúbrica adjunta.',
  'admin', CAST(r.fecha_aprobacion AS TIMESTAMP)
FROM radicaciones r
WHERE r.numero_radicado IN (
  'RAD-20260301-EE6F7A8B',
  'RAD-20260310-FF7A8B9C',
  'RAD-20260318-GG8B9C0D'
);

-- ============================================================
-- PROYECTOS PRODUCTIVOS para las radicaciones APROBADAS
-- ============================================================

INSERT INTO proyectos_productivos
  (nombre, descripcion, estado, fecha_creacion, fecha_inicio_real, fecha_estimada_fin,
   responsable_id, solicitud_origen_id)
VALUES
  ('Proyecto - Premoldeados Las Dos Riveras',
   'Planta de producción de premoldeados livianos: paneles, cercos y postes de hormigón armado.',
   'EN_EJECUCION', NOW(), '2026-03-20', '2026-09-20',
   NULL,
   (SELECT id FROM radicaciones WHERE numero_radicado = 'RAD-20260301-EE6F7A8B')),

  ('Proyecto - Crisa SRL',
   'Instalación de planta de fabricación de recipientes y envases plásticos industriales.',
   'EN_EJECUCION', NOW(), '2026-03-28', '2026-09-28',
   NULL,
   (SELECT id FROM radicaciones WHERE numero_radicado = 'RAD-20260310-FF7A8B9C')),

  ('Proyecto - Bassani S.A.',
   'Planta de manufactura de maquinaria agrícola liviana: rastras, arados y sembradoras.',
   'EN_EJECUCION', NOW(), '2026-04-05', '2026-10-05',
   NULL,
   (SELECT id FROM radicaciones WHERE numero_radicado = 'RAD-20260318-GG8B9C0D'));