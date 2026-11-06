# Éter

Aplicación Android de radio por internet, **sin publicidad**, sin cuentas de usuario y sin servidor propio. El móvil se conecta directamente a los streams que publican las emisoras.

> **Última actualización del estado:** Fase 1 completada ✅ (nov. 2026) · En curso: Fase 2

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
| Inyección de dependencias | Hilt (solo si no complica el proyecto) |
| SDK | minSdk 26 · targetSdk: última versión estable |

---

## Características

1. Lista de emisoras con buscador por nombre, país (España por defecto) y género.
2. Favoritas persistentes en Room, accesibles sin conexión.
3. Alta manual de emisoras (nombre + URL del stream), con edición y borrado.
4. Reproducción en segundo plano con notificación (play/pausa, anterior/siguiente entre favoritas).
5. Metadatos ICY (título de la canción) cuando la emisora los envía.
6. Reconexión automática si se corta el stream y mensajes de error claros.
7. Compatibilidad con controles Bluetooth/auriculares y, si es sencillo, con Android Auto.
8. Temporizador para dormir.

---

## Requisitos técnicos

- Permitir tráfico `http` solo para los dominios necesarios mediante `network_security_config`, ya que algunas emisoras no usan `https`.
- Permisos mínimos: `INTERNET`, `FOREGROUND_SERVICE` (tipo `mediaPlayback`) y `POST_NOTIFICATIONS`.
- Cero SDKs de publicidad, analítica o rastreo.
- Tema claro/oscuro y textos en español.
- Para Radio Browser: usar un mirror aleatorio y un `User-Agent` propio de la app.

---

## Estructura del proyecto (aplicada)

> Estructura acordada en la fase 1. Paquete raíz `com.jjcampac.eter`; sin Hilt por ahora
> (inyección manual ligera en `di/`).

```
app/src/main/java/com/jjcampac/eter/
├── di/                  # Inyección de dependencias simple (sin Hilt)
├── domain/
│   ├── model/           # Modelos de dominio (Station, etc.)
│   └── repository/      # Interfaz de repositorio (por completar en Fases 3-4)
├── data/                # (Se añadirá: Room local + API remota de Radio Browser)
├── player/              # RadioPlayer (ExoPlayer) + service/MediaSessionService
├── ui/
│   ├── components/      # Elementos reutilizables de Compose
│   ├── main/            # Pantalla principal (lista y buscador)
│   ├── player/          # Pantalla del reproductor + ViewModel
│   └── theme/           # Tema Material 3 (claro/oscuro)
├── util/                # Constantes y datos de prueba
└── MainActivity.kt
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

### Fase 2 — Segundo plano y notificación ⬜

Objetivo: que la reproducción continúe con la pantalla apagada.

- [ ] `MediaSessionService` con servicio en primer plano (`mediaPlayback`)
- [ ] Notificación con play/pausa
- [ ] Gestión del permiso `POST_NOTIFICATIONS`
- [ ] Respuesta a controles de auriculares y Bluetooth
- [ ] Metadatos ICY en la notificación y en la interfaz

**Cómo probar:** bloquear el móvil y comprobar que sigue sonando y que los controles responden.

### Fase 3 — Room y favoritas ⬜

Objetivo: persistir emisoras y favoritas.

- [ ] Entidades, DAO y base de datos Room
- [ ] Alta, edición y borrado manual de emisoras
- [ ] Marcar y desmarcar favoritas
- [ ] Navegación anterior/siguiente entre favoritas desde la notificación

**Cómo probar:** añadir emisoras, cerrar la app y comprobar que se conservan.

### Fase 4 — API de Radio Browser y buscador ⬜

Objetivo: descubrir emisoras sin mantener un listado propio.

- [ ] Cliente Retrofit con selección de mirror aleatorio y `User-Agent` propio
- [ ] Búsqueda por nombre, país (España por defecto) y género
- [ ] Añadir a favoritas desde los resultados
- [ ] Manejo de errores de red

**Cómo probar:** buscar una emisora conocida y reproducirla desde los resultados.

### Fase 5 — Pulido y robustez ⬜

Objetivo: dejar la app lista para el uso diario.

- [ ] Reconexión automática ante cortes de stream
- [ ] Mensajes de error claros para el usuario
- [ ] Temporizador para dormir
- [ ] Tema claro/oscuro
- [ ] Compatibilidad con Android Auto (opcional)
- [ ] Revisión final de permisos y dependencias (sin rastreo)

**Cómo probar:** cortar la conexión durante la reproducción y comprobar que se recupera.

---

## Resumen del estado

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Proyecto base y reproductor | ✅ Completada |
| 2 | Segundo plano y notificación | ⬜ Pendiente |
| 3 | Room y favoritas | ⬜ Pendiente |
| 4 | API de Radio Browser y buscador | ⬜ Pendiente |
| 5 | Pulido y robustez | ⬜ Pendiente |

---

## Notas de desarrollo

- Cada fase se compila y se prueba en el dispositivo antes de pasar a la siguiente.
- No se añaden librerías fuera de las indicadas sin consultarlo antes.
- Las URLs de los streams pueden cambiar: la app debe permitir editarlas.
- Si la app llegara a publicarse, comprobar que las emisoras permiten su retransmisión.

---

## Licencia

_(Por definir)_
