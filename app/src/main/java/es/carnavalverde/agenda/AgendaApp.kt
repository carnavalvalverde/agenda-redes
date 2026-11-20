package es.carnavalverde.agenda

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.video.VideoFrameDecoder
import es.carnavalverde.agenda.alarm.Notificaciones
import es.carnavalverde.agenda.data.Ajustes
import es.carnavalverde.agenda.data.Repositorio
import es.carnavalverde.agenda.data.SyncWorker

class AgendaApp : Application(), SingletonImageLoader.Factory {
    lateinit var ajustes: Ajustes
        private set
    lateinit var repo: Repositorio
        private set

    override fun onCreate() {
        super.onCreate()
        ajustes = Ajustes(this)
        repo = Repositorio(this, ajustes)
        Notificaciones.crearCanales(this)
        SyncWorker.programar(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                add(OkHttpNetworkFetcherFactory())
            }
            .build()
}

val Context.app: AgendaApp get() = applicationContext as AgendaApp
