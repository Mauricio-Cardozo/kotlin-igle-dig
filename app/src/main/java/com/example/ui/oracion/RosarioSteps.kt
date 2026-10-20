package com.example.ui.oracion

import com.example.util.esAdviento
import com.example.util.esCuaresma
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * Un paso del rezo del Santo Rosario. Espeja el `Step` de `data/prayers.ts` de la app RN.
 *
 * [mysteryIndex] viaja en todos los pasos de una misma decena (misterio, padrenuestro,
 * las diez avemarías, gloria y jaculatorias) y [hailMaryIndex] solo en las avemarías,
 * de 1 a 10: la pantalla los usa para pintar las cuentas.
 */
data class RosarioStep(
    val id: String,
    val title: String,
    val text: String,
    val subtitle: String? = null,
    val mysteryIndex: Int? = null,
    val hailMaryIndex: Int? = null
)

/**
 * Selección manual de los misterios del rosario. Los valores 1–4 coinciden con los de
 * Rosarium (MIT, © 2024 Krixec) para no reinterpretar una preferencia ya guardada;
 * [AUTOMATICO] (0) deja la regla litúrgica por fecha.
 */
enum class MisterioRosario(val valor: Int, val etiqueta: String) {
    AUTOMATICO(0, "Auto"),
    GOZOSOS(1, "Gozosos"),
    LUMINOSOS(2, "Luminosos"),
    DOLOROSOS(3, "Dolorosos"),
    GLORIOSOS(4, "Gloriosos");

    companion object {
        fun desdeValor(valor: Int): MisterioRosario =
            entries.firstOrNull { it.valor == valor } ?: AUTOMATICO
    }
}

/**
 * Oraciones opcionales del rezo. Se incluyen las tres por defecto para conservar la secuencia
 * de 90 pasos de la RN; Rosarium (MIT, © 2024 Krixec) usa las mismas opciones con las claves
 * `fatima`, `regina` (la Salve) y `final`, pero deja la oración final apagada.
 */
data class OracionesRosario(
    val fatima: Boolean = true,
    val salve: Boolean = true,
    val oracionFinal: Boolean = true
)

/** Oración que se puede incluir o quitar del rezo desde las píldoras del runner. */
enum class OracionOpcional(val etiqueta: String) {
    FATIMA("Fátima"),
    SALVE("Salve"),
    ORACION_FINAL("Oración final");

    fun incluidaEn(oraciones: OracionesRosario): Boolean = when (this) {
        FATIMA -> oraciones.fatima
        SALVE -> oraciones.salve
        ORACION_FINAL -> oraciones.oracionFinal
    }

    fun con(oraciones: OracionesRosario, incluida: Boolean): OracionesRosario = when (this) {
        FATIMA -> oraciones.copy(fatima = incluida)
        SALVE -> oraciones.copy(salve = incluida)
        ORACION_FINAL -> oraciones.copy(oracionFinal = incluida)
    }
}

/**
 * Serie de misterios que toca rezar según la fecha. Con [manual] en
 * [MisterioRosario.AUTOMATICO] la asignación semanal es la de la RN (lunes y sábado Gozosos;
 * martes y viernes Dolorosos; domingo y miércoles Gloriosos; jueves Luminosos), con las
 * excepciones del tiempo litúrgico tomadas de Rosarium (MIT+Credo, © 2024 Krixec — ver
 * `THIRD_PARTY_LICENSES/Rosarium-LICENSE.txt`): el 25 de diciembre y los domingos de Adviento
 * se rezan Gozosos, y los domingos de Cuaresma (hasta el Domingo de Ramos) se rezan Dolorosos.
 *
 * Devuelve la serie, no sus textos: el nombre y los misterios se toman del idioma del rezo
 * con [TextosRosario.nombreSerie] y [TextosRosario.misteriosDe].
 */
fun misteriosDelDia(
    fecha: LocalDate,
    manual: MisterioRosario = MisterioRosario.AUTOMATICO
): SerieMisterios {
    when (manual) {
        MisterioRosario.GOZOSOS -> return SerieMisterios.GOZOSOS
        MisterioRosario.LUMINOSOS -> return SerieMisterios.LUMINOSOS
        MisterioRosario.DOLOROSOS -> return SerieMisterios.DOLOROSOS
        MisterioRosario.GLORIOSOS -> return SerieMisterios.GLORIOSOS
        MisterioRosario.AUTOMATICO -> Unit
    }

    val esNavidad = fecha.month == Month.DECEMBER && fecha.dayOfMonth == 25
    if (esNavidad || (fecha.dayOfWeek == DayOfWeek.SUNDAY && esAdviento(fecha))) {
        return SerieMisterios.GOZOSOS
    }
    if (fecha.dayOfWeek == DayOfWeek.SUNDAY && esCuaresma(fecha)) {
        return SerieMisterios.DOLOROSOS
    }
    return when (fecha.dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.SATURDAY -> SerieMisterios.GOZOSOS
        DayOfWeek.TUESDAY, DayOfWeek.FRIDAY -> SerieMisterios.DOLOROSOS
        DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY -> SerieMisterios.GLORIOSOS
        DayOfWeek.THURSDAY -> SerieMisterios.LUMINOSOS
    }
}

