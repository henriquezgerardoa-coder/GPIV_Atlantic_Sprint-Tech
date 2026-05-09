const { app, BrowserWindow, dialog } = require("electron");
const { spawn } = require("child_process");
const http = require("http");
const path = require("path");
const fs = require("fs");
const net = require("net");

const HOST = "127.0.0.1";
const DEFAULT_PORT = Number(process.env.SERVER_PORT || "8090");
const MAX_PORT_TRIES = 20;

let backendProcess = null;
let splashWindow = null;
let runtimeConfig = null;

function getJavaCommand() {
  return process.env.JAVA_BIN || "java";
}

function getJarPath() {
  if (app.isPackaged) {
    // electron-builder copies extra resources under process.resourcesPath
    return path.join(process.resourcesPath, "backend", "backend-0.0.1-SNAPSHOT.jar");
  }

  // Running from repo: /desktop/main.js -> ../backend/target/*.jar
  return path.resolve(__dirname, "..", "backend", "target", "backend-0.0.1-SNAPSHOT.jar");
}

function isPortFree(port) {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.once("error", () => resolve(false));
    server.once("listening", () => {
      server.close(() => resolve(true));
    });
    server.listen(port, HOST);
  });
}

async function resolveBackendPort(basePort, maxTries) {
  for (let offset = 0; offset <= maxTries; offset += 1) {
    const candidate = basePort + offset;
    // eslint-disable-next-line no-await-in-loop
    const free = await isPortFree(candidate);
    if (free) {
      return candidate;
    }
  }
  throw new Error(`No hay puertos libres desde ${basePort} hasta ${basePort + maxTries}`);
}

function buildDbEnv(baseEnv) {
  const env = { ...baseEnv };
  const preset = (env.DB_PRESET || "").toLowerCase();

  // Si el usuario ya define DB_URL explícitamente, se respeta.
  if (env.DB_URL) {
    return env;
  }

  if (preset === "neon") {
    if (env.NEON_DB_URL) {
      env.DB_URL = env.NEON_DB_URL;
    }
    if (env.NEON_DB_USER) {
      env.DB_USER = env.NEON_DB_USER;
    }
    if (env.NEON_DB_PASSWORD) {
      env.DB_PASSWORD = env.NEON_DB_PASSWORD;
    }
    env.DB_SSLMODE = env.DB_SSLMODE || "require";
    return env;
  }

  // Default: local
  env.DB_URL = env.DB_URL || "jdbc:postgresql://127.0.0.1:5432/gpiv";
  env.DB_USER = env.DB_USER || "admin";
  env.DB_PASSWORD = env.DB_PASSWORD || "password123";
  return env;
}

function waitForBackend(saludUrl, maxMs) {
  const started = Date.now();

  return new Promise((resolve, reject) => {
    const tick = () => {
      const req = http.get(saludUrl, (res) => {
        res.resume();
        if (res.statusCode === 200) {
          resolve(true);
          return;
        }
        retry();
      });

      req.on("error", retry);
      req.setTimeout(1000, () => {
        req.destroy();
        retry();
      });
    };

    const retry = () => {
      if (Date.now() - started > maxMs) {
        reject(new Error(`Backend no responde en ${saludUrl} dentro de ${maxMs}ms`));
        return;
      }
      setTimeout(tick, 700);
    };

    tick();
  });
}

function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 460,
    height: 250,
    resizable: false,
    frame: false,
    alwaysOnTop: true,
    show: true,
    title: "Iniciando GPIV Desktop"
  });

  const splashHtml = `
    <html>
      <body style="margin:0;display:flex;align-items:center;justify-content:center;background:#0d6efd;color:#fff;font-family:Arial,sans-serif;">
        <div style="text-align:center;">
          <h2 style="margin:0 0 12px 0;">GPIV Desktop</h2>
          <p style="margin:0;opacity:.9;">Iniciando backend y cargando interfaz...</p>
        </div>
      </body>
    </html>
  `;
  splashWindow.loadURL(`data:text/html;charset=UTF-8,${encodeURIComponent(splashHtml)}`);
}

function closeSplashWindow() {
  if (splashWindow && !splashWindow.isDestroyed()) {
    splashWindow.close();
  }
  splashWindow = null;
}

function startBackend(port) {
  const jarPath = getJarPath();
  if (!fs.existsSync(jarPath)) {
    throw new Error(`No se encontró el JAR del backend: ${jarPath}`);
  }

  const javaCmd = getJavaCommand();
  const args = ["-jar", jarPath, `--server.port=${port}`];

  const env = buildDbEnv({
    ...process.env,
    SERVER_PORT: String(port)
  });

  backendProcess = spawn(javaCmd, args, {
    cwd: path.dirname(jarPath),
    env,
    stdio: "inherit"
  });

  backendProcess.on("exit", (code, signal) => {
    backendProcess = null;
    if (!app.isQuiting) {
      dialog.showErrorBox(
        "Backend detenido",
        `El backend finalizó inesperadamente (code=${code}, signal=${signal || "-"}).`
      );
    }
  });
}

function stopBackend() {
  if (!backendProcess) {
    return;
  }
  try {
    backendProcess.kill("SIGTERM");
  } catch (_err) {
    // no-op
  }
}

function createMainWindow(baseUrl) {
  const win = new BrowserWindow({
    width: 1366,
    height: 860,
    minWidth: 1100,
    minHeight: 720,
    autoHideMenuBar: true,
    title: "GPIV Desktop"
  });

  win.loadURL(`${baseUrl}/`);
}

async function bootstrap() {
  try {
    createSplashWindow();

    const port = await resolveBackendPort(DEFAULT_PORT, MAX_PORT_TRIES);
    const baseUrl = `http://${HOST}:${port}`;
    const saludUrl = `${baseUrl}/salud`;
    runtimeConfig = { port, baseUrl, saludUrl };

    startBackend(port);
    await waitForBackend(saludUrl, 45000);
    createMainWindow(baseUrl);
    closeSplashWindow();
  } catch (error) {
    closeSplashWindow();
    dialog.showErrorBox(
      "Error al iniciar GPIV Desktop",
      `${error.message}\n\nSugerencia: ejecuta 'npm run build:backend' en /desktop e intenta nuevamente.`
    );
    app.quit();
  }
}

app.whenReady().then(bootstrap);

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("before-quit", () => {
  app.isQuiting = true;
  stopBackend();
});

app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0 && runtimeConfig?.baseUrl) {
    createMainWindow(runtimeConfig.baseUrl);
  }
});

