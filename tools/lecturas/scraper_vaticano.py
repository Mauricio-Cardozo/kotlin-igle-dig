#!/usr/bin/env python3
"""
Scraper de lecturas diarias desde Vatican News → tabla `lecturas` del asset.

Fuente: https://www.vaticannews.va/es/evangelio-de-hoy/AAAA/MM/DD.html
  · Trae: título de la misa, 1ª lectura, 2ª lectura, evangelio y «palabras del Papa».
  · NO trae salmo ni aleluya (para eso: completar_leccionario.py).
  · Publica con ~77 días de anticipación (ventana móvil); más allá devuelve 404.

Diferencia clave con el scraper viejo (RN): dentro de la sección «Lectura del Día»
las etiquetas «Primera lectura» / «Segunda lectura» / «Salmo responsorial» / «Aleluya»
son párrafos, no títulos. El parser las detecta y cambia de sub-sección, así la
2ª lectura no queda pegada al final de la 1ª.

Uso:
  python3 tools/lecturas/scraper_vaticano.py                        # mes actual + siguiente (solo fechas nuevas)
  python3 tools/lecturas/scraper_vaticano.py --continuar            # desde max(fecha)+1 hasta el frente publicado
  python3 tools/lecturas/scraper_vaticano.py --fecha 2026-12-25 --preview
  python3 tools/lecturas/scraper_vaticano.py --desde 2026-01-01 --hasta 2027-01-02 --regenerar
  python3 tools/lecturas/scraper_vaticano.py --list

Aviso: los textos litúrgicos tienen los mismos recaudos de copyright que la Biblia.
El repo se mantiene privado.
"""

import argparse
import html.parser
import re
import sqlite3
import sys
import time
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DB_PATH = REPO_ROOT / "app/src/main/assets/iglesia_digital.db"
BASE_URL = "https://www.vaticannews.va/es/evangelio-de-hoy/{year}/{month:02d}/{day:02d}.html"
USER_AGENT = "Mozilla/5.0 (compatible; IglesiaDigital/1.0)"
SLEEP_SECS = 0.5
DIAS_FRENTE = 200  # para --continuar: cuánto intentar más allá de la última fecha

# ─── Parser ───────────────────────────────────────────────────────────────────

# Títulos <h2> de sección → clave interna
_SECCIONES = {
    "lectura del día": "lectura",
    "segunda lectura": "segunda",
    "salmo responsorial": "salmo",
    "aleluya": "aleluia",
    "evangelio del día": "evangelio",
    "las palabras de los papas": "papas",
}

# Etiquetas <p> que cambian de sub-sección dentro de «lectura»/«segunda»
_SUBLABELS = {
    "primera lectura": "lectura",
    "segunda lectura": "segunda",
    "salmo responsorial": "salmo",
    "salmo": "salmo",
    "aleluya": "aleluia",
    "aclamación": "aleluia",
}

# La fuente tiene erratas/formatos sueltos en las etiquetas (ej: «Priemera lectura»).
# Párrafos cortos que consisten solo en una etiqueta más o menos escrita:
_FUZZY_LABEL = re.compile(
    r"^(prim\w*|priem\w*|segund\w*|salmo(\s+responsorial)?|aleluya|aclamaci\w*)"
    r"(\s+lectura)?$"
)


def _label_de(texto: str) -> str | None:
    """Devuelve la sub-sección si el párrafo es una etiqueta (aun con erratas)."""
    low = texto.lower().strip().rstrip(".:")
    if low in _SUBLABELS:
        return _SUBLABELS[low]
    if len(texto) <= 25 and _FUZZY_LABEL.match(low):
        if low.startswith(("prim", "priem")):
            return "lectura"
        if low.startswith("segund"):
            return "segunda"
        if low.startswith("salmo"):
            return "salmo"
        return "aleluia"
    return None

