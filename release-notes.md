# AniVexa v0.1.7-beta.2

## Novedades de esta versión

### Pantalla de Onboarding multipágina con copia de seguridad y restauración `.avb`
- **Splash Screen limpio (`main`)**: Se mantiene la pantalla de bienvenida minimalista original antes de entrar a la app.
- **Onboarding multipágina interactivo**: Recorre las funciones principales de AniVexa en varias pantallas (estrenos con zona horaria JST/Local, Mi Lista con progreso y búsqueda, idioma global y apariencia) e incluye una pantalla final dedicada al respaldo cifrado **AES-256 (`.avb`)** con botón directo para restaurar tu base de datos desde el primer inicio.

### Cifrado automático `.avb` sin contraseña
- **Respaldo y restauración sin clave manual**: Los archivos `.avb` se cifran automáticamente con **AES-256-GCM** para proteger la base de datos y evitar su lectura por aplicaciones externas sin solicitar contraseñas al usuario.

### Corrección de autenticación con Google Drive (error código 8) y estabilidad
- **Respaldo en Google Drive sin error `8` (`INTERNAL_ERROR`)**: Fallback automático de autorización OAuth con selector de cuenta y resolución recuperable cuando Play Services devuelve código `8` tras el consentimiento.
- **Corrección de cierre abrupto al abrir la app**: Refactorización de `AppStrings` a interfaz para evitar `ClassFormatError` en tiempo de ejecución y limpieza de dependencias Firebase sin uso.

### Cambio de idioma global de la aplicación y sinopsis (Español, English, Français, 日本語)
- **Desplegable unificado en Ajustes**: Aplica de forma instantánea el idioma elegido tanto a toda la interfaz de AniVexa como a las sinopsis mostradas en las tarjetas de animes (Inicio, Ver todo, Calendario y Búsqueda) y detalles, conservando la preferencia en las copias `.avb`.
