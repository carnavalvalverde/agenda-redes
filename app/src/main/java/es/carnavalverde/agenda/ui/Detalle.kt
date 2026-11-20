package es.carnavalverde.agenda.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import es.carnavalverde.agenda.alarm.Programador
import es.carnavalverde.agenda.app
import es.carnavalverde.agenda.data.Archivo
import es.carnavalverde.agenda.data.Tarea
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaDetalle(vm: AgendaVM, id: String) {
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val ahora by vm.ahora.collectAsStateWithLifecycle()
    val tarea: Tarea? = agenda.tareas.firstOrNull { it.id == id }
        ?: if (id == Programador.PRUEBA_ID) Programador.tareaPrueba(estado.pruebaCuando.takeIf { it > 0 } ?: ahora) else null
    val hecha = id in estado.hechas

    Scaffold(
        containerColor = C.fondo,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { vm.atras() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = C.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.fondo),
            )
        },
        bottomBar = {
            if (tarea != null) {
                Box(Modifier.background(C.fondo).navigationBarsPadding().padding(16.dp)) {
                    if (!hecha) {
                        Button(
                            onClick = { vm.marcar(id, true); vm.atras() },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text("Hecho ✓", style = MaterialTheme.typography.titleMedium, color = C.fondo) }
                    } else {
                        OutlinedButton(
                            onClick = { vm.marcar(id, false) },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                        ) { Text("Marcar como pendiente", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        },
    ) { pad ->
        if (tarea == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Esta tarea ya no existe.", color = C.texto2)
            }
            return@Scaffold
        }
        val atrasada = !hecha && tarea.epochMs <= ahora
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppChip(tarea.app)
                if (hecha) Chip("Hecha", fondo = C.verde, color = C.fondo)
                if (atrasada) Chip("Atrasada", fondo = C.naranja, color = C.fondo)
                if (tarea.programable && !hecha) Chip("Se puede programar ya", color = C.verde)
            }
            Text(tarea.titulo, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${fechaLarga(tarea.epochMs)}  ·  ${relativo(tarea.epochMs, ahora)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (atrasada) C.naranja else C.texto2,
            )
            if (tarea.donde.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tarea.donde.forEach { Chip(it) }
                }
            }
            tarea.nota?.takeIf { it.isNotBlank() }?.let { Aviso(it, C.amarillo) }
            tarea.pendiente?.takeIf { it.isNotBlank() }?.let { Aviso(it, C.rojo) }

            if (tarea.pasos.isNotEmpty()) {
                Seccion("Pasos")
                tarea.pasos.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(26.dp).background(C.verde, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = C.fondo) }
                        Spacer(Modifier.width(12.dp))
                        Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
            if (tarea.textos.isNotEmpty()) {
                Seccion("Textos")
                tarea.textos.forEach { CajaTexto(it.etiqueta, it.texto) }
            }
            if (tarea.archivos.isNotEmpty()) {
                Seccion("Archivos")
                tarea.archivos.forEach { CajaArchivo(it, estado.url) }
            }
        }
    }
}

@Composable
private fun Seccion(t: String) {
    Text(t, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Aviso(texto: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun CajaTexto(etiqueta: String, texto: String) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        if (etiqueta.isNotBlank()) Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = C.texto2)
        Spacer(Modifier.height(6.dp))
        SelectionContainer { Text(texto, style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val cm = ctx.getSystemService(ClipboardManager::class.java)
                    cm.setPrimaryClip(ClipData.newPlainText(etiqueta.ifBlank { "Texto" }, texto))
                    Toast.makeText(ctx, "Copiado", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp), tint = C.fondo)
                Spacer(Modifier.width(6.dp)); Text("Copiar", color = C.fondo)
            }
            if (esUrl(texto)) {
                OutlinedButton(
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(texto.trim())))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "No hay app para abrirlo", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Abrir")
                }
            }
        }
    }
}

@Composable
private fun CajaArchivo(a: Archivo, jsonUrl: String) {
    val ctx = LocalContext.current
    val repo = ctx.app.repo
    val scope = rememberCoroutineScope()
    var ocupado by remember { mutableStateOf(false) }
    var reproducir by remember { mutableStateOf<Uri?>(null) }
    val local = repo.ficheroLocal(a)
    val url = remember(a, jsonUrl) { repo.urlDe(jsonUrl, a) }
    val fuente: Any = if (local.exists() && local.length() > 0) local else url

    // Ejecuta una acción con el fichero en local (bajándolo si hace falta).
    fun conFichero(accion: suspend (File) -> Unit) {
        if (ocupado) return
        ocupado = true
        scope.launch {
            try {
                accion(repo.asegurar(a))
            } catch (e: Exception) {
                Toast.makeText(ctx, "No se pudo: ${e.message ?: "error"}", Toast.LENGTH_LONG).show()
            } finally { ocupado = false }
        }
    }

    val guardar = {
        conFichero { f ->
            Ficheros.guardarEnGaleria(ctx, a, f)
            Toast.makeText(ctx, "Guardado en ${if (a.esVideo) "Movies" else "Pictures"}/AgendaRedes", Toast.LENGTH_SHORT).show()
        }
    }
    val permisoLegado = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) guardar() else Toast.makeText(ctx, "Sin permiso de almacenamiento", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .aspectRatio(if (a.esVideo) 9f / 12f else 4f / 5f, matchHeightConstraintsFirst = false)
                .clip(RoundedCornerShape(12.dp))
                .background(C.fondo)
                .clickable(enabled = a.esVideo) {
                    conFichero { f -> reproducir = Uri.fromFile(f) }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(fuente).apply { if (a.esVideo) videoFrameMillis(0) }.build(),
                contentDescription = a.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (a.esVideo) Icon(Icons.Filled.PlayCircle, "Reproducir", tint = C.texto, modifier = Modifier.size(64.dp))
            if (ocupado) CircularProgressIndicator(color = C.verde)
        }
        Spacer(Modifier.height(10.dp))
        Text(a.nombre.ifBlank { local.name }, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    ) permisoLegado.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else guardar()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Guardar")
            }
            Button(
                onClick = { conFichero { f -> Ficheros.compartir(ctx, a, f) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp), tint = C.fondo)
                Spacer(Modifier.width(6.dp)); Text("Compartir", color = C.fondo)
            }
        }
    }

    reproducir?.let { uri -> Reproductor(uri) { reproducir = null } }
}

@Composable
private fun Reproductor(uri: Uri, cerrar: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { PlayerView(it).apply { this.player = player } },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            TextButton(onClick = cerrar, modifier = Modifier.align(Alignment.End).padding(8.dp)) {
                Icon(Icons.Filled.Check, null, tint = C.texto); Spacer(Modifier.width(6.dp)); Text("Cerrar", color = C.texto)
            }
        }
    }
}
