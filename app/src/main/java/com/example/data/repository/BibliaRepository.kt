package com.example.data.repository

import android.database.Cursor
import android.util.Log
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.db.LibroBiblia
import com.example.data.db.VersiculoBiblia
import com.example.data.user.Favorito
import com.example.data.user.FavoritoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository providing access to the Bible content from the SQLite database
 * and user highlights/favorites from the Room database.
 */
class BibliaRepository(
    private val dbHelper: IglesiaDatabaseHelper,
    private val favoritoDao: FavoritoDao
) {
    companion object {
        private const val TAG = "BibliaRepository"

        /**
         * Builds an FTS5 search term by splitting the input string into whitespace-separated tokens,
         * escaping any quotes, and enclosing each token in double quotes, joined by spaces.
         * Example: 'amor prójimo' -> '"amor" "prójimo"'
         */
        fun buildFtsQuery(rawQuery: String): String {
            val words = rawQuery.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isEmpty()) return ""
            return words.joinToString(" ") { word ->
                val cleaned = word.replace("\"", "")
                "\"$cleaned\""
            }
        }
    }

    /**
     * 1) Libros: SELECT libro, testamento FROM biblia_pueblo_dios GROUP BY libro ORDER BY MIN(id)
     */
    suspend fun getLibros(): List<LibroBiblia> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LibroBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT libro, testamento FROM biblia_pueblo_dios GROUP BY libro ORDER BY MIN(id)"
            db.rawQuery(query, null).use { cursor ->
                val libroCol = cursor.getColumnIndexOrThrow("libro")
                val testamentoCol = cursor.getColumnIndexOrThrow("testamento")
                while (cursor.moveToNext()) {
                    result.add(
                        LibroBiblia(
                            libro = cursor.getString(libroCol),
                            testamento = cursor.getString(testamentoCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching books", e)
        }
        result
    }

    /**
     * 2) Capítulos: SELECT DISTINCT capitulo FROM biblia_pueblo_dios WHERE libro = ? ORDER BY capitulo
     */
    suspend fun getCapitulos(libro: String): List<Int> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Int>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT DISTINCT capitulo FROM biblia_pueblo_dios WHERE libro = ? ORDER BY capitulo"
            db.rawQuery(query, arrayOf(libro)).use { cursor ->
                val capCol = cursor.getColumnIndexOrThrow("capitulo")
                while (cursor.moveToNext()) {
                    result.add(cursor.getInt(capCol))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chapters for $libro", e)
        }
        result
    }

    /**
     * 3) Versículos: SELECT id, libro, capitulo, versiculo, texto, testamento FROM biblia_pueblo_dios
     *    WHERE libro = ? AND capitulo = ? ORDER BY versiculo
     */
    suspend fun getVersiculos(libro: String, capitulo: Int): List<VersiculoBiblia> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VersiculoBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT id, libro, capitulo, versiculo, texto, testamento FROM biblia_pueblo_dios WHERE libro = ? AND capitulo = ? ORDER BY versiculo"
            db.rawQuery(query, arrayOf(libro, capitulo.toString())).use { cursor ->
                mapVersiculos(cursor, result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching verses for $libro $capitulo", e)
        }
        result
    }

    /**
     * 4) Búsqueda (FTS5):
     *    SELECT v.* FROM biblia_pueblo_dios v
     *    JOIN biblia_pueblo_dios_fts f ON v.rowid = f.rowid
     *    WHERE biblia_pueblo_dios_fts MATCH ? ORDER BY f.rank LIMIT 50
     *
     * Falls back gracefully to LIKE search if FTS5 is unavailable or fails. Never crashes.
     */
    suspend fun searchVersiculos(rawQuery: String): List<VersiculoBiblia> = withContext(Dispatchers.IO) {
        val ftsTerm = buildFtsQuery(rawQuery)
        if (ftsTerm.isBlank()) return@withContext emptyList()

        val result = mutableListOf<VersiculoBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = """
                SELECT v.id, v.libro, v.capitulo, v.versiculo, v.texto, v.testamento
                FROM biblia_pueblo_dios v
                JOIN biblia_pueblo_dios_fts f ON v.id = f.rowid
                WHERE biblia_pueblo_dios_fts MATCH ?
                ORDER BY f.rank
                LIMIT 50
            """.trimIndent()
            db.rawQuery(query, arrayOf(ftsTerm)).use { cursor ->
                mapVersiculos(cursor, result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FTS5 query failed, attempting fallback query with LIKE", e)
            try {
                result.clear()
                val db = dbHelper.getReadableDb()
                val likeParam = "%${rawQuery.trim()}%"
                val query = """
                    SELECT id, libro, capitulo, versiculo, texto, testamento
                    FROM biblia_pueblo_dios
                    WHERE texto LIKE ? OR libro LIKE ?
                    ORDER BY id
                    LIMIT 50
                """.trimIndent()
                db.rawQuery(query, arrayOf(likeParam, likeParam)).use { cursor ->
                    mapVersiculos(cursor, result)
                }
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback search query also failed", fallbackError)
                return@withContext emptyList()
            }
        }
        result
    }

    private fun mapVersiculos(cursor: Cursor, destination: MutableList<VersiculoBiblia>) {
        val idCol = cursor.getColumnIndexOrThrow("id")
        val libroCol = cursor.getColumnIndexOrThrow("libro")
        val capCol = cursor.getColumnIndexOrThrow("capitulo")
        val versCol = cursor.getColumnIndexOrThrow("versiculo")
        val textoCol = cursor.getColumnIndexOrThrow("texto")
        val testCol = cursor.getColumnIndexOrThrow("testamento")

        while (cursor.moveToNext()) {
            destination.add(
                VersiculoBiblia(
                    id = cursor.getLong(idCol),
                    libro = cursor.getString(libroCol),
                    capitulo = cursor.getInt(capCol),
                    versiculo = cursor.getInt(versCol),
                    texto = cursor.getString(textoCol),
                    testamento = cursor.getString(testCol)
                )
            )
        }
    }

    // --- User Favorites / Highlights Delegation ---

    fun getFavoritosFlow(): Flow<List<Favorito>> = favoritoDao.getAllFavoritos()

    fun getFavoritosByIdsFlow(ids: List<String>): Flow<List<Favorito>> =
        favoritoDao.getFavoritosByIds(ids)

    suspend fun getFavorito(id: String): Favorito? = favoritoDao.getFavoritoById(id)

    suspend fun toggleFavorito(versiculo: VersiculoBiblia, highlightColorHex: String) {
        val favId = "biblia-${versiculo.id}"
        val existing = favoritoDao.getFavoritoById(favId)
        if (existing != null) {
            favoritoDao.deleteFavoritoById(favId)
        } else {
            val nuevo = Favorito(
                id = favId,
                tipo = "biblia",
                referencia = versiculo.referencia,
                preview = versiculo.texto,
                timestamp = System.currentTimeMillis(),
                color = highlightColorHex
            )
            favoritoDao.insertFavorito(nuevo)
        }
    }

    suspend fun removeFavorito(id: String) {
        favoritoDao.deleteFavoritoById(id)
    }
}
