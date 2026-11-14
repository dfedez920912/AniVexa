package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainNavScreen

@Composable
fun AniVexaBottomBar(
    currentScreen: MainNavScreen,
    watchlistCount: Int,
    onSelectScreen: (MainNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 0.7.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars
        ) {
            val items = listOf(
                Triple(MainNavScreen.HOME, Icons.Filled.Home, Icons.Outlined.Home),
                Triple(MainNavScreen.CALENDAR, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
                Triple(MainNavScreen.MY_LIST, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
                Triple(MainNavScreen.SEARCH, Icons.Filled.Search, Icons.Outlined.Search),
                Triple(MainNavScreen.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
            )

            items.forEach { (screen, filledIcon, outlinedIcon) ->
                val isSelected = currentScreen == screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectScreen(screen) },
                    icon = {
                        if (screen == MainNavScreen.MY_LIST && watchlistCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (watchlistCount > 99) "99+" else "$watchlistCount",
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = screen.title,
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
                    modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                )
            }
        }
    }
}
