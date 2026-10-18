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
import unicodedata
import urllib.error
import urllib.request
from datetime import date, timedelta
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
LITURGICAL_YEAR_CYCLE = {
    2023: "A", 2024: "B", 2025: "C", 2026: "A", 2027: "B", 2028: "C",
    2029: "A", 2030: "B", 2031: "C", 2032: "A",
}


# ─── Calendario litúrgico ────────────────────────────────────────────────────
# Para las ferias y memorias cuyo título no trae la semana, la temporada y el
# número de semana se calculan con el calendario (Pascua y Adviento incluidos).


def pascua_de(anio: int) -> date:
    """Domingo de Pascua (algoritmo gregoriano anónimo)."""
    a = anio % 19
    b, c = anio // 100, anio % 100
    d, e = b // 4, b % 4
    f = (b + 8) // 25
    g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30
    i, k = c // 4, c % 4
    l = (32 + 2 * e + 2 * i - h - k) % 7  # noqa: E741
    m = (a + 11 * h + 22 * l) // 451
    mes = (h + l - 7 * m + 114) // 31
    dia = ((h + l - 7 * m + 114) % 31) + 1
    return date(anio, mes, dia)


def _domingo_de_semana(fecha: date) -> date:
    """Domingo de la semana litúrgica que contiene a `fecha` (semana lun-sáb)."""
    return fecha - timedelta(days=(fecha.weekday() + 1) % 7)


def epifania_dominical(anio: int) -> date:
    """Epifanía del Señor: el domingo que cae entre el 2 y el 8 de enero
    (en Argentina la solemnidad se celebra el domingo)."""
    d = date(anio, 1, 2)
    return d + timedelta(days=(6 - d.weekday()) % 7)


def bautismo_de(anio: int) -> date:
    """Bautismo del Señor: el domingo a partir del 7 de enero; si la Epifanía
    cae el domingo 7 u 8 de enero, pasa al lunes siguiente."""
    epifania = epifania_dominical(anio)
    if epifania.day >= 7:
        return epifania + timedelta(days=1)
    d = date(anio, 1, 7)
    return d + timedelta(days=(6 - d.weekday()) % 7)


def adviento_inicio(anio: int) -> date:
    """Primer domingo de Adviento: primer domingo a partir del 27 de noviembre."""
    d = date(anio, 11, 27)
    return d + timedelta(days=(6 - d.weekday()) % 7)


def ciclo_dominical(fecha: date) -> str:
    """Ciclo A/B/C de la fecha, en minúscula.

    El año litúrgico empieza el primer domingo de Adviento: los domingos de
    Adviento y las fiestas de diciembre ya pertenecen al ciclo del año civil
    siguiente (p. ej. Adviento 2026 = ciclo B).
    """
    anio = fecha.year + (1 if fecha >= adviento_inicio(fecha.year) else 0)
    return LITURGICAL_YEAR_CYCLE.get(anio, "A").lower()


