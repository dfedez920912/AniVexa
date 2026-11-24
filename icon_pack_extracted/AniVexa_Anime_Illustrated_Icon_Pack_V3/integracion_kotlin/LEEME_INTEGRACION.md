# Integración 1 a 1 en AniVexa 0.1.6

**Esta carpeta incluye un overlay preparado a partir del código original recibido.**

## Contenido
- `overlay/app/src/main/java/`: 8 archivos Kotlin modificados con las 65 referencias Material dirigidas a los nuevos assets.
- `instalar_en_repositorio.sh`: script de copia de los 41 XML y overlay Kotlin.
- El resto del paquete incluye los recursos Android y el manifiesto 1 a 1.

## Cómo integrarlo

1. Crea una rama de Git en tu repositorio AniVexa 0.1.6.
2. Descomprime el ZIP del paquete fuera del repositorio.
3. En una terminal situada en la raíz del repositorio, ejecuta **la ruta absoluta** a `integracion/instalar_en_repositorio.sh`.
4. Inspecciona `git diff`, especialmente `AniVexaBottomBar.kt` y `SettingsScreen.kt`.
5. Compila, ejecuta en un emulador Android y comprueba los temas Light, Dark, OLED y Liquid Glass.

## Cambios realizados

- Sustituye referencias `Icons.Default/Outlined/Filled/AutoMirrored.Filled` por `R.drawable.ic_anime_*`.
- Cambia `Icon(imageVector=...)` a `Icon(painter = painterResource(id=...), tint = Color.Unspecified)` conservando `contentDescription`, tamaños y callbacks.
- Cambia `NavTabItem` de ImageVector a Int y `SettingsCard.icon` de ImageVector a Int.
- Conserva la lógica de navegación, recordatorios y filtros. Los iconos son multicolor.
- No se modifican Gradle, Manifest, ni lógica de red.
- **No hay prueba de compilación real** en este entorno. La integración se entrega como candidata y requiere build/verificación en el proyecto.
