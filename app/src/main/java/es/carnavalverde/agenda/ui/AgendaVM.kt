package es.carnavalverde.agenda.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.carnavalverde.agenda.alarm.Acciones
import es.carnavalverde.agenda.alarm.Programador
import es.carnavalverde.agenda.app
import es.carnavalverde.agenda.data.Agenda
import es.carnavalverde.agenda.data.Estado
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed interface Pantalla {
    data object Bienvenida : Pantalla
    data object Lista : Pantalla
    data class Detalle(val id: String) : Pantalla
    data object Ajustes : Pantalla
}

class AgendaVM(application: Application) : AndroidViewModel(application) {
    private val ctx get() = getApplication<Application>()
    private val ajustes = ctx.app.ajustes
    private val repo = ctx.app.repo

    val agenda: StateFlow<Agenda> = repo.agenda
    val estado: StateFlow<Estado> = ajustes.flujo.stateIn(
        viewModelScope, SharingStarted.Eagerly, runBlocking { ajustes.actual() },
    )

    val sincronizando = MutableStateFlow(false)
    val ahora = MutableStateFlow(System.currentTimeMillis())

    /** Pila de navegación sencilla. */
    val pila = MutableStateFlow(
        if (estado.value.bienvenidaVista) listOf<Pantalla>(Pantalla.Lista) else listOf(Pantalla.Bienvenida)
    )

    private var ultimaSyncPedida = 0L

    init {
        viewModelScope.launch {
            while (true) { ahora.value = System.currentTimeMillis(); delay(20_000) }
        }
    }

    fun ir(p: Pantalla) { pila.value = pila.value + p }
    fun atras(): Boolean {
        if (pila.value.size <= 1) return false
        pila.value = pila.value.dropLast(1); return true
    }
    fun raiz(p: Pantalla) { pila.value = listOf(p) }

    fun abrirTarea(id: String) {
        val base = pila.value.filter { it !is Pantalla.Detalle }.ifEmpty { listOf(Pantalla.Lista) }
        pila.value = base + Pantalla.Detalle(id)
    }

    fun terminarBienvenida() {
        viewModelScope.launch {
            ajustes.setBienvenidaVista()
            Programador.reprogramarTodo(ctx)
        }
        raiz(Pantalla.Lista)
    }

    /** Al abrir la app (con un mínimo de 60 s entre sincronizaciones automáticas). */
    fun alAbrir() {
        viewModelScope.launch { Programador.reprogramarTodo(ctx) }
        if (System.currentTimeMillis() - ultimaSyncPedida > 60_000) sincronizar(aviso = false)
    }

    fun sincronizar(aviso: Boolean = true) {
        if (sincronizando.value) return
        ultimaSyncPedida = System.currentTimeMillis()
        sincronizando.value = true
        viewModelScope.launch {
            val err = repo.sincronizar()
            Programador.reprogramarTodo(ctx)
            sincronizando.value = false
            if (aviso) toast(if (err == null) "Sincronizado" else "No se pudo sincronizar: $err")
        }
    }

    fun marcar(id: String, hecha: Boolean) {
        viewModelScope.launch {
            Acciones.parar(ctx, id)
            Acciones.marcar(ctx, id, hecha)
        }
    }

    fun setUrl(v: String) = viewModelScope.launch { ajustes.setUrl(v); sincronizar() }
    fun setPreaviso(v: Int) = viewModelScope.launch { ajustes.setPreaviso(v); Programador.reprogramarTodo(ctx) }
    fun setSonido(v: String?) = viewModelScope.launch { ajustes.setSonido(v) }
    fun setVibrar(v: Boolean) = viewModelScope.launch { ajustes.setVibrar(v) }

    fun probarAlarma() {
        viewModelScope.launch {
            val t = System.currentTimeMillis() + 60_000L
            ajustes.setPrueba(t)
            Programador.reprogramarTodo(ctx)
            toast("La alarma de prueba sonará dentro de 1 minuto")
        }
    }

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
}
