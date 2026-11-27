package com.github.dfedez920912.anivexa.ui.theme

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.dfedez920912.anivexa.model.AppThemeMode

val LocalIsLiquidGlass = staticCompositionLocalOf { false }
val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.ANIVEXA_DARK }
val LocalBottomDockPadding = staticCompositionLocalOf { 0.dp }

/**
 * Ambient luminous prism mesh backdrop reminiscent of Apple's Liquid Glass wallpaper/environment.
 * Provides soft refracted color sources behind translucent glass cards and the floating dock.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_mesh")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_glass_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Deep cosmic midnight foundation
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050A14),
                            Color(0xFF0A1428),
                            Color(0xFF0D1730),
                            Color(0xFF060B16)
                        )
                    )
                )

                val w = size.width
                val h = size.height

                // Top-left luminous cyan-azure aurora orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0EA5E9).copy(alpha = 0.28f),
                            Color(0xFF38BDF8).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.14f + 0.06f * drift), h * (0.12f + 0.04f * drift)),
                        radius = w * 0.85f
                    ),
                    center = Offset(w * (0.14f + 0.06f * drift), h * (0.12f + 0.04f * drift)),
                    radius = w * 0.85f
                )

                // Mid-right iridescent violet-indigo prism orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.22f),
                            Color(0xFF6366F1).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.88f - 0.07f * drift), h * (0.42f + 0.05f * drift)),
                        radius = w * 0.80f
                    ),
                    center = Offset(w * (0.88f - 0.07f * drift), h * (0.42f + 0.05f * drift)),
                    radius = w * 0.80f
                )

                // Subtle rose-magenta specular accent upper right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEC4899).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.16f),
                        radius = w * 0.55f
                    ),
                    center = Offset(w * 0.82f, h * 0.16f),
                    radius = w * 0.55f
                )

                // Bottom-center luminous cyan-sapphire glow behind the floating dock
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.24f),
                            Color(0xFF2563EB).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.50f + 0.05f * (drift - 0.5f)), h * 0.94f),
                        radius = w * 0.78f
                    ),
                    center = Offset(w * (0.50f + 0.05f * (drift - 0.5f)), h * 0.94f),
                    radius = w * 0.78f
                )
            },
        content = content
    )
}

/**
 * Transparent screen background in Liquid Glass mode so the global ambient prism mesh shows through;
 * wallpaper background with automatic dark/light and portrait/landscape orientation support otherwise.
 */
fun Modifier.screenBackdrop(): Modifier = composed {
    val context = LocalContext.current
    val themeMode = LocalAppThemeMode.current
    val systemDark = isSystemInDarkTheme()

    val forceDark = when (themeMode) {
        AppThemeMode.ANIVEXA_DARK, AppThemeMode.OLED, AppThemeMode.LIQUID_GLASS -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val themedContext = remember(context, forceDark) {
        val config = Configuration(context.resources.configuration)
        config.uiMode = if (forceDark) {
            (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        } else {
            (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
        context.createConfigurationContext(config)
    }

    val resId = remember(themedContext) {
        themedContext.resources.getIdentifier("bg_wallpaper", "drawable", themedContext.packageName)
    }

    if (resId != 0) {
        val painter = coil.compose.rememberAsyncImagePainter(
            model = coil.request.ImageRequest.Builder(themedContext)
                .data(resId)
                .crossfade(false)
                .build()
        )
        this.paint(
            painter = painter,
            contentScale = ContentScale.Crop
        )
    } else {
        this.background(MaterialTheme.colorScheme.background)
    }
}

/**
 * Specular diagonal rim border signature of Apple Liquid Glass surfaces.
 */
@Composable
fun glassCardBorder(
    standardWidth: Dp = 0.7.dp,
    glassWidth: Dp = 0.95.dp,
    highContrast: Boolean = false
): BorderStroke {
    return if (LocalIsLiquidGlass.current) {
        BorderStroke(
            width = glassWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (highContrast) 0.70f else 0.48f),
                    Color.White.copy(alpha = if (highContrast) 0.22f else 0.14f),
                    Color(0xFF7DD3FC).copy(alpha = if (highContrast) 0.50f else 0.32f)
                )
            )
        )
    } else {
        BorderStroke(standardWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    }
}

/**
 * Applies translucent frosted Liquid Glass background sheen + specular rim border when enabled.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(18.dp),
    elevated: Boolean = false
): Modifier = composed {
    if (!LocalIsLiquidGlass.current) {
        this
    } else {
        val baseTop = if (elevated) Color(0x36FFFFFF) else Color(0x26FFFFFF)
        val baseMid = if (elevated) Color(0x781D2E4C) else Color(0x5E142138)
        val baseBot = if (elevated) Color(0x247DD3FC) else Color(0x1638BDF8)
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (elevated) 0.65f else 0.48f),
                Color.White.copy(alpha = 0.14f),
                Color(0xFF7DD3FC).copy(alpha = if (elevated) 0.46f else 0.30f)
            )
        )
        this
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(baseTop, baseMid, baseBot)
                ),
                shape = shape
            )
            .border(width = 0.95.dp, brush = borderBrush, shape = shape)
    }
}

@Composable
fun adaptiveCardCorner(standard: Dp = 14.dp, glass: Dp = 18.dp): Dp {
    return if (LocalIsLiquidGlass.current) glass else standard
}

@Composable
fun adaptiveCardColor(elevated: Boolean = false): Color {
    return if (LocalIsLiquidGlass.current) {
        if (elevated) AniGlassSurfaceElevated else AniGlassSurface
    } else {
        if (elevated) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    }
}
