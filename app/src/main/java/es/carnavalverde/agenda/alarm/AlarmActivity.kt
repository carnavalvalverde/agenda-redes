package es.carnavalverde.agenda.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import es.carnavalverde.agenda.MainActivity
import es.carnavalverde.agenda.data.Tarea
import es.carnavalverde.agenda.data.enMadrid
import es.carnavalverde.agenda.ui.AgendaTema
import es.carnavalverde.agenda.ui.C
import es.carnavalverde.agenda.ui.Chip
import es.carnavalverde.agenda.ui.AppChip
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/** Pantalla completa de alarma, visible con el móvil bloqueado. */
class AlarmActivity : ComponentActivity() {
    private var tareaId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        tareaId = intent.getStringExtra(Programador.EXTRA_ID)
        setContent {
            AgendaTema {
                val id = tareaId
                var tarea by remember(id) { mutableStateOf<Tarea?>(null) }
                LaunchedEffect(id) { tarea = id?.let { Programador.buscar(this@AlarmActivity, it) } }
                PantallaAlarma(
                    tarea = tarea,
                    abrir = { abrir() },
                    posponer = { accion { Acciones.posponer(this, it) } },
                    hecho = { accion { Acciones.hecho(this, it) } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tareaId = intent.getStringExtra(Programador.EXTRA_ID)
    }

    private fun accion(bloque: suspend (String) -> Unit) {
        val id = tareaId ?: return finish()
        lifecycleScope.launch {
            bloque(id)
            finish()
        }
    }

    private fun abrir() {
        val id = tareaId ?: return finish()
        Acciones.parar(this, id)
        val ir = {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_TAREA, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
        val km = getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { ir() }
                override fun onDismissCancelled() { finish() }
                override fun onDismissError() { ir() }
            })
        } else ir()
    }
}

private val HORA = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PantallaAlarma(tarea: Tarea?, abrir: () -> Unit, posponer: () -> Unit, hecho: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.fondo)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.NotificationsActive, null, tint = C.verde, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            tarea?.epochMs?.enMadrid()?.format(HORA) ?: "",
            style = MaterialTheme.typography.displayLarge,
            color = C.texto,
        )
        Spacer(Modifier.height(12.dp))
        AppChip(tarea?.app)
        Spacer(Modifier.height(16.dp))
        Text(
            tarea?.titulo ?: "",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tarea?.donde?.forEach { Chip(it) }
        }
        Spacer(Modifier.weight(1.4f))
        Button(
            onClick = abrir,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Abrir tarea", style = MaterialTheme.typography.titleMedium, color = C.fondo) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = posponer,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.texto),
            ) {
                Icon(Icons.Filled.Snooze, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Posponer 5 min", maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = hecho,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = C.verde),
            ) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.size(8.dp)); Text("Hecho")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
