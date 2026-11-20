package es.carnavalverde.agenda.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import es.carnavalverde.agenda.app
import es.carnavalverde.agenda.data.Tarea
import kotlinx.coroutines.runBlocking

/**
 * Servicio en primer plano que hace sonar la alarma: tono en bucle con USAGE_ALARM
 * (suena en silencio y en No molestar), vibración y notificación con full-screen intent.
 * A los 2 minutos se calla y deja la notificación fija.
 */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrador: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var actual: Tarea? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(Programador.EXTRA_ID)
        val (tarea, estado) = runBlocking {
            val est = app.ajustes.actual()
            if (intent?.getBooleanExtra(EXTRA_POSPUESTA, false) == true && id != null) app.ajustes.quitarPospuesta(id)
            (id?.let { Programador.buscar(this@AlarmService, it) }) to est
        }
        val valida = tarea != null && tarea.id !in estado.hechas &&
            !(tarea.id == Programador.PRUEBA_ID && estado.pruebaCuando == 0L)

        // Quita las demás notificaciones de la app (preaviso, alarmas viejas). Si hay otras,
        // Android las agrupa con la alarma, la marca como silenciosa y no abre la pantalla completa.
        if (valida && actual == null) NotificationManagerCompat.from(this).cancelAll()

        // Siempre hay que llamar a startForeground tras startForegroundService.
        val t = tarea ?: Programador.tareaPrueba(System.currentTimeMillis())
        val notif = Notificaciones.alarma(this, t, sonando = true)
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Notificaciones.idAlarma(t.id), notif, tipo)

        if (!valida) {
            Log.i("Agenda", "Alarma descartada ($id): hecha o inexistente")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Si ya sonaba otra, la dejamos como notificación fija.
        actual?.takeIf { it.id != t.id }?.let { dejarFija(it) }
        actual = t
        pararSonido()
        sonar(estado.sonido, estado.vibrar)

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "agenda:alarma").apply { acquire(DURACION + 5_000) }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ tiempoAgotado() }, DURACION)
        return START_NOT_STICKY
    }

    private fun sonar(sonido: String?, vibrar: Boolean) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidatos = listOfNotNull(
            sonido?.let(Uri::parse),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in candidatos) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                Log.i("Agenda", "Sonando $uri")
                break
            } catch (e: Exception) {
                Log.w("Agenda", "No se pudo reproducir $uri", e)
                player?.release(); player = null
            }
        }
        if (vibrar) {
            vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
            }
            @Suppress("DEPRECATION")
            vibrador?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 700), 0), attrs)
        }
    }

    private fun pararSonido() {
        try { player?.stop() } catch (_: Exception) { }
        player?.release(); player = null
        vibrador?.cancel(); vibrador = null
    }

    private fun dejarFija(t: Tarea) {
        try {
            NotificationManagerCompat.from(this).notify(Notificaciones.idAlarma(t.id), Notificaciones.alarma(this, t, sonando = false))
        } catch (_: SecurityException) { }
    }

    private fun tiempoAgotado() {
        pararSonido()
        val t = actual
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        if (t != null) dejarFija(t)
        actual = null
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pararSonido()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        const val DURACION = 2 * 60_000L
        private const val EXTRA_POSPUESTA = "pospuesta"

        fun sonar(ctx: Context, id: String, pospuesta: Boolean) {
            val i = Intent(ctx, AlarmService::class.java)
                .putExtra(Programador.EXTRA_ID, id)
                .putExtra(EXTRA_POSPUESTA, pospuesta)
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (e: Exception) {
                Log.e("Agenda", "No se pudo arrancar el servicio de alarma", e)
            }
        }
    }
}
