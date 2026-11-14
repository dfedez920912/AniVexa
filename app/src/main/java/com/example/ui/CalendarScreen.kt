package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import com.example.model.AnimeRelease
import com.example.model.CountdownStatus
import com.example.model.DayScheduleTab
import com.example.model.GenreHelper
import com.example.model.MediaFormatFilter
import com.example.ui.components.AnimePosterCard
import com.example.ui.theme.AniAiringGreenBright
import com.example.ui.theme.AniGold
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: "Calendario" + Today + Filter + Bookmark shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calendario",
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
                        contentDescription = "Ir a Hoy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier.testTag("calendar_filter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros de formato, género y estudio",
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
                        contentDescription = "Ir a Mi Lista",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // 3-Segment Tab Bar with Blue Bottom Indicator (Semana | Mes | Lista)
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
                        text = mode.label,
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
                onAnimeClick = onAnimeClick
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
    var genreMenuOpen by remember { mutableStateOf(false) }
    var studioMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
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
                    label = { Text(fmt.labelEs, fontSize = 11.5.sp) },
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
                        label = { Text(selectedGenre ?: "Género ▾", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(expanded = genreMenuOpen, onDismissRequest = { genreMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos los géneros") },
                            onClick = {
                                onSelectGenre(null)
                                genreMenuOpen = false
                            }
                        )
                        GenreHelper.FEATURED_GENRES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
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
                        label = { Text(selectedStudio ?: "Estudio ▾", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(expanded = studioMenuOpen, onDismissRequest = { studioMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos los estudios") },
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
    onAnimeClick: (AnimeRelease) -> Unit
) {
    val selectedDayName = uiState.selectedDate.dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercase() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 7-day strip card matching Screenshot 4
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
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
                                    contentDescription = "Semana anterior",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val monthLabel = uiState.selectedDate.month
                                .getDisplayName(TextStyle.FULL, Locale("es"))
                                .replaceFirstChar { it.uppercase() }
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
                                    contentDescription = "Semana siguiente",
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

                // Section Title: "Emisiones del Viernes (8)"
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
                            text = "Emisiones del $selectedDayName",
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
                text = "${tab.dayOfMonth}",
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
    val month = uiState.selectedMonth
    val firstDayOfMonth = month.atDay(1)
    val leadingBlanks = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val daysInMonth = month.lengthOfMonth()

    val pool = if (uiState.monthReleases.isNotEmpty()) uiState.monthReleases else uiState.scheduleReleases
    val countsByDate = remember(pool, uiState.activeZoneId) {
        pool.groupBy { it.getAiringLocalDate(uiState.activeZoneId) }.mapValues { it.value.size }
    }
    val selectedDayList = remember(pool, uiState.selectedDate, uiState.activeZoneId) {
        pool.filter { it.getAiringLocalDate(uiState.activeZoneId) == uiState.selectedDate }
            .sortedBy { it.effectiveAiringSeconds() }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                        }
                        val monthTitle = month.month.getDisplayName(TextStyle.FULL, Locale("es"))
                            .replaceFirstChar { it.uppercase() }
                        Text(
                            text = "$monthTitle ${month.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onShiftMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
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
                .getDisplayName(TextStyle.FULL, Locale("es"))
                .replaceFirstChar { it.uppercase() }
            Text(
                text = "$dayLabel ${uiState.selectedDate.dayOfMonth} (${selectedDayList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedDayList.isEmpty()) {
            item { EmptyDayCard(dayName = "este día") }
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
            EmptyDayCard(dayName = "esta semana")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        grouped.forEach { (date, releases) ->
            item(key = "header_$date") {
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
                    .replaceFirstChar { it.uppercase() }
                val todayTag = if (date == LocalDate.now(zone)) " • HOY" else ""
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
    zoneId: java.time.ZoneId,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleReminder: () -> Unit,
    showQuickActions: Boolean
) {
    val context = LocalContext.current
    val countdown = anime.getCountdownStatus()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
                            text = "Ep ${anime.effectiveEpisodeNumber()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = anime.format,
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
                        tint = when (countdown) {
                            is CountdownStatus.AiringNow -> AniAiringGreenBright
                            is CountdownStatus.Upcoming -> MaterialTheme.colorScheme.primary
                            is CountdownStatus.Aired -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${anime.getFormattedTime(zoneId)} • ${countdown.text}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (countdown) {
                            is CountdownStatus.AiringNow -> AniAiringGreenBright
                            is CountdownStatus.Upcoming -> MaterialTheme.colorScheme.primary
                            is CountdownStatus.Aired -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (showQuickActions) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onToggleReminder) {
                        Icon(
                            imageVector = if (anime.hasReminder) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Recordatorio",
                            tint = if (anime.hasReminder) AniGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleWatched) {
                        Icon(
                            imageVector = if (anime.isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Marcar como visto",
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                text = "Sin emisiones para $dayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Prueba otro día de la semana o limpia los filtros activos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
