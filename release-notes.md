# AniVexa v0.1.7-beta.6

## Novedades y Correcciones de esta versión

### Soporte Extendido de Java 8+ (Desugaring en Android 7.0+)
- **Core Library Desugaring**: Habilitada la desazucaración de bibliotecas (`desugar_jdk_libs`) en Gradle para la API `java.time.*` (`LocalDate`, `YearMonth`, `DayOfWeek`, `ZoneId`), garantizando compatibilidad completa y eliminando errores de compilación/inspección en dispositivos con API < 26.

### Corrección Visual del Splash Screen en Modo Claro
- **Fondo Adaptativo del Splash Screen**: Corrección en `SplashScreen.kt` sustituyendo el degradado azul oscuro estático por `.screenBackdrop()`. Esto elimina la franja oscura en el centro de la pantalla en modo claro y asegura un contraste y legibilidad óptimos del texto.

### Limpieza de Código y Optimizaciones
- **Limpieza de Advertencias e Imports**: Removidos imports obsoletos y corregidas advertencias en `CalendarScreen`, `HomeScreen`, `SearchScreen`, `MyListScreen`, `AnimeDetailScreen`, `SettingsScreen`, `AnimePosterCard` y `AnimeViewModel`.
