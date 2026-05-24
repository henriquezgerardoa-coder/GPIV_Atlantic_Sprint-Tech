# Frontend module

Este modulo contiene exclusivamente recursos de interfaz (HTML, CSS, JS, imagenes) y codigo cliente.

## Regla de estructura

- Los assets de frontend deben vivir en `frontend/src/main/resources/static/`.
- El modulo `backend` no debe contener assets de UI en `backend/src/main/resources/static/`.

## Validacion automatica

El build de Maven del modulo `backend` falla en fase `validate` si detecta archivos dentro de `backend/src/main/resources/static/`.

Esto evita duplicacion y mantiene separadas las responsabilidades de backend y frontend.

