// Verificacion temporal (parte A.3). Compara santos RN vs Kotlin dia por dia.
import { readFileSync } from "node:fs";
import path from "node:path";

const dir = "/home/mauricionc/kotlin-igle-dig/verificacion";
const eq = (a, b) => JSON.stringify(a) === JSON.stringify(b);

for (const year of [2024, 2026]) {
  const rn = JSON.parse(readFileSync(path.join(dir, `rn_santos_${year}.json`), "utf8"));
  const kot = JSON.parse(readFileSync(path.join(dir, `kotlin_santos_${year}.json`), "utf8"));
  const keys = Object.keys(rn);
  const diffs = [];
  for (const k of keys) {
    if (!eq(rn[k] ?? [], kot[k] ?? [])) diffs.push(k);
  }
  // claves presentes solo en uno
  const onlyRn = Object.keys(rn).filter((k) => !(k in kot));
  const onlyKot = Object.keys(kot).filter((k) => !(k in rn));
  console.log(`\n===== ${year} =====`);
  console.log(`  dias RN: ${keys.length}  dias Kotlin: ${Object.keys(kot).length}`);
  console.log(`  dias IGUALES: ${keys.length - diffs.length}   DIFERENTES: ${diffs.length}`);
  if (onlyRn.length) console.log("  solo en RN:", onlyRn);
  if (onlyKot.length) console.log("  solo en Kotlin:", onlyKot);
  for (const k of diffs.slice(0, 60)) {
    console.log(`\n  --- ${k} ---`);
    console.log(`    RN     : ${JSON.stringify(rn[k])}`);
    console.log(`    Kotlin : ${JSON.stringify(kot[k])}`);
  }
  if (diffs.length > 60) console.log(`  ... (${diffs.length - 60} difs mas omitidas)`);
}

console.log("\n===== CASO PUNTUAL 2026-10-12 =====");
const rn26 = JSON.parse(readFileSync(path.join(dir, "rn_santos_2026.json"), "utf8"));
const ko26 = JSON.parse(readFileSync(path.join(dir, "kotlin_santos_2026.json"), "utf8"));
console.log("  RN     :", JSON.stringify(rn26["10-12"]));
console.log("  Kotlin :", JSON.stringify(ko26["10-12"]));
