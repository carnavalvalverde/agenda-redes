package es.carnavalverde.agenda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.carnavalverde.agenda.alarm.Acciones
import es.carnavalverde.agenda.ui.AgendaTema
import es.carnavalverde.agenda.ui.AgendaVM
import es.carnavalverde.agenda.ui.Pantalla
import es.carnavalverde.agenda.ui.PantallaAjustes
import es.carnavalverde.agenda.ui.PantallaBienvenida
import es.carnavalverde.agenda.ui.PantallaDetalle
import es.carnavalverde.agenda.ui.PantallaLista

class MainActivity : ComponentActivity() {
    private val vm: AgendaVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF1C1C22.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF1C1C22.toInt()),
        )
        if (savedInstanceState == null) manejar(intent)
        setContent {
            AgendaTema {
                val pila by vm.pila.collectAsStateWithLifecycle()
                BackHandler(enabled = pila.size > 1) { vm.atras() }
                when (val p = pila.last()) {
                    Pantalla.Bienvenida -> PantallaBienvenida(vm)
                    Pantalla.Lista -> PantallaLista(vm)
                    is Pantalla.Detalle -> PantallaDetalle(vm, p.id)
                    Pantalla.Ajustes -> PantallaAjustes(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.alAbrir()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejar(intent)
    }

    private fun manejar(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_TAREA) ?: return
        if (intent.getBooleanExtra(EXTRA_PARAR, false)) Acciones.parar(this, id)
        if (vm.estado.value.bienvenidaVista) vm.abrirTarea(id)
    }

    companion object {
        const val EXTRA_TAREA = "tarea"
        const val EXTRA_PARAR = "parar"
    }
}
