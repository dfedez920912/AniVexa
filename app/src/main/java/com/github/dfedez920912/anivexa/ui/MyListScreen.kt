package com.github.dfedez920912.anivexa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import cu.daniel.anical.R
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.dfedez920912.anivexa.data.local.WatchlistEntity
import com.github.dfedez920912.anivexa.data.local.WatchlistStatus
import com.github.dfedez920912.anivexa.i18n.AppLocalization
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.model.WatchlistSortOrder
import com.github.dfedez920912.anivexa.ui.theme.AniAiringGreenBright
import com.github.dfedez920912.anivexa.ui.theme.AniAzureSoft
import com.github.dfedez920912.anivexa.ui.theme.AniGlassSheetSurface
import com.github.dfedez920912.anivexa.ui.theme.AniGold
import com.github.dfedez920912.anivexa.ui.theme.LocalBottomDockPadding
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import com.github.dfedez920912.anivexa.ui.theme.screenBackdrop

@Composable
fun MyListScreen(
    uiState: AnimeUiState,
    onSelectFilterTab: (MyListFilterTab) -> Unit,
    onSelectSortOrder: (WatchlistSortOrder) -> Unit,
    onItemClick: (AnimeRelease) -> Unit,
    onToggleWatchedItem: (WatchlistEntity) -> Unit,
    onUpdateStatus: (Int, WatchlistStatus) -> Unit,
    onIncrementEpisode: (WatchlistEntity) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onExploreCatalog: () -> Unit,
    onToggleReminder: (AnimeRelease) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    val bottomDockPadding = LocalBottomDockPadding.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackdrop()
    ) {
        // Header: My List + Sort button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.myListTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Box {
                IconButton(
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier.testTag("mylist_sort_btn")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_anime_sort_automirrored_filled),
                        contentDescription = s.myListSortDesc,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                    containerColor = if (LocalIsLiquidGlass.current) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
                ) {
                    WatchlistSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = AppLocalization.watchlistSortLabel(order, s),
                                    fontWeight = if (uiState.myListSortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.myListSortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                if (uiState.myListSortOrder == order) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_anime_check_filled),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOrder(order)
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Pill Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MyListFilterTab.entries) { tab ->
                val isSelected = uiState.myListFilterTab == tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectFilterTab(tab) }
                        .testTag("mylist_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = AppLocalization.myListTabLabel(tab, s),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.filteredWatchlistItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(adaptiveCardCorner(18.dp, 24.dp)),
                    color = adaptiveCardColor(),
                    border = glassCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_anime_bookmark_border_outlined),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (uiState.watchlistItems.isEmpty()) s.myListEmptyTitle else s.myListEmptyCategoryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (uiState.watchlistItems.isEmpty()) {
                                s.myListEmptyBody
                            } else {
                                s.myListEmptyCategoryBody
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.watchlistItems.isEmpty()) {
                            Button(
                                onClick = onExploreCatalog,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(s.myListExploreBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 12.dp + bottomDockPadding
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mylist_items_column")
            ) {
                items(uiState.filteredWatchlistItems, key = { it.animeId }) { item ->
                    val watchedCount = maxOf(
                        uiState.watchedEpisodeKeys.count { it.startsWith("${item.animeId}_") },
                        item.watchedEpisodes
                    )
                    val isFullyWatched = item.statusEnum == WatchlistStatus.COMPLETED ||
                        (item.totalEpisodes != null && item.totalEpisodes > 0 && watchedCount >= item.totalEpisodes)
                    val isInProgress = !isFullyWatched && (item.statusEnum == WatchlistStatus.WATCHING || watchedCount > 0)
                    val hasReminder = uiState.favoriteIds.contains(item.animeId)
                    MyListCard(
                        item = item,
                        watchedCount = watchedCount,
                        isFullyWatched = isFullyWatched,
                        isInProgress = isInProgress,
                        hasReminder = hasReminder,
                        onClick = { onItemClick(item.toAnimeRelease()) },
                        onToggleWatched = { onToggleWatchedItem(item) },
                        onToggleReminder = { onToggleReminder(item.toAnimeRelease()) },
                        onStatusChange = { status -> onUpdateStatus(item.animeId, status) },
                        onIncrementEpisode = { onIncrementEpisode(item) },
                        onRemove = { onRemoveItem(item.animeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListCard(
    item: WatchlistEntity,
    watchedCount: Int,
    isFullyWatched: Boolean,
    isInProgress: Boolean,
    hasReminder: Boolean,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleReminder: () -> Unit,
    onStatusChange: (WatchlistStatus) -> Unit,
    onIncrementEpisode: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    var statusMenuOpen by remember { mutableStateOf(false) }

    val cardCorner = adaptiveCardCorner(14.dp, 20.dp)
    Surface(
        shape = RoundedCornerShape(cardCorner),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCorner))
            .clickable(onClick = onClick)
            .testTag("mylist_card_${item.animeId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.coverImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(74.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleReminder,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("mylist_reminder_bell_${item.animeId}")
                        ) {
                            Image(
                                painter = painterResource(id = if (hasReminder) R.drawable.ic_anime_notifications_active_filled else R.drawable.ic_anime_notifications_none_filled),
                                contentDescription = s.calendarReminderDesc,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_anime_delete_outline_filled),
                                contentDescription = s.myListRemoveDesc,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Status badge + Episode progress row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { statusMenuOpen = true }
                        ) {
                            Text(
                                text = when {
                                    item.statusEnum == WatchlistStatus.DROPPED -> s.statusPillPaused
                                    isFullyWatched -> s.statusPillWatched
                                    isInProgress -> s.statusPillWatching
                                    else -> s.statusPillUnwatched
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isFullyWatched -> AniAiringGreenBright
                                    isInProgress -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = statusMenuOpen,
                            onDismissRequest = { statusMenuOpen = false },
                            containerColor = if (LocalIsLiquidGlass.current) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
                        ) {
                            WatchlistStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(AppLocalization.watchlistStatusDisplay(st, s)) },
                                    onClick = {
                                        onStatusChange(st)
                                        statusMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "${s.episodeShortPrefix} $watchedCount / ${item.totalEpisodes ?: "?"}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable(onClick = onIncrementEpisode)
                    ) {
                        Text(
                            text = s.myListPlusOneEp,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Prominent status pill button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isFullyWatched -> AniAiringGreenBright.copy(alpha = 0.14f)
                        isInProgress -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        else -> AniAzureSoft.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isFullyWatched -> AniAiringGreenBright
                            isInProgress -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        }
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onToggleWatched)
                        .testTag("mylist_mark_watched_${item.animeId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = when {
                                    isFullyWatched -> R.drawable.ic_anime_check_circle_filled
                                    isInProgress -> R.drawable.ic_anime_play_circle_outline_filled
                                    else -> R.drawable.ic_anime_radio_button_unchecked_filled
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when {
                                isFullyWatched -> s.statusPillWatched.replaceFirstChar { it.uppercase() }
                                isInProgress -> s.statusPillWatching.replaceFirstChar { it.uppercase() }
                                else -> s.myListMarkAsWatched
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFullyWatched) AniAiringGreenBright else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
