# Modulo Frontend

Este modulo contiene exclusivamente los recursos de interfaz de usuario (cliente web).

## Ubicacion de recursos

Los archivos estaticos se encuentran en:

- `frontend/src/main/resources/META-INF/resources/`

Spring Boot los sirve automaticamente desde el backend al estar este modulo agregado como dependencia.

## Convencion por capa

- `frontend`: HTML, CSS, JavaScript, assets y logica de presentacion.
- `backend`: controladores API, servicios de negocio, seguridad y acceso a datos.
- `commons`: DTO/contratos compartidos.
- `entities`: entidades de dominio.

