// Verificacion temporal (parte B). Inspecciona la DB del asset Kotlin y la del RN.
import { createRequire } from "node:module";
import { readFileSync, statSync } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";

const RN = "/home/mauricionc/Documentos/bible-app/AppMovil";
const KOT = "/home/mauricionc/kotlin-igle-dig";
const requireRN = createRequire(path.join(RN, "package.json"));
const initSqlJs = requireRN("sql.js");

const kotDb = path.join(KOT, "app/src/main/assets/iglesia_digital.db");
const rnDb = path.join(RN, "assets/iglesia_digital.db");

const sha = (f) => createHash("sha256").update(readFileSync(f)).digest("hex");
console.log("=== B7: sha256 de los assets ===");
console.log("Kotlin:", kotDb);
console.log("  size:", statSync(kotDb).size, "sha256:", sha(kotDb));
console.log("RN:    ", rnDb);
console.log("  size:", statSync(rnDb).size, "sha256:", sha(rnDb));
console.log("IGUALES:", sha(kotDb) === sha(rnDb));

const SQL = await initSqlJs({
  locateFile: (f) => path.join(RN, "node_modules", "sql.js", "dist", f),
});

for (const [label, file] of [["KOTLIN", kotDb], ["RN", rnDb]]) {
  const db = new SQL.Database(readFileSync(file));
  const one = (sql, params = []) => {
    const s = db.prepare(sql);
    if (params.length) s.bind(params);
    const rows = [];
    while (s.step()) rows.push(s.getAsObject());
    s.free();
    return rows;
  };
  console.log(`\n================ DB ${label} ================`);
  console.log("=== Tablas y filas ===");
  const tables = one(
    "SELECT name, type FROM sqlite_master WHERE type IN ('table','view') ORDER BY name"
  );
  for (const t of tables) {
    let n = "?";
    try {
      if (t.type === "table") n = one(`SELECT COUNT(*) AS c FROM "${t.name}"`)[0].c;
    } catch (e) {
      n = "err:" + e.message;
    }
    console.log(`  ${t.type.padEnd(5)} ${String(n).padStart(8)}  ${t.name}`);
  }

  console.log("=== B5: 'Biblia de Navarra' en metadatos/tablas ===");
  const navSchema = one(
    "SELECT name FROM sqlite_master WHERE lower(name) LIKE '%navarra%'"
  );
  console.log("  tablas/vistas con 'navarra':", navSchema.length ? navSchema : "(ninguna)");
  // Buscar en cualquier tabla de metadatos/version
  console.log("  tablas tipo meta/version:");
  for (const t of tables.filter((x) =>
    /version|meta|info|config/i.test(x.name)
  )) {
    try {
      console.log("   -", t.name, JSON.stringify(one(`SELECT * FROM "${t.name}" LIMIT 20`)));
    } catch (e) {
      console.log("   -", t.name, "err", e.message);
    }
  }
}

const db = new SQL.Database(readFileSync(kotDb));
const one = (sql, params = []) => {
  const s = db.prepare(sql);
  if (params.length) s.bind(params);
  const rows = [];
  while (s.step()) rows.push(s.getAsObject());
  s.free();
  return rows;
};

console.log("\n=== B6: cobertura de 'lecturas' (DB Kotlin) ===");
console.log("  min/max:", JSON.stringify(one("SELECT MIN(fecha) mn, MAX(fecha) mx, COUNT(*) c FROM lecturas")[0]));
const fechas = one("SELECT fecha FROM lecturas WHERE fecha IS NOT NULL ORDER BY fecha").map((r) => r.fecha);
const set = new Set(fechas);
console.log("  filas con fecha:", fechas.length, " unicas:", set.size);
for (const f of ["2026-03-29", "2026-04-05"]) {
  console.log(`  existe ${f}:`, set.has(f));
}
// Faltantes dentro del rango
const [mn, mx] = [fechas[0], fechas[fechas.length - 1]];
const missing = [];
const d = new Date(mn + "T00:00:00Z");
const end = new Date(mx + "T00:00:00Z");
while (d <= end) {
  const iso = d.toISOString().slice(0, 10);
  if (!set.has(iso)) missing.push(iso);
  d.setUTCDate(d.getUTCDate() + 1);
}
console.log(`  faltantes en [${mn}..${mx}]:`, missing.length ? missing : "(ninguna)");

console.log("\n=== Columnas de tablas clave ===");
for (const t of ["lecturas", "santos", "misal_propio", "misal_santos", "catecismo"]) {
  try {
    const cols = one(`PRAGMA table_info("${t}")`).map((c) => c.name);
    console.log(`  ${t}: ${cols.join(", ")}`);
  } catch (e) {
    console.log(`  ${t}: err ${e.message}`);
  }
}
