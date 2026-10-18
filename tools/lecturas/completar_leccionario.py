#!/usr/bin/env python3
"""
Completa salmo responsorial y aleluya en la tabla `lecturas` a partir del
leccionario perpetuo de curas.com.ar (tabla `leccionario` del asset).

Contexto: Vatican News (fuente principal de `lecturas`) no publica el salmo ni el
aleluya en su versión en español. curas.com.ar tiene el leccionario completo por
día litúrgico (perpetuo, no por fecha), así que se scrapea una vez y luego se
empareja cada fecha con su página por temporada + semana + ciclo + domingo/feria.

Uso:
  python3 tools/lecturas/completar_leccionario.py             # emparejar y completar (sin red)
  python3 tools/lecturas/completar_leccionario.py --scrape    # re-scrapear curas.com.ar → tabla leccionario, y completar
  python3 tools/lecturas/completar_leccionario.py --preview   # estadísticas sin escribir

Nunca pisa un salmo/aleluya existente: solo rellena lo que está vacío.

Aviso: mismos recaudos de copyright que la Biblia. El repo se mantiene privado.
"""

import argparse
import html
import re
import sqlite3
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DB_PATH = REPO_ROOT / "app/src/main/assets/iglesia_digital.db"
BASE = "https://www.curas.com.ar/Leccionarios"
USER_AGENT = "Mozilla/5.0 (compatible; IglesiaDigital/1.0)"
SLEEP = 0.3

ROMAN = {
    "I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6, "VII": 7, "VIII": 8,
    "IX": 9, "X": 10, "XI": 11, "XII": 12, "XIII": 13, "XIV": 14, "XV": 15,
    "XVI": 16, "XVII": 17, "XVIII": 18, "XIX": 19, "XX": 20, "XXI": 21,
    "XXII": 22, "XXIII": 23, "XXIV": 24, "XXV": 25, "XXVI": 26, "XXVII": 27,
    "XXVIII": 28, "XXIX": 29, "XXX": 30, "XXXI": 31, "XXXII": 32, "XXXIII": 33,
    "XXXIV": 34,
}

# Ciclo dominical por año (A/B/C) — 2026=A, 2027=B, 2028=C...
LITURGICAL_YEAR_CYCLE = {2023: "A", 2024: "B", 2025: "C", 2026: "A", 2027: "B", 2028: "C", 2029: "A"}


# ─── Scrapeo de curas.com.ar ──────────────────────────────────────────────────


def fetch(url: str) -> str:
    time.sleep(SLEEP)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        print(f"  [WARN] HTTP {e.code} {url}")
        return ""
    except Exception as e:  # noqa: BLE001 - red
        print(f"  [WARN] {e} {url}")
        return ""


def extract_text(raw: str) -> str:
    raw = raw.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
    raw = re.sub(r"<script[^>]*>.*?</script>", "", raw, flags=re.DOTALL)
    raw = re.sub(r"<[^>]+>", "", raw)
    raw = re.sub(r"\n[ \t]+", "\n", raw)
    return html.unescape(raw)


def extract_sections(text: str) -> dict:
    """Extrae salmo y aleluya: texto entre los encabezados SALMO/ALELUIA/EVANGELIO."""
    salmo = ""
    aleluia = ""

    salmo_pos = text.upper().find("SALMO")
    aleluia_pos = text.upper().find("ALELUIA")
    evang_pos = text.upper().find("EVANGELIO")

    if salmo_pos >= 0:
        start = text.find("\n", salmo_pos) + 1
        if start <= 0:
            start = salmo_pos + 5
        if aleluia_pos > salmo_pos:
            end = text.rfind("\n", 0, aleluia_pos)
            salmo = text[start:end].strip() if end > start else text[start:].strip()
        elif evang_pos > salmo_pos:
            end = text.rfind("\n", 0, evang_pos)
            salmo = text[start:end].strip() if end > start else text[start:].strip()
        if salmo:
            salmo = re.sub(r"\n{3,}", "\n\n", salmo)

    if aleluia_pos >= 0:
        start = text.find("\n", aleluia_pos) + 1
        if start <= 0:
            start = aleluia_pos + 7
        if evang_pos > aleluia_pos:
            end = text.rfind("\n", 0, evang_pos)
            aleluia = text[start:end].strip() if end > start else text[start:].strip()
        else:
            aleluia = text[start:].strip()
        if aleluia:
            aleluia = re.sub(r"\n{3,}", "\n\n", aleluia)

    return {"salmo": salmo, "aleluia": aleluia}


def extract_links(html_str: str) -> list[str]:
    links = []
    for m in re.finditer(r'href=["\']([^"\']+)["\']', html_str, re.IGNORECASE):
        href = m.group(1)
        if any(
            href.startswith(p) or ("Leccionarios" in href)
            for p in ["Tiempos/", "Santoral/", "Dominical/", "Ferial/"]
        ):
            url = href if href.startswith("http") else BASE + "/" + href.lstrip("/")
            if url not in links:
                links.append(url)
    return links


