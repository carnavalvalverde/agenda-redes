package es.carnavalverde.agenda.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import es.carnavalverde.agenda.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Programador.ACCION_DISPARAR -> {
                val clave = intent.getStringExtra(Programador.EXTRA_CLAVE) ?: return
                val tipo = clave.substringBefore(':')
                val id = clave.substringAfter(':')
                if (tipo == "A" || tipo == "S") {
                    // Arranca ya el servicio (estamos en la ventana permitida por la alarma exacta).
                    AlarmService.sonar(ctx, id, pospuesta = tipo == "S")
                } else {
                    asincrono {
                        val t = Programador.buscar(ctx, id) ?: return@asincrono
                        if (id !in ctx.app.ajustes.actual().hechas) {
                            Notificaciones.preaviso(ctx, t, ctx.app.ajustes.actual().preavisoMin)
                        }
                    }
                }
            }
            ACCION_POSPONER -> asincrono { Acciones.posponer(ctx, intent.getStringExtra(Programador.EXTRA_ID) ?: return@asincrono) }
            ACCION_HECHO -> asincrono { Acciones.hecho(ctx, intent.getStringExtra(Programador.EXTRA_ID) ?: return@asincrono) }
        }
    }

    private fun asincrono(bloque: suspend () -> Unit) {
        val pr = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try { bloque() } catch (e: Exception) { Log.e("Agenda", "receiver", e) } finally { pr.finish() }
        }
    }

    companion object {
        const val ACCION_POSPONER = "es.carnavalverde.agenda.POSPONER"
        const val ACCION_HECHO = "es.carnavalverde.agenda.HECHO"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.i("Agenda", "Reprogramando por ${intent.action}")
        val pr = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try { Programador.reprogramarTodo(ctx) } finally { pr.finish() }
        }
    }
}

/** Acciones comunes de la alarma (pantalla, notificación). */
object Acciones {
    /** Para el sonido y quita la notificación de alarma. */
    fun parar(ctx: Context, id: String) {
        ctx.stopService(Intent(ctx, AlarmService::class.java))
        Notificaciones.cancelarAlarma(ctx, id)
    }

    suspend fun posponer(ctx: Context, id: String) {
        parar(ctx, id)
        ctx.app.ajustes.posponer(id, System.currentTimeMillis() + 5 * 60_000L)
        Programador.reprogramarTodo(ctx)
    }

    suspend fun hecho(ctx: Context, id: String) {
        parar(ctx, id)
        marcar(ctx, id, true)
    }

    suspend fun marcar(ctx: Context, id: String, hecha: Boolean) {
        if (id == Programador.PRUEBA_ID) {
            ctx.app.ajustes.setPrueba(0)
            ctx.app.ajustes.quitarPospuesta(id)
        } else {
            ctx.app.ajustes.setHecha(id, hecha)
        }
        Programador.reprogramarTodo(ctx)
    }
}
