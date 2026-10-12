package com.example.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Type converters for storing List<String> in Room database.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|||").filter { it.isNotBlank() }
    }
}

/**
 * Entity representing a user favorite item (Biblia, Catecismo, Evangelio, Youcat).
 */
@Entity(tableName = "favoritos")
@TypeConverters(Converters::class)
data class Favorito(
    @PrimaryKey
    val id: String, // e.g. "biblia-1234"
    val tipo: String, // "biblia", "cic", "evangelio", "youcat"
    val referencia: String, // e.g. "Juan 3:16"
    val preview: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notas: String? = null,
    val tags: List<String> = emptyList(),
    val color: String? = null // Hex color string, e.g. "#55C9A84C"
)

/**
 * Entity representing user tags.
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey
    val id: String,
    val label: String,
    val color: String
)
