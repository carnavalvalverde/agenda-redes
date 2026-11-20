package es.carnavalverde.agenda.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val MADRID: ZoneId = ZoneId.of("Europe/Madrid")

@Serializable
data class Agenda(
    val version: Int = 1,
    val actualizado: String? = null,
    val tareas: List<Tarea> = emptyList(),
    val pendientes: List<Pendiente> = emptyList(),
)

@Serializable
data class Tarea(
    val id: String,
    val cuando: String,
    val app: String? = null,
    val donde: List<String> = emptyList(),
    val titulo: String = "",
    val nota: String? = null,
    val pendiente: String? = null,
    val programable: Boolean = false,
    val pasos: List<String> = emptyList(),
    val textos: List<Texto> = emptyList(),
    val archivos: List<Archivo> = emptyList(),
) {
    /** `cuando` es hora local de Madrid sin zona. */
    @Transient
    val epochMs: Long = (parseMadrid(cuando) ?: Instant.EPOCH).toEpochMilli()
}

@Serializable
data class Texto(val etiqueta: String = "", val texto: String = "")

@Serializable
data class Archivo(val nombre: String = "", val fichero: String) {
    val extension: String get() = fichero.substringAfterLast('.', "").lowercase()
    val esVideo: Boolean
        get() = extension in setOf("mp4", "mov", "m4v", "webm", "3gp", "mkv")
    val mime: String
        get() = when (extension) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            else -> if (esVideo) "video/*" else "image/*"
        }
}

@Serializable
data class Pendiente(val id: String = "", val texto: String = "")

fun parseMadrid(s: String): Instant? = try {
    LocalDateTime.parse(s.trim()).atZone(MADRID).toInstant()
} catch (e: Exception) {
    null
}

fun Long.enMadrid(): ZonedDateTime = Instant.ofEpochMilli(this).atZone(MADRID)
