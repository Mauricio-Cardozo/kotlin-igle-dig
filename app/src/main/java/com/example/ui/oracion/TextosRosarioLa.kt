package com.example.ui.oracion

/**
 * Textos del rosario en latín. Se escribieron a partir de las oraciones tradicionales de
 * dominio público, sin copiar los archivos de Rosarium (MIT+Credo). Quedan pendientes de la
 * revisión litúrgica del usuario antes de publicar.
 *
 * La estructura es la misma que [TEXTOS_ROSARIO_ES]: mismos ids y orden de pasos.
 */
val TEXTOS_ROSARIO_LA: TextosRosario = TextosRosario(
    // Títulos de los pasos
    tituloSenal = "Signum Crucis",
    tituloContricion = "Actus Contritionis",
    tituloPadrenuestro = "Pater Noster",
    tituloGloria = "Gloria Patri",
    tituloOhJesus = "Oratio Fatimae",
    tituloMariaMadre = "Maria, Mater Gratiae",
    tituloLetanias = "Litaniae Lauretanae",
    tituloOracionFinal = "Oratio Finalis",
    tituloSalve = "Salve Regina",
    tituloJaculatoria = "Iaculatoria Finalis",
    tituloAvemariaFinal = "Ave Maria",
    tituloCompletado = "Rosarium Completum 🌹",

    // Plantillas de los pasos que se numeran
    ordinales = listOf("I", "II", "III", "IV", "V"),
    plantillaMisterio = "Mysterium %s %s",
    plantillaSubtituloMisterio = "Mysterium %d ex 5 · %s",
    plantillaAvemaria = "Ave Maria %d",
    intencionesDelSantoPadre = "Pro intentionibus Summi Pontificis",

    // Series de misterios y sus cinco decenas
    nombresSeries = mapOf(
        SerieMisterios.GOZOSOS to NombreSerie("Gaudiosum", "Gaudiosa"),
        SerieMisterios.LUMINOSOS to NombreSerie("Luminosum", "Luminosa"),
        SerieMisterios.DOLOROSOS to NombreSerie("Dolorosum", "Dolorosa"),
        SerieMisterios.GLORIOSOS to NombreSerie("Gloriosum", "Gloriosa")
    ),
    misterios = mapOf(
        SerieMisterios.GOZOSOS to listOf(
            "Annuntiatio Filii Dei",
            "Visitatio Beatae Mariae Virginis ad sanctam Elisabeth",
            "Nativitas Filii Dei",
            "Praesentatio Iesu in Templo",
            "Inventio Pueri Iesu in Templo"
        ),
        SerieMisterios.LUMINOSOS to listOf(
            "Baptisma Iesu in Iordane",
            "Revelatio Iesu in nuptiis Canae",
            "Annuntiatio Regni Dei et vocatio ad conversionem",
            "Transfiguratio",
            "Institutio Eucharistiae"
        ),
        SerieMisterios.DOLOROSOS to listOf(
            "Agonia Iesu in Horto",
            "Flagellatio Domini",
            "Coronatio spinis",
            "Iesus crucem baiulans ad Calvariae locum",
            "Crucifixio et Mors Domini nostri"
        ),
        SerieMisterios.GLORIOSOS to listOf(
            "Resurrectio Filii Dei",
            "Ascensio Domini in Caelum",
            "Descensus Spiritus Sancti super Apostolos",
            "Assumptio Beatae Mariae Virginis in Caelum",
            "Coronatio Sanctissimae Virginis ut Reginae Caeli et Terrae"
        )
    ),

    // Oraciones
    senal = """
        Per signum Crucis
        de inimicis nostris
        libera nos, Deus noster.

        In nomine Patris,
        et Filii,
        et Spiritus Sancti.
        Amen.
    """.trimIndent(),

    contricion = """
        Domine mi Iesu Christe,
        Deus et homo verus,
        Creator, Pater et Redemptor meus.

        Quia tu es qui es,
        bonitas infinita,
        et quia te super omnia diligo,
        ex toto corde dolet mihi te offendisse.

        Doleo etiam quod poenas inferni mereri possim.

        Adiutus divina gratia tua
        firmiter propono numquam amplius peccare,
        confiteri et paenitentiam mihi impositam implere.
        Amen.
    """.trimIndent(),

    padrenuestro = """
        Pater noster,
        qui es in caelis,
        sanctificetur nomen tuum;
        adveniat regnum tuum;
        fiat voluntas tua,
        sicut in caelo et in terra.

        Panem nostrum cotidianum da nobis hodie;
        et dimitte nobis debita nostra,
        sicut et nos dimittimus debitoribus nostris;
        et ne nos inducas in tentationem;
        sed libera nos a malo.
        Amen.
    """.trimIndent(),

    avemaria = """
        Ave Maria, gratia plena,
        Dominus tecum;
        benedicta tu in mulieribus,
        et benedictus fructus ventris tui, Iesus.

        Sancta Maria, Mater Dei,
        ora pro nobis peccatoribus
        nunc et in hora mortis nostrae.
        Amen.
    """.trimIndent(),

    gloria = """
        Gloria Patri,
        et Filio,
        et Spiritui Sancto.

        Sicut erat in principio,
        et nunc, et semper,
        et in saecula saeculorum.
        Amen.
    """.trimIndent(),

    ohJesus = """
        O mi Iesu, dimitte nobis debita nostra,
        libera nos ab igne inferni,
        conduc in caelum omnes animas,
        praesertim eas quae misericordiae tuae maxime indigent.
    """.trimIndent(),

    mariaMadre = """
        Maria, Mater gratiae,
        Mater misericordiae,
        tu nos ab hoste protege
        et mortis hora suscipe.
        Amen.
    """.trimIndent(),

    letanias = """
        Kyrie, eleison.
        Christe, eleison.
        Kyrie, eleison.

        Deus Pater caelestis,
        ✝ miserere nobis.
        Deus Fili, Redemptor mundi,
        ✝ miserere nobis.
        Deus Spiritus Sancte,
        ✝ miserere nobis.
        Sancta Trinitas, unus Deus,
        ✝ miserere nobis.

        Sancta Maria,
        ✝ ora pro nobis.
        Sancta Dei Genetrix,
        ✝ ora pro nobis.
        Sancta Virgo virginum,
        ✝ ora pro nobis.
        Mater Christi,
        ✝ ora pro nobis.
        Mater Ecclesiae,
        ✝ ora pro nobis.
        Mater misericordiae,
        ✝ ora pro nobis.
        Mater divinae gratiae,
        ✝ ora pro nobis.
        Mater spei,
        ✝ ora pro nobis.
        Mater purissima,
        ✝ ora pro nobis.
        Mater castissima,
        ✝ ora pro nobis.
        Mater semper virgo,
        ✝ ora pro nobis.
        Mater immaculata,
        ✝ ora pro nobis.
        Mater boni consilii,
        ✝ ora pro nobis.
        Mater Creatoris,
        ✝ ora pro nobis.
        Mater Salvatoris,
        ✝ ora pro nobis.

        Virgo prudentissima,
        ✝ ora pro nobis.
        Virgo veneranda,
        ✝ ora pro nobis.
        Virgo praedicanda,
        ✝ ora pro nobis.
        Virgo potens,
        ✝ ora pro nobis.
        Virgo clemens,
        ✝ ora pro nobis.
        Virgo fidelis,
        ✝ ora pro nobis.
        Speculum iustitiae,
        ✝ ora pro nobis.
        Sedes sapientiae,
        ✝ ora pro nobis.
        Causa nostrae laetitiae,
        ✝ ora pro nobis.
        Vas spirituale,
        ✝ ora pro nobis.
        Vas honorabile,
        ✝ ora pro nobis.
        Vas insigne devotionis,
        ✝ ora pro nobis.
        Rosa mystica,
        ✝ ora pro nobis.
        Turris Davidica,
        ✝ ora pro nobis.
        Turris eburnea,
        ✝ ora pro nobis.
        Domus aurea,
        ✝ ora pro nobis.
        Foederis arca,
        ✝ ora pro nobis.
        Ianua caeli,
        ✝ ora pro nobis.
        Stella matutina,
        ✝ ora pro nobis.
        Salus infirmorum,
        ✝ ora pro nobis.
        Refugium peccatorum,
        ✝ ora pro nobis.
        Solacium migrantium,
        ✝ ora pro nobis.
        Consolatrix afflictorum,
        ✝ ora pro nobis.
        Venia peccatorum,
        ✝ ora pro nobis.
        Spes christianorum,
        ✝ ora pro nobis.
        Regina Angelorum,
        ✝ ora pro nobis.
        Regina Patriarcharum,
        ✝ ora pro nobis.
        Regina Prophetarum,
        ✝ ora pro nobis.
        Regina Apostolorum,
        ✝ ora pro nobis.
        Regina Martyrum,
        ✝ ora pro nobis.
        Regina Confessorum,
        ✝ ora pro nobis.
        Regina Virginum,
        ✝ ora pro nobis.
        Regina Sanctorum omnium,
        ✝ ora pro nobis.
        Regina sine labe originali concepta,
        ✝ ora pro nobis.
        Regina in caelum assumpta,
        ✝ ora pro nobis.
        Regina Sacratissimi Rosarii,
        ✝ ora pro nobis.
        Regina pacis,
        ✝ ora pro nobis.

        Agnus Dei, qui tollis peccata mundi,
        ✝ parce nobis, Domine.
        Agnus Dei, qui tollis peccata mundi,
        ✝ exaudi nos, Domine.
        Agnus Dei, qui tollis peccata mundi,
        ✝ miserere nobis.

        Ora pro nobis, Sancta Dei Genetrix.
        Ut digni efficiamur promissionibus Christi.
        Amen.
    """.trimIndent(),

    oracionFinal = """
        Offerimus tibi, Domine,
        hanc orationem Sancti Rosarii
        in honorem sanctissimae Matris tuae,
        Virginis Mariae.

        Concede nobis,
        eius intercessione,
        in amore tuo vivere
        et vitam aeternam consequi.
        Per Christum Dominum nostrum.
        Amen.
    """.trimIndent(),

    padrenuestroPapa = """
        Pater noster…

        (intentiones Summi Pontificis)
    """.trimIndent(),

    avemariaFinal = "Ave Maria…",

    gloriaFinal = "Gloria Patri…",

    salve = """
        Salve, Regina, mater misericordiae,
        vita, dulcedo et spes nostra, salve.

        Ad te clamamus,
        exsules filii Hevae;
        ad te suspiramus,
        gementes et flentes
        in hac lacrimarum valle.

        Eia ergo, advocata nostra,
        illos tuos misericordes oculos
        ad nos converte;
        et Iesum, benedictum fructum ventris tui,
        nobis post hoc exsilium ostende.

        O clemens, o pia,
        o dulcis Virgo Maria.

        Ora pro nobis, Sancta Dei Genetrix.
        Ut digni efficiamur promissionibus Christi.
        Amen.
    """.trimIndent(),

    jaculatoria = """
        Ave Maria purissima.
        Sine labe concepta.
    """.trimIndent(),

    completado = """
        🌹

        Sanctum Rosarium absolvisti.

        Virgo Maria
        te benedicat et custodiat
        in hoc die.
    """.trimIndent()
)
