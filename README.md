# Agenda de redes

App Android de uso personal (APK, no está en Google Play). Suena como un despertador en cada tarea de redes que hay que hacer a mano. Dentro de cada tarea están los pasos, los textos para copiar y las imágenes y vídeos para guardar o compartir. Solo lee un `agenda.json` público; no envía nada.

## Instalar el APK

1. Pasa `agenda-redes-1.0.0.apk` al móvil (por cable, Drive, Telegram, etc.).
2. Ábrelo desde el móvil. Android pedirá permiso para **instalar apps de origen desconocido** desde la app con la que lo abriste (Archivos, Chrome, etc.). Concédelo y pulsa **Instalar**.
3. Si Play Protect avisa, pulsa **Más detalles → Instalar de todos modos** (la app no está en Play).
4. Para actualizar, instala el APK nuevo encima. Tiene que estar firmado con **el mismo keystore**; si no, Android no deja actualizar y habría que desinstalar la app (y se perderían las marcas de «hecho»).

Para compilar el APK: `./gradlew assembleRelease`. La firma se lee de `~/agenda-redes-firma/keystore.properties` o de la ruta que indique la variable `AGENDA_KEYSTORE_PROPS`. El resultado sale en `app/build/outputs/apk/release/app-release.apk`.

## Permisos que hay que activar

La primera vez que abras la app, la pantalla de bienvenida tiene un botón para cada permiso. Luego puedes volver a ella en **Ajustes → Revisar permisos**.

| Permiso | Para qué |
|---|---|
| **Notificaciones** | Mostrar los avisos y la alarma. |
| **Alarmas exactas** | Que suene a la hora en punto. En Android 13 o superior suele venir ya activado. |
| **Pantalla completa** | Abrir la pantalla de alarma aunque el móvil esté bloqueado (Android 14 o superior). |
| **Sin optimización de batería** | Que Android no duerma la app y pueda sincronizar cada hora. |

Además:
- En algunas marcas (Xiaomi, Huawei, Oppo, Samsung…) conviene permitir también el **inicio automático** y poner la batería de la app en **Sin restricciones**.
- La alarma usa el **volumen de alarma** del móvil, no el del timbre. Si lo tienes a cero, no sonará.
- Suena aunque el móvil esté en silencio o en No molestar, siempre que No molestar deje pasar las alarmas (es lo normal).

Para comprobarlo, usa **Ajustes → Probar alarma en 1 minuto** y bloquea el móvil.

## Cambiar la URL

**Ajustes → Origen de los datos → URL del JSON → Guardar URL**. Al guardar, sincroniza al momento.

Por defecto es:

```
https://raw.githubusercontent.com/carnavalvalverde/agenda-redes/main/agenda.json
```

Si el usuario de GitHub o el repo son otros, cámbiala aquí. También se puede cambiar el valor por defecto en `URL_POR_DEFECTO` (`app/src/main/java/es/carnavalverde/agenda/data/Ajustes.kt`). Las rutas de `archivos[].fichero` son relativas a la carpeta del JSON. Por ejemplo, `media/x.png` se descarga de `.../main/media/x.png`.

## Cómo funciona

- **Sincroniza** al abrir la app, al tirar hacia abajo en la lista, cada hora en segundo plano (si hay red) y con **Sincronizar ahora**. Guarda una copia del último JSON y descarga los ficheros de las tareas futuras, así que todo funciona sin conexión. Si falla, muestra «Sin conexión · última actualización: …».
- Por cada tarea futura que no esté hecha programa un **preaviso** (por defecto 5 minutos antes; se cambia en Ajustes) y una **alarma** a la hora exacta (`setAlarmClock`). Las horas del JSON se leen siempre como hora de Madrid.
- Cuando suena la alarma: tono de alarma en bucle y vibración durante 2 minutos como máximo. Se abre una pantalla completa con **Abrir tarea**, **Posponer 5 min** y **Hecho**. Si no tocas nada, queda una notificación fija.
- Las alarmas se vuelven a programar tras cada sincronización, al reiniciar el móvil, al cambiar la hora o la zona horaria y al actualizar la app. Ojo: si el móvil se reinicia y tiene PIN, hay que **desbloquearlo una vez** para que Android vuelva a programarlas.
- Lo de «hecho» se guarda solo en el móvil.
