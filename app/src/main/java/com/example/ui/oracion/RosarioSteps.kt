package com.example.ui.oracion

import java.time.DayOfWeek
import java.time.LocalDate

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

/** Misterios que toca rezar según el día, con su nombre ("Gozosos", "Dolorosos", ...). */
data class MisteriosDelDia(
    val nombre: String,
    val misterios: List<String>
)

private const val INTENCIONES_DEL_SANTO_PADRE = "Por las intenciones del Santo Padre"

private val MISTERIOS_GOZOSOS = listOf(
    "La Encarnación del Hijo de Dios",
    "La Visitación de Nuestra Señora a su prima Santa Isabel",
    "El Nacimiento del Hijo de Dios",
    "La Presentación de Jesús en el templo",
    "El Niño Jesús perdido y hallado en el templo"
)

private val MISTERIOS_DOLOROSOS = listOf(
    "La Oración de Jesús en el huerto",
    "La Flagelación del Señor",
    "La Coronación de espinas",
    "Jesús con la Cruz a cuestas, camino del Calvario",
    "La Crucifixión y Muerte de nuestro Señor"
)

private val MISTERIOS_GLORIOSOS = listOf(
    "La Resurrección del Hijo de Dios",
    "La Ascensión del Señor a los Cielos",
    "La Venida del Espíritu Santo sobre los Apóstoles",
    "La Asunción de Nuestra Señora a los Cielos",
    "La Coronación de la Santísima Virgen como Reina de Cielos y Tierra"
)

private val MISTERIOS_LUMINOSOS = listOf(
    "El Bautismo de Jesús en el Jordán",
    "La autorrevelación de Jesús en las bodas de Caná",
    "El anuncio del Reino de Dios invitando a la conversión",
    "La Transfiguración",
    "La Institución de la Eucaristía"
)

/**
 * Textos de las oraciones del rosario, tomados de `data/prayers.ts` de la app RN (se portan
 * solo los que usa el rosario; la coronilla agregará los suyos).
 *
 * Desvíos intencionales respecto del RN, ambos señalados en la revisión del PR:
 * - Las letanías lauretanas repetían cuatro invocaciones seguidas (Salud de los enfermos,
 *   Refugio de los pecadores, Consuelo de los migrantes y Consoladora de los afligidos):
 *   era una copia accidental y se dejó una sola vez.
 * - El carácter `十` (diez CJK usado como cruz) se reemplazó por `✝`, el glifo que ya usa
 *   la app (y que evita depender de las fuentes CJK del dispositivo).
 */
private object RosarioTextos {

    val SENAL = """
        Por la señal de la Santa Cruz,
        de nuestros enemigos,
        líbranos Señor Dios Nuestro.

        En el nombre del Padre,
        del Hijo y del Espíritu Santo.
        Amén.
    """.trimIndent()

    val CONTRICION = """
        Señor mío Jesucristo,
        Dios y hombre verdadero,
        Creador, Padre y Redentor mío.

        Por ser Tú quién eres,
        Bondad infinita,
        y porque te amo sobre todas las cosas,
        me pesa de todo corazón haberte ofendido.

        También me pesa que puedes castigarme
        con las penas del infierno.

        Ayudado de tu divina gracia
        propongo firmemente nunca más pecar,
        confesarme y cumplir la penitencia
        que me fuere impuesta.
        Amén.
    """.trimIndent()

    val PADRENUESTRO = """
        Padre nuestro,
        que estás en el cielo,
        santificado sea tu Nombre;
        venga a nosotros tu reino;
        hágase tu voluntad en la tierra
        como en el cielo.

        Danos hoy nuestro pan de cada día;
        perdona nuestras ofensas
        como también nosotros perdonamos
        a los que nos ofenden;
        no nos dejes caer en la tentación
        y líbranos del mal.
        Amén.
    """.trimIndent()

