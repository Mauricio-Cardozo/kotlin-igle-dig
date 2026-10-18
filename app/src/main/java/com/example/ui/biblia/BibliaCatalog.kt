package com.example.ui.biblia

/** Referencia de un libro dentro de su sección canónica. */
data class LibroRef(
    val libro: String,
    val deuterocanonico: Boolean = false
)

/** Sección canónica del canon católico (solo UI; no toca la DB). */
data class GrupoBiblia(
    val testamento: String,
    val nombre: String,
    val libros: List<LibroRef>
)

/**
 * Agrupación del canon en 9 secciones, portada de `constants/bible-sections.ts`
 * de la app RN (mismos nombres y marcas de deuterocanónicos).
 */
val GRUPOS_BIBLIA: List<GrupoBiblia> = listOf(
    GrupoBiblia(
        testamento = "Antiguo",
        nombre = "Pentateuco",
        libros = listOf(
            LibroRef("Génesis"), LibroRef("Éxodo"), LibroRef("Levítico"),
            LibroRef("Números"), LibroRef("Deuteronomio")
        )
    ),
    GrupoBiblia(
        testamento = "Antiguo",
        nombre = "Libros Históricos",
        libros = listOf(
            LibroRef("Josué"), LibroRef("Jueces"), LibroRef("Rut"),
            LibroRef("1 Samuel"), LibroRef("2 Samuel"),
            LibroRef("1 Reyes"), LibroRef("2 Reyes"),
            LibroRef("1 Crónicas"), LibroRef("2 Crónicas"),
            LibroRef("Esdras"), LibroRef("Nehemías"),
            LibroRef("Tobías", deuterocanonico = true),
            LibroRef("Judit", deuterocanonico = true),
            LibroRef("Ester", deuterocanonico = true),
            LibroRef("1 Macabeos", deuterocanonico = true),
            LibroRef("2 Macabeos", deuterocanonico = true)
        )
    ),
    GrupoBiblia(
        testamento = "Antiguo",
        nombre = "Libros Sapienciales",
        libros = listOf(
            LibroRef("Job"), LibroRef("Salmos"), LibroRef("Proverbios"),
            LibroRef("Eclesiastés"), LibroRef("Cantar de los Cantares"),
            LibroRef("Sabiduría", deuterocanonico = true),
            LibroRef("Eclesiástico", deuterocanonico = true)
        )
    ),
    GrupoBiblia(
        testamento = "Antiguo",
        nombre = "Libros Proféticos",
        libros = listOf(
            LibroRef("Isaías"), LibroRef("Jeremías"), LibroRef("Lamentaciones"),
            LibroRef("Baruc", deuterocanonico = true),
            LibroRef("Ezequiel"), LibroRef("Daniel", deuterocanonico = true),
            LibroRef("Oseas"), LibroRef("Joel"), LibroRef("Amós"),
            LibroRef("Abdías"), LibroRef("Jonás"), LibroRef("Miqueas"),
            LibroRef("Nahúm"), LibroRef("Habacuc"), LibroRef("Sofonías"),
            LibroRef("Ageo"), LibroRef("Zacarías"), LibroRef("Malaquías")
        )
    ),
    GrupoBiblia(
        testamento = "Nuevo",
        nombre = "Evangelios",
        libros = listOf(
            LibroRef("Mateo"), LibroRef("Marcos"), LibroRef("Lucas"), LibroRef("Juan")
        )
    ),
    GrupoBiblia(
        testamento = "Nuevo",
        nombre = "Libro Histórico",
        libros = listOf(LibroRef("Hechos de los Apóstoles"))
    ),
    GrupoBiblia(
        testamento = "Nuevo",
        nombre = "Cartas Paulinas",
        // Tradicionalmente incluye Hebreos dentro del grupo.
        libros = listOf(
            LibroRef("Romanos"), LibroRef("1 Corintios"), LibroRef("2 Corintios"),
            LibroRef("Gálatas"), LibroRef("Efesios"), LibroRef("Filipenses"),
            LibroRef("Colosenses"), LibroRef("1 Tesalonicenses"), LibroRef("2 Tesalonicenses"),
            LibroRef("1 Timoteo"), LibroRef("2 Timoteo"), LibroRef("Tito"),
            LibroRef("Filemón"), LibroRef("Hebreos")
        )
    ),
    GrupoBiblia(
        testamento = "Nuevo",
        nombre = "Cartas Católicas",
        libros = listOf(
            LibroRef("Santiago"), LibroRef("1 Pedro"), LibroRef("2 Pedro"),
            LibroRef("1 Juan"), LibroRef("2 Juan"), LibroRef("3 Juan"), LibroRef("Judas")
        )
    ),
    GrupoBiblia(
        testamento = "Nuevo",
        nombre = "Libro Profético",
        libros = listOf(LibroRef("Apocalipsis"))
    )
)

