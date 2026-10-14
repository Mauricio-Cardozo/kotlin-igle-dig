package com.example.data.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manages the read-only Iglesia Digital SQLite database containing the Bible,
 * Missal, Catechism, etc.
 *
 * Copies the database from assets if not present, and creates the FTS virtual tables
 * used for Bible search when they are missing. The check is done directly against
 * sqlite_master (not PRAGMA user_version), so the index is rebuilt even when the bundled
 * asset already ships its own user_version marker.
 *
 * FTS4 (not FTS5) is used because the Android framework SQLite does not ship the FTS5
 * module; FTS4/FTS3 are available on every supported API level. The React Native app can
 * use FTS5 only because expo-sqlite bundles its own SQLite build.
 */
class IglesiaDatabaseHelper(
    private val context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "IglesiaDatabaseHelper"
        const val DATABASE_NAME = "iglesia_digital.db"
        const val DATABASE_VERSION = 1

        /**
         * FTS index table -> content table it indexes, derived from [BibliaVersion].
         * Mirrors the React Native app, which builds an FTS table per searchable Bible version.
         */
        private val FTS_TABLES = BibliaVersion.entries.map { it.ftsTableName to it.tableName }

        // Increment this whenever assets/iglesia_digital.db is updated with new content
        const val CURRENT_CONTENT_VERSION = 1
        private const val PREFS_NAME = "iglesia_db_prefs"
        private const val KEY_INSTALLED_CONTENT_VERSION = "installed_content_version"
    }

    private var databaseInstance: SQLiteDatabase? = null

    /**
     * Prepares the database by copying it from assets if not present or if an updated
     * content version is bundled, and building the FTS index if not already built.
     */
    suspend fun initializeDatabase(): Unit = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installedVersion = prefs.getInt(KEY_INSTALLED_CONTENT_VERSION, 0)

        // Close any existing open connection if re-copying
        if (!dbFile.exists() || installedVersion < CURRENT_CONTENT_VERSION) {
            databaseInstance?.close()
            databaseInstance = null
            copyDatabaseFromAssets(dbFile)
            prefs.edit().putInt(KEY_INSTALLED_CONTENT_VERSION, CURRENT_CONTENT_VERSION).apply()
        }

        // Open writable DB to ensure FTS index exists
        val db = SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        databaseInstance = db

        ensureFtsIndex(db)
    }

    private fun copyDatabaseFromAssets(dbFile: File) {
        val parent = dbFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        try {
            context.assets.open(DATABASE_NAME).use { input: InputStream ->
                FileOutputStream(dbFile).use { output ->
                    val buffer = ByteArray(8192)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                    }
                    output.flush()
                }
            }
            Log.d(TAG, "Database copied from assets successfully to ${dbFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying database from assets", e)
            throw e
        }
    }

    private fun ensureFtsIndex(db: SQLiteDatabase) {
        for ((ftsTable, contentTable) in FTS_TABLES) {
            try {
                if (ftsTableExists(db, ftsTable)) {
                    Log.d(TAG, "FTS table $ftsTable already exists.")
                    continue
                }

                Log.d(TAG, "Building FTS table $ftsTable from $contentTable...")
                db.beginTransaction()
                try {
                    // FTS4 external-content table: it reads columns from the content table
                    // and keeps the docid in sync with the content table's rowid.
                    db.execSQL(
                        "CREATE VIRTUAL TABLE IF NOT EXISTS $ftsTable " +
                            "USING fts4(libro, texto, content='$contentTable');"
                    )
                    db.execSQL("INSERT INTO $ftsTable($ftsTable) VALUES('rebuild');")
                    db.setTransactionSuccessful()
                    Log.d(TAG, "FTS table $ftsTable created and rebuilt successfully.")
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating FTS table $ftsTable, will fallback to LIKE query if needed", e)
            }
        }
    }

    /**
     * Returns true if the given table already exists in the database. Used instead of
     * PRAGMA user_version because the bundled asset ships its own user_version (10).
     */
    private fun ftsTableExists(db: SQLiteDatabase, table: String): Boolean {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun getReadableDb(): SQLiteDatabase {
        val current = databaseInstance
        if (current != null && current.isOpen) {
            return current
        }
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val opened = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        databaseInstance = opened
        return opened
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Not used as DB is preloaded from assets
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle migrations in future phases
    }
}
