package com.example.model

enum class AppThemeMode(val label: String, val description: String) {
    ANIVEXA_DARK("AniVexa Dark", "Obsidiana profunda con azul eléctrico"),
    OLED("OLED Midnight", "Negro puro #000000 para pantallas AMOLED"),
    LIGHT("Claro Limpio", "Interfaz luminosa para lectura diurna"),
    SYSTEM("Sistema", "Sigue el modo oscuro o claro del dispositivo")
}

enum class StreamingPlatform(val displayName: String, val iconColorHex: Long) {
    ALL("Todas", 0xFF2196F3),
    CRUNCHYROLL("Crunchyroll", 0xFFF47521),
    NETFLIX("Netflix", 0xFFE50914),
    PRIME_VIDEO("Prime Video", 0xFF00A8E1),
    DISNEY_PLUS("Disney+", 0xFF113CCF),
    HIDIVE("HIDIVE", 0xFF00B4D8)
}
