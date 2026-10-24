#!/usr/bin/env node

const { spawn, spawnSync } = require('node:child_process');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const raiz = path.resolve(__dirname, '..');
const args = process.argv.slice(2);
const perfil = args.find((a) => !a.startsWith('--')) ?? '';
const puertoListo = Number((args.find((a) => a.startsWith('--ready-port=')) ?? '').split('=')[1]) || 0;
const esWindows = process.platform === 'win32';

const servicios = [
  { nombre: 'eureka-server', puerto: 8761 },
  { nombre: 'ms-auth', puerto: 8083 },
  { nombre: 'ms-catalogos', puerto: 8081 },
  { nombre: 'ms-cobranza', puerto: 8082 },
  { nombre: 'ms-egresos', puerto: 8084 },
  { nombre: 'ms-reportes', puerto: 8085 },
  { nombre: 'ms-auditoria', puerto: 8086 },
  { nombre: 'api-gateway', puerto: 8762 },
];

const jar = (nombre) => path.join(raiz, nombre, 'target', `${nombre}-0.0.1-SNAPSHOT.jar`);

if (servicios.some((s) => !fs.existsSync(jar(s.nombre)))) {
  console.log('[launcher] Compilando los servicios (mvnw package -DskipTests)...');
  const mvnw = esWindows ? 'mvnw.cmd' : './mvnw';
  const r = spawnSync(mvnw, ['-q', '-DskipTests', 'package'], { cwd: raiz, stdio: 'inherit', shell: esWindows });
  if (r.status !== 0) {
    console.error('[launcher] La compilacion fallo.');
    process.exit(1);
  }
}

const hijos = [];
function detenerTodo() {
  for (const hijo of hijos) {
    if (esWindows) {
      spawnSync('taskkill', ['/pid', String(hijo.pid), '/T', '/F'], { stdio: 'ignore' });
    } else {
      hijo.kill('SIGTERM');
    }
  }
}
process.on('SIGINT', () => { detenerTodo(); process.exit(0); });
process.on('SIGTERM', () => { detenerTodo(); process.exit(0); });
process.on('exit', detenerTodo);

function arrancar({ nombre }) {
  const argsJava = ['-jar', jar(nombre)];
  if (perfil) argsJava.push(`--spring.profiles.active=${perfil}`);
  const hijo = spawn('java', argsJava, { cwd: raiz, stdio: ['ignore', 'pipe', 'pipe'] });
  const prefijo = `[${nombre}] `;
  const escribir = (datos) => {
    for (const linea of datos.toString().split(/\r?\n/)) {
      if (/Started \w+ in|ERROR|APPLICATION FAILED/.test(linea)) console.log(prefijo + linea.slice(0, 200));
    }
  };
  hijo.stdout.on('data', escribir);
  hijo.stderr.on('data', escribir);
  hijo.on('exit', (codigo) => console.log(`${prefijo}termino (codigo ${codigo})`));
  hijos.push(hijo);
}

function responde(puerto, ruta = '/') {
  return new Promise((resolve) => {
    const req = http.get({ host: 'localhost', port: puerto, path: ruta, timeout: 2000 }, (res) => {
      res.resume();
      resolve(res.statusCode < 500);
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => { req.destroy(); resolve(false); });
  });
}

const espera = (ms) => new Promise((r) => setTimeout(r, ms));

async function esperar(condicion, descripcion, maximoSeg = 240) {
  const limite = Date.now() + maximoSeg * 1000;
  while (Date.now() < limite) {
    if (await condicion()) return;
    await espera(1500);
  }
  throw new Error(`Tiempo agotado esperando: ${descripcion}`);
}

async function stackListo() {
  try {
    const login = await fetch('http://localhost:8762/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' }),
    });
    if (login.status !== 200) return false;
    const { token } = await login.json();
    for (const ruta of ['socios', 'cuentas-por-cobrar', 'egresos']) {
      const r = await fetch(`http://localhost:8762/api/${ruta}`, { headers: { Authorization: `Bearer ${token}` } });
      if (r.status !== 200) return false;
    }
    return true;
  } catch {
    return false;
  }
}

(async () => {
  console.log(`[launcher] Perfil: ${perfil || 'por defecto (MySQL + RabbitMQ)'}`);
  arrancar(servicios[0]);
  await esperar(() => responde(8761), 'eureka-server');

  for (const servicio of servicios.slice(1, -1)) arrancar(servicio);
  arrancar(servicios[servicios.length - 1]);

  await esperar(stackListo, 'que el gateway enrute a todos los servicios');
  console.log('[launcher] Todos los servicios estan listos. Gateway: http://localhost:8762  Eureka: http://localhost:8761');

  if (puertoListo) {
    http.createServer((req, res) => { res.end('listo'); }).listen(puertoListo);
    console.log(`[launcher] Endpoint de disponibilidad: http://localhost:${puertoListo}/`);
  }
})().catch((error) => {
  console.error('[launcher]', error.message);
  detenerTodo();
  process.exit(1);
});
