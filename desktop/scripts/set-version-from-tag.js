#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const raizDesktop = path.resolve(__dirname, '..');
const packageJsonPath = path.join(raizDesktop, 'package.json');
const dryRun = process.argv.includes('--dry-run');

function leerPackageJson() {
  return JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
}

function obtenerTagGit() {
  if (process.env.GIT_TAG && process.env.GIT_TAG.trim()) {
    return process.env.GIT_TAG.trim();
  }

  try {
    const tagExacto = execSync('git describe --tags --exact-match', {
      cwd: raizDesktop,
      stdio: ['ignore', 'pipe', 'ignore'],
      encoding: 'utf8'
    }).trim();
    if (tagExacto) {
      return tagExacto;
    }
  } catch (_) {
    // Sin tag exacto en HEAD.
  }

  return '';
}

function normalizarSemver(tag) {
  const valor = String(tag || '').trim();
  const sinPrefijo = valor.startsWith('v') ? valor.slice(1) : valor;
  const semverRegex = /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/;
  return semverRegex.test(sinPrefijo) ? sinPrefijo : '';
}

function main() {
  const pkg = leerPackageJson();
  const tag = obtenerTagGit();
  const nuevaVersion = normalizarSemver(tag);

  if (!nuevaVersion) {
    console.log(`[version:from-tag] Sin tag semver en HEAD. Se mantiene version ${pkg.version}.`);
    return;
  }

  if (pkg.version === nuevaVersion) {
    console.log(`[version:from-tag] Version ya sincronizada: ${pkg.version}.`);
    return;
  }

  console.log(`[version:from-tag] ${pkg.version} -> ${nuevaVersion}`);
  if (dryRun) {
    return;
  }

  pkg.version = nuevaVersion;
  fs.writeFileSync(packageJsonPath, `${JSON.stringify(pkg, null, 2)}\n`, 'utf8');
  console.log('[version:from-tag] package.json actualizado.');
}

main();