    val AVEMARIA = """
        Dios te salve, María,
        llena eres de gracia;
        el Señor es contigo,
        bendita Tú eres entre todas las mujeres
        y bendito es el fruto de tu vientre, Jesús.

        Santa María, Madre de Dios,
        ruega por nosotros, pecadores,
        ahora y en la hora de nuestra muerte.
        Amén.
    """.trimIndent()

    val GLORIA = """
        Gloria al Padre
        y al Hijo
        y al Espíritu Santo.

        Como era en el principio,
        ahora y siempre,
        por los siglos de los siglos.
        Amén.
    """.trimIndent()

    val OH_JESUS = """
        Oh Jesús mío, perdónanos.
        Líbranos del fuego del infierno,
        lleva a todas las almas al cielo,
        especialmente a las más necesitadas.
    """.trimIndent()

    val MARIA_MADRE = """
        María, Madre de gracia,
        Madre de misericordia.
        Defiéndenos de nuestros enemigos
        y ampáranos ahora
        y en la hora de nuestra muerte.
        Amén.
    """.trimIndent()

    val LETANIAS = """
        Señor, ten piedad.
        Cristo, ten piedad.
        Señor, ten piedad.

        Dios, Padre celestial,
        ✝ ten piedad de nosotros.
        Dios, Hijo, Redentor del mundo,
        ✝ ten piedad de nosotros.
        Dios, Espíritu Santo,
        ✝ ten piedad de nosotros.
        Santísima Trinidad, un solo Dios,
        ✝ ten piedad de nosotros.

        Santa María,
        ✝ ruega por nosotros.
        Santa Madre de Dios,
        ✝ ruega por nosotros.
        Santa Virgen de las vírgenes,
        ✝ ruega por nosotros.
        Madre de Cristo,
        ✝ ruega por nosotros.
        Madre de la Iglesia,
        ✝ ruega por nosotros.
        Madre de la Misericordia,
        ✝ ruega por nosotros.
        Madre de la divina gracia,
        ✝ ruega por nosotros.
        Madre de la esperanza,
        ✝ ruega por nosotros.
        Madre purísima,
        ✝ ruega por nosotros.
        Madre castísima,
        ✝ ruega por nosotros.
        Madre siempre virgen,
        ✝ ruega por nosotros.
        Madre inmaculada,
        ✝ ruega por nosotros.
        Madre del buen consejo,
        ✝ ruega por nosotros.
        Madre del Creador,
        ✝ ruega por nosotros.
        Madre del Salvador,
        ✝ ruega por nosotros.
        Virgen prudentísima,
        ✝ ruega por nosotros.
        Virgen digna de veneración,
        ✝ ruega por nosotros.
        Virgen digna de alabanza,
        ✝ ruega por nosotros.
        Virgen poderosa,
        ✝ ruega por nosotros.
        Virgen clemente,
        ✝ ruega por nosotros.
        Virgen fiel,
        ✝ ruega por nosotros.
        Espejo de justicia,
        ✝ ruega por nosotros.
        Trono de la sabiduría,
        ✝ ruega por nosotros.
        Causa de nuestra alegría,
        ✝ ruega por nosotros.
        Vaso espiritual,
        ✝ ruega por nosotros.
        Vaso digno de honor,
        ✝ ruega por nosotros.
        Vaso insigne de devoción,
        ✝ ruega por nosotros.
        Rosa mística,
        ✝ ruega por nosotros.
        Torre de David,
        ✝ ruega por nosotros.
        Torre de marfil,
        ✝ ruega por nosotros.
        Casa de oro,
        ✝ ruega por nosotros.
        Arca de la alianza,
        ✝ ruega por nosotros.
        Puerta del cielo,
        ✝ ruega por nosotros.
        Estrella de la mañana,
        ✝ ruega por nosotros.
        Salud de los enfermos,
        ✝ ruega por nosotros.
        Refugio de los pecadores,
        ✝ ruega por nosotros.
        Consuelo de los migrantes,
        ✝ ruega por nosotros.
        Consoladora de los afligidos,
        ✝ ruega por nosotros.
        Perdón de los pecadores,
        ✝ ruega por nosotros.
        Esperanza de los cristianos,
        ✝ ruega por nosotros.
        Reina de los Ángeles,
        ✝ ruega por nosotros.
        Reina de los Patriarcas,
        ✝ ruega por nosotros.
        Reina de los Profetas,
        ✝ ruega por nosotros.
        Reina de los Apóstoles,
        ✝ ruega por nosotros.
        Reina de los Mártires,
        ✝ ruega por nosotros.
        Reina de los Confesores,
        ✝ ruega por nosotros.
        Reina de las Vírgenes,
        ✝ ruega por nosotros.
        Reina de todos los Santos,
        ✝ ruega por nosotros.
        Reina concebida sin pecado original,
        ✝ ruega por nosotros.
        Reina elevada al cielo,
        ✝ ruega por nosotros.
        Reina del Santísimo Rosario,
        ✝ ruega por nosotros.
        Reina de la paz,
        ✝ ruega por nosotros.

        Cordero de Dios, que quitas el pecado del mundo,
        ✝ perdónanos, Señor.
        Cordero de Dios, que quitas el pecado del mundo,
        ✝ escúchanos, Señor.
        Cordero de Dios, que quitas el pecado del mundo,
        ✝ ten piedad de nosotros.

        Ruega por nosotros, Santa Madre de Dios.
        Para que seamos dignos de alcanzar
        las promesas de Nuestro Señor Jesucristo.
        Amén.
    """.trimIndent()

