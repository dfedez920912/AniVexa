<div align="center">
  <br />
  <img src="media-resources/app_icon.png" width="144" alt="AniVexa App Icon" />
  <h1>AniVexa</h1>
  <p><strong>Calendario de estrenos de anime, seguimiento de episodios, watchlist y widgets para Android</strong></p>
  <p>
    <a href="https://github.com/dfedez920912/AniVexa/releases/latest">
      <img src="https://img.shields.io/github/v/release/dfedez920912/AniVexa?label=APK%20Release&color=00C2FF&style=for-the-badge" alt="Latest Release" />
    </a>
    <a href="https://github.com/dfedez920912/AniVexa/releases">
      <img src="https://img.shields.io/github/downloads/dfedez920912/AniVexa/total?label=Downloads&color=7C4DFF&style=for-the-badge" alt="Downloads" />
    </a>
    <a href="https://github.com/dfedez920912/AniVexa/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/dfedez920912/AniVexa?color=10B981&style=for-the-badge" alt="GPL-2.0 License" />
    </a>
  </p>
</div>

---

**AniVexa** es una aplicación Android nativa escrita en Kotlin y Jetpack Compose conectada a la API GraphQL de AniList para seguir los estrenos de anime por día, semana, mes y temporada, gestionar tu lista personal con progreso episodio a episodio y mantener widgets en tiempo real en la pantalla de inicio.

## Instalación y actualizaciones

Puedes descargar e instalar **AniVexa** directamente desde:

- **Komi Store**: Busca `AniVexa` (`dfedez920912/AniVexa`) dentro de [Komi Store](https://github.com/komi-store/komi-store) para instalar y recibir actualizaciones automáticas con verificación de firma.
- **GitHub Releases**: Descarga el archivo `AniVexa-vX.Y.Z.apk` (y verifica su checksum `.sha256` opcionalmente) desde la sección de [Releases](https://github.com/dfedez920912/AniVexa/releases).
- **Obtainium**: Añade `https://github.com/dfedez920912/AniVexa` como fuente de GitHub Releases.

## Funcionalidades

- **Inicio**: Estrenos de hoy, catálogo de **Esta Temporada** y **Ranking Popular** con exploración completa en **Ver todo** por temporada y año.
- **Calendario avanzado**: Vistas de **Semana**, **Mes** y **Lista** con filtros por formato (`TV`, `ONA`, `OVA`, `Película`, etc.), género y estudio.
- **Mi Lista (Watchlist) y progreso completo**: Estados *Por ver*, *En proceso* y *Visto*, selector desplazable para series largas de cualquier cantidad de capítulos y barra de progreso calculada sobre el total real de la temporada.
- **Contenido relacionado**: Navegación directa entre precuelas, secuelas, OVAs y entregas vinculadas desde la ficha de cada serie.
- **Widgets nativos**:
  - Widget `1x1` con el contador de estrenos del día real del dispositivo.
  - Widget `3x2` con la lista desplazable de estrenos de hoy y horarios (compatible con zona horaria local o JST).
- **Notificaciones locales**: Recordatorios configurables antes del estreno de tus series favoritas con modo horas de silencio.
- **Temas Material 3**: *AniVexa Dark*, *Light*, *Dynamic Color* y **Cristal (Liquid Glass)** con barra inferior estilo dock flotante.
- **Persistencia local segura**: Base de datos Room con migraciones no destructivas y respaldo automático para conservar todo tu historial al actualizar.

## Requisitos

- **Android 7.0 (API 24)** o superior (`minSdk = 24`, `targetSdk = 36`).
- Para compilación desde el código fuente:
  - JDK 25 (según `gradle/gradle-daemon-jvm.properties`) y Android SDK 36.
  - Pruebas unitarias: `./gradlew testDebugUnitTest` (Linux/macOS) o `gradlew.bat testDebugUnitTest` (Windows).
  - APK de depuración: `./gradlew assembleDebug`.

## Versiones y distribución

Cada etiqueta `vMAJOR.MINOR.PATCH` ejecuta el flujo de CI en GitHub Actions que corre las pruebas unitarias Robolectric, compila el APK de release firmado y publica el binario `.apk` junto con su archivo `.sha256` en [GitHub Releases](https://github.com/dfedez920912/AniVexa/releases).

## Documentación adicional

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [SECURITY.md](SECURITY.md)

## Tecnologías

- Kotlin & Coroutines / Flow
- Jetpack Compose & Material Design 3
- Room (persistencia local con migraciones)
- WorkManager & AppWidgets
- OkHttp & Retrofit / GraphQL (AniList API)

## Licencia

Este proyecto se distribuye bajo la licencia **GNU General Public License v2.0 (GPL-2.0)**. Consulta el texto completo en [`LICENSE`](./LICENSE).
