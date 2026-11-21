# AniVexa v0.1.7-beta.1

## Novedades de esta versión

### Copias de respaldo cifradas (.avb) y restauración
- **Cifrado real AES-256-GCM + PBKDF2**: Cada copia de seguridad se cifra con contraseña elegida por el usuario, salt aleatorio y vector IV único en archivos binarios con extensión `.avb` (contiene favoritos, Mi Lista, episodios vistos, calificaciones y preferencias).
- **Almacenamiento Local (`Anivexa-Backuo`)**: Solicita permiso de carpeta en el dispositivo mediante Storage Access Framework (SAF), crea la carpeta `Anivexa-Backuo` y almacena archivos `.avb` con marca de tiempo para su respaldo o restauración.
- **Google Drive personal (`AniVexa`)**: Solicita autorización directamente en el dispositivo del usuario (`drive.file`), crea la carpeta `AniVexa` en su propio Google Drive y permite subir, listar y restaurar archivos `.avb`.
- **Tarjeta en SplashScreen**: Se añadió un aviso destacado en la pantalla inicial presentando las copias cifradas `.avb` con botón directo de **Restaurar** antes de entrar a la app.