    val ORACION_FINAL = """
        Te ofrecemos, Señor,
        esta oración del Santo Rosario
        en honor de tu Santísima Madre,
        la Virgen María.

        Concédenos,
        por la intercesión de ella,
        vivir en tu amor
        y alcanzar la vida eterna.
        Por Jesucristo, nuestro Señor.
        Amén.
    """.trimIndent()

    val PADRENUESTRO_PAPA = """
        Padre nuestro…

        (intenciones del Santo Padre)
    """.trimIndent()

    val AVEMARIA_FINAL = "Dios te salve, María…"

    val GLORIA_FINAL = "Gloria al Padre…"

    val SALVE = """
        Dios te salve, Reina y Madre de misericordia,
        vida, dulzura y esperanza nuestra;
        Dios te salve.

        A Ti llamamos
        los desterrados hijos de Eva;
        a Ti suspiramos,
        gimiendo y llorando,
        en este valle de lágrimas.

        Ea, pues, Señora, abogada nuestra,
        vuelve a nosotros
        esos tus ojos misericordiosos;
        y después de este destierro
        muéstranos a Jesús,
        fruto bendito de tu vientre.

        ¡Oh clementísima, oh piadosa,
        oh dulce Virgen María!

        Ruega por nosotros,
        Santa Madre de Dios.
        Para que seamos dignos
        de alcanzar las promesas
        de Nuestro Señor Jesucristo.
        Amén.
    """.trimIndent()

    val JACULATORIA = """
        Ave María Purísima.
        Sin pecado concebida.
    """.trimIndent()

    val COMPLETADO = """
        🌹

        Has completado el Santo Rosario.

        Que la Virgen María
        te bendiga y te guarde
        en este día.
    """.trimIndent()
}

/**
 * Misterios que toca rezar según el día de la semana, con la misma asignación que la RN:
 * lunes y sábado Gozosos; martes y viernes Dolorosos; domingo y miércoles Gloriosos;
 * jueves Luminosos.
 */
fun misteriosDelDia(dia: DayOfWeek): MisteriosDelDia = when (dia) {
    DayOfWeek.MONDAY, DayOfWeek.SATURDAY -> MisteriosDelDia("Gozosos", MISTERIOS_GOZOSOS)
    DayOfWeek.TUESDAY, DayOfWeek.FRIDAY -> MisteriosDelDia("Dolorosos", MISTERIOS_DOLOROSOS)
    DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY -> MisteriosDelDia("Gloriosos", MISTERIOS_GLORIOSOS)
    DayOfWeek.THURSDAY -> MisteriosDelDia("Luminosos", MISTERIOS_LUMINOSOS)
}