def scrape_all(conn: sqlite3.Connection) -> None:
    print("Buscando índices de curas.com.ar…")
    idx_urls = [f"{BASE}/{p}" for p in ["Domingos.htm", "Ferias.htm", "Santos.htm"]]
    all_links: list[str] = []
    for url in idx_urls:
        html_str = fetch(url)
        if html_str:
            links = extract_links(html_str)
            all_links.extend(links)
            print(f"  {url.split('/')[-1]}: {len(links)} links")
        else:
            print(f"  {url.split('/')[-1]}: FALLÓ")

    all_links = list(dict.fromkeys(all_links))
    print(f"\nPáginas únicas: {len(all_links)}")

    results = []
    for i, url in enumerate(all_links):
        html_str = fetch(url)
        if not html_str:
            continue
        sec = extract_sections(extract_text(html_str))
        results.append({"url": url, "salmo": sec["salmo"], "aleluia": sec["aleluia"]})
        if (i + 1) % 50 == 0:
            print(f"  [{i + 1}/{len(all_links)}]…")

    conn.execute(
        """CREATE TABLE IF NOT EXISTS leccionario (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            url TEXT UNIQUE,
            salmo TEXT DEFAULT '',
            aleluia TEXT DEFAULT ''
        )"""
    )
    conn.execute("DELETE FROM leccionario")
    conn.executemany(
        "INSERT OR REPLACE INTO leccionario (url, salmo, aleluia) VALUES (:url, :salmo, :aleluia)",
        results,
    )
    conn.commit()
    row = conn.execute(
        """SELECT COUNT(*),
                  SUM(CASE WHEN salmo != '' THEN 1 ELSE 0 END),
                  SUM(CASE WHEN aleluia != '' THEN 1 ELSE 0 END)
           FROM leccionario"""
    ).fetchone()
    print(f"leccionario: {row[0]} filas, {row[1]} con salmo, {row[2]} con aleluya")


# ─── Emparejamiento ───────────────────────────────────────────────────────────

# Fechas cuya misa del día usa lecturas propias (fiesta/solemnidad/semana santa)
# y cuyo título no permite el emparejamiento automático. Verificado una a una
# contra las lecturas publicadas por Vatican News (el día litúrgico coincide).
# Las memorias con lecturas feriales NO van acá: la página del santoral traería
# un salmo que no corresponde a los textos que muestra la app.
# Formato: fecha → fragmento identificador de la URL en la tabla `leccionario`.
_CURADOS = {
    "2026-03-30": "Llnsto",          # Lunes Santo
    "2026-03-31": "Lmtsto",          # Martes Santo
    "2026-04-01": "Lmcsanto",        # Miércoles Santo
    "2026-05-14": "LascencionA",     # Ascensión (ciclo A)
    "2026-07-25": "Lsgoap",          # Santiago, apóstol
    "2026-11-02": "Lmtuttimorti",    # Todos los fieles difuntos
    "2026-11-09": "Ldedbasjdletran", # Dedicación de San Juan de Letrán
    "2026-11-21": "Lmptacionsvm",    # Presentación de la Virgen
    "2026-11-30": "Lsandres",        # San Andrés, apóstol
    "2026-12-08": "Linmaculada",     # Inmaculada Concepción
    "2026-12-26": "Lsesteban",       # San Esteban, protomártir
    "2027-01-01": "Ltheotokos",      # Santa María, Madre de Dios
}


def _roman_to_int(s: str) -> int:
    return ROMAN.get(s.upper(), 0)


def find_match(titulo: str | None, fecha: str, url_to_data: dict) -> dict | None:
    if not titulo:
        return None
    t = titulo.lower()

    has_dom = "domingo" in t
    year_cycle = LITURGICAL_YEAR_CYCLE.get(int(fecha[:4]), "")

    season = None
    if "adviento" in t:
        season = "adviento"
    elif "navidad" in t:
        season = "navidad"
    elif "cuaresma" in t or "ceniza" in t or "santo" in t:
        season = "cuaresma"
    elif "pascua" in t or "pentecost" in t:
        season = "pascua"
    elif "ordinario" in t or "durante el año" in t:
        season = "ordinario"

    if not season:
        return None

    week_num = 0
    m = re.search(r"\b([IVXLCDM]{1,6})\b", titulo.upper())
    if m:
        week_num = _roman_to_int(m.group(1))

    if not week_num:
        for ordinal, num in [
            ("primera", 1), ("primero", 1), ("segunda", 2), ("segundo", 2),
            ("tercera", 3), ("tercero", 3), ("cuarta", 4), ("cuarto", 4),
            ("quinta", 5), ("quinto", 5),
        ]:
            if ordinal in t:
                week_num = num
                break

    candidates = []
    for url in url_to_data:
        url_lower = url.lower()
        score = 0

        if has_dom and ("dominical" in url_lower or "dgo" in url_lower):
            score += 10

        if season == "adviento" and "adviento" in url_lower:
            score += 10
        elif season == "navidad" and "navidad" in url_lower:
            score += 10
        elif season == "cuaresma" and "cuaresma" in url_lower:
            score += 10
        elif season == "pascua" and "pascua" in url_lower:
            score += 10
        elif season == "ordinario" and "dgo" in url_lower:
            score += 10

        if week_num:
            wstr = str(week_num).zfill(2)
            if wstr in url_lower:
                score += 8

        if has_dom and year_cycle and url_lower.endswith(year_cycle.lower() + ".htm"):
            score += 12

        if score > 0:
            candidates.append((score, url))

    if not candidates:
        return None

    candidates.sort(key=lambda x: -x[0])
    return url_to_data[candidates[0][1]]


