-- Hace nullable columnas heredadas de un esquema anterior que ya no mapea el entity ProyectoProductivo
ALTER TABLE proyectos_productivos ALTER COLUMN descripcion DROP NOT NULL;
ALTER TABLE proyectos_productivos ALTER COLUMN empresa_id DROP NOT NULL;
ALTER TABLE proyectos_productivos ALTER COLUMN lote_id DROP NOT NULL;
ALTER TABLE proyectos_productivos ALTER COLUMN responsable_seguimiento_id DROP NOT NULL;
ALTER TABLE proyectos_productivos ALTER COLUMN monto_inversion DROP NOT NULL;
ALTER TABLE proyectos_productivos ALTER COLUMN fecha_estimada_fin DROP NOT NULL;
ALTER TABLE proyectos_productivos DROP CONSTRAINT IF EXISTS proyectos_productivos_estado_check;

-- Amplía tipo_documento y su check constraint para los nuevos valores del enum TipoDocumentoRadicacion
ALTER TABLE radicacion_documentos ALTER COLUMN tipo_documento TYPE VARCHAR(30);
ALTER TABLE radicacion_documentos DROP CONSTRAINT IF EXISTS radicacion_documentos_tipo_documento_check;
ALTER TABLE radicacion_documentos ADD CONSTRAINT radicacion_documentos_tipo_documento_check
    CHECK (tipo_documento IN ('OBLIGATORIO', 'ADICIONAL', 'ACTA_RUBRICA', 'CERTIFICACION_RADICACION'));
