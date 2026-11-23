# AniVexa v0.1.7-beta.2

## Novedades de esta versión

### Pantalla de Onboarding con copia de seguridad y restauración `.avb`
- **Splash Screen limpio (`main`)**: Se mantiene la pantalla de bienvenida minimalista original antes de entrar a la app.
- **Nuevo Onboarding interactivo**: Presenta las novedades principales de AniVexa e incluye una tarjeta destacada de respaldo cifrado **AES-256 (`.avb`)** para restaurar tu base de datos personal (Mi Lista, favoritos, episodios vistos, calificaciones y ajustes) directamente desde la configuración inicial.

### Corrección de autenticación con Google Drive y estabilidad de arranque
- **Google Drive OAuth en Ajustes y Restauración**: Cambio a `launchMode="singleTop"` en `MainActivity`, preservación del estado del diálogo tras el consentimiento y reintento automático de obtención del token cuando Google Play Services finaliza la autorización.
- **Corrección de cierre abrupto al abrir la app**: Refactorización de `AppStrings` a interfaz para evitar `ClassFormatError` en tiempo de ejecución y limpieza de dependencias Firebase sin uso.

### Cambio de idioma global de la aplicación (Español, English, Français, 日本語)
- **Desplegable unificado en Ajustes**: Aplica de forma instantánea el idioma elegido a toda la aplicación y lo conserva en las copias cifradas `.avb`.
