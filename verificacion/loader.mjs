// Loader: sustituye "expo-sqlite" y "@/..." por un stub para poder importar
// el getSantosDelDia REAL del RN sin instalar nada ni tocar el repo RN.
import { pathToFileURL } from "node:url";

const STUB = pathToFileURL(
  "/home/mauricionc/kotlin-igle-dig/verificacion/_stub.mjs"
).href;

export async function resolve(specifier, context, nextResolve) {
  if (specifier === "expo-sqlite" || specifier.startsWith("@/")) {
    return { shortCircuit: true, url: STUB };
  }
  return nextResolve(specifier, context);
}
