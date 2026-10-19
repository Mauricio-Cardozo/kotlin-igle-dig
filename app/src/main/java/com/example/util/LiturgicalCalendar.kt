package com.example.util

import java.time.LocalDate
import java.time.Month

/**
 * Cómputo litúrgico por fecha, para reglas que dependen del tiempo en curso
 * (hoy: los misterios del rosario).
 *
 * Las reglas de misterios siguen a Rosarium (MIT, © 2024 Krixec): el 25 de diciembre y
 * los domingos de Adviento se rezan los misterios Gozosos, y los domingos de Cuaresma,
 * los Dolorosos. La Pascua se calcula con el algoritmo gregoriano estándar
 * (Meeus/Jones/Butcher), el mismo que usa Rosarium.
 */

/** Domingo de Pascua del [anio] (computus gregoriano). */
fun domingoDePascua(anio: Int): LocalDate {
    val a = anio % 19
    val b = anio / 100
    val c = anio % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val n = h + l - 7 * m + 114
    return LocalDate.of(anio, n / 31, n % 31 + 1)
}

/** Miércoles de Ceniza: 46 días antes del Domingo de Pascua. */
fun miercolesDeCeniza(anio: Int): LocalDate = domingoDePascua(anio).minusDays(46)

/**
 * Primer domingo de Adviento: el cuarto domingo antes de Navidad (cae siempre entre el
 * 27 de noviembre y el 3 de diciembre).
 */
fun primerDomingoDeAdviento(anio: Int): LocalDate {
    val navidad = LocalDate.of(anio, Month.DECEMBER, 25)
    return navidad.minusDays((21 + navidad.dayOfWeek.value).toLong())
}

/** ¿[fecha] cae en Adviento? (desde el primer domingo, Nochebuena incluida). */
fun esAdviento(fecha: LocalDate): Boolean =
    fecha >= primerDomingoDeAdviento(fecha.year) &&
        fecha < LocalDate.of(fecha.year, Month.DECEMBER, 25)

/** ¿[fecha] cae en Cuaresma? (desde el Miércoles de Ceniza hasta el Sábado Santo). */
fun esCuaresma(fecha: LocalDate): Boolean =
    fecha >= miercolesDeCeniza(fecha.year) && fecha < domingoDePascua(fecha.year)