def _match_curado(fecha: str, url_to_data: dict) -> dict | None:
    """Emparejamiento manual verificado para días con lecturas propias."""
    fragmento = _CURADOS.get(fecha)
    if not fragmento:
        return None
    for url, data in url_to_data.items():
        if fragmento in url:
            return data
    return None


def completar(conn: sqlite3.Connection) -> None:
    try:
        lecc = conn.execute("SELECT url, salmo, aleluia FROM leccionario").fetchall()
    except sqlite3.OperationalError:
        sys.exit("No existe la tabla `leccionario`: corré primero con --scrape.")
    if not lecc:
        sys.exit("La tabla `leccionario` está vacía: corré primero con --scrape.")

    url_to_data = {r[0]: {"salmo": r[1], "aleluia": r[2]} for r in lecc}

    rows = conn.execute(
        """SELECT fecha, titulo_misa, salmo, aleluia FROM lecturas
           WHERE salmo IS NULL OR salmo = '' OR aleluia IS NULL OR aleluia = ''"""
    ).fetchall()
    print(f"Filas incompletas: {len(rows)}")

    lleno_salmo = lleno_aleluia = sin_match = 0
    sin_match_fechas: list[str] = []
    for fecha, titulo, salmo, aleluia in rows:
        match = _match_curado(fecha, url_to_data) or find_match(titulo, fecha, url_to_data)
        if not match:
            sin_match += 1
            sin_match_fechas.append(fecha)
            continue
        nuevo_salmo = match["salmo"] if (not salmo and match["salmo"]) else salmo
        nuevo_aleluia = match["aleluia"] if (not aleluia and match["aleluia"]) else aleluia
        if nuevo_salmo != salmo or nuevo_aleluia != aleluia:
            conn.execute(
                "UPDATE lecturas SET salmo = ?, aleluia = ? WHERE fecha = ?",
                (nuevo_salmo, nuevo_aleluia, fecha),
            )
            lleno_salmo += 1 if (not salmo and nuevo_salmo) else 0
            lleno_aleluia += 1 if (not aleluia and nuevo_aleluia) else 0
    conn.commit()
    print(f"Completados: {lleno_salmo} salmos, {lleno_aleluia} aleluyas · sin match: {sin_match}")
    if sin_match_fechas:
        print(f"Fechas sin match: {', '.join(sin_match_fechas)}")


def preview(conn: sqlite3.Connection) -> None:
    row = conn.execute(
        """SELECT COUNT(*), SUM(CASE WHEN fecha IS NULL THEN 1 ELSE 0 END) FROM lecturas"""
    ).fetchone()
    print(f"lecturas: {row[0]} filas")
    try:
        row = conn.execute(
            """SELECT COUNT(*),
                      SUM(CASE WHEN salmo != '' THEN 1 ELSE 0 END),
                      SUM(CASE WHEN aleluia != '' THEN 1 ELSE 0 END)
               FROM leccionario"""
        ).fetchone()
        print(f"leccionario: {row[0]} filas, {row[1]} con salmo, {row[2]} con aleluya")
    except sqlite3.OperationalError:
        print("leccionario: no existe la tabla")
    row = conn.execute(
        """SELECT
               SUM(CASE WHEN salmo IS NULL OR salmo = '' THEN 1 ELSE 0 END),
               SUM(CASE WHEN aleluia IS NULL OR aleluia = '' THEN 1 ELSE 0 END)
           FROM lecturas"""
    ).fetchone()
    print(f"lecturas sin salmo: {row[0]} · sin aleluya: {row[1]}")


# ─── CLI ──────────────────────────────────────────────────────────────────────


def main() -> None:
    ap = argparse.ArgumentParser(description="Completa salmo/aleluya desde curas.com.ar")
    ap.add_argument("--scrape", action="store_true", help="Re-scrapear curas.com.ar → tabla leccionario")
    ap.add_argument("--preview", action="store_true", help="Estadísticas sin escribir")
    args = ap.parse_args()

    if not DB_PATH.exists():
        sys.exit(f"No existe la base: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)

    if args.preview:
        preview(conn)
        return

    if args.scrape:
        scrape_all(conn)

    completar(conn)
    # Consolidar WAL en el archivo principal: el asset se versiona como un único .db
    conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    conn.close()


if __name__ == "__main__":
    main()
