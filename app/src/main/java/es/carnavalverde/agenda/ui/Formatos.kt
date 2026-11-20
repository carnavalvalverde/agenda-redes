package es.carnavalverde.agenda.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.carnavalverde.agenda.data.MADRID
import es.carnavalverde.agenda.data.enMadrid
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val ES: Locale = Locale.forLanguageTag("es-ES")
val F_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val F_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)
private val F_FECHA_HORA = DateTimeFormatter.ofPattern("d/MM 'a las' HH:mm", ES)

fun String.mayus() = replaceFirstChar { it.titlecase(ES) }

fun hora(ms: Long): String = ms.enMadrid().format(F_HORA)

fun etiquetaDia(fecha: LocalDate): String {
    val hoy = LocalDate.now(MADRID)
    val dia = fecha.format(F_DIA)
    return when (fecha) {
        hoy -> "Hoy · $dia"
        hoy.plusDays(1) -> "Mañana · $dia"
        hoy.minusDays(1) -> "Ayer · $dia"
        else -> dia.mayus()
    }
}

fun fechaLarga(ms: Long): String = "${etiquetaDia(ms.enMadrid().toLocalDate())} · ${hora(ms)}"

fun fechaCorta(ms: Long): String = ms.enMadrid().format(F_FECHA_HORA)

/** «en 2 h», «en 15 min», «hace 3 h»… */
fun relativo(ms: Long, ahora: Long): String {
    val futuro = ms >= ahora
    val min = kotlin.math.abs(ms - ahora) / 60_000
    val txt = when {
        min < 1 -> return "ahora"
        min < 60 -> "$min min"
        min < 24 * 60 -> {
            val h = min / 60; val m = min % 60
            if (h >= 3 || m == 0L) "$h h" else "$h h $m min"
        }
        else -> {
            val d = (min + 12 * 60) / (24 * 60)
            if (d == 1L) "1 día" else "$d días"
        }
    }
    return if (futuro) "en $txt" else "hace $txt"
}

fun esUrl(s: String): Boolean {
    val t = s.trim()
    return (t.startsWith("http://") || t.startsWith("https://")) && !t.contains(' ') && !t.contains('\n')
}

@Composable
fun AppChip(app: String?, modifier: Modifier = Modifier) {
    if (app.isNullOrBlank()) return
    when (app.trim().lowercase()) {
        "coplea" -> Chip(app, fondo = C.granate, color = C.texto, modifier = modifier)
        "play" -> Chip(app, fondo = C.verde, color = C.fondo, modifier = modifier)
        else -> Chip(app, fondo = C.tarjeta2, color = C.texto, modifier = modifier)
    }
}
