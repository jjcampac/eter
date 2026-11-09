<!-- Autor: jjcampac - IA: cline -->
# Simple Radio

Aplicación Android de radio por internet, **sin publicidad**, sin cuentas de usuario y sin servidor propio. El móvil se conecta directamente a los streams que publican las emisoras.

> **Última actualización del estado:** Fases 1-5 completadas ✅ (aplicación base funcional). Reordenamiento de Favoritas (drag & drop + accesibilidad) ✅ probado en dispositivo. Notificación multimedia del sistema (tarjeta de la cortina con emisora, canción, estado y controles) ✅ compilada, pendiente de prueba en dispositivo. Android Auto (app de audio con biblioteca de emisoras) ✅ probada en coche. Build release (despliegue desde Android Studio, R8 y firma) ✅ corregido y probado en dispositivo.

---

## Descripción

El objetivo es disponer de un reproductor de radio sencillo, limpio y respetuoso con el usuario:

- Sin anuncios, analítica ni rastreo.
- Sin registro ni cuentas.
- Sin backend propio: la app consume directamente los streams (Icecast/Shoutcast, MP3, AAC o HLS) y, opcionalmente, el directorio público [Radio Browser](https://www.radio-browser.info/).
- Funciona en segundo plano, con la pantalla apagada y con controles de auriculares, Bluetooth y, si es viable, Android Auto.

---

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose (Material 3) |
| Reproducción | Media3 (ExoPlayer + `MediaSessionService`) |
| Persistencia | Room |
| Red | Retrofit + OkHttp (solo para la API de Radio Browser) |
| Arquitectura | MVVM con repositorio |
| Inyección de dependencias | Manual ligera en `di/` (sin Hilt) |
| SDK | minSdk 26 · targetSdk 37 |

---

## Características

1. Lista de emisoras con buscador por nombre, país y género (España por defecto, adaptado al idioma de la UI).
2. Dos orígenes de búsqueda: **"Mis Emisoras"** (añadidas a mano, con edición, borrado y gesto de deslizar) e **"Internet"** (Radio Browser).
3. Favoritas persistentes en Room, accesibles sin conexión.
4. Reproducción en segundo plano con notificación (play/pausa, anterior/siguiente entre favoritas).
5. Metadatos ICY/HLS (título de la canción) cuando la emisora los envía, mostrados sin metaetiquetas.
6. Reconexión automática si se corta el stream, con aviso al usuario tras un tiempo configurable en Ajustes.
7. Compatibilidad con controles Bluetooth/auriculares y, si es sencillo, con Android Auto.
8. Temporizador para dormir (15/30/45/60 min) con aviso del tiempo restante en el reproductor.
9. Interfaz en 6 idiomas (español, inglés, alemán, francés, italiano y japonés) con cambio al instante.
10. Tema claro/oscuro (Sistema / Claro / Oscuro) con cambio en vivo.
11. Iconos dinámicos en la lista: ondas animadas mientras suena, pausa clásica cuando está pausada.
12. Reordenar las **favoritas** arrastrando con el dedo (pulsación larga) o mediante las acciones de accesibilidad "Mover arriba"/"Mover abajo". El orden se conserva al cerrar la app.
13. **Notificación multimedia del sistema**: al desplegar la cortina desde el borde superior aparece la tarjeta nativa de reproducción con el logo de la emisora de fondo, su nombre como título, la canción en curso (metadatos ICY) o el estado ("Conectando…", "Error de conexión") como texto, el botón grande de reproducir/pausar, el selector de salida de audio ("Este teléfono"), flechas anterior/siguiente entre favoritas **que solo aparecen si hay otra emisora antes o después en la lista**, y una estrella que únicamente permite **añadir** la emisora en curso a favoritas (quitarla se hace desde la app).

### Controles de la tarjeta de notificación

1. Reproducir una emisora y desplegar la cortina desde el borde superior de la pantalla.
2. La tarjeta muestra el icono de la app en la esquina, el logo de la emisora de
   fondo, su nombre y la canción o el estado.
3. El botón central reproduce o pausa. Las flechas cambian de favorita y solo se
   muestran cuando existen emisora anterior o siguiente (en la primera no hay
   "anterior" y en la última no hay "siguiente").
4. La estrella añade la emisora en curso a favoritas y desaparece cuando ya lo es.
5. Deslizar la tarjeta hacia un lado descarta la notificación y detiene la reproducción.

---

## Requisitos técnicos

- Permitir tráfico `http` mediante `network_security_config` (base-config con cleartext permitido, decisión de Fase 4) porque muchas emisoras usan stream `http` o HLS en dominios arbitrarios que no pueden prefijarse. No se activa `usesCleartextTraffic` de forma global.
- Permisos mínimos: `INTERNET`, `FOREGROUND_SERVICE` (tipo `mediaPlayback`), `POST_NOTIFICATIONS` y `VIBRATE` (vibración háptica al iniciar el arrastre de favoritas).
- Cero SDKs de publicidad, analítica o rastreo.
- Tema claro/oscuro y textos en varios idiomas (español, inglés, alemán, francés, italiano y japonés).
- Para Radio Browser: usar un mirror aleatorio y un `User-Agent` propio de la app.

---

## Estructura del proyecto (aplicada)

> Estructura aplicada tras las Fases 1-5. Paquete raíz `com.jjcampac.eter`;
> inyección manual ligera en `di/` (sin Hilt).

```
app/src/main/java/com/jjcampac/eter/
├── EterApplication.kt        # Application: reproductor, temporizador y repositorio compartidos
├── MainActivity.kt           # Actividad principal (tema e idioma aplicados en attachBaseContext)
├── di/
│   └── AppModule.kt          # Inyección de dependencias manual (sin Hilt)
├── domain/
│   ├── model/                # Modelos de dominio (Station)
│   └── repository/           # Contrato del repositorio
├── data/
│   ├── local/                # Room: EterDatabase, StationDao, StationEntity
│   ├── remote/               # API de Radio Browser (Retrofit) + DTOs
│   └── repository/           # Implementación del repositorio (Room + remoto)
├── player/
│   ├── RadioPlayer.kt        # Wrapper de ExoPlayer con reconexión controlada
│   ├── SleepTimer.kt         # Temporizador para dormir (Fase 5)
│   └── service/              # RadioMediaSessionService (MediaLibraryService: notificación + Android Auto)
├── ui/
│   ├── components/           # Componentes reutilizables (StationItem, MiniPlayer…)
│   ├── main/                 # Pestañas Favoritas/Estaciones (buscador) + ViewModels
│   ├── player/               # Pantalla del reproductor + ViewModel
│   ├── settings/             # Ajustes (tema, idioma, reconexión, temporizador)
│   ├── station/              # Formulario de alta/edición de emisoras + ViewModel
│   └── theme/                # Tema Material 3 (claro/oscuro) + preferencias
├── util/                     # Datos de prueba, idioma (AppLanguage) y preferencias
└── res/
    ├── values/strings.xml      # Textos (es) + values-en, values-de, values-fr, values-it, values-ja
    └── xml/automotive_app_desc.xml   # Declaración de Eter como app de audio para Android Auto
```

---

## Fases de desarrollo

Leyenda: ⬜ Pendiente · 🟨 En curso · ✅ Completada

### Fase 1 — Proyecto base y reproductor ✅

Objetivo: tener el proyecto creado y reproducir una emisora con una URL fija.

- [x] Crear proyecto con Compose y Material 3
- [x] Proponer y confirmar la estructura de paquetes
- [x] Integrar Media3 (ExoPlayer)
- [x] Reproducir una URL de stream fija con controles básicos
- [x] Configurar `network_security_config` para tráfico `http`

**Cómo probar:** ejecutar la app y comprobar que suena la emisora de prueba.

### Fase 2 — Segundo plano y notificación ✅

Objetivo: que la reproducción continúe con la pantalla apagada.

- [x] `MediaSessionService` con servicio en primer plano (`mediaPlayback`)
- [x] Notificación con play/pausa y siguiente/anterior (placeholders)
- [x] Gestión del permiso `POST_NOTIFICATIONS`
- [x] Respuesta a controles de auriculares y Bluetooth (vía MediaSession)
- [x] Metadatos ICY (título de canción) en la notificación y en la interfaz
- [x] Fix de play/pausa: `resume()` establece `PLAYING` de inmediato (evita "Conectando…" al reanudar)
- [x] Fix de ciclo de vida: `updateNotification()` mantiene `startForeground` en `LOADING`/`PLAYING`/`PAUSED` (evita que Android mate el proceso a los ~5s de pantalla apagada)
- [x] Iconos en la lista: ondas animadas al reproducir, pausa clásica al pausar

**Cómo probar:** bloquear el móvil y comprobar que sigue sonando y que los controles responden.

### Fase 3 — Room y favoritas ✅

Objetivo: persistir emisoras y favoritas.

- [x] Entidades, DAO y base de datos Room
- [x] Alta, edición y borrado manual de emisoras
- [x] Marcar y desmarcar favoritas
- [x] Navegación anterior/siguiente entre favoritas desde el reproductor (nextFavorite/prevFavorite)

**Cómo probar:** añadir emisoras, cerrar la app y comprobar que se conservan.

### Fase 4 — API de Radio Browser y buscador ✅

Objetivo: descubrir emisoras sin mantener un listado propio.

- [x] Cliente Retrofit con selección de mirror aleatorio y `User-Agent` propio
- [x] Búsqueda por nombre, país (España por defecto) y género
- [x] Añadir a favoritas desde los resultados (persiste en Room al marcar)
- [x] Manejo de errores de red y estados de cargando/vacío
- [x] Selector de origen (Mis emisoras / Internet) con debounce de 500 ms

**Cómo probar:** buscar una emisora conocida y reproducirla desde los resultados.

### Fase 5 — Pulido y robustez ✅

Objetivo: dejar la app lista para el uso diario.

- [x] Reconexión automática ante cortes de stream, con aviso al usuario tras un tiempo configurable en Ajustes
- [x] Mensajes de error claros para el usuario
- [x] Temporizador para dormir (con aviso del tiempo restante en el reproductor)
- [x] Tema claro/oscuro
- [x] Compatibilidad con Android Auto (integración de medios nativa, sin pantallas propias)

**Cómo probar:** cortar la conexión durante la reproducción (reconexión con aviso tras la ventana configurada), probar el temporizador de sueño y cambiar de idioma en Ajustes (la UI cambia al instante y el país por defecto de la búsqueda se adapta).

### Post-5 — Android Auto ✅

Objetivo: que Eter aparezca en el coche como app de audio, sin desarrollar pantallas propias.

- [x] Declaración en `res/xml/automotive_app_desc.xml` (`<uses name="media"/>`) y `<meta-data android:name="com.google.android.gms.car.application">` en el manifiesto
- [x] `RadioMediaSessionService` pasa a `MediaLibraryService` con la misma sesión (la notificación, las favoritas, el temporizador y los controles de auriculares/Bluetooth siguen igual)
- [x] Biblioteca de navegación: raíz → "Favoritas" / "Todas" → emisoras (con logo y "país · género")
- [x] Al elegir una emisora en el coche, Media3 resuelve su `mediaId` a la URL del stream y reproduce en la misma sesión
- [x] Si la reproducción arranca desde el coche, la app sincroniza la emisora en curso (UI, mini reproductor y notificación) y guarda la última URL
- [x] Sin librerías nuevas: solo se usa `media3-session`, ya presente en el proyecto

**Validado en coche:** Nothing Phone (3a) + Android Auto. Eter aparece en el lanzador, navega por "Favoritas" y "Todas", reproduce la emisora elegida en la misma sesión (controles del volante y de la pantalla) y la app sincroniza la emisora en curso al volver al móvil. Para repetir la prueba: instalar la app, conectar con el coche y, si no aparece Eter, activarla en Android Auto → Personalizar lanzador.

---

## Resumen del estado

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Proyecto base y reproductor | ✅ Completada |
| 2 | Segundo plano y notificación | ✅ Completada |
| 3 | Room y favoritas | ✅ Completada |
| 4 | API de Radio Browser y buscador | ✅ Completada (probada en dispositivo) |
| 5 | Pulido y robustez | ✅ Completada |
| Post-5 | Reordenamiento de Favoritas (drag & drop + accesibilidad) | ✅ Completada (probada en dispositivo) |
| Post-5 | Notificación multimedia del sistema (tarjeta con emisora, canción, estado y controles) | ✅ Compilada (pendiente de prueba en dispositivo) |
| Post-5 | Android Auto (app de audio: biblioteca de favoritas y todas las emisoras) | ✅ Completada (probada en coche) |
| Post-5 | Build release: despliegue desde Android Studio (sin `.dm`) y R8 sin `packageScope` | ✅ Completada (probada en dispositivo) |

---

## Notas de desarrollo

- Cada fase se compila y se prueba en el dispositivo antes de pasar a la siguiente.
- No se añaden librerías fuera de las indicadas sin consultarlo antes.
- Las URLs de los streams pueden cambiar: la app debe permitir editarlas.
- Si la app llegara a publicarse, comprobar que las emisoras permiten su retransmisión.
- **Release:** la firma se lee de `keystore.properties` (no versionado; ver `keystore/eter-release.jks`). Si el fichero no existe, release se firma con la keystore de debug. No usar `optimization.packageScope` (R8 gradual, experimental, genera un dex inválido con `IllegalAccessError`) ni generar los `.dm` (`installation.enableBaselineProfile = false`): así Android Studio no falla con `INSTALL_BASELINE_PROFILE_FAILED`. El perfil de arranque sigue dentro del APK y lo aplica `androidx.profileinstaller` en el primer arranque.

---

## Licencia

Esta aplicación se distribuye bajo la licencia **Creative Commons Atribución-NoComercial 4.0 Internacional (CC BY-NC 4.0)**, disponible en: https://creativecommons.org/licenses/by-nc/4.0/deed.es

Puedes **usar, copiar, modificar y compartir libremente** el software, pero solo con fines no comerciales. El uso con fines comerciales está prohibido sin permiso expreso del autor.

La única condición es que, en cualquier copia o trabajo derivado, siempre debes:
- Mencionar al autor (**jjcampac**).
- Mencionar el nombre de la aplicación (**Simple Radio**).
- Indicar la URL del repositorio original (**https://github.com/jjcampac/eter**).
- Indicar si se han realizado cambios.

El software se ofrece "tal cual" (**as is**), sin garantía de ningún tipo.

La licencia completa y el aviso de copyright se encuentran en el archivo [LICENSE](LICENSE).
