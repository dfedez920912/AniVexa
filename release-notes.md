# AniVexa v0.1.7-beta.2

## Novedades de esta versión

### Cambio de idioma global de la aplicación (Español, English, Français, 日本語)
- **Desplegable unificado en Ajustes**: Se ha sustituido el selector de sinopsis por un menú desplegable (`Español`, `English`, `Français`, `日本語`) que aplica el idioma seleccionado de forma instantánea a toda la aplicación.
- **Cobertura completa de la interfaz**: Pantalla inicial, barra de navegación, inicio y catálogo, calendario (semana, mes, lista, días y conteos regresivos), Mi Lista, filtros de búsqueda, ficha detallada y sinopsis, diálogos de copia/restauración `.avb`, notificaciones locales y widgets.
- **Persistencia en preferencias y copias de respaldo**: El idioma elegido se guarda localmente y se incluye dentro de las copias de seguridad cifradas `.avb`.

### Seguridad e historial limpio
- Corrección de prueba unitaria para mantener 0 hallazgos en análisis estático (MobSF) y verificación limpia en todo el código fuente.
- Mantiene intactas las copias de respaldo cifradas AES-256-GCM `.avb` locales (`Anivexa-Backuo`) y en Google Drive personal (`AniVexa`).
