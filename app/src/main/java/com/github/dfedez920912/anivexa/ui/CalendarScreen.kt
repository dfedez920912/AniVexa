package com.github.dfedez920912.anivexa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.github.dfedez920912.anivexa.i18n.AppLocalization
import com.github.dfedez920912.anivexa.i18n.LocalAppLanguage
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.model.DayScheduleTab
import com.github.dfedez920912.anivexa.model.GenreHelper
import com.github.dfedez920912.anivexa.model.MediaFormatFilter
import com.github.dfedez920912.anivexa.ui.components.AnimePosterCard
import com.github.dfedez920912.anivexa.ui.theme.AniAiringGreenBright
import com.github.dfedez920912.anivexa.ui.theme.AniGlassSheetSurface
import com.github.dfedez920912.anivexa.ui.theme.AniGold
import com.github.dfedez920912.anivexa.ui.theme.LocalBottomDockPadding
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import com.github.dfedez920912.anivexa.ui.theme.screenBackdrop
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

@Composable
fun CalendarScreen(
    uiState: AnimeUiState,
    onSelectMode: (CalendarViewMode) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onFormatFilter: (MediaFormatFilter) -> Unit,
    onGenreFilter: (String?) -> Unit,
    onStudioFilter: (String?) -> Unit,
    onAnimeClick: (AnimeRelease) -> Unit,
    onToggleWatched: (AnimeRelease) -> Unit,
    onToggleReminder: (AnimeRelease) -> Unit,
    onGoToMyList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    var showFilters by remember { mutableStateOf(value = false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackdrop()
    ) {
        // Header: Calendar title + Today + Filter + Bookmark shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.calendarTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onJumpToToday,
                    modifier = Modifier.testTag("calendar_today_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = s.calendarTodayBtnDesc,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier.testTag("calendar_filter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = s.calendarFilterBtnDesc,
                        tint = if (showFilters || uiState.calendarFormatFilter != MediaFormatFilter.ALL ||
                            uiState.calendarGenreFilter != null || uiState.calendarStudioFilter != null
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(
                    onClick = onGoToMyList,
                    modifier = Modifier.testTag("calendar_mylist_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = s.calendarMyListBtnDesc,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // 3-Segment Tab Bar with Blue Bottom Indicator (Week | Month | List)
        Row(modifier = Modifier.fillMaxWidth()) {
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = uiState.calendarViewMode == mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectMode(mode) }
                        .testTag("calendar_mode_${mode.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppLocalization.calendarModeLabel(mode, s),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.7.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Collapsible Genre / Format / Studio Filter Strip
        AnimatedVisibility(visible = showFilters) {
            CalendarFilterPanel(
                selectedFormat = uiState.calendarFormatFilter,
                selectedGenre = uiState.calendarGenreFilter,
                selectedStudio = uiState.calendarStudioFilter,
                onSelectFormat = onFormatFilter,
                onSelectGenre = onGenreFilter,
                onSelectStudio = onStudioFilter
            )
        }

        when (uiState.calendarViewMode) {
            CalendarViewMode.WEEK -> WeekCalendarContent(
                uiState = uiState,
                onSelectDate = onSelectDate,
                onShiftWeek = onShiftWeek,
                onAnimeClick = onAnimeClick,
                onToggleReminder = onToggleReminder
            )
            CalendarViewMode.MONTH -> MonthCalendarContent(
                uiState = uiState,
                onSelectDate = onSelectDate,
                onShiftMonth = onShiftMonth,
                onAnimeClick = onAnimeClick
            )
            CalendarViewMode.LIST -> ListCalendarContent(
                uiState = uiState,
                onAnimeClick = onAnimeClick,
                onToggleWatched = onToggleWatched,
                onToggleReminder = onToggleReminder
            )
        }
    }
}

@Composable
private fun CalendarFilterPanel(
    selectedFormat: MediaFormatFilter,
    selectedGenre: String?,
    selectedStudio: String?,
    onSelectFormat: (MediaFormatFilter) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onSelectStudio: (String?) -> Unit
) {
    val s = LocalAppStrings.current
    var genreMenuOpen by remember { mutableStateOf(false) }
    var studioMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(adaptiveCardColor())
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MediaFormatFilter.entries) { fmt ->
                FilterChip(
                    selected = selectedFormat == fmt,
                    onClick = { onSelectFormat(fmt) },
                    label = { Text(AppLocalization.formatFilterLabel(fmt, s), fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                Box {
                    FilterChip(
                        selected = selectedGenre != null,
                        onClick = { genreMenuOpen = true },
                        label = {
                            Text(
                                text = selectedGenre?.let { AppLocalization.genreLabel(it, s) }
                                    ?: s.calendarGenreChipDefault,
                                fontSize = 11.5.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(
                        expanded = genreMenuOpen,
                        onDismissRequest = { genreMenuOpen = false },
                        containerColor = if (LocalIsLiquidGlass.current) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.calendarAllGenres) },
                            onClick = {
                                onSelectGenre(null)
                                genreMenuOpen = false
                            }
                        )
                        GenreHelper.FEATURED_GENRES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(AppLocalization.genreLabel(g, s)) },
                                onClick = {
                                    onSelectGenre(g)
                                    genreMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                Box {
                    FilterChip(
                        selected = selectedStudio != null,
                        onClick = { studioMenuOpen = true },
                        label = { Text(selectedStudio ?: s.calendarStudioChipDefault, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(
                        expanded = studioMenuOpen,
                        onDismissRequest = { studioMenuOpen = false },
                        containerColor = if (LocalIsLiquidGlass.current) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.calendarAllStudios) },
                            onClick = {
                                onSelectStudio(null)
                                studioMenuOpen = false
                            }
                        )
                        GenreHelper.POPULAR_STUDIOS.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    onSelectStudio(st)
                                    studioMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCalendarContent(
    uiState: AnimeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onAnimeClick: (AnimeRelease) -> Unit,
    onToggleReminder: (AnimeRelease) -> Unit
) {
    val s = LocalAppStrings.current
    val locale = LocalAppLanguage.current.toLocale()
    val selectedDayName = uiState.selectedDate.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }

    val bottomDockPadding = LocalBottomDockPadding.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 14.dp + bottomDockPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 7-day strip card matching Screenshot 4
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Surface(
                    shape = RoundedCornerShape(adaptiveCardCorner(16.dp, 22.dp)),
                    color = adaptiveCardColor(),
                    border = glassCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)) {
                        // Subtle week navigation header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onShiftWeek(-1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = s.calendarPrevWeekDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val monthLabel = uiState.selectedDate.month
                                .getDisplayName(TextStyle.FULL, locale)
                                .replaceFirstChar { it.uppercase(locale) }
                            Text(
                                text = "$monthLabel ${uiState.selectedDate.year}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { onShiftWeek(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = s.calendarNextWeekDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            uiState.dayTabs.forEach { tab ->
                                WeekDayCell(
                                    tab = tab,
                                    isSelected = tab.date == uiState.selectedDate,
                                    onClick = { onSelectDate(tab.date) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = s.calendarReleasesForDay(selectedDayName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "(${uiState.filteredDayReleases.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingSchedule && uiState.scheduleReleases.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (uiState.filteredDayReleases.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyDayCard(dayName = selectedDayName)
            }
        } else {
            items(uiState.filteredDayReleases, key = { "week_${it.id}_${it.episode}" }) { anime ->
                AnimePosterCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime) },
                    showAiringTime = true,
                    showEpisodeBadge = true,
                    zoneId = uiState.activeZoneId,
                    onToggleReminder = { onToggleReminder(anime) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WeekDayCell(
    tab: DayScheduleTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("week_day_${tab.date}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = tab.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected || tab.isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> Color.White
                    tab.isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = tab.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            tab.episodeCount == 0 -> Color.Transparent
                            isSelected -> Color.White.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

@Composable
private fun MonthCalendarContent(
    uiState: AnimeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onAnimeClick: (AnimeRelease) -> Unit
) {
    val s = LocalAppStrings.current
    val locale = LocalAppLanguage.current.toLocale()
    val month = uiState.selectedMonth
    val firstDayOfMonth = month.atDay(1)
    val leadingBlanks = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val daysInMonth = month.lengthOfMonth()

    val pool = uiState.monthReleases.ifEmpty { uiState.scheduleReleases }
    val countsByDate = remember(pool, uiState.activeZoneId) {
        pool.groupBy { it.getAiringLocalDate(uiState.activeZoneId) }.mapValues { it.value.size }
    }
    val selectedDayList = remember(pool, uiState.selectedDate, uiState.activeZoneId) {
        pool.filter { it.getAiringLocalDate(uiState.activeZoneId) == uiState.selectedDate }
            .sortedBy { it.effectiveAiringSeconds() }
    }

    val bottomDockPadding = LocalBottomDockPadding.current

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp + bottomDockPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(adaptiveCardCorner(16.dp, 22.dp)),
                color = adaptiveCardColor(),
                border = glassCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = s.calendarPrevMonthDesc)
                        }
                        val monthTitle = month.month.getDisplayName(TextStyle.FULL, locale)
                            .replaceFirstChar { it.uppercase(locale) }
                        Text(
                            text = "$monthTitle ${month.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onShiftMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = s.calendarNextMonthDesc)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DayOfWeek.entries.forEach { dow ->
                            val d = dow.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
                            Text(
                                text = d,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val totalCells = leadingBlanks + daysInMonth
                    val rows = (totalCells + 6) / 7
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - leadingBlanks + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = month.atDay(dayNum)
                                    val isSelected = date == uiState.selectedDate
                                    val count = countsByDate[date] ?: 0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectDate(date) }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (count > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val dayLabel = uiState.selectedDate.dayOfWeek
                .getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase(locale) }
            Text(
                text = "$dayLabel ${uiState.selectedDate.dayOfMonth} (${selectedDayList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedDayList.isEmpty()) {
            item { EmptyDayCard(dayName = s.calendarThisDay) }
        } else {
            items(selectedDayList, key = { "month_${it.id}_${it.episode}" }) { anime ->
                ScheduleListRowCard(
                    anime = anime,
                    zoneId = uiState.activeZoneId,
                    onClick = { onAnimeClick(anime) },
                    onToggleWatched = {},
                    onToggleReminder = {},
                    showQuickActions = false
                )
            }
        }
    }
}

@Composable
private fun ListCalendarContent(
    uiState: AnimeUiState,
    onAnimeClick: (AnimeRelease) -> Unit,
    onToggleWatched: (AnimeRelease) -> Unit,
    onToggleReminder: (AnimeRelease) -> Unit
) {
    val s = LocalAppStrings.current
    val locale = LocalAppLanguage.current.toLocale()
    val zone = uiState.activeZoneId
    val grouped = remember(uiState.scheduleReleases, uiState.calendarFormatFilter, uiState.calendarGenreFilter, uiState.calendarStudioFilter) {
        uiState.scheduleReleases.filter { item ->
            val fmtOk = uiState.calendarFormatFilter.apiValue == null ||
                item.format.equals(uiState.calendarFormatFilter.apiValue, ignoreCase = true)
            val genreOk = uiState.calendarGenreFilter == null ||
                GenreHelper.matchesAnyGenre(item.genres, setOf(uiState.calendarGenreFilter))
            val studioOk = uiState.calendarStudioFilter == null ||
                item.studio?.contains(uiState.calendarStudioFilter, ignoreCase = true) == true
            fmtOk && genreOk && studioOk
        }.sortedBy { it.effectiveAiringSeconds() }
            .groupBy { it.getAiringLocalDate(zone) }
    }

    if (grouped.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyDayCard(dayName = s.calendarThisWeek)
        }
        return
    }

    val bottomDockPadding = LocalBottomDockPadding.current

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp + bottomDockPadding
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        grouped.forEach { (date, releases) ->
            item(key = "header_$date") {
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                    .replaceFirstChar { it.uppercase(locale) }
                val todayTag = if (date == LocalDate.now(zone)) s.calendarTodayTag else ""
                Text(
                    text = "$dayName ${date.dayOfMonth}$todayTag (${releases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(releases, key = { "list_${date}_${it.id}_${it.episode}" }) { anime ->
                ScheduleListRowCard(
                    anime = anime,
                    zoneId = zone,
                    onClick = { onAnimeClick(anime) },
                    onToggleWatched = { onToggleWatched(anime) },
                    onToggleReminder = { onToggleReminder(anime) },
                    showQuickActions = true
                )
            }
        }
    }
}

@Composable
private fun ScheduleListRowCard(
    anime: AnimeRelease,
    zoneId: ZoneId,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleReminder: () -> Unit,
    showQuickActions: Boolean
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val countdown = AppLocalization.countdown(anime, s)

    val rowCorner = adaptiveCardCorner(14.dp, 20.dp)
    Surface(
        shape = RoundedCornerShape(rowCorner),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(rowCorner))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(anime.coverImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = anime.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(64.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${s.episodeShortPrefix} ${anime.effectiveEpisodeNumber()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = AppLocalization.formatLabel(anime.format, s),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    anime.studio?.let { st ->
                        Text(
                            text = "• $st",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = when (countdown.kind) {
                            AppLocalization.CountdownKind.AIRING_NOW -> AniAiringGreenBright
                            AppLocalization.CountdownKind.UPCOMING -> MaterialTheme.colorScheme.primary
                            AppLocalization.CountdownKind.AIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${anime.getFormattedTime(zoneId)} • ${countdown.text}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (countdown.kind) {
                            AppLocalization.CountdownKind.AIRING_NOW -> AniAiringGreenBright
                            AppLocalization.CountdownKind.UPCOMING -> MaterialTheme.colorScheme.primary
                            AppLocalization.CountdownKind.AIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                val activeSynopsis = anime.getActiveSynopsis()
                if (activeSynopsis.isNotBlank()) {
                    Text(
                        text = activeSynopsis,
                        fontSize = 11.sp,
                        lineHeight = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("schedule_card_synopsis_${anime.id}")
                    )
                }
            }

            if (showQuickActions) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onToggleReminder) {
                        Icon(
                            imageVector = if (anime.hasReminder) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = s.calendarReminderDesc,
                            tint = if (anime.hasReminder) AniGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleWatched) {
                        Icon(
                            imageVector = if (anime.isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = s.calendarMarkWatchedDesc,
                            tint = if (anime.isWatched) AniAiringGreenBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard(dayName: String) {
    val s = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(adaptiveCardCorner(16.dp, 22.dp)),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = s.calendarEmptyDayTitle(dayName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = s.calendarEmptyDaySubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
