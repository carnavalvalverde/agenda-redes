package es.carnavalverde.agenda.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.carnavalverde.agenda.BuildConfig
import es.carnavalverde.agenda.data.URL_POR_DEFECTO

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaAjustes(vm: AgendaVM) {
    val ctx = LocalContext.current
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sinc by vm.sincronizando.collectAsStateWithLifecycle()
    var url by rememberSaveable(estado.url) { mutableStateOf(estado.url) }

    val elegirTono = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33)
                r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else @Suppress("DEPRECATION") r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            vm.setSonido(uri?.toString())
        }
    }
    val nombreTono = remember(estado.sonido) {
        estado.sonido?.let {
            try { RingtoneManager.getRingtone(ctx, Uri.parse(it))?.getTitle(ctx) } catch (_: Exception) { null }
        } ?: "Tono de alarma del sistema"
    }

    Scaffold(
        containerColor = C.fondo,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { vm.atras() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = C.texto) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.fondo),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Bloque("Origen de los datos") {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL del JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.verde, focusedLabelColor = C.verde),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setUrl(url) }, enabled = url.trim() != estado.url && url.isNotBlank()) {
                        Text("Guardar URL", color = C.fondo)
                    }
                    if (estado.url != URL_POR_DEFECTO) {
                        OutlinedButton(
                            onClick = { url = URL_POR_DEFECTO; vm.setUrl(URL_POR_DEFECTO) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                        ) { Text("Por defecto") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.sincronizar() }, enabled = !sinc) {
                        Text(if (sinc) "Sincronizando…" else "Sincronizar ahora", color = C.fondo)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        (if (estado.ultimoFallo > 0) "Sin conexión · " else "") + "última: " +
                            if (estado.ultimaSync > 0) fechaCorta(estado.ultimaSync) else "nunca",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Bloque("Preaviso") {
                Text("Aviso unos minutos antes de cada tarea.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { m ->
                        FilterChip(
                            selected = estado.preavisoMin == m,
                            onClick = { vm.setPreaviso(m) },
                            label = { Text(if (m == 0) "Desactivado" else "$m min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.verde, selectedLabelColor = C.fondo,
                            ),
                        )
                    }
                }
            }

            Bloque("Alarma") {
                Text("Sonido", style = MaterialTheme.typography.titleSmall)
                Text(nombreTono, style = MaterialTheme.typography.bodyMedium, color = C.texto2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            elegirTono.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonido de la alarma")
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, estado.sonido?.let(Uri::parse))
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                    ) { Text("Elegir tono") }
                    if (estado.sonido != null) {
                        OutlinedButton(
                            onClick = { vm.setSonido(null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                        ) { Text("Tono del sistema") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibración", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = estado.vibrar, onCheckedChange = { vm.setVibrar(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.verde, checkedThumbColor = C.fondo),
                    )
                }
                Button(onClick = { vm.probarAlarma() }) { Text("Probar alarma en 1 minuto", color = C.fondo) }
                if (estado.pruebaCuando > System.currentTimeMillis()) {
                    Text("Alarma de prueba a las ${hora(estado.pruebaCuando)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Bloque("Permisos") {
                Text(
                    "Notificaciones, alarmas exactas, pantalla completa y batería.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = { vm.ir(Pantalla.Bienvenida) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
                ) { Text("Revisar permisos") }
            }

            Text(
                "Agenda de redes · versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Bloque(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        content()
    }
}