# Textos que abren un párrafo y son referencia, no contenido
_RE_REF = re.compile(
    r"^(Lectura del|Lectura de la|Lectura de|De la|Comienzo)",
    re.IGNORECASE,
)


class LecturasParser(html.parser.HTMLParser):
    """Parseador del HTML de Vatican News extrayendo lecturas del día."""

    def __init__(self) -> None:
        super().__init__()
        self.titulo_misa: str | None = None
        self.primera_lectura_ref: str | None = None
        self.primera_lectura: str | None = None
        self.segunda_lectura_ref: str | None = None
        self.segunda_lectura: str | None = None
        self.salmo: str | None = None
        self.aleluia: str | None = None
        self.evangelio_ref: str | None = None
        self.evangelio: str | None = None
        self.comentario_papal: str | None = None

        self._in_section = False
        self._in_h1 = False
        self._in_h2 = False
        self._section: str | None = None
        self._sub: str | None = None
        self._in_content = False
        self._in_p = False
        self._in_titulo_span = False
        self._buff: list[str] = []

    # -- acumulación ----------------------------------------------------------

    def _append(self, attr: str, texto: str) -> None:
        setattr(self, attr, (getattr(self, attr) or "") + texto + "\n\n")

    def _flush_buff(self) -> None:
        texto = "".join(self._buff).strip()
        self._buff = []
        if not texto or self._section is None:
            return

        if self._section in ("lectura", "segunda"):
            etiqueta = _label_de(texto)
            if etiqueta:
                self._sub = etiqueta
                return
            sub = self._sub or ("lectura" if self._section == "lectura" else "segunda")
            if sub == "lectura":
                if self.primera_lectura_ref is None and _RE_REF.match(texto):
                    self.primera_lectura_ref = texto
                else:
                    self._append("primera_lectura", texto)
            elif sub == "segunda":
                if self.segunda_lectura_ref is None and _RE_REF.match(texto):
                    self.segunda_lectura_ref = texto
                else:
                    self._append("segunda_lectura", texto)
            elif sub == "salmo":
                self._append("salmo", texto)
            elif sub == "aleluia":
                self._append("aleluia", texto)
        elif self._section == "salmo":
            self._append("salmo", texto)
        elif self._section == "aleluia":
            self._append("aleluia", texto)
        elif self._section == "evangelio":
            if self.evangelio_ref is None and _RE_REF.match(texto):
                self.evangelio_ref = texto
            else:
                self._append("evangelio", texto)
        elif self._section == "papas":
            self._append("comentario_papal", texto)

    # -- eventos HTML ---------------------------------------------------------

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attr_dict = dict(attrs)
        classes = (attr_dict.get("class") or "").split()

        if tag == "section":
            self._in_section = True
            self._section = None
            self._sub = None
            self._in_content = False
        elif tag == "h1":
            self._in_h1 = True
        elif tag == "h2":
            self._in_h2 = True
        elif tag == "div" and "section__content" in classes:
            self._in_content = True
        elif tag == "div" and "indicazioneLiturgica" in classes:
            self._in_titulo_span = True
        elif tag == "p" and self._in_section:
            self._in_p = True

    def handle_endtag(self, tag: str) -> None:
        if tag == "section":
            self._in_section = False
            self._section = None
            self._sub = None
            self._in_content = False
        elif tag == "h1":
            self._in_h1 = False
        elif tag == "h2":
            self._in_h2 = False
        elif tag == "p":
            if self._in_content and self._in_p:
                self._flush_buff()
            self._in_p = False
        elif tag == "div" and self._in_titulo_span:
            self._in_titulo_span = False

    def handle_data(self, data: str) -> None:
        texto = data.strip()
        if not texto:
            return

        # El h1 es el título de la página ("Evangelio del día"): no interesa
        if self._in_h1:
            return

        # h2 dentro de una section → qué sección es
        if self._in_section and self._in_h2:
            clave = texto.lower().strip()
            if clave in _SECCIONES:
                self._section = _SECCIONES[clave]
                self._sub = None
            return

        # Título litúrgico (ej: "Memoria de san Ignacio de Antioquía")
        if self._in_titulo_span and self.titulo_misa is None:
            self.titulo_misa = texto
            return

        # Contenido de <p> dentro de section__content
        if self._in_p and self._in_content and self._section:
            self._buff.append(texto + " ")


