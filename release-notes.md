# AniVexa v0.1.4

## Novedades y correcciones de esta versión

### Temporada actual aislada de "Ver todo"
- **Inicio siempre en la temporada real**: Al entrar en **Esta Temporada → Ver todo** y filtrar por otros años o estaciones (como `2025`, `2026` o `2027`), la pantalla principal conserva intactos la temporada y el año reales actuales del dispositivo al regresar.
- **Caché independiente**: Explorar catálogos históricos o futuros desde **Ver todo** ya no sobrescribe la caché de **Esta Temporada** de la pantalla de inicio.

### Widgets y "Hoy en Emisión" siempre al día real
- **Widgets independientes del Calendario**: Consultar otros días, semanas o meses en la pantalla de **Calendario** ya no afecta a los widgets de escritorio ni reemplaza sus estrenos con fechas de otras semanas.
- **Instantánea dedicada para hoy**: Los widgets y la sección **Hoy en Emisión** utilizan un almacenamiento aislado exclusivo para la fecha real actual del dispositivo (respetando la zona horaria elegida en Ajustes).

### Estabilidad y pruebas
- **Verificación automatizada**: Se incorporaron pruebas unitarias para validar que tanto el snapshot de los widgets como la temporada actual de Inicio se mantienen aislados al navegar por el calendario o el catálogo.
