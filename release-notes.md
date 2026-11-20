# AniVexa v0.1.6-beta.1 (Beta)

## Novedades de esta versión beta

### Seguridad y endurecimiento de la aplicación
- **Protección contra StrandHogg 2.0 (`singleInstance` + `taskAffinity=""`)**: Se protegió `MainActivity` frente a secuestro de pila de tareas sin alterar el flujo de notificaciones, widgets ni compartir.
- **Tráfico HTTPS obligatorio y Certificate Transparency**: Se añadió `network_security_config.xml` bloqueando tráfico HTTP sin cifrar y habilitando Certificate Transparency en Android 16+.
- **Registros de depuración controlados**: Se centralizaron los logs en `AppLogger` activos únicamente en compilaciones `DEBUG` (silenciados en `release`).
- **Migraciones SQL endurecidas**: Se eliminó la interpolación dinámica en las migraciones de Room manteniendo intacta la conservación de datos entre versiones.
- **Copias externas desactivadas (`allowBackup="false"`)**: Se bloqueó la extracción de datos locales mediante `adb backup` y respaldo externo sin afectar las actualizaciones locales de APK.
- **Detección de Root e Integridad (Play Integrity API)**: Se integró `DeviceSecurityManager` con detección de indicadores de root y verificación con Google Play Integrity API en modo informativo no bloqueante, visible en **Ajustes → Seguridad e Integridad**.
- **Configuración de escaneo SAST (`.mobsf`)**: Se documentaron los falsos positivos y las reglas descartadas por diseño para mantener limpio el análisis de seguridad en GitHub.
