package com.example.data.repository

import android.database.Cursor
import android.util.Log
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.db.Lectura
import com.example.data.db.MisalPropioEntry
import com.example.data.db.NearestSeasonResult
import com.example.data.db.Santo
import com.example.data.db.TemporadaInfo
import com.example.util.deduplicateSantos
import com.example.util.detectSeason
import com.example.util.isSunday
import com.example.util.parseWeekNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiturgiaRepository(
    private val dbHelper: IglesiaDatabaseHelper
) {
    companion object {
        private const val TAG = "LiturgiaRepository"
        private val ROMAN_ORDINARIO = listOf(
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
            "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII", "XXIX", "XXX",
            "XXXI", "XXXII", "XXXIII", "XXXIV"
        )
    }

    /**
     * SELECT * FROM lecturas WHERE fecha = ?
     */
    suspend fun getLecturaDelDia(fecha: String): Lectura? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM lecturas WHERE fecha = ?"
            db.rawQuery(query, arrayOf(fecha)).use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext mapLectura(cursor)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lectura for $fecha", e)
        }
        null
    }

    /**
     * Fallback to determine liturgical season from the nearest preceding/succeeding readings.
     */
    suspend fun getSeasonFromNearestLectura(fecha: String): NearestSeasonResult? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.getReadableDb()
            val prevQuery = "SELECT titulo_misa FROM lecturas WHERE titulo_misa IS NOT NULL AND fecha < ? ORDER BY fecha DESC LIMIT 1"
            val nextQuery = "SELECT titulo_misa FROM lecturas WHERE titulo_misa IS NOT NULL AND fecha > ? ORDER BY fecha LIMIT 1"

            val candidateTitles = mutableListOf<String>()
            db.rawQuery(prevQuery, arrayOf(fecha)).use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndexOrThrow("titulo_misa")
                    val title = cursor.getString(col)
                    if (!title.isNullOrBlank()) candidateTitles.add(title)
                }
            }

            db.rawQuery(nextQuery, arrayOf(fecha)).use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndexOrThrow("titulo_misa")
                    val title = cursor.getString(col)
                    if (!title.isNullOrBlank()) candidateTitles.add(title)
                }
            }

            for (title in candidateTitles) {
                val s = detectSeason(title)
                if (s != null) {
                    return@withContext NearestSeasonResult(
                        season = s,
                        week = parseWeekNumber(title),
                        isSunday = isSunday(title)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching nearest season for $fecha", e)
        }
        null
    }

    /**
     * SELECT temporada, temporada_label, COUNT(*) as count FROM misal_propio GROUP BY temporada ORDER BY MIN(id)
     */
    suspend fun getMisalTemporadas(): List<TemporadaInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<TemporadaInfo>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT temporada, temporada_label, COUNT(*) as count FROM misal_propio GROUP BY temporada ORDER BY MIN(id)"
            db.rawQuery(query, null).use { cursor ->
                val tempCol = cursor.getColumnIndexOrThrow("temporada")
                val labelCol = cursor.getColumnIndexOrThrow("temporada_label")
                val countCol = cursor.getColumnIndexOrThrow("count")
                while (cursor.moveToNext()) {
                    result.add(
                        TemporadaInfo(
                            temporada = cursor.getString(tempCol),
                            temporadaLabel = cursor.getString(labelCol) ?: "",
                            count = cursor.getInt(countCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching temporadas", e)
        }
        result
    }

    /**
     * Returns the Propio del Tiempo entry for a given season and week number.
     * In Tiempo Ordinario:
     * 1) Checks "$n DOMINGO DEL TIEMPO ORDINARIO"
     * 2) If not found, checks "$n SEMANA DEL TIEMPO ORDINARIO"
     */
    suspend fun getMisalPropioPorSemana(temporada: String, semana: Int): MisalPropioEntry? = withContext(Dispatchers.IO) {
        if (temporada == "ordinario" && semana in 1..34) {
            val n = ROMAN_ORDINARIO[semana - 1]
            val dom = getMisalPropioByDia(temporada, "$n DOMINGO DEL TIEMPO ORDINARIO")
            if (dom != null) return@withContext dom
            return@withContext getMisalPropioByDia(temporada, "$n SEMANA DEL TIEMPO ORDINARIO")
        }
        null
    }

    private fun getMisalPropioByDia(temporada: String, dia: String): MisalPropioEntry? {
        val db = dbHelper.getReadableDb()
        val query = "SELECT * FROM misal_propio WHERE temporada = ? AND dia = ? LIMIT 1"
        db.rawQuery(query, arrayOf(temporada, dia)).use { cursor ->
            if (cursor.moveToFirst()) {
                return mapMisalPropio(cursor)
            }
        }
        return null
    }

    /**
     * SELECT * FROM santos WHERE mes = ? AND dia = ? ORDER BY id
     * With Levenshtein and normalizeKey grouping.
     */
    suspend fun getSantosDelDia(mes: Int, dia: Int): List<Santo> = withContext(Dispatchers.IO) {
        val rows = mutableListOf<Santo>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM santos WHERE mes = ? AND dia = ? ORDER BY id"
            db.rawQuery(query, arrayOf(mes.toString(), dia.toString())).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val mesCol = cursor.getColumnIndexOrThrow("mes")
                val diaCol = cursor.getColumnIndexOrThrow("dia")
                val nombreCol = cursor.getColumnIndexOrThrow("nombre")
                val tituloCol = cursor.getColumnIndexOrThrow("titulo")
                val bioCol = cursor.getColumnIndexOrThrow("biografia")
                while (cursor.moveToNext()) {
                    rows.add(
                        Santo(
                            id = cursor.getLong(idCol),
                            mes = cursor.getInt(mesCol),
                            dia = cursor.getInt(diaCol),
                            nombre = cursor.getString(nombreCol),
                            titulo = cursor.getString(tituloCol),
                            biografia = cursor.getString(bioCol) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching santos for $mes/$dia", e)
        }
        deduplicateSantos(rows)
    }

    suspend fun getSantoById(id: Long): Santo? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM santos WHERE id = ? LIMIT 1"
            db.rawQuery(query, arrayOf(id.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow("id")
                    val mesCol = cursor.getColumnIndexOrThrow("mes")
                    val diaCol = cursor.getColumnIndexOrThrow("dia")
                    val nombreCol = cursor.getColumnIndexOrThrow("nombre")
                    val tituloCol = cursor.getColumnIndexOrThrow("titulo")
                    val bioCol = cursor.getColumnIndexOrThrow("biografia")
                    return@withContext Santo(
                        id = cursor.getLong(idCol),
                        mes = cursor.getInt(mesCol),
                        dia = cursor.getInt(diaCol),
                        nombre = cursor.getString(nombreCol),
                        titulo = cursor.getString(tituloCol),
                        biografia = cursor.getString(bioCol) ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching santo $id", e)
        }
        null
    }

    private fun mapLectura(cursor: Cursor): Lectura {
        fun getStringOrNull(colName: String): String? {
            val idx = cursor.getColumnIndex(colName)
            return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
        }

        val idCol = cursor.getColumnIndexOrThrow("id")
        val fechaCol = cursor.getColumnIndexOrThrow("fecha")

        return Lectura(
            id = cursor.getLong(idCol),
            fecha = cursor.getString(fechaCol),
            url = getStringOrNull("url"),
            tituloMisa = getStringOrNull("titulo_misa"),
            primeraLecturaRef = getStringOrNull("primera_lectura_ref"),
            primeraLectura = getStringOrNull("primera_lectura"),
            salmo = getStringOrNull("salmo"),
            aleluia = getStringOrNull("aleluia"),
            evangelioRef = getStringOrNull("evangelio_ref"),
            evangelio = getStringOrNull("evangelio"),
            comentarioPapal = getStringOrNull("comentario_papal"),
            segundaLecturaRef = getStringOrNull("segunda_lectura_ref"),
            segundaLectura = getStringOrNull("segunda_lectura")
        )
    }

    private fun mapMisalPropio(cursor: Cursor): MisalPropioEntry {
        fun getStringOrNull(colName: String): String? {
            val idx = cursor.getColumnIndex(colName)
            return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
        }

        val idCol = cursor.getColumnIndexOrThrow("id")
        val tempCol = cursor.getColumnIndexOrThrow("temporada")
        val diaCol = cursor.getColumnIndexOrThrow("dia")

        return MisalPropioEntry(
            id = cursor.getLong(idCol),
            temporada = cursor.getString(tempCol),
            temporadaLabel = getStringOrNull("temporada_label"),
            dia = cursor.getString(diaCol),
            colecta = getStringOrNull("colecta"),
            oracionOfrendas = getStringOrNull("oracion_ofrendas"),
            postcomunion = getStringOrNull("postcomunion"),
            prefacio = getStringOrNull("prefacio"),
            antifonaEntrada = getStringOrNull("antifona_entrada"),
            antifonaComunion = getStringOrNull("antifona_comunion")
        )
    }
}