def tiempo_semana(fecha: date) -> tuple[str, int]:
    """(tiempo, semana) para una fecha: ordinario / pascua / adviento / navidad / cuaresma."""
    anio = fecha.year
    pascua = pascua_de(anio)
    ceniza = pascua - timedelta(days=46)
    pentecostes = pascua + timedelta(days=49)
    adv1 = adviento_inicio(anio)
    navidad = date(anio, 12, 25)

    if navidad <= fecha or fecha < bautismo_de(anio):
        return ("navidad", 0)
    if adv1 <= fecha:
        return ("adviento", min((fecha - adv1).days // 7 + 1, 4))
    if ceniza <= fecha < pascua:
        primer_domingo = ceniza + timedelta(days=4)
        if fecha < primer_domingo:
            return ("cuaresma", 0)  # ceniza y ferias inmediatas
        return ("cuaresma", min((fecha - primer_domingo).days // 7 + 1, 6))
    if pascua <= fecha <= pentecostes:
        return ("pascua", min((fecha - pascua).days // 7 + 1, 7))
    if fecha < ceniza:  # ordinario antes de Cuaresma (semanas 1-6)
        n = 6 - ((ceniza - timedelta(days=3)) - _domingo_de_semana(fecha)).days // 7
        return ("ordinario", max(n, 1))
    # ordinario después de Pentecostés (semanas 8-34)
    n = 34 - ((adv1 - timedelta(days=7)) - _domingo_de_semana(fecha)).days // 7
    return ("ordinario", min(n, 34))


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


_CITA_SUELTA = re.compile(
    r"^(?:Cf\.?\s*)?(?:[1-3]\s?)?[A-ZÁÉÍÓÚÜÑ][a-záéíóúüñ]{1,4}\.?\s+\d+[\d,.\s;:¡!¿?()a-záéíóúüñ\-]*$"
)


def _quita_cita(trozo: str) -> str:
    """Quita la cita bíblica que la maqueta de curas a veces deja suelta como
    primera línea de la sección, cuando no cabe en la línea del encabezado
    (p. ej. «ALELUIA   » y en la línea siguiente «Flp 3, 8-9»)."""
    lineas_sec = trozo.split("\n")
    i = 0
    while i < len(lineas_sec) and not lineas_sec[i].strip():
        i += 1
    if i < len(lineas_sec):
        v = lineas_sec[i].strip()
        if len(v) <= 45 and _CITA_SUELTA.match(v):
            i += 1
    return "\n".join(lineas_sec[i:]).strip()


_ALELUIA_INICIAL = re.compile(r"^(?:Aleluia|Aleluya)\.?\s*$", re.IGNORECASE)
_ALELUIA_INLINE = re.compile(r"^(?:Aleluia|Aleluya)\.\s+(.+)$", re.IGNORECASE)


def _sin_aleluia_inicial(trozo: str) -> str:
    """Quita el «Aleluia.» inicial de la aclamación: la app ya rotula el bloque
    como «Aleluya» y los textos heredados del pipeline RN no lo traen."""
    lineas_sec = trozo.split("\n")
    i = 0
    while i < len(lineas_sec) and not lineas_sec[i].strip():
        i += 1
    if i < len(lineas_sec):
        v = lineas_sec[i].strip()
        if _ALELUIA_INICIAL.match(v):
            i += 1
        else:
            m = _ALELUIA_INLINE.match(v)
            if m:
                lineas_sec[i] = m.group(1)
    return "\n".join(lineas_sec[i:]).strip()


def _secciones(lineas: list[str]) -> dict:
    """Extrae salmo y aleluya de un bloque de líneas entre encabezados.

    Los encabezados de sección van en mayúsculas (p. ej. «ALELUIA   Jn 20, 29»).
    Buscar «aleluia» sin distinguir mayúsculas enganchaba la respuesta del salmo
    («Aleluia.») antes de la aclamación real (visible en Pascua), así que la
    aclamación se ancla a una línea que empiece con ALELUIA/ALELUYA en mayúsculas.
    """
    salmo_i = aleluia_i = evang_i = -1
    for i, linea in enumerate(lineas):
        v = linea.strip()
        if salmo_i < 0 and v.upper().startswith("SALMO"):
            salmo_i = i
        elif aleluia_i < 0 and (v.startswith("ALELUIA") or v.startswith("ALELUYA")):
            aleluia_i = i
        elif evang_i < 0 and v.upper().startswith("EVANGELIO"):
            evang_i = i

    def _seccion(ini: int, fin: int) -> str:
        if ini < 0 or fin <= ini:
            return ""
        trozo = _quita_cita("\n".join(lineas[ini + 1:fin]))
        return re.sub(r"\n{3,}", "\n\n", trozo) if trozo else ""

    if salmo_i >= 0:
        fin = aleluia_i if aleluia_i > salmo_i else (evang_i if evang_i > salmo_i else len(lineas))
        salmo = _seccion(salmo_i, fin)
    else:
        salmo = ""

    if aleluia_i >= 0:
        fin = evang_i if evang_i > aleluia_i else len(lineas)
        aleluia = _sin_aleluia_inicial(_seccion(aleluia_i, fin))
    else:
        aleluia = ""

    return {"salmo": salmo, "aleluia": aleluia}


def extract_sections(text: str) -> dict:
    return _secciones(text.split("\n"))


_DIAS_CANON = {
    "lunes": "lunes", "martes": "martes", "miercoles": "miercoles",
    "miércoles": "miercoles", "jueves": "jueves", "viernes": "viernes",
    "sabado": "sabado", "sábado": "sabado", "domingo": "domingo",
}

_MESES = {
    "enero": 1, "febrero": 2, "marzo": 3, "abril": 4, "mayo": 5, "junio": 6,
    "julio": 7, "agosto": 8, "septiembre": 9, "setiembre": 9, "octubre": 10,
    "noviembre": 11, "diciembre": 12,
}


def _canon_dia(linea: str) -> str:
    """Clave de día para una línea-encabezado: «lunes»..«sabado» o «MM-DD»."""
    v = linea.strip().strip(".:").strip().lower()
    if v in _DIAS_CANON:
        return _DIAS_CANON[v]
    m = re.fullmatch(r"d[íi]a\s+(\d{1,2})\s+de\s+([a-záéíóú]+)", linea.strip().lower())
    if m and m.group(2) in _MESES:
        return f"{_MESES[m.group(2)]:02d}-{int(m.group(1)):02d}"
    return ""


def extract_dias(text: str) -> dict:
    """Secciones por día de una página ferial (lunes..sábado) o por fecha
    («DÍA 2 DE ENERO»). Devuelve {} si la página no es multi-día."""
    lineas = text.split("\n")
    encabezados = [(i, _canon_dia(l)) for i, l in enumerate(lineas)]
    encabezados = [(i, d) for i, d in encabezados if d]
    if len(encabezados) < 2:
        return {}
    resultado = {}
    for k, (i, dia) in enumerate(encabezados):
        fin = encabezados[k + 1][0] if k + 1 < len(encabezados) else len(lineas)
        resultado[dia] = _secciones(lineas[i + 1:fin])
    return resultado


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
    # Páginas que la web ya no enlaza desde los índices pero siguen publicadas:
    # Semana Santa trae salmo (en esas ferias no se dice «Aleluya»).
    for extra in ["Tiempos/Cuaresma/Llnsto.htm", "Tiempos/Cuaresma/Lmtsto.htm",
                  "Tiempos/Cuaresma/Lmcsanto.htm"]:
        url = f"{BASE}/{extra}"
        if url not in all_links:
            all_links.append(url)
    print(f"\nPáginas únicas: {len(all_links)}")

    results = []
    multi_dia = 0
    for i, url in enumerate(all_links):
        html_str = fetch(url)
        if not html_str:
            continue
        text = extract_text(html_str)
        dias = extract_dias(text)
        if len(dias) >= 2:
            multi_dia += 1
            for dia, sec in dias.items():
                results.append({"url": url, "dia": dia, "salmo": sec["salmo"], "aleluia": sec["aleluia"]})
        else:
            sec = extract_sections(text)
            results.append({"url": url, "dia": "", "salmo": sec["salmo"], "aleluia": sec["aleluia"]})
        if (i + 1) % 50 == 0:
            print(f"  [{i + 1}/{len(all_links)}]…")

    conn.execute("DROP TABLE IF EXISTS leccionario")
    conn.execute(
        """CREATE TABLE leccionario (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            url TEXT NOT NULL,
            dia TEXT NOT NULL DEFAULT '',
            salmo TEXT DEFAULT '',
            aleluia TEXT DEFAULT '',
            UNIQUE (url, dia)
        )"""
    )
    conn.executemany(
        "INSERT OR REPLACE INTO leccionario (url, dia, salmo, aleluia) VALUES (:url, :dia, :salmo, :aleluia)",
        results,
    )
    conn.commit()
    row = conn.execute(
        """SELECT COUNT(*), COUNT(DISTINCT url),
                  SUM(CASE WHEN salmo != '' THEN 1 ELSE 0 END),
                  SUM(CASE WHEN aleluia != '' THEN 1 ELSE 0 END)
           FROM leccionario"""
    ).fetchone()
    print(f"leccionario: {row[0]} filas de {row[1]} páginas ({multi_dia} multi-día), "
          f"{row[2]} con salmo, {row[3]} con aleluya")


# ─── Emparejamiento ───────────────────────────────────────────────────────────

# Fiestas y solemnidades con página propia en curas.com.ar: palabra clave del
# título (normalizado sin acentos) → fragmento de URL, con {c} = ciclo dominical.
# Verificadas contra las lecturas que publica Vatican News para cada fecha.
# Las memorias con lecturas feriales NO van acá: se resuelven con el calendario
# (página ferial de su semana), porque su aclamación es la del día ferial.
_ESPECIALES = [
    (r"vigilia de pentecost", "lvispentecostes"),
    (r"pentecost", "lpentecostes{c}"),
    (r"trinidad", "lsmatrinidad{c}"),
    (r"cuerpo y la sangre|corpus", "lcorpus{c}"),
    (r"sagrado corazon", "lsgdocorazon{c}"),
    (r"rey del universo", "l34dgoto{c}"),
    (r"sagrada familia", "lsgdaflia{c}"),
    (r"(solemnidad|fiesta) de (la )?epifania", "lepifania"),
    (r"resurreccion del senor", "ldiadepascua"),
    (r"ascension", "lascencion{c}"),
    (r"bautismo del senor", "lbmodlsenior{c}"),
    (r"domingo de ramos", "lramos{c}"),
    (r"lunes santo|lunes de la semana santa", "llnsto"),
    (r"martes santo|martes de la semana santa", "lmtsto"),
    (r"miercoles santo|miercoles de la semana santa", "lmcsanto"),
    (r"jueves santo", "ljvsanto"),
    (r"viernes santo", "lvsanto"),
    (r"miercoles de ceniza", "ceniza/miercoles de ceniza"),
    (r"natividad del senor", "l2512n"),
    (r"inmaculada", "linmaculada"),
    (r"san esteban", "lsesteban"),
    (r"santiago", "lsgoap"),
    (r"san andres", "11noviembre/lsandres.htm"),
    (r"presentacion de la sant", "lmptacionsvm"),
    (r"presentacion del senor", "lptaciondelsr"),
    (r"dedicacion de la basilica", "ldedbasjdletran"),
    (r"fieles difuntos", "lmtuttimorti"),
    (r"todos los santos", "lmtuttisanti"),
    (r"del rosario", "lnsdelrosario"),
    (r"de los dolores", "lnsdlosdolores"),
    (r"maria reina", "lmreina"),
    (r"madre de dios", "ltheotokos"),
    # Fiestas y solemnidades del Señor con lecturas propias
    (r"san jose obrero", "lsjoseobrero"),
    (r"san jose", "03marzo/lsjose"),
    (r"anunciacion", "lanunciacion"),
    (r"transfiguracion", "ltransfigabc"),
    (r"exaltacion de la (santa )?cruz", "lexaltacscruz"),
    (r"inocentes", "lsinocentes"),
    # Apóstoles, evangelistas y santos con fiesta propia
    (r"natividad de san juan", "l2406"),
    (r"pedro y pablo", "l2906"),
    # Santo Tomás de Aquino (28/1) usa lecturas feriales en Vatican News, así
    # que el lookahead lo deja caer a la página de la feria.
    (r"santo tomas(?! de aquino)", "lstomasap"),
    (r"metodio", "02febrero/lsciriloymetodio"),
    (r"custodios", "10octubre/lsacustodios"),
    (r"san benito", "07julio/lsbenito"),
    (r"santas? marta", "lsmarta"),
    (r"bernabe", "lsbernabe"),
    (r"san marcos", "lsmarcos"),
    (r"san mateo", "lsmateo"),
    (r"simon y judas", "lssimonyjudas"),
    (r"maria magdalena", "lsmagdalena"),
    (r"san lorenzo", "08agosto/lslorenzo"),
    (r"san bartolome", "lsbartolome"),
    (r"arcangeles", "lsangelesmgr"),
    (r"asuncion", "lasuncion"),
    (r"natividad de la santisima virgen", "lnatividadsvm"),
]

# Celebraciones con lecturas propias en Vatican News pero sin página en
# curas.com.ar: no se rellenan (mejor vacío que la aclamación de la feria).
_SIN_FUENTE = [
    r"madre de la iglesia",
    r"corazon inmaculado",
]

# Días que no se identifican por título sino por fecha (octava de Navidad).
_POR_FECHA = {
    # El 24/12 (misa matutina) cae en las ferias mayores de Adviento y lo
    # resuelve `_pagina_ferial` con la sección 12-24 de «Ferias de Adviento».
    # (Ladv2512Iv.htm es la misa vespertina de la vigilia de Navidad.)
    "12-29": "l2912",        # 5º día dentro de la octava de Navidad
    "12-30": "l3012",        # 6º día dentro de la octava de Navidad
    "12-31": "l3112",        # 7º día dentro de la octava de Navidad
}

# Números de semana escritos con palabras (además de los romanos del título).
# Ordenados de más largos a más cortos para que gane la forma compuesta
# («decimoctava» antes que «octava»).
_ORDINALES = sorted([
    ("decimonovena", 19), ("decimoctava", 18), ("decimosetima", 17), ("decimosexta", 16),
    ("decimoquinta", 15), ("decimocuarta", 14), ("decimotercera", 13), ("decimosegunda", 12),
    ("decimoprimera", 11), ("vigesimonovena", 29), ("vigesimoctava", 28), ("vigesimosetima", 27),
    ("vigesimosexta", 26), ("vigesimoquinta", 25), ("vigesimocuarta", 24), ("vigesimotercera", 23),
    ("vigesimosegunda", 22), ("vigesimoprimera", 21), ("trigesimocuarta", 34),
    ("trigesimotercera", 33), ("trigesimosegunda", 32), ("trigesimoprimera", 31),
    ("decima primera", 11), ("decima segunda", 12), ("decima tercera", 13), ("decima cuarta", 14),
    ("decima quinta", 15), ("decima sexta", 16), ("decima septima", 17), ("decima octava", 18),
    ("decima novena", 19), ("vigesima primera", 21), ("vigesima segunda", 22), ("vigesima tercera", 23),
    ("vigesima cuarta", 24), ("vigesima quinta", 25), ("vigesima sexta", 26), ("vigesima septima", 27),
    ("vigesima octava", 28), ("vigesima novena", 29), ("trigesima primera", 31),
    ("trigesima segunda", 32), ("trigesima tercera", 33), ("trigesima cuarta", 34),
    ("primera", 1), ("primer", 1), ("segunda", 2), ("segundo", 2), ("tercera", 3), ("tercero", 3),
    ("tercer", 3), ("cuarta", 4), ("cuarto", 4), ("quinta", 5), ("quinto", 5), ("sexta", 6),
    ("sexto", 6), ("septima", 7), ("septimo", 7), ("octava", 8), ("octavo", 8), ("novena", 9),
    ("noveno", 9), ("decima", 10), ("decimo", 10), ("undecima", 11), ("duodecima", 12),
    ("vigesima", 20), ("trigesima", 30),
], key=lambda x: -len(x[0]))

_PALABRA_SEMANA = {1: "primera", 2: "segunda", 3: "tercera", 4: "cuarta", 5: "quinta"}
_DIAS_SEMANA = ["lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"]


def _norm(texto: str) -> str:
    """Minúsculas sin acentos (para comparar títulos y URLs)."""
    plano = unicodedata.normalize("NFD", (texto or "").lower())
    return "".join(c for c in plano if not unicodedata.combining(c))


def _norm_url(url: str) -> str:
    return _norm(url.replace("%20", " "))


def _buscar(index: dict, fragmento: str, dia: str = "") -> dict | None:
    """Primera fila cuya URL normalizada contenga el fragmento, con ese día."""
    frag = _norm(fragmento)
    if not frag:
        return None
    for (url, d), data in index.items():
        if frag in url and d == dia:
            return data
    return None


def _semana_desde(titulo_norm: str) -> int:
    """Número de semana que menciona el título (romano o en palabras); 0 si no hay."""
    m = re.search(r"\b([IVXLCDM]{1,6})\b", titulo_norm.upper())
    if m:
        numero = _roman_to_int(m.group(1))
        if numero:
            return numero
    for palabra, numero in _ORDINALES:
        if palabra in titulo_norm:
            return numero
    return 0


def _tiempo_titulo(titulo_norm: str) -> str:
    if "adviento" in titulo_norm:
        return "adviento"
    if "navidad" in titulo_norm:
        return "navidad"
    if "cuaresma" in titulo_norm or "ceniza" in titulo_norm:
        return "cuaresma"
    if "pascua" in titulo_norm or "pentecost" in titulo_norm:
        return "pascua"
    if "ordinario" in titulo_norm or "durante el ano" in titulo_norm:
        return "ordinario"
    return ""


def _pagina_ferial(index: dict, tiempo: str, semana: int, fecha: date) -> dict | None:
    """Página ferial de curas.com.ar (con su día) para una fecha, según el calendario."""
    dia = _DIAS_SEMANA[fecha.weekday()]
    if tiempo == "ordinario":
        ciclo = "p" if fecha.year % 2 == 0 else "i"
        return _buscar(index, f"l{semana:02d}semto{ciclo}", dia)
    if tiempo == "pascua":
        if not 2 <= semana <= 7:  # la octava de Pascua no tiene página ferial
            return None
        return _buscar(index, f"l{semana}sempascua", dia)
    if tiempo == "adviento":
        if fecha.month == 12 and 17 <= fecha.day <= 24:  # ferias mayores (17–24/12)
            return _buscar(index, "ferias de adviento", f"12-{fecha.day:02d}")
        if not 1 <= semana <= 3:
            return None
        return _buscar(index, f"1 adviento/{_PALABRA_SEMANA[semana]} semana", dia)
    if tiempo == "cuaresma":
        if semana >= 6:  # Semana Santa: se resuelve por título o no se rellena
            return None
        if semana == 0:
            if fecha.weekday() == 2:  # miércoles de ceniza
                return _buscar(index, "ceniza/miercoles de ceniza")
            return _buscar(index, "3 cuaresma/primera semana", dia)
        return _buscar(index, f"3 cuaresma/{_PALABRA_SEMANA[semana]} semana", dia)
    if tiempo == "navidad":
        if fecha.month != 1:
            return None
        # Ferias de Navidad (2–12 de enero). Cuando la Epifanía se celebra el
        # domingo (siempre en Argentina: el domingo entre el 2 y el 8), las
        # ferias posteriores toman los formularios de los días 7 al 12 de
        # enero en orden (rúbrica del leccionario); las anteriores usan el
        # formulario de su propia fecha.
        epifania = epifania_dominical(fecha.year)
        if fecha.day == epifania.day:
            return None  # la Epifanía tiene lecturas propias (se resuelve por título)
        if fecha.day < epifania.day:
            clave_dia = fecha.day
        else:
            clave_dia = fecha.day + (6 - epifania.day)
        if not 2 <= clave_dia <= 12:
            return None
        return _buscar(index, "ferias del tiempo de navidad", f"01-{clave_dia:02d}")
    return None


def _roman_to_int(s: str) -> int:
    return ROMAN.get(s.upper(), 0)


def resolver(fecha: str, titulo: str | None, index: dict) -> dict | None:
    """Fila del leccionario (salmo/aleluya) que corresponde a una fecha litúrgica.

    Orden: fiestas/solemnidades por título → días fijados por fecha (octava de
    Navidad) → domingos por la semana que menciona el título → ferias y memorias
    por calendario (con control cruzado si el título también trae la semana).
    """
    fecha_d = date.fromisoformat(fecha)
    ciclo = ciclo_dominical(fecha_d)
    t = _norm(titulo)

    for patron in _SIN_FUENTE:
        if re.search(patron, t):
            return None

    for patron, fragmento in _ESPECIALES:
        if re.search(patron, t):
            fila = _buscar(index, fragmento.replace("{c}", ciclo))
            if fila:
                return fila

    fragmento = _POR_FECHA.get(fecha[5:])
    if fragmento:
        fila = _buscar(index, fragmento)
        if fila:
            return fila

    if "domingo" in t:
        # Ojo: «Santo Domingo» u otros nombres propios contienen «domingo» sin
        # ser un domingo; solo se trata como domingo si el título lo delata.
        es_domingo = (
            any(k in t for k in ("adviento", "pascua", "cuaresma", "ordinario", "durante el ano", "navidad", "octava"))
            or _semana_desde(t) > 0
        )
    else:
        es_domingo = False

    if es_domingo:
        semana = _semana_desde(t)
        fila = None
        if "adviento" in t and 1 <= semana <= 4:
            fila = _buscar(index, f"ladvdgo{semana}{ciclo}")
        elif "pascua" in t and semana == 0:
            fila = _buscar(index, "ldiadepascua")  # «Domingo de Pascua» a secas
        elif "pascua" in t and 2 <= semana <= 7:
            fila = _buscar(index, f"l{semana}dgopascua{ciclo}")
        elif "cuaresma" in t and 1 <= semana <= 5:
            fila = _buscar(index, f"l{semana}dgocuaresma{ciclo}")
        elif "ordinario" in t and 1 <= semana <= 34:
            fila = _buscar(index, f"l{semana:02d}dgoto{ciclo}")
        return fila

    tiempo, semana = tiempo_semana(fecha_d)
    semana_t = _semana_desde(t)
    tiempo_t = _tiempo_titulo(t)
    if semana_t and tiempo_t and tiempo_t == tiempo and semana_t != semana:
        return None  # el título y el calendario no coinciden: mejor no rellenar
    return _pagina_ferial(index, tiempo, semana, fecha_d)


def _es_aleluia_stub(valor: str) -> bool:
    """Un stub vacío («Aleluia.» a secas o «&nbsp;») no es contenido: se puede rellenar."""
    limpio = (valor or "").replace("&nbsp;", " ").strip().rstrip(".").strip().lower()
    return limpio in ("", "aleluia", "aleluya")


def completar(conn: sqlite3.Connection, escribir: bool = True) -> None:
    try:
        lecc = conn.execute("SELECT url, dia, salmo, aleluia FROM leccionario").fetchall()
    except sqlite3.OperationalError:
        sys.exit("No existe la tabla `leccionario` (o es de un formato viejo): corré primero con --scrape.")
    if not lecc:
        sys.exit("La tabla `leccionario` está vacía: corré primero con --scrape.")

    index = {
        (_norm_url(u), d or ""): {"salmo": s or "", "aleluia": a or ""}
        for u, d, s, a in lecc
    }

    rows = conn.execute(
        "SELECT fecha, titulo_misa, salmo, aleluia FROM lecturas ORDER BY fecha"
    ).fetchall()

    lleno_salmo = lleno_aleluia = sin_match = 0
    sin_match_fechas: list[str] = []
    planes: list[tuple] = []
    for fecha, titulo, salmo, aleluia in rows:
        salmo = salmo or ""
        aleluia = aleluia or ""
        falta_salmo = not salmo
        falta_aleluia = not aleluia or _es_aleluia_stub(aleluia)
        if not falta_salmo and not falta_aleluia:
            continue
        match = resolver(fecha, titulo, index)
        if not match:
            sin_match += 1
            sin_match_fechas.append(fecha)
            continue
        nuevo_salmo = match["salmo"] if (falta_salmo and match["salmo"]) else salmo
        nuevo_aleluia = match["aleluia"] if (falta_aleluia and match["aleluia"]) else aleluia
        if nuevo_salmo == salmo and nuevo_aleluia == aleluia:
            continue
        lleno_salmo += 1 if (falta_salmo and nuevo_salmo) else 0
        lleno_aleluia += 1 if (falta_aleluia and nuevo_aleluia) else 0
        planes.append((fecha, titulo, nuevo_salmo, nuevo_aleluia))

    if escribir:
        conn.executemany(
            "UPDATE lecturas SET salmo = ?, aleluia = ? WHERE fecha = ?",
            [(p[2], p[3], p[0]) for p in planes],
        )
        conn.commit()
        print(f"Completados: {lleno_salmo} salmos, {lleno_aleluia} aleluyas · sin match: {sin_match}")
    else:
        print(f"[dry-run] Se completarían: {lleno_salmo} salmos, {lleno_aleluia} aleluyas · sin match: {sin_match}")
        for fecha, titulo, _, aleluia in planes[:80]:
            print(f"  {fecha}  {(titulo or '')[:52]:<52}  {aleluia[:64]!r}")
        if len(planes) > 80:
            print(f"  … y {len(planes) - 80} más")
    if sin_match_fechas:
        print(f"Fechas sin match ({len(sin_match_fechas)}): {', '.join(sin_match_fechas[:40])}"
              + (" …" if len(sin_match_fechas) > 40 else ""))


def preview(conn: sqlite3.Connection) -> None:
    row = conn.execute("SELECT COUNT(*) FROM lecturas").fetchone()
    print(f"lecturas: {row[0]} filas")
    try:
        row = conn.execute(
            """SELECT COUNT(*), COUNT(DISTINCT url),
                      SUM(CASE WHEN aleluia != '' THEN 1 ELSE 0 END)
               FROM leccionario"""
        ).fetchone()
        print(f"leccionario: {row[0]} filas de {row[1]} páginas, {row[2]} con aleluya")
    except sqlite3.OperationalError:
        print("leccionario: no existe la tabla")
    filas = conn.execute("SELECT salmo, aleluia FROM lecturas").fetchall()
    sin_salmo = sum(1 for s, _ in filas if not (s or "").strip())
    sin_aleluia = sum(1 for _, a in filas if not (a or "").strip() or _es_aleluia_stub(a or ""))
    print(f"lecturas sin salmo: {sin_salmo} · sin aleluya (o solo «Aleluia.»): {sin_aleluia}")


# ─── CLI ──────────────────────────────────────────────────────────────────────


def main() -> None:
    ap = argparse.ArgumentParser(description="Completa salmo/aleluya desde curas.com.ar")
    ap.add_argument("--scrape", action="store_true", help="Re-scrapear curas.com.ar → tabla leccionario")
    ap.add_argument("--preview", action="store_true", help="Estadísticas sin escribir")
    ap.add_argument("--dry-run", action="store_true", help="Mostrar qué se completaría, sin escribir")
    args = ap.parse_args()

    if not DB_PATH.exists():
        sys.exit(f"No existe la base: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)

    if args.preview:
        preview(conn)
        return

    if args.scrape:
        scrape_all(conn)

    completar(conn, escribir=not args.dry_run)
    # Consolidar WAL en el archivo principal: el asset se versiona como un único .db
    conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    conn.close()


if __name__ == "__main__":
    main()
