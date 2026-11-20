package es.carnavalverde.agenda.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import es.carnavalverde.agenda.data.Archivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Ficheros {
    private const val CARPETA = "AgendaRedes"

    /** Guarda en Pictures/AgendaRedes o Movies/AgendaRedes. */
    suspend fun guardarEnGaleria(ctx: Context, a: Archivo, f: File) = withContext(Dispatchers.IO) {
        val video = a.esVideo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val coleccion = if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                put(MediaStore.MediaColumns.MIME_TYPE, a.mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/" + CARPETA,
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val cr = ctx.contentResolver
            val uri = cr.insert(coleccion, valores) ?: error("MediaStore no devolvió URI")
            try {
                cr.openOutputStream(uri)!!.use { out -> f.inputStream().use { it.copyTo(out) } }
                cr.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                cr.delete(uri, null, null); throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(
                    if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                ), CARPETA,
            )
            dir.mkdirs()
            val destino = File(dir, f.name)
            f.copyTo(destino, overwrite = true)
            MediaScannerConnection.scanFile(ctx, arrayOf(destino.path), arrayOf(a.mime), null)
        }
    }

    fun compartir(ctx: Context, a: Archivo, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND)
            .setType(a.mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Compartir ${a.nombre.ifBlank { f.name }}"))
    }
}
