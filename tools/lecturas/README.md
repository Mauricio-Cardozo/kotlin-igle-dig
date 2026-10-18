# Herramientas de lecturas (Hito 2)

Scripts para mantener al día la tabla `lecturas` del asset
(`app/src/main/assets/iglesia_digital.db`): lecturas de la misa diaria
(título, 1ª lectura, 2ª lectura, evangelio, palabras del Papa, salmo y aleluya).

## Fuentes

| Fuente | Aporta | Límite |
|---|---|---|
| Vatican News `vaticannews.va/es/evangelio-de-hoy/AAAA/MM/DD.html` | título de la misa, 1ª lectura, 2ª lectura, evangelio y «palabras del Papa» | Ventana móvil de ~77 días (publica hacia adelante y algo más allá rota a 404). No trae salmo ni aleluya |
| curas.com.ar/Leccionarios (leccionario perpetuo) | salmo responsorial + aleluya: páginas dominicales, feriales por semana (con secciones por día lunes–sábado), ferias mayores de Adviento y Navidad (por fecha) y fiestas del Señor y de los santos | Emparejamiento por reglas + calendario litúrgico; nunca pisa campos con contenido |
| dominicos.org (`rellenar_dominicos.py`) | relleno puntual de una fecha que Vatican News todavía no publicó (título, 1ª lectura, salmo y evangelio; formato propio de dominicos) y relleno de campos vacíos de fechas existentes (`--completar` / `--faltantes`) | Domingos/solemnidades futuras «pendiente de publicación»; no trae 2ª lectura, palabras del Papa ni aleluya |

## Cómo actualizar el corpus

```bash
# 1. Extender desde la última fecha de la tabla hasta el frente publicado
#    (los 404 del frente se saltean solos; ~77 días de ventana)
python3 tools/lecturas/scraper_vaticano.py --continuar

# 2. Refrescar fechas recientes: las páginas se completan con el correr de los
#    días (se agregan comentarios, se corrigen textos). Regenerar desde el
#    comienzo del mes pasado, por ejemplo:
python3 tools/lecturas/scraper_vaticano.py --desde 2026-11-01 --hasta 2026-12-31 --regenerar

# 3. Completar salmo/aleluya en las filas que quedaron vacías
#    (reglas por celebración + calendario litúrgico; --dry-run muestra qué
#    se completaría sin escribir nada)
python3 tools/lecturas/completar_leccionario.py --dry-run
python3 tools/lecturas/completar_leccionario.py

# 4. Si alguna fecha quedó sin fila (la página de Vatican News todavía no
#    existe: 404 dentro de su ventana), rellenarla con dominicos.org:
python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12 --preview
python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12

# o rellenar los salmos que hayan quedado vacíos en filas existentes
# (no pisa ningún campo con contenido):
python3 tools/lecturas/rellenar_dominicos.py --faltantes --preview
python3 tools/lecturas/rellenar_dominicos.py --faltantes

# 5. Ver estadísticas y muestras
python3 tools/lecturas/completar_leccionario.py --preview
python3 tools/lecturas/scraper_vaticano.py --fecha 2027-01-01 --preview

# 6. Subir CURRENT_CONTENT_VERSION en IglesiaDatabaseHelper.kt (fuerza la
#    recopia del asset en cada instalación existente), compilar, correr tests,
#    y commitear el asset junto con la subida de versión.
```

**Cadencia recomendada**: mensual, o al menos antes de que falten 2 semanas para
que la última fecha de `lecturas` quede atrás (con la ventana actual, esa fecha
siempre estará ~2,5 meses adelante después de cada corrida).

## Notas de implementación

- En las páginas de Vatican News, «Segunda lectura» viene como **párrafo**
  dentro de la sección «Lectura del Día» (no como título). El parser lo detecta
  como sub-etiqueta y separa el texto; también tolera erratas de la fuente
  («Priemera lectura»).
- `--regenerar` usa `COALESCE(nuevo, viejo)`: solo pisa una columna cuando el
  scrapeo trajo contenido. `salmo`/`aleluia` nunca se tocan desde el scraper de
  Vatican News (los llena `completar_leccionario.py`).
- La tabla `leccionario` (curas.com.ar) ya viene dentro del asset, con una fila
  por página y, en las feriales, por día (columnas `url` y `dia`: lunes–sábado,
  o `MM-DD` para las ferias de Navidad). Re-scrapearla (`--scrape`) trae la
  tabla al día: el scrapeo guardado puede estar viejo (p. ej. sin aleluyas que
  la página sí tiene). No pisa los textos de `lecturas`.
