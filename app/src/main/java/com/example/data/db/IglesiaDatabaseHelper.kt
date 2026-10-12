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
 * Copies the database from assets if not present, and initializes the FTS5 virtual table
 * on the first launch using a database transaction and PRAGMA user_version.
 */
class IglesiaDatabaseHelper(
    private val context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "IglesiaDatabaseHelper"
        const val DATABASE_NAME = "iglesia_digital.db"
        const val DATABASE_VERSION = 1
        private const val USER_VERSION_FTS_INITIALIZED = 1

        // Increment this whenever assets/iglesia_digital.db is updated with new content
        const val CURRENT_CONTENT_VERSION = 1
        private const val PREFS_NAME = "iglesia_db_prefs"
        private const val KEY_INSTALLED_CONTENT_VERSION = "installed_content_version"
    }

    private var databaseInstance: SQLiteDatabase? = null

    /**
     * Prepares the database by copying it from assets if not present or if an updated
     * content version is bundled, and building the FTS5 index if not already built.
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
        try {
            var currentVersion = 0
            db.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    currentVersion = cursor.getInt(0)
                }
            }

            if (currentVersion < USER_VERSION_FTS_INITIALIZED) {
                Log.d(TAG, "Building FTS5 table biblia_pueblo_dios_fts...")
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS biblia_pueblo_dios_fts USING fts5(
                            libro, texto, content='biblia_pueblo_dios', content_rowid='id'
                        );
                        """.trimIndent()
                    )
                    db.execSQL("INSERT INTO biblia_pueblo_dios_fts(biblia_pueblo_dios_fts) VALUES('rebuild');")
                    db.execSQL("PRAGMA user_version = $USER_VERSION_FTS_INITIALIZED;")
                    db.setTransactionSuccessful()
                    Log.d(TAG, "FTS5 table created and rebuilt successfully.")
                } finally {
                    db.endTransaction()
                }
            } else {
                Log.d(TAG, "FTS5 table already initialized (user_version = $currentVersion).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating FTS5 table, will fallback to LIKE query if needed", e)
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
