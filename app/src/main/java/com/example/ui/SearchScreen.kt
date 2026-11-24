package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLocalization
import com.example.i18n.LocalAppStrings
import com.example.model.AnimeRelease
import com.example.model.GenreHelper
import com.example.model.MediaFormatFilter
import com.example.model.MediaSeason
import com.example.model.MediaStatusFilter
import com.example.ui.components.AnimePosterCard
import com.example.ui.theme.LocalBottomDockPadding
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.adaptiveCardColor
import com.example.ui.theme.adaptiveCardCorner
import com.example.ui.theme.glassCardBorder
import com.example.ui.theme.screenBackdrop
import java.time.LocalDate

@Composable
fun SearchScreen(
    uiState: AnimeUiState,
    onQueryChange: (String) -> Unit,
    onToggleFilters: () -> Unit,
    onToggleGenre: (String) -> Unit,
    onSelectSeason: (MediaSeason?) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onSelectStatus: (MediaStatusFilter) -> Unit,
    onSelectFormat: (MediaFormatFilter) -> Unit,
    onSelectStudio: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onAnimeClick: (AnimeRelease) -> Unit,
    onToggleReminder: (AnimeRelease) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val s = LocalAppStrings.current
    val isGlass = LocalIsLiquidGlass.current
    val bottomDockPadding = LocalBottomDockPadding.current
    val currentYear = LocalDate.now().year
    val years = listOf(currentYear + 1, currentYear, currentYear - 1, currentYear - 2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackdrop()
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = s.searchPlaceholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = s.searchIconDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = s.searchClearDesc,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(if (isGlass) 22.dp else 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = adaptiveCardColor(elevated = isGlass),
                unfocusedContainerColor = adaptiveCardColor(),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isGlass) Color.White.copy(alpha = 0.36f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("search_input_field")
        )

        // Centered Blue Toggle "Mostrar Filtros v" / "Ocultar Filtros ^"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleFilters)
                .padding(vertical = 4.dp)
                .testTag("toggle_search_filters_btn"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.showSearchFilters) s.searchHideFilters else s.searchShowFilters,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (uiState.showSearchFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Expandable Filter Section (Genre, Season, Year, Status, Format, Studio)
        AnimatedVisibility(visible = uiState.showSearchFilters) {
            Surface(
                color = adaptiveCardColor(),
                border = glassCardBorder(),
                shape = RoundedCornerShape(adaptiveCardCorner(14.dp, 20.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.searchDiscoveryFilters,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.searchClearAll,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onClearFilters)
                        )
                    }

                    FilterLabel(s.searchLabelGenre)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(GenreHelper.FEATURED_GENRES) { genre ->
                            val selected = uiState.searchSelectedGenres.contains(genre)
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleGenre(genre) },
                                label = { Text(AppLocalization.genreLabel(genre, s), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    FilterLabel(s.searchLabelSeasonYear)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = (uiState.searchSeason == null) && (uiState.searchYear == null),
                                onClick = {
                                    onSelectSeason(null)
                                    onSelectYear(null)
                                },
                                label = { Text(s.searchAnyChip, fontSize = 11.sp) }
                            )
                        }
                        items(MediaSeason.entries) { season ->
                            FilterChip(
                                selected = uiState.searchSeason == season,
                                onClick = { onSelectSeason(if (uiState.searchSeason == season) null else season) },
                                label = { Text(AppLocalization.seasonLabel(season, s), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(years) { yr ->
                            FilterChip(
                                selected = uiState.searchYear == yr,
                                onClick = { onSelectYear(if (uiState.searchYear == yr) null else yr) },
                                label = { Text(yr.toString(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    FilterLabel(s.searchLabelStatusFormat)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(MediaStatusFilter.entries) { st ->
                            FilterChip(
                                selected = uiState.searchStatus == st,
                                onClick = { onSelectStatus(st) },
                                label = { Text(AppLocalization.statusFilterLabel(st, s), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(MediaFormatFilter.entries.filter { it != MediaFormatFilter.ALL }) { fmt ->
                            FilterChip(
                                selected = uiState.searchFormat == fmt,
                                onClick = {
                                    onSelectFormat(if (uiState.searchFormat == fmt) MediaFormatFilter.ALL else fmt)
                                },
                                label = { Text(AppLocalization.formatFilterLabel(fmt, s), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    FilterLabel(s.searchLabelStudio)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.searchStudio == null,
                                onClick = { onSelectStudio(null) },
                                label = { Text(s.searchAllChip, fontSize = 11.sp) }
                            )
                        }
                        items(GenreHelper.POPULAR_STUDIOS) { studio ->
                            FilterChip(
                                selected = uiState.searchStudio == studio,
                                onClick = { onSelectStudio(if (uiState.searchStudio == studio) null else studio) },
                                label = { Text(studio, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (uiState.isLoadingSearch && uiState.searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = s.searchNoResultsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = s.searchNoResultsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 14.dp + bottomDockPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("search_results_grid")
            ) {
                items(uiState.searchResults, key = { "search_${it.id}" }) { anime ->
                    AnimePosterCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime) },
                        onToggleReminder = { onToggleReminder(anime) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}
