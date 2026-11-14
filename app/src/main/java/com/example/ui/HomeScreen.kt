package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimeRelease
import com.example.model.MediaSeason
import com.example.ui.components.AnimePosterCard
import java.time.LocalDate

@Composable
fun HomeScreen(
    uiState: AnimeUiState,
    onAnimeClick: (AnimeRelease) -> Unit,
    onSeeAllSeason: () -> Unit,
    onSeeAllPopular: () -> Unit,
    onGoToCalendar: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now(uiState.activeZoneId)
    val todayReleases = uiState.scheduleReleases.filter {
        it.getAiringLocalDate(uiState.activeZoneId) == today
    }.sortedBy { it.effectiveAiringSeconds() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Clean Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "AniVexa",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${uiState.selectedHomeSeason.labelEs} ${uiState.selectedHomeYear}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (uiState.isOfflineMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Sin conexión",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Offline",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("home_refresh_button")
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: This Season (Esta Temporada)
        item {
            SectionHeader(
                title = "Esta Temporada",
                actionLabel = "Ver todo",
                onActionClick = onSeeAllSeason
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isLoadingHome && uiState.seasonalAnime.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_this_season_row")
                ) {
                    items(uiState.seasonalAnime.take(15), key = { "season_${it.id}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section 2: Popular Ranking (Ranking Popular)
        item {
            SectionHeader(
                title = "Ranking Popular",
                actionLabel = "Ver todo",
                onActionClick = onSeeAllPopular
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isLoadingHome && uiState.popularAnime.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_popular_row")
                ) {
                    items(uiState.popularAnime.take(15), key = { "pop_${it.id}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section 3: Airing Today quick preview
        if (todayReleases.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Hoy en Emisión",
                    actionLabel = "Calendario",
                    onActionClick = onGoToCalendar
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_today_row")
                ) {
                    items(todayReleases, key = { "today_${it.id}_${it.episode}" }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            showAiringTime = true,
                            showEpisodeBadge = true,
                            zoneId = uiState.activeZoneId,
                            modifier = Modifier.width(142.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .clickable(onClick = onActionClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = actionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllCatalogScreen(
    mode: SeeAllMode,
    uiState: AnimeUiState,
    onBack: () -> Unit,
    onSelectSeason: (MediaSeason, Int) -> Unit,
    onAnimeClick: (AnimeRelease) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val items = if (mode == SeeAllMode.THIS_SEASON) uiState.seasonalAnime else uiState.popularAnime
    val currentYear = LocalDate.now().year
    val years = listOf(currentYear - 1, currentYear, currentYear + 1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = mode.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (mode == SeeAllMode.THIS_SEASON) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MediaSeason.entries) { season ->
                    val selected = uiState.selectedHomeSeason == season
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSeason(season, uiState.selectedHomeYear) },
                        label = { Text(season.labelEs, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(years) { yr ->
                    val selected = uiState.selectedHomeYear == yr
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSeason(uiState.selectedHomeSeason, yr) },
                        label = { Text("$yr", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (uiState.isLoadingHome && items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { "seeall_${it.id}" }) { anime ->
                    AnimePosterCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
