package com.example

import android.database.sqlite.SQLiteDatabase
import com.example.data.user.UserDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica la migración de Room 1 -> 2 (Fase 0.2): los favoritos bíblicos antiguos
 * (`biblia-<id>`) pasan a la forma versionada `biblia-pueblo-<id>` sin tocar el resto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritoMigrationTest {

    @Test
    fun migratesLegacyBibliaIds_toVersionedForm() {
        val db = SQLiteDatabase.create(null)
        try {
            db.execSQL("CREATE TABLE favoritos (id TEXT PRIMARY KEY NOT NULL, tipo TEXT NOT NULL)")
            db.execSQL("INSERT INTO favoritos (id, tipo) VALUES ('biblia-1234', 'biblia')")
            db.execSQL("INSERT INTO favoritos (id, tipo) VALUES ('biblia-pueblo-9', 'biblia')")
            db.execSQL("INSERT INTO favoritos (id, tipo) VALUES ('biblia-navarra-7', 'biblia')")
            db.execSQL("INSERT INTO favoritos (id, tipo) VALUES ('cic-42', 'cic')")

            db.execSQL(UserDatabase.MIGRATE_FAVORITO_IDS_SQL)

            assertFalse("el id antiguo debe desaparecer", exists(db, "biblia-1234"))
            assertTrue("se crea la forma versionada", exists(db, "biblia-pueblo-1234"))
            assertTrue("un id ya versionado no se toca", exists(db, "biblia-pueblo-9"))
            assertTrue("un id de navarra no se toca", exists(db, "biblia-navarra-7"))
            assertTrue("otro tipo no se toca", exists(db, "cic-42"))
            assertEquals("no se pierden filas", 4, count(db))
        } finally {
            db.close()
        }
    }

    private fun exists(db: SQLiteDatabase, id: String): Boolean =
        db.rawQuery("SELECT 1 FROM favoritos WHERE id = ?", arrayOf(id)).use { it.moveToFirst() }

    private fun count(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM favoritos", null).use {
            it.moveToFirst(); it.getInt(0)
        }
}
