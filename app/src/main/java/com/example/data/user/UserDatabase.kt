package com.example.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Favorito::class, Tag::class],
    version = 2,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun favoritoDao(): FavoritoDao
    abstract fun tagDao(): TagDao

    companion object {
        private const val DATABASE_NAME = "user.db"

        /**
         * Migrates legacy Bible favorite ids from `biblia-<verseId>` to the versioned
         * `biblia-pueblo-<verseId>` used since the Bible version selector was added.
         *
         * Only affects `tipo = 'biblia'` rows whose id is still the unversioned form
         * (`biblia-` followed by a digit). Already-versioned ids and other tipos are untouched.
         */
        const val MIGRATE_FAVORITO_IDS_SQL =
            "UPDATE favoritos SET id = 'biblia-pueblo-' || substr(id, 8) " +
                "WHERE tipo = 'biblia' AND id GLOB 'biblia-[0-9]*'"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MIGRATE_FAVORITO_IDS_SQL)
            }
        }

        @Volatile
        private var instance: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
