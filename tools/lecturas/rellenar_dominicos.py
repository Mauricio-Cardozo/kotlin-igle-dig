#!/usr/bin/env python3
"""
Relleno puntual de UNA fecha desde dominicos.org (fallback).

Cuando Vatican News todavía no publicó la página de una fecha (404 dentro de su
ventana) o su página no existe, dominicos.org publica las lecturas completas de
las ferias con meses de anticipación. Este script arma la fila de `lecturas` con
ese contenido (título del día, 1ª lectura, salmo, evangelio) en el formato de la
app.

Limitaciones de dominicos como fuente de relleno:
  · Domingos y solemnidades lejanas suelen estar «Pendiente de publicación»
    (no los cubre este script; los cubre Vatican News cuando se acerca la fecha).
  · No publica «segunda lectura» ni las palabras del Papa.
  · El formato del salmo es el suyo (con la referencia incluida); sirve como
    excepción documentada, no como fuente masiva.

Uso:
  python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12 --preview
  python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12

Aviso: mismos recaudos de copyright que la Biblia. El repo se mantiene privado.
"""

import argparse
import html
import re
import sqlite3
import sys
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DB_PATH = REPO_ROOT / "app/src/main/assets/iglesia_digital.db"
BASE_URL = "https://www.dominicos.org/predicacion/evangelio-del-dia/{fecha}/"
USER_AGENT = "Mozilla/5.0 (compatible; IglesiaDigital/1.0)"

# Prefijos que dominicos usa en las referencias y que no son parte del nombre del libro
_PREFIJOS_LIBRO = (
    "Lectura del libro del ",
    "Lectura del libro de ",
    "Lectura de la carta del apóstol san ",
    "Lectura de la carta del apóstol ",
    "Lectura de la carta a los ",
    "Lectura de la carta a ",
    "Lectura de la primera carta del apóstol san ",
    "Lectura de la segunda carta del apóstol san ",
    "Lectura del santo evangelio según san ",
    "Lectura del santo evangelio según ",
    "Lectura de la ",
    "Lectura del ",
    "Lectura de ",
)

_ETIQUETAS = {
    "primera lectura": "primera",
    "salmo de hoy": "salmo",
    "evangelio del día": "evangelio",
    "reflexión del evangelio de hoy": "reflexion",
    "suscripción gratuita al evangelio del día": "fin",
}


def fetch_texto(fecha: date) -> str | None:
    url = BASE_URL.format(fecha=fecha.isoformat())
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code} en {url}")
        return None
    except Exception as e:  # noqa: BLE001 - red
        print(f"Error: {e}")
        return None

    raw = re.sub(r"<(script|style)[^>]*>.*?</\1>", "", raw, flags=re.DOTALL)
    texto = re.sub(r"<[^>]+>", "\n", raw)
    texto = html.unescape(texto)
    return "\n".join(l.strip() for l in texto.split("\n") if l.strip())


def parsear(texto: str) -> dict:
    """Devuelve los campos de la fila a partir del texto plano de la página."""
    lineas = texto.split("\n")
    secciones: dict[str, list[str]] = {}
    actual = "inicio"
    primera_evangelio = True

    for linea in lineas:
        clave = _ETIQUETAS.get(linea.lower())
        if clave == "evangelio" and primera_evangelio:
            # La primera aparición es el encabezado de la página; la siguiente
            # línea trae el título del día litúrgico.
            primera_evangelio = False
            actual = "titulo"
            continue
        if clave:
            actual = clave
            secciones.setdefault(actual, [])
            continue
        secciones.setdefault(actual, []).append(linea)

    titulo_lineas = []
    for linea in secciones.get("titulo", []):
        if any(m in linea for m in ("Descargar", "Ver otros años", "Compartir", "Imprimir", "Suscripción")):
            break
        titulo_lineas.append(linea)
    titulo = " ".join(titulo_lineas).strip() or None
    if not secciones.get("evangelio"):
        print("La página no tiene lecturas publicadas (¿«Pendiente de publicación»?)")
        return {}

    def _ref_y_texto(lineas_sec: list[str]) -> tuple[str | None, str | None]:
        if not lineas_sec:
            return None, None
        combinada = lineas_sec[0]
        m = re.search(r"(\d.*)$", combinada)
        if m:
            ref_plana = combinada[: m.start()].strip().rstrip(",;.")
            numeros = m.group(1).strip()
            libro = ref_plana
            for prefijo in _PREFIJOS_LIBRO:
                if libro.startswith(prefijo):
                    libro = libro[len(prefijo):]
                    break
            primera_linea = f"{libro} {numeros}"
        else:
            ref_plana, primera_linea = combinada, None
        cuerpo = "\n\n".join(lineas_sec[1:])
        partes = [p for p in (primera_linea, cuerpo) if p]
        return ref_plana or None, "\n\n".join(partes) or None

    primera_ref, primera_texto = _ref_y_texto(secciones.get("primera", []))
    evangelio_ref, evangelio_texto = _ref_y_texto(secciones.get("evangelio", []))
    salmo_lineas = secciones.get("salmo", [])
    salmo = "\n\n".join(salmo_lineas) if salmo_lineas else None

    return {
        "titulo_misa": titulo,
        "primera_lectura_ref": primera_ref,
        "primera_lectura": primera_texto,
        "salmo": salmo,
        "evangelio_ref": evangelio_ref,
        "evangelio": evangelio_texto,
    }


def main() -> None:
    ap = argparse.ArgumentParser(description="Rellena una fecha desde dominicos.org")
    ap.add_argument("--fecha", required=True, type=str, help="YYYY-MM-DD")
    ap.add_argument("--preview", action="store_true", help="Mostrar sin guardar")
    args = ap.parse_args()

    fecha = date.fromisoformat(args.fecha)
    texto = fetch_texto(fecha)
    if texto is None:
        sys.exit(1)

    datos = parsear(texto)
    if not datos.get("evangelio"):
        sys.exit(2)

    if args.preview:
        for clave, valor in datos.items():
            recorte = (valor or "").replace("\n", " ")[:110]
            print(f"{clave:22s}: {recorte}")
        return

    if not DB_PATH.exists():
        sys.exit(f"No existe la base: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)

    existe = conn.execute("SELECT 1 FROM lecturas WHERE fecha = ?", (args.fecha,)).fetchone()
    if existe:
        sys.exit(f"La fecha {args.fecha} ya existe en lecturas: no se toca.")

    conn.execute(
        """INSERT INTO lecturas
           (fecha, url, titulo_misa, primera_lectura_ref, primera_lectura,
            salmo, evangelio_ref, evangelio)
           VALUES (:fecha, :url, :titulo_misa, :primera_lectura_ref,
                   :primera_lectura, :salmo, :evangelio_ref, :evangelio)""",
        {
            "fecha": args.fecha,
            "url": BASE_URL.format(fecha=args.fecha),
            **datos,
        },
    )
    conn.commit()
    conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    conn.close()
    print(f"Fila {args.fecha} insertada desde dominicos (1ª lectura + salmo + evangelio).")


if __name__ == "__main__":
    main()
