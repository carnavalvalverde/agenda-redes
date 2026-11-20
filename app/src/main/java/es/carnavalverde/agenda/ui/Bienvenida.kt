package es.carnavalverde.agenda.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import es.carnavalverde.agenda.alarm.Programador

object Permisos {
    fun notificaciones(ctx: Context) =
        if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun exactas(ctx: Context) = Programador.puedeExactas(ctx)

    fun pantallaCompleta(ctx: Context) =
        if (Build.VERSION.SDK_INT >= 34) ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        else true

    fun bateria(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun abrir(ctx: Context, accion: String, conPaquete: Boolean = true) {
        val i = Intent(accion)
        if (conPaquete) i.data = Uri.parse("package:" + ctx.packageName)
        try {
            ctx.startActivity(i)
        } catch (_: Exception) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)))
        }
    }
}

@Composable
fun PantallaBienvenida(vm: AgendaVM) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val pedirNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (!ok) {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
    }

    @Suppress("UNUSED_EXPRESSION") tick // fuerza recomposición al volver de Ajustes
    val notif = remember(tick) { Permisos.notificaciones(ctx) }
    val exactas = remember(tick) { Permisos.exactas(ctx) }
    val fsi = remember(tick) { Permisos.pantallaCompleta(ctx) }
    val bateria = remember(tick) { Permisos.bateria(ctx) }

    Column(
        Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Filled.NotificationsActive, null, tint = C.verde, modifier = Modifier.size(48.dp).padding(top = 8.dp))
        Text("Agenda de redes", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Esta app te avisa como un despertador de cada tarea de redes que tienes que hacer a mano, " +
                "y dentro tienes los pasos, los textos para copiar y las imágenes y vídeos listos para compartir.\n\n" +
                "Para que la alarma suene siempre (con el móvil bloqueado, en silencio o en No molestar) " +
                "necesita estos permisos:",
            style = MaterialTheme.typography.bodyLarge,
        )
        FilaPermiso(
            "Notificaciones", "Para mostrar los avisos y la alarma.", notif,
        ) {
            if (Build.VERSION.SDK_INT >= 33) pedirNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            else ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
        FilaPermiso("Alarmas exactas", "Para que suene a la hora en punto, no «más o menos».", exactas) {
            if (Build.VERSION.SDK_INT >= 31) Permisos.abrir(ctx, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
        FilaPermiso("Pantalla completa", "Para abrir la pantalla de alarma aunque el móvil esté bloqueado.", fsi) {
            if (Build.VERSION.SDK_INT >= 34) Permisos.abrir(ctx, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        }
        FilaPermiso("Sin optimización de batería", "Para que Android no la duerma y sincronice cada hora.", bateria) {
            Permisos.abrir(ctx, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.terminarBienvenida() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text(if (notif && exactas && fsi && bateria) "Empezar" else "Continuar de todos modos", color = C.fondo, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun FilaPermiso(titulo: String, texto: String, ok: Boolean, activar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(10.dp))
        if (ok) {
            Icon(Icons.Filled.CheckCircle, "Activado", tint = C.verde, modifier = Modifier.size(28.dp))
        } else {
            Button(onClick = activar, shape = RoundedCornerShape(12.dp)) { Text("Activar", color = C.fondo) }
        }
    }
}
