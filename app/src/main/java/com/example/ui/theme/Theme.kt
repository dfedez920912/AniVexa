package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.i18n.AppLocalization
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalAppStrings
import com.example.model.AppThemeMode
import com.example.model.SynopsisLanguage

private val AniVexaDarkScheme = darkColorScheme(
    primary = AniAzureBright,
    onPrimary = Color.White,
    primaryContainer = AniAzureSoft,
    onPrimaryContainer = Color(0xFF90CAF9),
    secondary = AniSecondary,
    onSecondary = Color(0xFF051829),
    secondaryContainer = AniNavyAction,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = AniAiringGreenBright,
    onTertiary = Color.White,
    background = AniDarkBackground,
    onBackground = AniTextPrimaryDark,
    surface = AniDarkSurface,
    onSurface = AniTextPrimaryDark,
    surfaceVariant = AniDarkSurfaceVariant,
    onSurfaceVariant = AniTextSecondaryDark,
    outline = AniDarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AniAzure,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = AniAzureDark,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = AniAiringGreen,
    onTertiary = Color.White,
    background = AniLightBackground,
    onBackground = AniTextPrimaryLight,
    surface = AniLightSurface,
    onSurface = AniTextPrimaryLight,
    surfaceVariant = AniLightSurfaceVariant,
    onSurfaceVariant = AniTextSecondaryLight,
    outline = AniLightCardBorder
)

private val OledColorScheme = darkColorScheme(
    primary = AniAzureBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF102033),
    onPrimaryContainer = Color(0xFF90CAF9),
    secondary = AniSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF141821),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = AniAiringGreenBright,
    onTertiary = Color.Black,
    background = AniOledBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = AniOledSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AniOledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AniOledBorder
)

private val LiquidGlassColorScheme = darkColorScheme(
    primary = AniGlassPrimary,
    onPrimary = Color(0xFF041322),
    primaryContainer = Color(0x4756CCF2),
    onPrimaryContainer = Color(0xFFE0F7FF),
    secondary = AniGlassSecondary,
    onSecondary = Color(0xFF0F0724),
    secondaryContainer = Color(0x45A78BFA),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF042218),
    background = AniGlassBackground,
    onBackground = AniGlassTextPrimary,
    surface = AniGlassSurface,
    onSurface = AniGlassTextPrimary,
    surfaceVariant = AniGlassSurfaceVariant,
    onSurfaceVariant = AniGlassTextSecondary,
    surfaceContainer = AniGlassSheetSurface,
    surfaceContainerHigh = AniGlassSheetSurface,
    outline = AniGlassBorderMid
)

@Composable
fun AniVexaTheme(
    themeMode: AppThemeMode = AppThemeMode.ANIVEXA_DARK,
    appLanguage: SynopsisLanguage = SynopsisLanguage.SPANISH,
    systemDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isLiquidGlass = themeMode == AppThemeMode.LIQUID_GLASS
    val colorScheme = when (themeMode) {
        AppThemeMode.ANIVEXA_DARK -> AniVexaDarkScheme
        AppThemeMode.OLED -> OledColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.LIQUID_GLASS -> LiquidGlassColorScheme
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (systemDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (systemDarkTheme) {
                AniVexaDarkScheme
            } else {
                LightColorScheme
            }
        }
    }

    val strings = AppLocalization.stringsFor(appLanguage)
    CompositionLocalProvider(
        LocalIsLiquidGlass provides isLiquidGlass,
        LocalAppThemeMode provides themeMode,
        LocalAppStrings provides strings,
        LocalAppLanguage provides appLanguage
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
