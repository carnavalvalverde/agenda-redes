package es.carnavalverde.agenda.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val URL_POR_DEFECTO =
    "https://raw.githubusercontent.com/carnavalvalverde/agenda-redes/main/agenda.json"

private val Context.dataStore by preferencesDataStore("ajustes")

data class Estado(
    val url: String = URL_POR_DEFECTO,
    val preavisoMin: Int = 5,
    val sonido: String? = null, // null = tono de alarma del sistema
    val vibrar: Boolean = true,
    val hechas: Set<String> = emptySet(),
    val bienvenidaVista: Boolean = false,
    val ultimaSync: Long = 0L,      // última sincronización correcta
    val ultimoFallo: Long = 0L,     // último intento fallido (0 = el último fue bien)
    val pruebaCuando: Long = 0L,    // alarma de prueba (0 = ninguna)
    val pospuestas: Map<String, Long> = emptyMap(), // id -> epochMs
    val programadas: Set<String> = emptySet(),      // claves de alarmas en AlarmManager
)

class Ajustes(private val ctx: Context) {
    private object K {
        val url = stringPreferencesKey("url")
        val preaviso = intPreferencesKey("preaviso")
        val sonido = stringPreferencesKey("sonido")
        val vibrar = booleanPreferencesKey("vibrar")
        val hechas = stringSetPreferencesKey("hechas")
        val bienvenida = booleanPreferencesKey("bienvenida")
        val ultimaSync = longPreferencesKey("ultima_sync")
        val ultimoFallo = longPreferencesKey("ultimo_fallo")
        val prueba = longPreferencesKey("prueba")
        val pospuestas = stringSetPreferencesKey("pospuestas")
        val programadas = stringSetPreferencesKey("programadas")
    }

    val flujo: Flow<Estado> = ctx.dataStore.data.map { p -> p.aEstado() }

    suspend fun actual(): Estado = flujo.first()

    private fun Preferences.aEstado() = Estado(
        url = this[K.url] ?: URL_POR_DEFECTO,
        preavisoMin = this[K.preaviso] ?: 5,
        sonido = this[K.sonido],
        vibrar = this[K.vibrar] ?: true,
        hechas = this[K.hechas] ?: emptySet(),
        bienvenidaVista = this[K.bienvenida] ?: false,
        ultimaSync = this[K.ultimaSync] ?: 0L,
        ultimoFallo = this[K.ultimoFallo] ?: 0L,
        pruebaCuando = this[K.prueba] ?: 0L,
        pospuestas = (this[K.pospuestas] ?: emptySet()).mapNotNull { e ->
            val i = e.lastIndexOf('|')
            val t = if (i > 0) e.substring(i + 1).toLongOrNull() else null
            if (t == null) null else e.substring(0, i) to t
        }.toMap(),
        programadas = this[K.programadas] ?: emptySet(),
    )

    suspend fun setUrl(v: String) { ctx.dataStore.edit { it[K.url] = v.trim() } }
    suspend fun setPreaviso(v: Int) { ctx.dataStore.edit { it[K.preaviso] = v } }
    suspend fun setSonido(v: String?) { ctx.dataStore.edit { if (v == null) it.remove(K.sonido) else it[K.sonido] = v } }
    suspend fun setVibrar(v: Boolean) { ctx.dataStore.edit { it[K.vibrar] = v } }
    suspend fun setBienvenidaVista() { ctx.dataStore.edit { it[K.bienvenida] = true } }
    suspend fun setPrueba(v: Long) { ctx.dataStore.edit { it[K.prueba] = v } }
    suspend fun setProgramadas(v: Set<String>) { ctx.dataStore.edit { it[K.programadas] = v } }

    suspend fun syncOk(t: Long) { ctx.dataStore.edit { it[K.ultimaSync] = t; it[K.ultimoFallo] = 0L } }
    suspend fun syncFallo(t: Long) { ctx.dataStore.edit { it[K.ultimoFallo] = t } }

    suspend fun setHecha(id: String, hecha: Boolean) {
        ctx.dataStore.edit {
            val s = (it[K.hechas] ?: emptySet()).toMutableSet()
            if (hecha) s += id else s -= id
            it[K.hechas] = s
            if (hecha) quitarPospuesta(it, id)
        }
    }

    suspend fun posponer(id: String, cuando: Long) {
        ctx.dataStore.edit {
            val s = (it[K.pospuestas] ?: emptySet()).filterNot { e -> e.substringBeforeLast('|') == id }.toMutableSet()
            s += "$id|$cuando"
            it[K.pospuestas] = s
        }
    }

    suspend fun quitarPospuesta(id: String) { ctx.dataStore.edit { quitarPospuesta(it, id) } }

    private fun quitarPospuesta(p: MutablePreferences, id: String) {
        p[K.pospuestas] = (p[K.pospuestas] ?: emptySet()).filterNot { e -> e.substringBeforeLast('|') == id }.toSet()
    }
}
