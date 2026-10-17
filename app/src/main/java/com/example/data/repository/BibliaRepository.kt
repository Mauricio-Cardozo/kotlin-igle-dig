package com.example.data.repository

import android.database.Cursor
import android.util.Log
import com.example.data.db.BibliaVersion
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

        /** Cantidad de versículos por página de búsqueda. */
        const val SEARCH_PAGE_SIZE = 50

        /**
         * Builds an FTS4 search expression from the raw user input: each whitespace-separated
         * token is stripped of quotes and asterisks and wrapped in double quotes with a
         * trailing prefix star *inside* the quotes, so every word matches as a prefix.
         * Example: 'amor' -> '"amor*"' (this also matches 'amores', 'amoroso', ...).
         *
         * The star must live inside the quotes for FTS4 to treat it as a prefix search.
         * Removing quote/star characters first means user input can never inject FTS syntax.
         */
        fun buildFtsQuery(rawQuery: String): String {
            val words = rawQuery.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isEmpty()) return ""
            return words
                .map { word -> word.replace("\"", "").replace("*", "").trim() }
                .filter { it.isNotBlank() }
                .joinToString(" ") { cleaned -> "\"$cleaned*\"" }
        }
    }

    /**
     * 1) Libros: SELECT libro, testamento FROM <version table> GROUP BY libro ORDER BY MIN(id)
     */
    suspend fun getLibros(version: BibliaVersion = BibliaVersion.DEFAULT): List<LibroBiblia> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LibroBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT libro, testamento FROM ${version.tableName} GROUP BY libro ORDER BY MIN(id)"
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
     * 2) Capítulos: SELECT DISTINCT capitulo FROM <version table> WHERE libro = ? ORDER BY capitulo
     */
    suspend fun getCapitulos(libro: String, version: BibliaVersion = BibliaVersion.DEFAULT): List<Int> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Int>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT DISTINCT capitulo FROM ${version.tableName} WHERE libro = ? ORDER BY capitulo"
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
     * 3) Versículos: SELECT id, libro, capitulo, versiculo, texto, testamento FROM <version table>
     *    WHERE libro = ? AND capitulo = ? ORDER BY versiculo
     */
    suspend fun getVersiculos(
        libro: String,
        capitulo: Int,
        version: BibliaVersion = BibliaVersion.DEFAULT
    ): List<VersiculoBiblia> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VersiculoBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT id, libro, capitulo, versiculo, texto, testamento FROM ${version.tableName} WHERE libro = ? AND capitulo = ? ORDER BY versiculo"
            db.rawQuery(query, arrayOf(libro, capitulo.toString())).use { cursor ->
                mapVersiculos(cursor, result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching verses for $libro $capitulo", e)
        }
        result
    }

    /**
     * 4) Búsqueda (FTS4) sobre la versión indicada:
     *    SELECT v.* FROM <version table> v
     *    JOIN <version fts> f ON v.id = f.rowid
     *    WHERE <version fts> MATCH ? ORDER BY v.id LIMIT ? OFFSET ?
     *
     * FTS4 is used instead of FTS5 because the Android framework SQLite does not include the
     * FTS5 module. Unlike FTS5, FTS4 has no bm25 "rank" column, so results are ordered by id.
     *
     * [limit] and [offset] paginate the results ([SEARCH_PAGE_SIZE] per page by default).
     * Falls back gracefully to LIKE search if FTS is unavailable or fails. Never crashes.
     */
    suspend fun searchVersiculos(
        rawQuery: String,
        version: BibliaVersion = BibliaVersion.DEFAULT,
        limit: Int = SEARCH_PAGE_SIZE,
        offset: Int = 0
    ): List<VersiculoBiblia> = withContext(Dispatchers.IO) {
        val ftsTerm = buildFtsQuery(rawQuery)
        if (ftsTerm.isBlank()) return@withContext emptyList()

        val table = version.tableName
        val ftsTable = version.ftsTableName

        val result = mutableListOf<VersiculoBiblia>()
        try {
            val db = dbHelper.getReadableDb()
            val query = """
                SELECT v.id, v.libro, v.capitulo, v.versiculo, v.texto, v.testamento
                FROM $table v
                JOIN $ftsTable f ON v.id = f.rowid
                WHERE $ftsTable MATCH ?
                ORDER BY v.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            db.rawQuery(query, arrayOf(ftsTerm, limit.toString(), offset.toString())).use { cursor ->
                mapVersiculos(cursor, result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FTS query failed, attempting fallback query with LIKE", e)
            try {
                result.clear()
                val db = dbHelper.getReadableDb()
                val likeParam = "%${rawQuery.trim()}%"
                val query = """
                    SELECT id, libro, capitulo, versiculo, texto, testamento
                    FROM $table
                    WHERE texto LIKE ? OR libro LIKE ?
                    ORDER BY id
                    LIMIT ? OFFSET ?
                """.trimIndent()
                db.rawQuery(
                    query,
                    arrayOf(likeParam, likeParam, limit.toString(), offset.toString())
                ).use { cursor ->
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

    suspend fun toggleFavorito(
        versiculo: VersiculoBiblia,
        highlightColorHex: String,
        version: BibliaVersion = BibliaVersion.DEFAULT
    ) {
        val favId = "${version.favoritoPrefix}${versiculo.id}"
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
