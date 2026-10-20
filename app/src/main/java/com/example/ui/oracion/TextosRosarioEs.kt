package com.example.ui.oracion

/**
 * Textos del rosario en español. Es el contenido que vivía en `RosarioSteps.kt`, movido tal
 * cual: salvo el orden de las claves, ninguna oración cambia.
 *
 * Desvíos intencionales respecto del RN, señalados en la revisión de su PR:
 * - Las letanías lauretanas repetían cuatro invocaciones seguidas (Salud de los enfermos,
 *   Refugio de los pecadores, Consuelo de los migrantes y Consoladora de los afligidos):
 *   era una copia accidental y se dejó una sola vez.
 * - El carácter `十` (diez CJK usado como cruz) se reemplazó por `✝`, el glifo que ya usa
 *   la app (y que evita depender de las fuentes CJK del dispositivo).
 */
val TEXTOS_ROSARIO_ES: TextosRosario = TextosRosario(
    // Títulos de los pasos
    tituloSenal = "Señal de la Cruz",
    tituloContricion = "Acto de Contrición",
    tituloPadrenuestro = "Padrenuestro",
    tituloGloria = "Gloria",
    tituloOhJesus = "Oh Jesús Mío",
    tituloMariaMadre = "María, Madre de Gracia",
    tituloLetanias = "Letanías de la Santísima Virgen",
    tituloOracionFinal = "Oración Final",
    tituloSalve = "Salve",
    tituloJaculatoria = "Jaculatoria Final",
    tituloAvemariaFinal = "Avemaría",
    tituloCompletado = "Rosario Completado 🌹",

    // Plantillas de los pasos que se numeran
    ordinales = listOf("1.º", "2.º", "3.º", "4.º", "5.º"),
    plantillaMisterio = "%s Misterio %s",
    plantillaSubtituloMisterio = "Misterio %d de 5 · %s",
    plantillaAvemaria = "%dª Avemaría",
    intencionesDelSantoPadre = "Por las intenciones del Santo Padre",

    // Series de misterios y sus cinco decenas
    nombresSeries = mapOf(
        SerieMisterios.GOZOSOS to NombreSerie("Gozosos", "Gozosos"),
        SerieMisterios.LUMINOSOS to NombreSerie("Luminosos", "Luminosos"),
        SerieMisterios.DOLOROSOS to NombreSerie("Dolorosos", "Dolorosos"),
        SerieMisterios.GLORIOSOS to NombreSerie("Gloriosos", "Gloriosos")
    ),
    misterios = mapOf(
        SerieMisterios.GOZOSOS to listOf(
            "La Encarnación del Hijo de Dios",
            "La Visitación de Nuestra Señora a su prima Santa Isabel",
            "El Nacimiento del Hijo de Dios",
            "La Presentación de Jesús en el templo",
            "El Niño Jesús perdido y hallado en el templo"
        ),
        SerieMisterios.LUMINOSOS to listOf(
            "El Bautismo de Jesús en el Jordán",
            "La autorrevelación de Jesús en las bodas de Caná",
            "El anuncio del Reino de Dios invitando a la conversión",
            "La Transfiguración",
            "La Institución de la Eucaristía"
        ),
        SerieMisterios.DOLOROSOS to listOf(
            "La Oración de Jesús en el huerto",
            "La Flagelación del Señor",
            "La Coronación de espinas",
            "Jesús con la Cruz a cuestas, camino del Calvario",
            "La Crucifixión y Muerte de nuestro Señor"
        ),
        SerieMisterios.GLORIOSOS to listOf(
            "La Resurrección del Hijo de Dios",
            "La Ascensión del Señor a los Cielos",
            "La Venida del Espíritu Santo sobre los Apóstoles",
            "La Asunción de Nuestra Señora a los Cielos",
            "La Coronación de la Santísima Virgen como Reina de Cielos y Tierra"
        )
    ),

    // Oraciones
    senal = """
        Por la señal de la Santa Cruz,
        de nuestros enemigos,
        líbranos Señor Dios Nuestro.

        En el nombre del Padre,
        del Hijo y del Espíritu Santo.
        Amén.
    """.trimIndent(),
    contricion = """
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
    """.trimIndent(),
    padrenuestro = """
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
    """.trimIndent(),
    avemaria = """
        Dios te salve, María,
        llena eres de gracia;
        el Señor es contigo,
        bendita Tú eres entre todas las mujeres
        y bendito es el fruto de tu vientre, Jesús.

        Santa María, Madre de Dios,
        ruega por nosotros, pecadores,
        ahora y en la hora de nuestra muerte.
        Amén.
    """.trimIndent(),
    gloria = """
        Gloria al Padre
        y al Hijo
        y al Espíritu Santo.

        Como era en el principio,
        ahora y siempre,
        por los siglos de los siglos.
        Amén.
    """.trimIndent(),
    ohJesus = """
        Oh Jesús mío, perdónanos.
        Líbranos del fuego del infierno,
        lleva a todas las almas al cielo,
        especialmente a las más necesitadas.
    """.trimIndent(),
    mariaMadre = """
        María, Madre de gracia,
        Madre de misericordia.
        Defiéndenos de nuestros enemigos
        y ampáranos ahora
        y en la hora de nuestra muerte.
        Amén.
    """.trimIndent(),
    letanias = """
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
    """.trimIndent(),
    oracionFinal = """
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
    """.trimIndent(),
    padrenuestroPapa = """
        Padre nuestro…

        (intenciones del Santo Padre)
    """.trimIndent(),
    avemariaFinal = "Dios te salve, María…",
    gloriaFinal = "Gloria al Padre…",
    salve = """
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
    """.trimIndent(),
    jaculatoria = """
        Ave María Purísima.
        Sin pecado concebida.
    """.trimIndent(),
    completado = """
        🌹

        Has completado el Santo Rosario.

        Que la Virgen María
        te bendiga y te guarde
        en este día.
    """.trimIndent(),
)