# ─── Limpieza ─────────────────────────────────────────────────────────────────


def _limpiar(texto: str | None) -> str | None:
    if texto is None:
        return None
    texto = texto.replace("&nbsp;", " ").replace("\xa0", " ")
    texto = re.sub(r"[ \t]+\n", "\n", texto)
    texto = re.sub(r"\s*\n\s*", "\n", texto)
    texto = re.sub(r"\n{3,}", "\n\n", texto)
    texto = re.sub(r" {2,}", " ", texto)
    return texto.strip() or None


# ─── Scraper ──────────────────────────────────────────────────────────────────


def scrapear_fecha(fecha: date, preview: bool = False) -> dict | None:
    """Scrapea las lecturas de una fecha. Devuelve dict o None (404 / sin contenido)."""
    url = BASE_URL.format(year=fecha.year, month=fecha.month, day=fecha.day)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})

    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            html_bytes = resp.read()
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        print(f"  [{fecha}] HTTP {e.code} - skip")
        return None
    except Exception as e:  # noqa: BLE001 - red
        print(f"  [{fecha}] Error: {e} - skip")
        return None

    parser = LecturasParser()
    parser.feed(html_bytes.decode("utf-8", errors="replace"))

    if not parser.evangelio and not parser.primera_lectura:
        if not preview:
            print(f"  [{fecha}] sin contenido parseable - skip")
        return None

    return {
        "fecha": fecha.isoformat(),
        "url": url,
        "titulo_misa": _limpiar(parser.titulo_misa),
        "primera_lectura_ref": _limpiar(parser.primera_lectura_ref),
        "primera_lectura": _limpiar(parser.primera_lectura),
        "segunda_lectura_ref": _limpiar(parser.segunda_lectura_ref),
        "segunda_lectura": _limpiar(parser.segunda_lectura),
        "salmo": _limpiar(parser.salmo),
        "aleluia": _limpiar(parser.aleluia),
        "evangelio_ref": _limpiar(parser.evangelio_ref),
        "evangelio": _limpiar(parser.evangelio),
        "comentario_papal": _limpiar(parser.comentario_papal),
    }


def _preview(d: dict) -> None:
    print(f"\n{'=' * 62}\n  Fecha: {d['fecha']}  ·  {d['titulo_misa']}\n{'=' * 62}")
    if d["primera_lectura_ref"]:
        print(f"\n  📖 1ª Lectura: {d['primera_lectura_ref']}")
        print(f"     {(d['primera_lectura'] or '')[:150]}…")
    if d["segunda_lectura"]:
        print(f"\n  📖 2ª Lectura: {d['segunda_lectura_ref']}")
        print(f"     {(d['segunda_lectura'] or '')[:150]}…")
    if d["salmo"]:
        print(f"\n  🎵 Salmo: {(d['salmo'] or '')[:100]}…")
    if d["aleluia"]:
        print(f"\n  🎵 Aleluya: {(d['aleluia'] or '')[:100]}…")
    if d["evangelio_ref"]:
        print(f"\n  ✝ Evangelio: {d['evangelio_ref']}")
        print(f"     {(d['evangelio'] or '')[:150]}…")
    if d["comentario_papal"]:
        print(f"\n  📜 Papas: {(d['comentario_papal'] or '')[:120]}…")


# ─── Base de datos ────────────────────────────────────────────────────────────


def _get_db() -> sqlite3.Connection:
    if not DB_PATH.exists():
        sys.exit(f"No existe la base: {DB_PATH}")
    return sqlite3.connect(DB_PATH)


