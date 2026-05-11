# GPIV Desktop

Wrapper desktop para GPIV usando Electron, reutilizando el backend Spring Boot existente.

## Requisitos

- Node.js 18+
- Java 17+
- Maven 3.9+
- Base de datos accesible (PostgreSQL local o Neon)

## Primer uso

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm install
npm run dev
```

`npm run dev` hace:

1. Build del backend (`mvn -pl backend -DskipTests package`)
2. Inicia Electron
3. Electron muestra un splash de arranque
4. Electron arranca el JAR del backend y espera `GET /salud`
5. Si `8090` esta ocupado, usa un puerto libre (fallback)
6. Abre `http://127.0.0.1:<puerto>/`

## Variables útiles

- `SERVER_PORT` (default: `8090`)
- `DB_PRESET` (`local` o `neon`)
- `JAVA_BIN` (default: `java`)
- `DB_URL`, `DB_USER`, `DB_PASSWORD` (si el backend las necesita)
- `NEON_DB_URL`, `NEON_DB_USER`, `NEON_DB_PASSWORD` (si `DB_PRESET=neon`)

Ejemplo:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
export SERVER_PORT='8090'
export DB_PRESET='local'
export DB_URL='jdbc:postgresql://localhost:5432/gpiv'
export DB_USER='admin'
export DB_PASSWORD='password123'
npm run dev
```

Ejemplo Neon:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
export DB_PRESET='neon'
export NEON_DB_URL='jdbc:postgresql://ep-xxxx.sa-east-1.aws.neon.tech/AST-GPIV-DB?sslmode=require'
export NEON_DB_USER='neondb_owner'
export NEON_DB_PASSWORD='***'
npm run dev
```

## Verificación rápida

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm run smoke
```

## Empaquetado (Linux)

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm run dist:linux
```

El binario se genera en `desktop/dist/`.

## Empaquetado (Windows/macOS)

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
npm run dist:win
npm run dist:mac
```

Nota: para firmar instaladores en entornos productivos se requieren certificados y variables adicionales del sistema operativo.

## Versionado desde tags

Antes de empaquetar, los scripts `dist:*` sincronizan `version` de `package.json` desde el tag git de `HEAD` si cumple formato semver (por ejemplo `v1.2.3` o `1.2.3`).

Prueba manual:

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop
node scripts/set-version-from-tag.js --dry-run
```

## Instalar/Ejecutar AppImage (Linux)

```bash
cd /home/gerardo/IdeaProjects/GPIV_Atlantic_Sprint-Tech/desktop/dist
chmod +x "GPIV Desktop-0.1.0.AppImage"
./"GPIV Desktop-0.1.0.AppImage"
```

Si prefieres crear un lanzador del sistema, puedes usar AppImageLauncher o copiar el archivo en una ruta fija (por ejemplo `~/Aplicaciones`).