/**
 * Genera los pasos del Santo Rosario para [fecha] en [idioma]: apertura (señal, contrición,
 * padrenuestro y tres avemarías), las cinco decenas con sus diez avemarías cada una y el
 * cierre (letanías, oración final, padrenuestro por el Santo Padre, salve y jaculatoria), más
 * el paso final de rezo completado. Con todas las [oraciones] opcionales (el valor por
 * defecto) son 90 pasos, la misma secuencia de la RN.
 *
 * Los ids y el orden de los pasos no dependen de [idioma]: cambiar de idioma no reinicia el
 * rezo, solo cambia los textos.
 */
fun generarPasosRosario(
    fecha: LocalDate,
    manual: MisterioRosario = MisterioRosario.AUTOMATICO,
    oraciones: OracionesRosario = OracionesRosario(),
    idioma: IdiomaRosario = IdiomaRosario.ESPANOL
): List<RosarioStep> {
    val textos = textosDe(idioma)
    val serie = misteriosDelDia(fecha, manual)
    val misterios = textos.misteriosDe(serie)
    val pasos = mutableListOf<RosarioStep>()

    pasos += RosarioStep("senal", textos.tituloSenal, textos.senal)
    pasos += RosarioStep("contricion", textos.tituloContricion, textos.contricion)
    pasos += RosarioStep(
        id = "padrenuestro-inicial",
        title = textos.tituloPadrenuestro,
        text = textos.padrenuestro,
        subtitle = textos.intencionesDelSantoPadre
    )
    for (i in 1..3) {
        pasos += RosarioStep(
            "avemaria-inicial-$i",
            textos.tituloAvemaria(i),
            textos.avemaria
        )
    }
    pasos += RosarioStep("gloria-inicial", textos.tituloGloria, textos.gloria)

    for (m in 0 until 5) {
        pasos += RosarioStep(
            id = "misterio-$m",
            title = textos.tituloMisterio(m, serie),
            text = misterios[m],
            subtitle = textos.subtituloMisterio(m, serie),
            mysteryIndex = m
        )
        pasos += RosarioStep(
            id = "padrenuestro-$m",
            title = textos.tituloPadrenuestro,
            text = textos.padrenuestro,
            mysteryIndex = m
        )
        for (a in 1..10) {
            pasos += RosarioStep(
                id = "avemaria-$m-$a",
                title = textos.tituloAvemaria(a),
                text = textos.avemaria,
                mysteryIndex = m,
                hailMaryIndex = a
            )
        }
        pasos += RosarioStep(
            "gloria-$m",
            textos.tituloGloria,
            textos.gloria,
            mysteryIndex = m
        )
        if (oraciones.fatima) {
            pasos += RosarioStep(
                "ohjesus-$m",
                textos.tituloOhJesus,
                textos.ohJesus,
                mysteryIndex = m
            )
        }
        pasos += RosarioStep(
            id = "mariamadre-$m",
            title = textos.tituloMariaMadre,
            text = textos.mariaMadre,
            mysteryIndex = m
        )
    }

    pasos += RosarioStep("letanias", textos.tituloLetanias, textos.letanias)
    if (oraciones.oracionFinal) {
        pasos += RosarioStep("oracion-final", textos.tituloOracionFinal, textos.oracionFinal)
    }
    pasos += RosarioStep(
        id = "padrenuestro-papa",
        title = textos.tituloPadrenuestro,
        text = textos.padrenuestroPapa,
        subtitle = textos.intencionesDelSantoPadre
    )
    pasos += RosarioStep("avemaria-final", textos.tituloAvemariaFinal, textos.avemariaFinal)
    pasos += RosarioStep("gloria-final", textos.tituloGloria, textos.gloriaFinal)
    if (oraciones.salve) {
        pasos += RosarioStep("salve", textos.tituloSalve, textos.salve)
    }
    pasos += RosarioStep("jaculatoria", textos.tituloJaculatoria, textos.jaculatoria)
    pasos += RosarioStep("completado", textos.tituloCompletado, textos.completado)

    return pasos
}
