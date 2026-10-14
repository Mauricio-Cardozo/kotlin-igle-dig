// Verificacion temporal. Corre el getSantosDelDia REAL del RN sobre la DB local.
// No modifica el repo RN. Salida: rn_santos_2024.json / rn_santos_2026.json
import { createRequire } from "node:module";
import { readFileSync, writeFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import path from "node:path";

const RN = "/home/mauricionc/Documentos/bible-app/AppMovil";
const OUT = "/home/mauricionc/kotlin-igle-dig/verificacion";
const requireRN = createRequire(path.join(RN, "package.json"));
const initSqlJs = requireRN("sql.js");

const SQL = await initSqlJs({
  locateFile: (f) => path.join(RN, "node_modules", "sql.js", "dist", f),
});
const buf = readFileSync(path.join(RN, "assets", "iglesia_digital.db"));
const sqlDb = new SQL.Database(buf);

// Adaptador minimo compatible con la interfaz que usa getSantosDelDia (getAllAsync).
const db = {
  getAllAsync: async (sql, params = []) => {
    const stmt = sqlDb.prepare(sql);
    if (params && params.length) stmt.bind(params);
    const rows = [];
    while (stmt.step()) rows.push(stmt.getAsObject());
    stmt.free();
    return rows;
  },
};

// Importa la funcion REAL (no reescrita) del repo RN.
const santosMod = await import(pathToFileURL(path.join(RN, "db", "santos.ts")).href);
const getSantosDelDia = santosMod.getSantosDelDia;
if (typeof getSantosDelDia !== "function") throw new Error("No se importo getSantosDelDia");

async function dumpYear(year) {
  const res = {};
  const d = new Date(Date.UTC(year, 0, 1));
  while (d.getUTCFullYear() === year) {
    const mm = String(d.getUTCMonth() + 1).padStart(2, "0");
    const dd = String(d.getUTCDate()).padStart(2, "0");
    const rows = await getSantosDelDia(db, d.getUTCMonth() + 1, d.getUTCDate());
    res[`${mm}-${dd}`] = rows.map((r) => r.nombre);
    d.setUTCDate(d.getUTCDate() + 1);
  }
  const file = path.join(OUT, `rn_santos_${year}.json`);
  writeFileSync(file, JSON.stringify(res, null, 2) + "\n");
  console.log(`RN ${year}: ${Object.keys(res).length} dias -> ${file}`);
}

await dumpYear(2024);
await dumpYear(2026);
