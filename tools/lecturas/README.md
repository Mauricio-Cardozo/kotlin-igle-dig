# Herramientas de lecturas (Hito 2)

Scripts para mantener al día la tabla `lecturas` del asset
(`app/src/main/assets/iglesia_digital.db`): lecturas de la misa diaria
(título, 1ª lectura, 2ª lectura, evangelio, palabras del Papa, salmo y aleluya).

## Fuentes

| Fuente | Aporta | Límite |
|---|---|---|
| Vatican News `vaticannews.va/es/evangelio-de-hoy/AAAA/MM/DD.html` | título de la misa, 1ª lectura, 2ª lectura, evangelio y «palabras del Papa» | Ventana móvil de ~77 días (publica hacia adelante y algo más allá rota a 404). No trae salmo ni aleluya |
| curas.com.ar/Leccionarios (leccionario perpetuo) | salmo responsorial + aleluya por día litúrgico | Emparejamiento heurístico; nunca pisa datos existentes |
| dominicos.org (`rellenar_dominicos.py`) | relleno puntual de una fecha que Vatican News todavía no publicó (título, 1ª lectura, salmo y evangelio; formato propio de dominicos) | Domingos/solemnidades futuras «pendiente de publicación»; no trae 2ª lectura ni palabras del Papa |

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
#    (emparejamiento por tiempo/semana + tabla curada de fiestas con lecturas propias)
python3 tools/lecturas/completar_leccionario.py

# 4. Si alguna fecha quedó sin fila (la página de Vatican News todavía no
#    existe: 404 dentro de su ventana), rellenarla con dominicos.org:
python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12 --preview
python3 tools/lecturas/rellenar_dominicos.py --fecha 2026-12-12

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
- La tabla `leccionario` (curas.com.ar) ya viene dentro del asset. Re-scrapearla
  (`--scrape`) solo hace falta si curas.com.ar cambia algo.
- `_CURADOS` (en `completar_leccionario.py`) empareja a mano los días con lecturas
  propias que la heurística no puede resolver (Semana Santa, Ascensión, fiestas).
  Solo se agregan ahí fechas **verificadas** contra las lecturas que publica
  Vatican News: las memorias con lecturas feriales quedan afuera a propósito
  (la página del santoral traería un salmo que no corresponde a los textos).
- Los scripts hacen `PRAGMA wal_checkpoint(TRUNCATE)` al cerrar: la base se
  versiona como un único `.db` (los `-shm`/`-wal` están en `.gitignore`).
- Cobertura de referencia (2026-01-01 → 2027-01-02, 367 filas): 2ª lectura solo
  en domingos/solemnidades (correcto); salmo ~94 % (faltan ferias «después de
  Epifanía», memorias feriales y la página del Martes Santo, que falló en el
  scrapeo original); aleluya ~11 % (curas solo la publica en una minoría de
  días); comentario ~91 %.

## Copyright

Los textos litúrgicos tienen los mismos recaudos que la Biblia: material con
derechos, no publicar el asset ni el repo si se vuelve público.
