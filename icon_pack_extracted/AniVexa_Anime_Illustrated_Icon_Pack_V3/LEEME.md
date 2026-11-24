# AniVexa V3 — pack ilustrado de iconos (borrador de integración)

Este paquete se ha rehecho para incorporar ilustraciones de estilo anime con objetos reconocibles de series. **No contiene 41 vectores nuevos dibujados por un artista**. Incluye 41 PNG transparentes en 512 y 256 px y sus recursos para Android. Se incluyen los XML antiguos en una carpeta separada como alternativa, sin fingir que son los nuevos diseños.

## Contenido
- `png_512_transparent/`: 41 imágenes RGBA 512x512
- `png_256_transparent/`: 41 imágenes RGBA 256x256
- `android/app/src/main/res/drawable-nodpi/`: 41 PNG para Compose, mismo nombre que los recursos originales propuestos
- `integracion_kotlin/`: overlay de código para las 65 referencias ya auditadas (sin compilar)
- `vector_legacy_v2_alternative/`: XML de la versión anterior; no instalarlos si quieres ver los nuevos PNG
- `preview/`: catálogo y cinco iconos de navegación a escala de interfaz
- `docs/`: mapeo de iconos y procedencia visual

## Procedencia y límites
- Ilustraciones generadas expresamente en una lámina de 9 símbolos: 15 variantes/estados.
- Ilustraciones extraídas de láminas promocionales generadas en esta sesión: 19 iconos.
- Recursos reaprovechados de V2 porque no se consiguió aún una nueva imagen convincente: 7 iconos.

**Aviso de calidad:** las imágenes extraídas de póster tienen resolución real inferior a 512 px. Se amplían a 512 pero ello no agrega detalle. Es preciso revisar cada icono a 24 dp en un dispositivo; algunos extraídos pueden conservar defectos de recorte. No declarar listo para producción sin pasar la revisión visual y una compilación Android.

## Instalación
Desde la raíz del repositorio AniVexa, ejecutar con una copia de seguridad o rama dedicada:
```
/path/al/paquete/instalar_en_repositorio.sh
```
El script copia 41 PNG a `drawable-nodpi` y el overlay Kotlin incluido. El overlay fue generado para una auditoría de código previa y **no ha sido compilado ni probado** en Android aquí. Comprueba que no existan recursos con el mismo nombre en `drawable` que impidan que Android seleccione el PNG correcto.

## Derechos
Las inspiraciones corresponden a universos de franquicias comerciales. Las ilustraciones son fan art generativo y deben revisarse los derechos antes de publicar o comercializar.
