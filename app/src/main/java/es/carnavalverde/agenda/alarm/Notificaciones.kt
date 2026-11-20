package es.carnavalverde.agenda.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import es.carnavalverde.agenda.MainActivity
import es.carnavalverde.agenda.R
import es.carnavalverde.agenda.data.Tarea
import es.carnavalverde.agenda.data.enMadrid
import java.time.format.DateTimeFormatter

object Notificaciones {
    const val CANAL_ALARMAS = "alarmas_v1"
    const val CANAL_PREAVISOS = "preavisos_v1"

    fun crearCanales(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        // El sonido de la alarma lo reproduce AlarmService en bucle con USAGE_ALARM;
        // el canal lleva los mismos atributos de alarma pero sin sonido propio para no duplicarlo.
        val alarmas = NotificationChannel(CANAL_ALARMAS, "Alarmas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarma a la hora exacta de cada tarea"
            setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val preavisos = NotificationChannel(CANAL_PREAVISOS, "Preavisos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso unos minutos antes de cada tarea"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(alarmas, preavisos))
    }

    fun idAlarma(id: String) = ("A:$id").hashCode() and 0x7fffffff
    fun idPreaviso(id: String) = ("P:$id").hashCode() and 0x7fffffff

    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    fun abrirTareaPi(ctx: Context, id: String, parar: Boolean = false): PendingIntent = PendingIntent.getActivity(
        ctx, if (parar) 5 else 2,
        Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TAREA, id)
            .putExtra(MainActivity.EXTRA_PARAR, parar)
            .setData(Uri.parse("agenda://tarea/" + (if (parar) "parar/" else "") + Uri.encode(id)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun accionPi(ctx: Context, accion: String, id: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, 3,
        Intent(ctx, AlarmReceiver::class.java).setAction(accion)
            .setData(Uri.parse("agenda://accion/" + Uri.encode(id)))
            .putExtra(Programador.EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun preaviso(ctx: Context, t: Tarea, minutos: Int) {
        val n = NotificationCompat.Builder(ctx, CANAL_PREAVISOS)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFF9CC45A.toInt())
            .setContentTitle(if (minutos > 0) "En $minutos min: ${t.titulo}" else t.titulo)
            .setContentText("${t.epochMs.enMadrid().format(HORA)} · ${t.donde.joinToString(" · ")}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirTareaPi(ctx, t.id))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(idPreaviso(t.id), n)
        } catch (_: SecurityException) { }
    }

    /** Notificación de alarma. Con [sonando] lleva full-screen intent. */
    fun alarma(ctx: Context, t: Tarea, sonando: Boolean): Notification {
        val pantalla = PendingIntent.getActivity(
            ctx, 4,
            Intent(ctx, AlarmActivity::class.java)
                .putExtra(Programador.EXTRA_ID, t.id)
                .setData(Uri.parse("agenda://pantalla/" + Uri.encode(t.id)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(ctx, CANAL_ALARMAS)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFF9CC45A.toInt())
            .setContentTitle(t.titulo)
            .setContentText("${t.epochMs.enMadrid().format(HORA)} · ${t.donde.joinToString(" · ")}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(!sonando)
            .setContentIntent(if (sonando) pantalla else abrirTareaPi(ctx, t.id))
            .addAction(0, "Abrir tarea", abrirTareaPi(ctx, t.id, parar = true))
            .addAction(0, "Posponer 5 min", accionPi(ctx, AlarmReceiver.ACCION_POSPONER, t.id))
            .addAction(0, "Hecho", accionPi(ctx, AlarmReceiver.ACCION_HECHO, t.id))
        if (sonando) b.setFullScreenIntent(pantalla, true)
        return b.build()
    }

    fun cancelarAlarma(ctx: Context, id: String) {
        NotificationManagerCompat.from(ctx).cancel(idAlarma(id))
    }
}
