package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AnimeDetailScreen
import com.example.ui.AnimeViewModel
import com.example.ui.CalendarScreen
import com.example.ui.HomeScreen
import com.example.ui.MainNavScreen
import com.example.ui.MyListScreen
import com.example.ui.SearchScreen
import com.example.ui.SeeAllCatalogScreen
import com.example.ui.SeeAllMode
import com.example.ui.SettingsScreen
import com.example.ui.SplashScreen
import com.example.ui.components.AniVexaBottomBar
import com.example.ui.theme.AniVexaTheme
import com.example.ui.theme.LiquidGlassBackdrop
import com.example.ui.theme.LocalBottomDockPadding
import com.example.ui.theme.LocalIsLiquidGlass

class MainActivity : ComponentActivity() {

    private val viewModel: AnimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.notificationMessage) {
                uiState.notificationMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearNotificationMessage()
                }
            }

            AniVexaTheme(themeMode = uiState.appThemeMode) {
                val isGlass = LocalIsLiquidGlass.current
                val dockPadding = if (isGlass && uiState.seeAllMode == SeeAllMode.NONE) 104.dp else 0.dp

                CompositionLocalProvider(LocalBottomDockPadding provides dockPadding) {
                    GlassHostContainer(isGlass = isGlass) {
                        when {
                            showSplash -> {
                                SplashScreen(onSplashFinished = { showSplash = false })
                            }

                            uiState.selectedAnimeForDetail != null -> {
                                val detailAnime = uiState.selectedAnimeForDetail!!
                                AnimeDetailScreen(
                                    anime = detailAnime,
                                    reviews = uiState.detailReviews,
                                    characters = uiState.detailCharacters,
                                    relatedAnime = uiState.detailRelations,
                                    isLoadingDetail = uiState.isLoadingDetail,
                                    watchedEpisodeKeys = uiState.watchedEpisodeKeys,
                                    defaultReminderMinutes = uiState.defaultReminderMinutes,
                                    zoneId = uiState.activeZoneId,
                                    onBack = { viewModel.closeDetail() },
                                    onRelatedAnimeClick = { viewModel.openRelatedDetail(it) },
                                    onToggleWatchlist = { viewModel.toggleWatchlist(detailAnime) },
                                    onToggleReminder = { enabled, mins ->
                                        viewModel.updateReminder(detailAnime, enabled, mins)
                                    },
                                    onTestNotification = { viewModel.testEpisodeNotification(detailAnime) },
                                    onToggleWatchedEpisodeNum = { ep ->
                                        viewModel.toggleWatchedEpisodeNum(detailAnime.id, ep)
                                    },
                                    onLanguageSelected = { lang ->
                                        viewModel.setAnimeSynopsisLanguage(detailAnime.id, lang)
                                    },
                                    onRatingChanged = { rating ->
                                        viewModel.setUserRating(detailAnime.id, rating)
                                    }
                                )
                            }

                            else -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        containerColor = if (isGlass) {
                                            Color.Transparent
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        },
                                        contentWindowInsets = WindowInsets.statusBars,
                                        snackbarHost = {
                                            SnackbarHost(
                                                hostState = snackbarHostState,
                                                modifier = if (isGlass) {
                                                    Modifier
                                                        .navigationBarsPadding()
                                                        .padding(bottom = 86.dp)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                        },
                                        bottomBar = {
                                            if (!isGlass && uiState.seeAllMode == SeeAllMode.NONE) {
                                                AniVexaBottomBar(
                                                    currentScreen = uiState.currentNavScreen,
                                                    watchlistCount = uiState.watchlistIds.size,
                                                    onSelectScreen = { viewModel.setNavScreen(it) }
                                                )
                                            }
                                        }
                                    ) { innerPadding ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(innerPadding)
                                        ) {
                                            if (uiState.seeAllMode != SeeAllMode.NONE) {
                                                SeeAllCatalogScreen(
                                                    mode = uiState.seeAllMode,
                                                    uiState = uiState,
                                                    onBack = { viewModel.closeSeeAll() },
                                                    onSelectSeason = { season, year ->
                                                        viewModel.selectSeeAllSeason(season, year)
                                                    },
                                                    onAnimeClick = { viewModel.openDetail(it) }
                                                )
                                            } else {
                                                when (uiState.currentNavScreen) {
                                                    MainNavScreen.HOME -> HomeScreen(
                                                        uiState = uiState,
                                                        onAnimeClick = { viewModel.openDetail(it) },
                                                        onSeeAllSeason = { viewModel.openSeeAll(SeeAllMode.THIS_SEASON) },
                                                        onSeeAllPopular = { viewModel.openSeeAll(SeeAllMode.POPULAR_RANKING) },
                                                        onGoToCalendar = { viewModel.setNavScreen(MainNavScreen.CALENDAR) },
                                                        onRefresh = { viewModel.refreshAll() }
                                                    )

                                                    MainNavScreen.CALENDAR -> CalendarScreen(
                                                        uiState = uiState,
                                                        onSelectMode = { viewModel.setCalendarViewMode(it) },
                                                        onSelectDate = { viewModel.selectCalendarDate(it) },
                                                        onShiftWeek = { viewModel.shiftWeek(it) },
                                                        onShiftMonth = { viewModel.shiftMonth(it) },
                                                        onJumpToToday = { viewModel.jumpToToday() },
                                                        onFormatFilter = { viewModel.setCalendarFormatFilter(it) },
                                                        onGenreFilter = { viewModel.setCalendarGenreFilter(it) },
                                                        onStudioFilter = { viewModel.setCalendarStudioFilter(it) },
                                                        onAnimeClick = { viewModel.openDetail(it) },
                                                        onToggleWatched = { viewModel.toggleWatchedEpisode(it) },
                                                        onToggleReminder = { viewModel.toggleFavoriteReminder(it) },
                                                        onGoToMyList = { viewModel.setNavScreen(MainNavScreen.MY_LIST) }
                                                    )

                                                    MainNavScreen.MY_LIST -> MyListScreen(
                                                        uiState = uiState,
                                                        onSelectFilterTab = { viewModel.setMyListFilterTab(it) },
                                                        onSelectSortOrder = { viewModel.setMyListSortOrder(it) },
                                                        onItemClick = { viewModel.openDetail(it) },
                                                        onToggleWatchedItem = { viewModel.toggleWatchlistItemWatched(it) },
                                                        onUpdateStatus = { id, status -> viewModel.updateWatchlistStatus(id, status) },
                                                        onIncrementEpisode = { item ->
                                                            viewModel.updateWatchlistEpisodes(item.animeId, item.watchedEpisodes + 1)
                                                        },
                                                        onRemoveItem = { viewModel.removeFromWatchlist(it) },
                                                        onExploreCatalog = { viewModel.setNavScreen(MainNavScreen.HOME) }
                                                    )

                                                    MainNavScreen.SEARCH -> SearchScreen(
                                                        uiState = uiState,
                                                        onQueryChange = { viewModel.setSearchQuery(it) },
                                                        onToggleFilters = { viewModel.toggleShowSearchFilters() },
                                                        onToggleGenre = { viewModel.toggleSearchGenre(it) },
                                                        onSelectSeason = { viewModel.setSearchSeason(it) },
                                                        onSelectYear = { viewModel.setSearchYear(it) },
                                                        onSelectStatus = { viewModel.setSearchStatus(it) },
                                                        onSelectFormat = { viewModel.setSearchFormat(it) },
                                                        onSelectStudio = { viewModel.setSearchStudio(it) },
                                                        onClearFilters = { viewModel.clearSearchFilters() },
                                                        onAnimeClick = { viewModel.openDetail(it) }
                                                    )

                                                    MainNavScreen.SETTINGS -> SettingsScreen(
                                                        uiState = uiState,
                                                        onToggleTimeZone = { viewModel.toggleJapanTimeZone() },
                                                        onSetNotificationsEnabled = { viewModel.setNotificationsEnabled(it) },
                                                        onSetDefaultReminderMinutes = { viewModel.setDefaultReminderMinutes(it) },
                                                        onToggleQuietHours = { viewModel.toggleQuietHours() },
                                                        onSetGlobalLanguage = { viewModel.setGlobalLanguage(it) },
                                                        onSetThemeMode = { viewModel.setThemeMode(it) },
                                                        onSyncNow = { viewModel.refreshAll() },
                                                        onClearCache = { viewModel.clearOfflineCache() }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Overlaid floating Apple Liquid Glass Dock so scrolling content flows behind it
                                    if (isGlass && uiState.seeAllMode == SeeAllMode.NONE) {
                                        AniVexaBottomBar(
                                            currentScreen = uiState.currentNavScreen,
                                            watchlistCount = uiState.watchlistIds.size,
                                            onSelectScreen = { viewModel.setNavScreen(it) },
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassHostContainer(
    isGlass: Boolean,
    content: @Composable () -> Unit
) {
    if (isGlass) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