def existe_fecha(conn: sqlite3.Connection, fecha: str) -> bool:
    row = conn.execute("SELECT 1 FROM lecturas WHERE fecha = ?", (fecha,)).fetchone()
    return row is not None


def _max_fecha(conn: sqlite3.Connection) -> str | None:
    row = conn.execute("SELECT MAX(fecha) FROM lecturas").fetchone()
    return row[0] if row and row[0] else None


_INSERT_SQL = """INSERT INTO lecturas
    (fecha, url, titulo_misa, primera_lectura_ref, primera_lectura,
     segunda_lectura_ref, segunda_lectura, salmo, aleluia, evangelio_ref,
     evangelio, comentario_papal)
    VALUES (:fecha, :url, :titulo_misa, :primera_lectura_ref, :primera_lectura,
     :segunda_lectura_ref, :segunda_lectura, :salmo, :aleluia, :evangelio_ref,
     :evangelio, :comentario_papal)"""

# En --regenerar: reemplaza cada columna con el valor nuevo si vino;
# si vino vacío, conserva el viejo. `salmo`/`aleluia` nunca se pisan desde acá.
_UPDATE_SQL = """UPDATE lecturas SET
    url = :url,
    titulo_misa = COALESCE(:titulo_misa, titulo_misa),
    primera_lectura_ref = COALESCE(:primera_lectura_ref, primera_lectura_ref),
    primera_lectura = COALESCE(:primera_lectura, primera_lectura),
    segunda_lectura_ref = COALESCE(:segunda_lectura_ref, segunda_lectura_ref),
    segunda_lectura = COALESCE(:segunda_lectura, segunda_lectura),
    evangelio_ref = COALESCE(:evangelio_ref, evangelio_ref),
    evangelio = COALESCE(:evangelio, evangelio),
    comentario_papal = COALESCE(:comentario_papal, comentario_papal)
    WHERE fecha = :fecha"""


def guardar(conn: sqlite3.Connection, lectura: dict, regenerar: bool) -> str:
    """Devuelve: 'ok' | 'update' | 'existe'."""
    if existe_fecha(conn, lectura["fecha"]):
        if not regenerar:
            return "existe"
        conn.execute(_UPDATE_SQL, lectura)
        return "update"
    conn.execute(_INSERT_SQL, lectura)
    return "ok"


def listar_registros(conn: sqlite3.Connection) -> None:
    rows = conn.execute(
        "SELECT fecha, titulo_misa, evangelio_ref FROM lecturas ORDER BY fecha DESC"
    ).fetchall()
    if not rows:
        print("No hay lecturas guardadas.")
        return
    print(f"{'Fecha':<14} {'Título':<50} {'Evangelio'}")
    print("-" * 110)
    for r in rows:
        print(f"{r[0]:<14} {(r[1] or '')[:48]:<50} {(r[2] or '')[:48]}")


def _resumen_calidad(conn: sqlite3.Connection) -> None:
    row = conn.execute(
        """SELECT COUNT(*),
                  SUM(CASE WHEN segunda_lectura IS NULL OR segunda_lectura = '' THEN 1 ELSE 0 END),
                  SUM(CASE WHEN salmo IS NULL OR salmo = '' THEN 1 ELSE 0 END),
                  SUM(CASE WHEN aleluia IS NULL OR aleluia = '' THEN 1 ELSE 0 END),
                  SUM(CASE WHEN comentario_papal IS NULL OR comentario_papal = '' THEN 1 ELSE 0 END),
                  MIN(fecha), MAX(fecha)
           FROM lecturas"""
    ).fetchone()
    print(
        f"\nCalidad: {row[0]} filas ({row[5]} → {row[6]}) · "
        f"sin 2ª: {row[1]} · sin salmo: {row[2]} · sin aleluya: {row[3]} · sin comentario: {row[4]}"
    )


# ─── Generación de fechas ─────────────────────────────────────────────────────


