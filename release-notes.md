# AniVexa v0.1.6

## Novedades de esta versión

### Seguridad y estabilidad
- **Protección contra StrandHogg 2.0**: Se configuró `MainActivity` con `launchMode="singleInstance"` y `taskAffinity=""` para evitar secuestros de pila de tareas sin alterar el acceso desde notificaciones, widgets ni compartir.
- **Tráfico HTTPS obligatorio y Certificate Transparency**: Se incorporó `network_security_config.xml` bloqueando tráfico HTTP sin cifrar y habilitando Certificate Transparency en Android 16+.
- **Registros de depuración controlados**: Todos los logs quedaron centralizados en `AppLogger` y desactivados automáticamente en compilaciones `release`.
- **Migraciones SQL endurecidas**: Se eliminó la interpolación dinámica en las migraciones de Room manteniendo intactos los datos y el progreso del usuario entre versiones.
- **Copias externas desactivadas (`allowBackup="false"`)**: Se restringió la extracción externa de datos locales mediante `adb backup` y respaldo externo sin afectar las actualizaciones locales.
- **Detección de Root y Google Play Integrity API**: Se añadió `DeviceSecurityManager` con diagnóstico de integridad no bloqueante visible en **Ajustes → Seguridad e Integridad**.
- **Análisis de seguridad MobSF SAST y CI**: Se integraron las exclusiones documentadas en `.mobsf` y soporte completo en los workflows de pruebas unitarias, compilación y seguridad.
