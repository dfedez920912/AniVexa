package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLocalization
import com.example.i18n.LocalAppStrings
import com.example.ui.MainNavScreen
import com.example.ui.theme.LocalIsLiquidGlass

private data class NavTabItem(
    val screen: MainNavScreen,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val navTabItems = listOf(
    NavTabItem(MainNavScreen.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    NavTabItem(MainNavScreen.CALENDAR, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavTabItem(MainNavScreen.MY_LIST, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    NavTabItem(MainNavScreen.SEARCH, Icons.Filled.Search, Icons.Outlined.Search),
    NavTabItem(MainNavScreen.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AniVexaBottomBar(
    currentScreen: MainNavScreen,
    watchlistCount: Int,
    onSelectScreen: (MainNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalIsLiquidGlass.current) {
        AppleLiquidGlassDock(
            currentScreen = currentScreen,
            watchlistCount = watchlistCount,
            onSelectScreen = onSelectScreen,
            modifier = modifier
        )
    } else {
        StandardBottomNavigationBar(
            currentScreen = currentScreen,
            watchlistCount = watchlistCount,
            onSelectScreen = onSelectScreen,
            modifier = modifier
        )
    }
}

@Composable
private fun AppleLiquidGlassDock(
    currentScreen: MainNavScreen,
    watchlistCount: Int,
    onSelectScreen: (MainNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(34.dp)
    val selectedIndex = navTabItems.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)
    val s = LocalAppStrings.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = dockShape,
                    ambientColor = Color(0xFF38BDF8).copy(alpha = 0.24f),
                    spotColor = Color(0xFF0EA5E9).copy(alpha = 0.42f)
                )
                .clip(dockShape)
        ) {
            // Layer 1: Softly diffused frosted glass base (Android 12+ blur on backdrop tint layer only)
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(18.dp)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x9E0E1A30),
                                Color(0xAA162A4A),
                                Color(0x9E14203C)
                            )
                        )
                    )
            )

            // Layer 2: Optical vertical sheen + specular refraction rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color(0x75122038),
                                Color(0x960A1324),
                                Color(0x2E7DD3FC)
                            )
                        ),
                        shape = dockShape
                    )
                    .border(
                        width = 1.15.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.78f),
                                Color.White.copy(alpha = 0.16f),
                                Color(0xFF7DD3FC).copy(alpha = 0.54f),
                                Color(0xFFA78BFA).copy(alpha = 0.34f)
                            )
                        ),
                        shape = dockShape
                    )
            )

            // Top specular hairline glint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 1.5.dp)
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Layer 3: Sliding Liquid Lens Pill + Dock Icons
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                val tabWidth = maxWidth / navTabItems.size
                val lensOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.74f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "liquid_lens_offset"
                )

                val lensShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .offset(x = lensOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(lensShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.36f),
                                    Color(0xFF56CCF2).copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.14f)
                                )
                            ),
                            shape = lensShape
                        )
                        .border(
                            width = 0.95.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.88f),
                                    Color.White.copy(alpha = 0.20f),
                                    Color(0xFF7DD3FC).copy(alpha = 0.62f)
                                )
                            ),
                            shape = lensShape
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navTabItems.forEach { item ->
                        val isSelected = item.screen == currentScreen
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.09f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dock_icon_scale_${item.screen.name}"
                        )
                        val contentTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                Color.White
                            } else {
                                Color(0xFFD6E2F5).copy(alpha = 0.78f)
                            },
                            animationSpec = tween(durationMillis = 180),
                            label = "dock_tint_${item.screen.name}"
                        )
                        val interactionSource = remember { MutableInteractionSource() }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(
                                        bounded = true,
                                        color = Color.White.copy(alpha = 0.35f)
                                    ),
                                    onClick = { onSelectScreen(item.screen) }
                                )
                                .testTag("nav_${item.screen.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                            ) {
                                DockItemIcon(
                                    item = item,
                                    label = AppLocalization.navTitle(item.screen, s),
                                    isSelected = isSelected,
                                    watchlistCount = watchlistCount,
                                    tint = contentTint,
                                    badgeContainerColor = Color(0xFF38BDF8),
                                    badgeContentColor = Color(0xFF041322)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppLocalization.navTitle(item.screen, s),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentTint,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardBottomNavigationBar(
    currentScreen: MainNavScreen,
    watchlistCount: Int,
    onSelectScreen: (MainNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 0.7.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars
        ) {
            navTabItems.forEach { item ->
                val isSelected = currentScreen == item.screen
                val label = AppLocalization.navTitle(item.screen, s)
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectScreen(item.screen) },
                    icon = {
                        DockItemIcon(
                            item = item,
                            label = label,
                            isSelected = isSelected,
                            watchlistCount = watchlistCount,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            badgeContainerColor = MaterialTheme.colorScheme.primary,
                            badgeContentColor = Color.White
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_${item.screen.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun DockItemIcon(
    item: NavTabItem,
    label: String,
    isSelected: Boolean,
    watchlistCount: Int,
    tint: Color,
    badgeContainerColor: Color,
    badgeContentColor: Color
) {
    val iconVector = if (isSelected) item.filledIcon else item.outlinedIcon
    if (item.screen == MainNavScreen.MY_LIST && watchlistCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = badgeContainerColor,
                    contentColor = badgeContentColor
                ) {
                    Text(
                        text = if (watchlistCount > 99) "99+" else "$watchlistCount",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
