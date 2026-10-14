package com.example.data.db

/**
 * Available Bible versions (searchable editions). Mirrors the React Native app's
 * `BibleVersion` type / `BIBLE_TABLES` map.
 *
 * @param key stable identifier persisted in preferences and used inside favorite ids.
 * @param tableName SQLite content table.
 * @param label human-readable label shown in the UI.
 */
enum class BibliaVersion(
    val key: String,
    val tableName: String,
    val label: String
) {
    PUEBLO("pueblo", "biblia_pueblo_dios", "Pueblo de Dios"),
    NAVARRA("navarra", "biblia_navarra", "Navarra");

    /** FTS index table that indexes [tableName]. */
    val ftsTableName: String get() = "${tableName}_fts"

    /** Prefix used to build the favorite id for verses of this version. */
    val favoritoPrefix: String get() = "biblia-$key-"

    companion object {
        val DEFAULT = PUEBLO

        fun fromKey(key: String?): BibliaVersion =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Model representing a biblical book summary with its testament.
 */
data class LibroBiblia(
    val libro: String,
    val testamento: String
)

/**
 * Model representing a biblical verse in the database.
 */
data class VersiculoBiblia(
    val id: Long,
    val libro: String,
    val capitulo: Int,
    val versiculo: Int,
    val texto: String,
    val testamento: String
) {
    val referencia: String
        get() = "$libro $capitulo:$versiculo"
}
