package es.carnavalverde.agenda.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import es.carnavalverde.agenda.MainActivity
import es.carnavalverde.agenda.app
import es.carnavalverde.agenda.data.MADRID
import es.carnavalverde.agenda.data.Tarea
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Programa en AlarmManager, por cada tarea futura no hecha:
 *  - "P:<id>" preaviso X minutos antes (notificación normal),
 *  - "A:<id>" la alarma a la hora exacta (setAlarmClock),
 *  - "S:<id>" una alarma pospuesta.
 */
object Programador {
    const val PRUEBA_ID = "__prueba__"
    const val EXTRA_CLAVE = "clave"
    const val EXTRA_ID = "id"
    const val ACCION_DISPARAR = "es.carnavalverde.agenda.DISPARAR"

    private val mutex = Mutex()

    fun tareaPrueba(epochMs: Long) = Tarea(
        id = PRUEBA_ID,
        cuando = Instant.ofEpochMilli(epochMs).atZone(MADRID).toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
        app = "Prueba",
        donde = listOf("Alarma de prueba"),
        titulo = "Alarma de prueba",
        nota = "Esto es una tarea ficticia para comprobar que la alarma suena.",
        pasos = listOf("Comprueba que ha sonado aunque el móvil estuviera bloqueado.", "Pulsa «Hecho»."),
    )

    /** Busca una tarea por id en la caché (o la tarea de prueba). */
    suspend fun buscar(ctx: Context, id: String): Tarea? {
        if (id == PRUEBA_ID) {
            val t = ctx.app.ajustes.actual().pruebaCuando
            return tareaPrueba(if (t > 0) t else System.currentTimeMillis())
        }
        return ctx.app.repo.agenda.value.tareas.firstOrNull { it.id == id }
    }

    suspend fun reprogramarTodo(ctx: Context) = mutex.withLock {
        val app = ctx.app
        val est = app.ajustes.actual()
        val ahora = System.currentTimeMillis()
        val deseadas = linkedMapOf<String, Long>()

        for (t in app.repo.agenda.value.tareas) {
            if (t.id in est.hechas || t.epochMs <= ahora) continue
            deseadas["A:${t.id}"] = t.epochMs
            if (est.preavisoMin > 0) {
                val p = t.epochMs - est.preavisoMin * 60_000L
                if (p > ahora) deseadas["P:${t.id}"] = p
            }
        }
        val idsVivos = app.repo.agenda.value.tareas.map { it.id }.toSet() + PRUEBA_ID
        for ((id, t) in est.pospuestas) {
            if (t > ahora && id !in est.hechas && id in idsVivos) deseadas["S:$id"] = t
        }
        if (est.pruebaCuando > ahora) deseadas["A:$PRUEBA_ID"] = est.pruebaCuando

        val am = ctx.getSystemService(AlarmManager::class.java)
        for (clave in est.programadas - deseadas.keys) {
            am.cancel(pi(ctx, clave))
        }
        for ((clave, cuando) in deseadas) {
            programar(ctx, am, clave, cuando)
        }
        app.ajustes.setProgramadas(deseadas.keys)
        Log.i("Agenda", "Alarmas programadas: ${deseadas.size}")
    }

    fun puedeExactas(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun programar(ctx: Context, am: AlarmManager, clave: String, cuando: Long) {
        val pi = pi(ctx, clave)
        val exactas = puedeExactas(ctx)
        try {
            if (clave.startsWith("P:")) {
                if (exactas) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
            } else {
                if (exactas) {
                    val mostrar = PendingIntent.getActivity(
                        ctx, 1,
                        Intent(ctx, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_TAREA, clave.substring(2))
                            .setData(Uri.parse("agenda://ver/" + Uri.encode(clave))),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(cuando, mostrar), pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
                }
            }
        } catch (e: SecurityException) {
            Log.w("Agenda", "sin permiso de alarmas exactas", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }

    private fun pi(ctx: Context, clave: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACCION_DISPARAR)
            .setData(Uri.parse("agenda://alarma/" + Uri.encode(clave)))
            .putExtra(EXTRA_CLAVE, clave)
        return PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