- `completar_leccionario.py` resuelve cada fecha en este orden: **(1)** fiestas y
  solemnidades con página propia, por palabra clave del título (Pentecostés,
  Trinidad, Corpus, Sagrado Corazón, Cristo Rey, Sagrada Familia, Triduo,
  Epifanía, Apóstoles, y memorias con lecturas propias y página en curas como
  Cirilo y Metodio, los Ángeles Custodios o San Benito) con el ciclo dominical
  A/B/C, que cambia con el año litúrgico (Adviento 2026 = ciclo B); **(2)** días
  fijados por fecha (la octava de Navidad, del 29 al 31/12); **(3)** domingos,
  por la semana que trae el título (Adviento 1–4, Pascua 2–7, Cuaresma 1–5,
  Ordinario 1–34); **(4)** ferias y memorias, por calendario litúrgico: la
  temporada y la semana salen del cómputo de Pascua/Adviento/ceniza, y la
  página ferial semanal se lee en su sección del día (lunes–sábado). El ciclo
  ferial del Tiempo Ordinario es par en años pares e impar en impares.
- Las ferias mayores de Adviento (17–24/12) se leen en la página «Ferias de
  Adviento», que trae una sección por fecha (12-17 … 12-24); el 24/12 es la
  misa matutina (la vespertina es la vigilia de Navidad, y la maqueta que la
  web enlaza desde «Navidad» no es la de la tabla). Las ferias de Navidad
  (2/1 en adelante) usan los formularios «DÍA 2 … DÍA 12» por fecha, pero
  desplazados si la Epifanía se traslada al domingo entre el 2 y el 8 de enero:
  las ferias posteriores a la Epifanía toman los formularios en orden (p. ej.
  Epifanía el 4/1 → el 5/1 usa «DÍA 7», el 10/1 usa «DÍA 12»). Las memorias
  que en Vatican News llevan lecturas feriales (p. ej. Tomás de Aquino, 28/1)
  caen a la feria: la regla por título se omite a propósito.
- Los salmos y aleluyas con contenido **nunca se pisan**. Una aleluya reducida a
  «Aleluia.» a secas o a restos de HTML (`&nbsp;`) se considera vacía y se
  rellena; los textos heredados del pipeline RN se limpian de entidades HTML.
  El extractor también descarta la cita bíblica que la maqueta de curas a veces
  deja suelta como primera línea de una sección (p. ej. «ALELUIA» seguido de
  «Flp 3, 8-9») y el «Aleluia.» inicial de la aclamación: la app ya rotula el
  bloque como «Aleluya» (14 de los 16 textos heredados tampoco lo traen).
- Si el título de la fecha trae una semana y no coincide con el calendario, no
  se rellena: mejor vacío que un salmo equivocado (la UI oculta el bloque).
- Las celebraciones con lecturas propias que **no** tienen página en curas
  quedan sin rellenar (mejor vacío que la aclamación de la feria): p. ej. María
  Madre de la Iglesia (25/5) o el Corazón Inmaculado (sábado siguiente al
  Sagrado Corazón). Las páginas de Semana Santa que la web dejó de enlazar
  (`Llnsto`, `Lmtsto`, `Lmcsanto`) se agregan a mano en el scrapeo: traen el
  salmo del día; la aclamación de esas ferias no dice «Aleluya», así que su
  aleluya está vacía.
- Fuente evaluada y no integrada: la API pública de Evangelizo
  (`https://publication.evangelizo.ws/SP/days/<fecha>/readings`) trae las
  lecturas del día (Libro del Pueblo de Dios) con salmo y respuesta, además de
  audio MP3 del evangelio, pero **no** trae aleluya; queda como posible respaldo
  para salmos.
- Los scripts hacen `PRAGMA wal_checkpoint(TRUNCATE)` al cerrar: la base se
  versiona como un único `.db` (los `-shm`/`-wal` están en `.gitignore`).
- El relleno se verifica cruzando cada fecha con su página: la 1ª lectura de
  Vatican News y los bloques de lectura de la página deben coincidir en libro y
  números de capítulo/versículo (las dos fuentes usan ediciones distintas, así
  que no se exige texto idéntico; la comparación por libro tolera las glosas
  «a los Gálatas» = «a los cristianos de Galacia»). La última corrida verificó
  los 297 rellenos sin diferencias. Avisos conocidos de **Vatican News** en los
  campos de evangelio (datos heredados, no del relleno): el 4/6 figura
  «Juan 6, 51-58» y el 7/6 «Marcos 12, 28b-34» —intercambiados entre sí— y el
  26/6 el texto empieza «Marcos 8, 1-4» cuando el contenido es Mateo 8. Un
  `--regenerar` del scraper de Vatican News los corregiría cuando toque.
- Cobertura de referencia (2026-01-01 → 2027-01-02, 367 filas): 1ª lectura,
  salmo y evangelio 100 %; aleluya ~85 % (312/367: las 55 ausencias son
  esperables — 44 de Cuaresma/Semana Santa, donde la aclamación no dice
  «Aleluya»; 6 de la octava de Pascua, sin páginas feriales; 5 sin fuente en
  curas: María Madre de la Iglesia, Corazón Inmaculado, los fieles difuntos y
  dos ferias de la IX semana que la web dejó sin aclamación); 2ª lectura ~19 %
  (solo domingos y solemnidades, correcto); comentario ~91 %.

## Copyright

Los textos litúrgicos tienen los mismos recaudos que la Biblia: material con
derechos, no publicar el asset ni el repo si se vuelve público.
