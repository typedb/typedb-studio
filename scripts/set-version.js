const fs = require("fs");
const path = require("path");

const version = process.argv[2];
if (!version || !/^\d+\.\d+\.\d+/.test(version)) {
  console.error("Usage: pnpm set-version <semver>  (e.g. 3.10.0)");
  process.exit(1);
}

const root = path.resolve(__dirname, "..");

function readJson(rel) {
  const abs = path.join(root, rel);
  return { abs, data: JSON.parse(fs.readFileSync(abs, "utf8")) };
}

function writeJson(abs, data) {
  fs.writeFileSync(abs, JSON.stringify(data, null, 2) + "\n");
}

// VERSION file
const versionFile = path.join(root, "VERSION");
fs.writeFileSync(versionFile, version + "\n");
console.log(`  VERSION -> ${version}`);

// package.json
const pkg = readJson("package.json");
pkg.data.version = version;
writeJson(pkg.abs, pkg.data);
console.log(`  package.json -> ${version}`);

// src-tauri/Cargo.toml
const cargoTomlPath = path.join(root, "src-tauri/Cargo.toml");
let cargoToml = fs.readFileSync(cargoTomlPath, "utf8");
cargoToml = cargoToml.replace(
  /^(version\s*=\s*").*(")/m,
  `$1${version}$2`
);
fs.writeFileSync(cargoTomlPath, cargoToml);
console.log(`  src-tauri/Cargo.toml -> ${version}`);

// src-tauri/Cargo.lock
const cargoLockPath = path.join(root, "src-tauri/Cargo.lock");
let cargoLock = fs.readFileSync(cargoLockPath, "utf8");
cargoLock = cargoLock.replace(
  /(name = "typedb-studio"\nversion = ").*(")/,
  `$1${version}$2`
);
fs.writeFileSync(cargoLockPath, cargoLock);
console.log(`  src-tauri/Cargo.lock -> ${version}`);

// src-tauri/tauri.conf.json (top-level version + wix version)
const tauri = readJson("src-tauri/tauri.conf.json");
tauri.data.version = version;
tauri.data.bundle.windows.wix.version = version;
writeJson(tauri.abs, tauri.data);
console.log(`  src-tauri/tauri.conf.json -> ${version}`);

console.log(`\nVersion set to ${version}`);
