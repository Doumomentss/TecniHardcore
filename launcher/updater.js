const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Readable, Transform } = require('stream');
const { pipeline } = require('stream/promises');

const REPOSITORY = 'Doumomentss/TecniHardcore';
const FEED = `https://github.com/${REPOSITORY}/releases/latest/download/update.json`;
function versionParts(value) {
  if (typeof value !== 'string' || !/^\d+\.\d+\.\d+$/.test(value)) throw Error('Versión de actualización inválida.');
  return value.split('.').map(Number);
}
function newer(next, current) {
  const a = versionParts(next), b = versionParts(current);
  for (let i = 0; i < 3; i++) if (a[i] !== b[i]) return a[i] > b[i];
  return false;
}
function validateManifest(manifest) {
  if (!manifest || manifest.schema !== 1) throw Error('Formato de actualización no compatible.');
  versionParts(manifest.version);
  const expected = `https://github.com/${REPOSITORY}/releases/download/v${manifest.version}/TecniHardcore-Setup-${manifest.version}.exe`;
  if (manifest.installer?.url !== expected || !/^[a-f0-9]{64}$/.test(manifest.installer.sha256 || '') ||
      !Number.isSafeInteger(manifest.installer.bytes) || manifest.installer.bytes < 1 || manifest.installer.bytes > 2 * 1024 ** 3) {
    throw Error('La actualización no tiene una descarga oficial válida.');
  }
  if (typeof manifest.notes !== 'string' || manifest.notes.length > 8000) throw Error('Notas de actualización inválidas.');
  return manifest;
}
async function check(current, fetcher = fetch) {
  const response = await fetcher(FEED, { signal: AbortSignal.timeout(15000), headers: {'User-Agent':'TecniHardcore-Launcher'} });
  if (!response.ok) throw Error(response.status === 404 ? 'Todavía no hay una actualización publicada.' : `GitHub no respondió (${response.status}).`);
  const body = await response.text();
  if (Buffer.byteLength(body) > 32768) throw Error('Respuesta de actualización demasiado grande.');
  const manifest = validateManifest(JSON.parse(body));
  return {current, available:newer(manifest.version, current), manifest};
}
async function download(manifest, directory, progress = () => {}, signal, fetcher = fetch) {
  validateManifest(manifest);
  await fs.promises.mkdir(directory, {recursive:true});
  const disk = await fs.promises.statfs(directory);
  if (Number(disk.bavail) * Number(disk.bsize) < manifest.installer.bytes * 6) throw Error('Libera al menos 3 GB para descargar, instalar y guardar la copia de seguridad.');
  // A previous attempt may still be executing on Windows. Never replace its EXE
  // or share a .part file with another launcher instance.
  const target = path.join(directory, `TecniHardcore-Setup-${manifest.version}-${crypto.randomUUID()}.exe`);
  const temporary = target + '.part';
  let received = 0;
  const hash = crypto.createHash('sha256');
  try {
    const timeout = AbortSignal.timeout(30 * 60 * 1000);
    const response = await fetcher(manifest.installer.url, {signal:signal ? AbortSignal.any([signal,timeout]) : timeout});
    if (!response.ok || !response.body) throw Error(`No se pudo descargar la actualización (${response.status}).`);
    const observer = new Transform({transform(chunk, _, callback) {
      received += chunk.length;
      if (received > manifest.installer.bytes) return callback(Error('La descarga supera el tamaño publicado.'));
      hash.update(chunk);
      progress({received,total:manifest.installer.bytes,percentage:Math.min(99,Math.floor(received / manifest.installer.bytes * 100))});
      callback(null,chunk);
    }});
    await pipeline(Readable.fromWeb(response.body),observer,fs.createWriteStream(temporary),{signal});
    if (received !== manifest.installer.bytes || hash.digest('hex') !== manifest.installer.sha256) throw Error('La actualización no superó la verificación SHA-256. No se instaló ningún archivo.');
    await fs.promises.rename(temporary,target);
    progress({received,total:manifest.installer.bytes,percentage:100,message:'Descarga verificada. Preparando la instalación…'});
    return target;
  } catch(error) {
    await fs.promises.rm(temporary,{force:true});
    throw error;
  }
}
module.exports = {REPOSITORY,FEED,newer,validateManifest,check,download};