/** Ordinales de los misterios; la RN titula "2er", "3er"... y aquí se usa la forma correcta. */
private val ORDINALES_MISTERIO = listOf("1.º", "2.º", "3.º", "4.º", "5.º")

/**
 * Genera los 90 pasos del Santo Rosario para [fecha]: apertura (señal, contrición,
 * padrenuestro y tres avemarías), las cinco decenas con sus diez avemarías cada una y el
 * cierre (letanías, oración final, padrenuestro por el Santo Padre, salve y jaculatoria),
 * más el paso final de rezo completado.
 */
fun generarPasosRosario(fecha: LocalDate): List<RosarioStep> {
    val (nombre, misterios) = misteriosDelDia(fecha.dayOfWeek)
    val pasos = mutableListOf<RosarioStep>()

    pasos += RosarioStep("senal", "Señal de la Cruz", RosarioTextos.SENAL)
    pasos += RosarioStep("contricion", "Acto de Contrición", RosarioTextos.CONTRICION)
    pasos += RosarioStep(
        id = "padrenuestro-inicial",
        title = "Padrenuestro",
        text = RosarioTextos.PADRENUESTRO,
        subtitle = INTENCIONES_DEL_SANTO_PADRE
    )
    for (i in 1..3) {
        pasos += RosarioStep("avemaria-inicial-$i", "${i}ª Avemaría", RosarioTextos.AVEMARIA)
    }
    pasos += RosarioStep("gloria-inicial", "Gloria", RosarioTextos.GLORIA)

    for (m in 0 until 5) {
        pasos += RosarioStep(
            id = "misterio-$m",
            title = "${ORDINALES_MISTERIO[m]} Misterio $nombre",
            text = misterios[m],
            subtitle = "Misterio ${m + 1} de 5 · $nombre",
            mysteryIndex = m
        )
        pasos += RosarioStep(
            id = "padrenuestro-$m",
            title = "Padrenuestro",
            text = RosarioTextos.PADRENUESTRO,
            mysteryIndex = m
        )
        for (a in 1..10) {
            pasos += RosarioStep(
                id = "avemaria-$m-$a",
                title = "${a}ª Avemaría",
                text = RosarioTextos.AVEMARIA,
                mysteryIndex = m,
                hailMaryIndex = a
            )
        }
        pasos += RosarioStep("gloria-$m", "Gloria", RosarioTextos.GLORIA, mysteryIndex = m)
        pasos += RosarioStep("ohjesus-$m", "Oh Jesús Mío", RosarioTextos.OH_JESUS, mysteryIndex = m)
        pasos += RosarioStep(
            id = "mariamadre-$m",
            title = "María, Madre de Gracia",
            text = RosarioTextos.MARIA_MADRE,
            mysteryIndex = m
        )
    }

    pasos += RosarioStep("letanias", "Letanías de la Santísima Virgen", RosarioTextos.LETANIAS)
    pasos += RosarioStep("oracion-final", "Oración Final", RosarioTextos.ORACION_FINAL)
    pasos += RosarioStep(
        id = "padrenuestro-papa",
        title = "Padrenuestro",
        text = RosarioTextos.PADRENUESTRO_PAPA,
        subtitle = INTENCIONES_DEL_SANTO_PADRE
    )
    pasos += RosarioStep("avemaria-final", "Avemaría", RosarioTextos.AVEMARIA_FINAL)
    pasos += RosarioStep("gloria-final", "Gloria", RosarioTextos.GLORIA_FINAL)
    pasos += RosarioStep("salve", "Salve", RosarioTextos.SALVE)
    pasos += RosarioStep("jaculatoria", "Jaculatoria Final", RosarioTextos.JACULATORIA)
    pasos += RosarioStep("completado", "Rosario Completado 🌹", RosarioTextos.COMPLETADO)

    return pasos
}
