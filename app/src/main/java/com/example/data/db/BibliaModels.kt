package com.example.data.db

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
