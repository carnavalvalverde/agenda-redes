package es.carnavalverde.agenda.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Lee el JSON remoto, lo guarda en caché y descarga los ficheros de las tareas futuras. */
class Repositorio(private val ctx: Context, private val ajustes: Ajustes) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val mutex = Mutex()

    private val cacheFile get() = File(ctx.filesDir, "agenda.json")
    private val mediaDir get() = File(ctx.filesDir, "media")

    private val _agenda = MutableStateFlow(leerCache())
    val agenda: StateFlow<Agenda> = _agenda

    private fun leerCache(): Agenda = try {
        if (cacheFile.exists()) json.decodeFromString(Agenda.serializer(), cacheFile.readText()) else Agenda()
    } catch (e: Exception) {
        Log.w(TAG, "caché ilegible", e); Agenda()
    }

    /** Devuelve null si fue bien, o el mensaje de error. */
    suspend fun sincronizar(): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val url = ajustes.actual().url
            try {
                val cuerpo = http.newCall(
                    Request.Builder().url(url).header("Cache-Control", "no-cache").build()
                ).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    r.body!!.string()
                }
                val nueva = json.decodeFromString(Agenda.serializer(), cuerpo)
                val tmp = File(ctx.filesDir, "agenda.json.tmp")
                tmp.writeText(cuerpo)
                if (!tmp.renameTo(cacheFile)) { cacheFile.writeText(cuerpo); tmp.delete() }
                _agenda.value = nueva
                ajustes.syncOk(System.currentTimeMillis())
                descargarFicheros(nueva, url)
                null
            } catch (e: Exception) {
                Log.w(TAG, "sync falló", e)
                ajustes.syncFallo(System.currentTimeMillis())
                e.message ?: e.javaClass.simpleName
            }
        }
    }

    private fun descargarFicheros(agenda: Agenda, url: String) {
        val desde = System.currentTimeMillis() - 24 * 3600_000L
        for (t in agenda.tareas.filter { it.epochMs > desde }) for (a in t.archivos) {
            val f = ficheroLocal(a)
            if (f.exists() && f.length() > 0) continue
            try {
                descargar(urlDe(url, a), f)
            } catch (e: Exception) {
                Log.w(TAG, "no se pudo bajar ${a.fichero}", e)
            }
        }
        // Borra ficheros que ya no aparecen en el JSON.
        val vivos = agenda.tareas.flatMap { it.archivos }.map { ficheroLocal(it).canonicalPath }.toSet()
        if (mediaDir.exists()) {
            mediaDir.walkBottomUp()
                .filter { it.isFile && it.canonicalPath !in vivos && !it.name.endsWith(".part") }
                .forEach { it.delete() }
        }
    }

    private fun descargar(url: String, destino: File) {
        destino.parentFile?.mkdirs()
        val tmp = File(destino.path + ".part")
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            tmp.outputStream().use { out -> r.body!!.byteStream().copyTo(out) }
        }
        if (!tmp.renameTo(destino)) { tmp.copyTo(destino, true); tmp.delete() }
    }

    fun ficheroLocal(a: Archivo): File {
        val partes = a.fichero.replace('\\', '/').substringBefore('?')
            .split('/').filter { it.isNotEmpty() && it != ".." && it != "." }
        return File(mediaDir, partes.joinToString("/"))
    }

    fun urlDe(jsonUrl: String, a: Archivo): String {
        if (a.fichero.startsWith("http://") || a.fichero.startsWith("https://")) return a.fichero
        val base = jsonUrl.substringBefore('?').substringBeforeLast('/') + "/"
        return base + a.fichero.trimStart('/').split('/').joinToString("/") { Uri.encode(it) }
    }

    /** Asegura que el fichero está en local (lo baja si hace falta). */
    suspend fun asegurar(a: Archivo): File = withContext(Dispatchers.IO) {
        val f = ficheroLocal(a)
        if (!f.exists() || f.length() == 0L) descargar(urlDe(ajustes.actual().url, a), f)
        f
    }

    companion object { const val TAG = "Agenda" }
}