def _rangos_por_defecto() -> list[date]:
    """15 días antes del mes actual hasta el fin del mes siguiente."""
    hoy = date.today()
    desde = hoy.replace(day=1) - timedelta(days=15)
    if hoy.month == 12:
        fin = date(hoy.year + 1, 2, 1)
    elif hoy.month == 11:
        fin = date(hoy.year + 1, 1, 1)
    else:
        fin = date(hoy.year, hoy.month + 2, 1)
    return [desde + timedelta(days=i) for i in range((fin - desde).days)]


def _fechas_en_rango(desde: date, hasta: date) -> list[date]:
    return [desde + timedelta(days=i) for i in range((hasta - desde).days + 1)]


# ─── CLI ──────────────────────────────────────────────────────────────────────


def main() -> None:
    parser = argparse.ArgumentParser(description="Scrapea lecturas diarias desde Vatican News")
    grupo = parser.add_mutually_exclusive_group()
    grupo.add_argument("--fecha", type=str, help="Una fecha YYYY-MM-DD")
    grupo.add_argument("--desde", type=str, help="Inicio de rango YYYY-MM-DD")
    grupo.add_argument("--continuar", action="store_true",
                       help="Desde max(fecha)+1 hacia adelante hasta el frente publicado")
    parser.add_argument("--hasta", type=str, help="Fin de rango YYYY-MM-DD (con --desde)")
    parser.add_argument("--regenerar", action="store_true",
                        help="Actualizar filas existentes con lo scrapeado, en vez de saltearlas")
    parser.add_argument("--preview", action="store_true", help="Mostrar sin guardar")
    parser.add_argument("--list", action="store_true", help="Listar registros guardados")
    args = parser.parse_args()

    conn = _get_db()

    if args.list:
        listar_registros(conn)
        return

    # Determinar fechas
    if args.fecha:
        fechas = [date.fromisoformat(args.fecha)]
    elif args.desde:
        hasta = date.fromisoformat(args.hasta) if args.hasta else date.today()
        fechas = _fechas_en_rango(date.fromisoformat(args.desde), hasta)
    elif args.continuar:
        maxima = _max_fecha(conn)
        if not maxima:
            sys.exit("La tabla lecturas está vacía: usá --desde para la carga inicial.")
        desde = date.fromisoformat(maxima) + timedelta(days=1)
        hasta = date.today() + timedelta(days=DIAS_FRENTE)
        fechas = _fechas_en_rango(desde, hasta)
    else:
        fechas = _rangos_por_defecto()

    if args.preview:
        for f in fechas:
            data = scrapear_fecha(f, preview=True)
            if data:
                _preview(data)
            time.sleep(SLEEP_SECS)
        return

    print(f"Procesando {len(fechas)} fechas (regenerar={args.regenerar})…\n")
    ok = upd = existentes = skipped = 0
    for f in fechas:
        fecha_str = f.isoformat()
        data = scrapear_fecha(f)
        if data is None:
            skipped += 1
            time.sleep(SLEEP_SECS)
            continue
        try:
            resultado = guardar(conn, data, args.regenerar)
        except sqlite3.IntegrityError:
            resultado = "existe"
        if resultado == "ok":
            ok += 1
            print(f"  [{fecha_str}] OK ({data['titulo_misa'] or 'sin título'})")
        elif resultado == "update":
            upd += 1
            print(f"  [{fecha_str}] REGENERADA ({data['titulo_misa'] or 'sin título'})")
        else:
            existentes += 1
            print(f"  [{fecha_str}] ya existe - skip")
        conn.commit()
        time.sleep(SLEEP_SECS)

    print(f"\nTotal: {len(fechas)} fechas · nuevas: {ok} · regeneradas: {upd} · "
          f"existentes: {existentes} · sin datos: {skipped}")
    _resumen_calidad(conn)
    # Consolidar WAL en el archivo principal: el asset se versiona como un único .db
    conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    conn.close()


if __name__ == "__main__":
    main()
