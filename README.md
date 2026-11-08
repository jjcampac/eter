<!-- Autor: jjcampac - IA: cline -->
# Simple Radio

Aplicación Android de radio por internet, **sin publicidad**, sin cuentas de usuario y sin servidor propio. El móvil se conecta directamente a los streams que publican las emisoras.

> **Última actualización del estado:** Fases 1-4 completadas ✅ · Fase 5 en curso (reconexión, temporizador, idiomas y ajustes)

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

---

## Requisitos técnicos

- Permitir tráfico `http` mediante `network_security_config` (base-config con cleartext permitido, decisión de Fase 4) porque muchas emisoras usan stream `http` o HLS en dominios arbitrarios que no pueden prefijarse. No se activa `usesCleartextTraffic` de forma global.
- Permisos mínimos: `INTERNET`, `FOREGROUND_SERVICE` (tipo `mediaPlayback`) y `POST_NOTIFICATIONS`.
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
│   └── service/              # RadioMediaSessionService + PlayerNotification
├── ui/
│   ├── components/           # Componentes reutilizables (StationItem, MiniPlayer…)
│   ├── main/                 # Pestañas Favoritas/Estaciones (buscador) + ViewModels
│   ├── player/               # Pantalla del reproductor + ViewModel
│   ├── settings/             # Ajustes (tema, idioma, reconexión, temporizador)
│   ├── station/              # Formulario de alta/edición de emisoras + ViewModel
│   └── theme/                # Tema Material 3 (claro/oscuro) + preferencias
├── util/                     # Datos de prueba, idioma (AppLanguage) y preferencias
└── res/
    └── values/strings.xml      # Textos (es) + values-en, values-de, values-fr, values-it, values-ja
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

### Fase 5 — Pulido y robustez 🟨

Objetivo: dejar la app lista para el uso diario.

- [x] Reconexión automática ante cortes de stream, con aviso al usuario tras un tiempo configurable en Ajustes
- [x] Mensajes de error claros para el usuario
- [x] Temporizador para dormir (con aviso del tiempo restante en el reproductor)
- [x] Tema claro/oscuro
- [ ] Compatibilidad con Android Auto (opcional)
- [ ] Revisión final de permisos y dependencias (sin rastreo)

**Cómo probar:** cortar la conexión durante la reproducción (reconexión con aviso tras la ventana configurada), probar el temporizador de sueño y cambiar de idioma en Ajustes (la UI cambia al instante y el país por defecto de la búsqueda se adapta).

---

## Resumen del estado

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Proyecto base y reproductor | ✅ Completada |
| 2 | Segundo plano y notificación | ✅ Completada |
| 3 | Room y favoritas | ✅ Completada |
| 4 | API de Radio Browser y buscador | ✅ Completada (build OK, pendiente de prueba en dispositivo) |
| 5 | Pulido y robustez | 🟨 En curso (reconexión, temporizador, idiomas y ajustes) |

---

## Notas de desarrollo

- Cada fase se compila y se prueba en el dispositivo antes de pasar a la siguiente.
- No se añaden librerías fuera de las indicadas sin consultarlo antes.
- Las URLs de los streams pueden cambiar: la app debe permitir editarlas.
- Si la app llegara a publicarse, comprobar que las emisoras permiten su retransmisión.

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