private val ABREVIATURAS: Map<String, String> = mapOf(
    "Génesis" to "Gn", "Éxodo" to "Éx", "Levítico" to "Lv", "Números" to "Nm",
    "Deuteronomio" to "Dt", "Josué" to "Jos", "Jueces" to "Jue", "Rut" to "Rut",
    "1 Samuel" to "1 Sm", "2 Samuel" to "2 Sm", "1 Reyes" to "1 Re", "2 Reyes" to "2 Re",
    "1 Crónicas" to "1 Cr", "2 Crónicas" to "2 Cr", "Esdras" to "Esd", "Nehemías" to "Ne",
    "Tobías" to "Tb", "Judit" to "Jdt", "Ester" to "Est", "1 Macabeos" to "1 Mac",
    "2 Macabeos" to "2 Mac", "Job" to "Job", "Salmos" to "Sal", "Proverbios" to "Prov",
    "Eclesiastés" to "Ecl", "Cantar de los Cantares" to "Cant", "Sabiduría" to "Sab",
    "Eclesiástico" to "Sir", "Isaías" to "Is", "Jeremías" to "Jr", "Lamentaciones" to "Lam",
    "Baruc" to "Bar", "Ezequiel" to "Ez", "Daniel" to "Dn", "Oseas" to "Os",
    "Joel" to "Jl", "Amós" to "Am", "Abdías" to "Abd", "Jonás" to "Jon",
    "Miqueas" to "Miq", "Nahúm" to "Na", "Habacuc" to "Hab", "Sofonías" to "Sof",
    "Ageo" to "Ag", "Zacarías" to "Za", "Malaquías" to "Mal",
    "Mateo" to "Mt", "Marcos" to "Mc", "Lucas" to "Lc", "Juan" to "Jn",
    "Hechos de los Apóstoles" to "Hch", "Romanos" to "Rom", "1 Corintios" to "1 Cor",
    "2 Corintios" to "2 Cor", "Gálatas" to "Gal", "Efesios" to "Ef", "Filipenses" to "Fil",
    "Colosenses" to "Col", "1 Tesalonicenses" to "1 Tes", "2 Tesalonicenses" to "2 Tes",
    "1 Timoteo" to "1 Tim", "2 Timoteo" to "2 Tim", "Tito" to "Tit", "Filemón" to "Flm",
    "Hebreos" to "Heb", "Santiago" to "Sant", "1 Pedro" to "1 Pe", "2 Pedro" to "2 Pe",
    "1 Juan" to "1 Jn", "2 Juan" to "2 Jn", "3 Juan" to "3 Jn", "Judas" to "Jds",
    "Apocalipsis" to "Ap"
)

/** Abreviatura litúrgica del libro (con el mismo fallback que la RN). */
fun abreviatura(libro: String): String = ABREVIATURAS[libro] ?: libro.take(3)

/** Testamento ("Antiguo" / "Nuevo") del libro según las secciones canónicas. */
fun testamentoDe(libro: String): String? =
    GRUPOS_BIBLIA.firstOrNull { grupo -> grupo.libros.any { it.libro == libro } }?.testamento
