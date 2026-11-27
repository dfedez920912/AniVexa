package com.github.dfedez920912.anivexa.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.dfedez920912.anivexa.i18n.AppLocalization
import com.github.dfedez920912.anivexa.i18n.LocalAppLanguage
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.model.AnimeCharacter
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.model.AnimeReview
import com.github.dfedez920912.anivexa.model.RelatedAnimeItem
import com.github.dfedez920912.anivexa.ui.theme.AniAiringGreen
import com.github.dfedez920912.anivexa.ui.theme.AniAiringGreenBright
import com.github.dfedez920912.anivexa.ui.theme.AniAzureBright
import com.github.dfedez920912.anivexa.ui.theme.AniGlassSheetSurface
import com.github.dfedez920912.anivexa.ui.theme.AniGold
import com.github.dfedez920912.anivexa.ui.theme.AniGoldBg
import com.github.dfedez920912.anivexa.ui.theme.AniNavyAction
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import com.github.dfedez920912.anivexa.ui.theme.screenBackdrop
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Locale

private enum class DetailSheetType {
    NONE, REVIEWS, CHARACTERS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    anime: AnimeRelease,
    reviews: List<AnimeReview>,
    characters: List<AnimeCharacter>,
    relatedAnime: List<RelatedAnimeItem> = emptyList(),
    isLoadingDetail: Boolean,
    watchedEpisodeKeys: Set<String>,
    defaultReminderMinutes: Int,
    zoneId: ZoneId,
    onBack: () -> Unit,
    onRelatedAnimeClick: (RelatedAnimeItem) -> Unit = {},
    onToggleWatchlist: () -> Unit,
    onToggleReminder: (Boolean, Int) -> Unit,
    onTestNotification: () -> Unit,
    onToggleWatchedEpisodeNum: (Int) -> Unit,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val s = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val scrollState = rememberScrollState()
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(DetailSheetType.NONE) }
    var reminderEnabled by remember(anime.id, anime.hasReminder) { mutableStateOf(anime.hasReminder) }
    var reminderMinutes by remember(anime.id, anime.reminderMinutesBefore) {
        mutableStateOf(if (anime.reminderMinutesBefore > 0) anime.reminderMinutesBefore else defaultReminderMinutes)
    }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNotificationAction?.invoke()
        }
        pendingNotificationAction = null
    }
    val runWithNotificationPermission: (() -> Unit) -> Unit = { action ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val activeSynopsis = anime.getActiveSynopsis()
    val countdown = AppLocalization.countdown(anime, s)
    val popularityFormatted = remember(anime.popularity, appLanguage) {
        if (anime.popularity > 0) NumberFormat.getNumberInstance(appLanguage.toLocale()).format(anime.popularity) else null
    }

    val isGlass = LocalIsLiquidGlass.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .screenBackdrop()
            .testTag("anime_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
        ) {
            // Hero Poster/Banner matching Screenshot 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(anime.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = if (isGlass) {
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Black,
                                        0.58f to Color.Black,
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    } else {
                        Modifier.fillMaxSize()
                    }
                )

                // Top & bottom gradient scrim into background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isGlass) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.48f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.background
                                    )
                                }
                            )
                        )
                )

                // Top Overlay Controls (Back left, Reminder + Share right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val overlayCircleColor = if (isGlass) Color(0x70101D33) else Color.Black.copy(alpha = 0.55f)
                    val overlayCircleBorder = if (isGlass) glassCardBorder(highContrast = true) else null

                    Surface(
                        shape = CircleShape,
                        color = overlayCircleColor,
                        border = overlayCircleBorder,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("detail_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = s.backContentDesc,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = overlayCircleColor,
                            border = overlayCircleBorder,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val next = !reminderEnabled
                                    if (next) {
                                        runWithNotificationPermission {
                                            reminderEnabled = true
                                            onToggleReminder(true, reminderMinutes)
                                        }
                                    } else {
                                        reminderEnabled = false
                                        onToggleReminder(false, reminderMinutes)
                                    }
                                }
                                .testTag("detail_bell_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = s.detailAlertDesc,
                                    tint = if (reminderEnabled) AniGold else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = overlayCircleColor,
                            border = overlayCircleBorder,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val formattedDt = anime.getFormattedDateTimeShort(zoneId, s.noDateText)
                                    val shareText = s.detailShareTemplate(
                                        anime.displayTitle,
                                        anime.effectiveEpisodeNumber(),
                                        formattedDt
                                    )
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, s.detailShareChooser))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = s.detailShareDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!anime.titleEnglish.isNullOrBlank() && anime.titleRomaji != anime.titleEnglish) {
                    Text(
                        text = anime.titleRomaji,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rating Pill + Status Pill matching Screenshot 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AniGoldBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AniGold,
                                modifier = Modifier.size(15.dp)
                            )
                            val scoreText = anime.score?.let { String.format("%.1f", it) } ?: "N/A"
                            val popSuffix = popularityFormatted?.let { s.detailFansSuffix(it) } ?: ""
                            Text(
                                text = "$scoreText$popSuffix",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AniGold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (anime.isCurrentlyAiringStatus) AniAiringGreen else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = AppLocalization.animeStatusLabel(anime.status, s),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (anime.isCurrentlyAiringStatus) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Synopsis & expand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = s.detailSynopsisTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (anime.isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                ) {
                    Text(
                        text = activeSynopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isSynopsisExpanded) s.detailShowLess else s.detailReadMore,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Blue CTA: "+ Add to My List"
                Button(
                    onClick = onToggleWatchlist,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (anime.isInWatchlist) AniAiringGreen else AniAzureBright,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_add_mylist_btn")
                ) {
                    Icon(
                        imageVector = if (anime.isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (anime.isInWatchlist) s.detailInMyListRemove else s.detailAddToMyList,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Two Secondary Buttons: Reviews | Characters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val actionShape = RoundedCornerShape(adaptiveCardCorner(10.dp, 16.dp))
                    val actionColor = if (isGlass) adaptiveCardColor(elevated = true) else AniNavyAction
                    val actionBorder = if (isGlass) glassCardBorder() else null

                    Surface(
                        shape = actionShape,
                        color = actionColor,
                        border = actionBorder,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(actionShape)
                            .clickable { activeSheet = DetailSheetType.REVIEWS }
                            .testTag("detail_reviews_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = s.detailReviewsBtn + if (reviews.isNotEmpty()) " (${reviews.size})" else "",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = actionShape,
                        color = actionColor,
                        border = actionBorder,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(actionShape)
                            .clickable { activeSheet = DetailSheetType.CHARACTERS }
                            .testTag("detail_characters_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = s.detailCharactersBtn + if (characters.isNotEmpty()) " (${characters.size})" else "",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Basic Info 3-column grid
                Text(
                    text = s.detailBasicInfoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BasicInfoCol(
                        label = s.detailTypeLabel,
                        value = AppLocalization.formatLabel(anime.format, s),
                        modifier = Modifier.weight(1f)
                    )
                    BasicInfoCol(
                        label = s.detailEpisodesLabel,
                        value = anime.totalEpisodes?.toString() ?: s.detailEpisodesUnknown,
                        modifier = Modifier.weight(1f)
                    )
                    BasicInfoCol(
                        label = s.detailBroadcastPeriodLabel,
                        value = AppLocalization.broadcastPeriod(anime, s),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Studio & Genres
                if (!anime.studio.isNullOrBlank() || anime.genres.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        anime.studio?.let { st ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = s.detailStudioPrefix(st),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        anime.genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = AppLocalization.genreLabel(genre, s),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Airing Countdown & Reminder Card
                Surface(
                    shape = RoundedCornerShape(adaptiveCardCorner(14.dp, 20.dp)),
                    color = adaptiveCardColor(),
                    border = glassCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = s.detailEpisodeLabel(anime.effectiveEpisodeNumber()),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (anime.effectiveAiringSeconds() > 0L) {
                                            "${anime.getFormattedDateTimeShort(zoneId, s.noDateText)} • ${countdown.text}"
                                        } else {
                                            s.detailNoUpcomingSchedule
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        runWithNotificationPermission {
                                            reminderEnabled = true
                                            onToggleReminder(true, reminderMinutes)
                                        }
                                    } else {
                                        reminderEnabled = false
                                        onToggleReminder(false, reminderMinutes)
                                    }
                                }
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15 to "15m", 30 to "30m", 60 to "1h", 0 to s.settingsOnAiringShort).forEach { (mins, label) ->
                                    FilterChip(
                                        selected = reminderMinutes == mins,
                                        onClick = {
                                            reminderMinutes = mins
                                            onToggleReminder(true, mins)
                                        },
                                        label = { Text(label, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { runWithNotificationPermission { onTestNotification() } },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.detailTestAlertBtn, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Episode Tracker Grid
                EpisodeTrackerSection(
                    animeId = anime.id,
                    totalEpisodes = anime.totalEpisodes,
                    effectiveEpisode = anime.effectiveEpisodeNumber(),
                    watchedEpisodeKeys = watchedEpisodeKeys,
                    onToggleWatchedEpisodeNum = onToggleWatchedEpisodeNum
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User 5-star Rating
                Surface(
                    shape = RoundedCornerShape(adaptiveCardCorner(14.dp, 20.dp)),
                    color = adaptiveCardColor(),
                    border = glassCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = s.detailYourRatingTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { star ->
                                val active = star <= anime.userRating
                                IconButton(
                                    onClick = { onRatingChanged(if (anime.userRating == star) 0 else star) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = s.detailRateStarsDesc(star),
                                        tint = if (active) AniGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Related Content
                if (relatedAnime.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.detailRelatedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = if (isGlass) glassCardBorder(glassWidth = 0.75.dp) else null
                        ) {
                            Text(
                                text = "${relatedAnime.size}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_related_row")
                    ) {
                        items(relatedAnime, key = { "rel_${it.id}" }) { rel ->
                            RelatedAnimeCard(
                                item = rel,
                                onClick = { onRelatedAnimeClick(rel) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Modal Bottom Sheet for Reviews & Characters
        if (activeSheet != DetailSheetType.NONE) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { activeSheet = DetailSheetType.NONE },
                sheetState = sheetState,
                containerColor = if (isGlass) AniGlassSheetSurface else MaterialTheme.colorScheme.surface
            ) {
                if (activeSheet == DetailSheetType.REVIEWS) {
                    ReviewsSheetContent(reviews = reviews, isLoading = isLoadingDetail)
                } else {
                    CharactersSheetContent(characters = characters, isLoading = isLoadingDetail)
                }
            }
        }
    }
}

@Composable
private fun RelatedAnimeCard(
    item: RelatedAnimeItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val isGlass = LocalIsLiquidGlass.current
    val cardRadius = adaptiveCardCorner(standard = 12.dp, glass = 18.dp)
    val cardShape = RoundedCornerShape(cardRadius)
    val relationLabel = AppLocalization.relationLabel(item.relationType, item.format, s)
    val formatLabel = AppLocalization.formatLabel(item.format, s)

    val badgeColor = when (item.relationType.uppercase()) {
        "PREQUEL" -> Color(0xFF6366F1)
        "SEQUEL" -> Color(0xFF0EA5E9)
        "PARENT" -> Color(0xFF2563EB)
        "SIDE_STORY" -> if (item.format.equals("OVA", ignoreCase = true)) Color(0xFF10B981) else Color(0xFF8B5CF6)
        else -> when (item.format.uppercase()) {
            "OVA" -> Color(0xFF10B981)
            "MOVIE" -> Color(0xFFD97706)
            "SPECIAL" -> Color(0xFFEC4899)
            else -> Color(0xFF64748B)
        }
    }

    Surface(
        shape = cardShape,
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier
            .width(138.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
            .testTag("related_anime_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.50f))
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(if (isGlass) 8.dp else 6.dp),
                    color = badgeColor.copy(alpha = 0.92f),
                    border = if (isGlass) glassCardBorder(glassWidth = 0.7.dp, highContrast = true) else null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = relationLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (!relationLabel.equals(formatLabel, ignoreCase = true) && !relationLabel.equals(item.format, ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.70f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = formatLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.height(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val metaParts = listOfNotNull(
                    item.seasonYear?.toString(),
                    item.totalEpisodes?.let { "$it ${s.episodesUnitShort}" }
                ).joinToString(" • ")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (item.score != null) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AniGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", item.score),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AniGold
                        )
                        if (metaParts.isNotEmpty()) {
                            Text(
                                text = "•",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

internal data class EpisodeTrackerMetrics(
    val displayEpisodeCount: Int,
    val watchedCount: Int,
    val progress: Float,
    val progressPercent: Int
)

internal fun calculateEpisodeTrackerMetrics(
    totalEpisodes: Int?,
    effectiveEpisode: Int,
    animeId: Int,
    watchedEpisodeKeys: Set<String>
): EpisodeTrackerMetrics {
    val displayCount = totalEpisodes?.takeIf { it > 0 } ?: effectiveEpisode.coerceAtLeast(12)
    val prefix = "${animeId}_"
    val watched = watchedEpisodeKeys.count { key ->
        if (!key.startsWith(prefix)) return@count false
        val epNum = key.removePrefix(prefix).toIntOrNull() ?: return@count false
        epNum in 1..displayCount
    }
    val progress = if (displayCount > 0) {
        (watched.toFloat() / displayCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = when {
        displayCount <= 0 || watched <= 0 -> 0
        watched >= displayCount -> 100
        else -> (progress * 100f).toInt().coerceIn(0, 99)
    }
    return EpisodeTrackerMetrics(
        displayEpisodeCount = displayCount,
        watchedCount = watched,
        progress = progress,
        progressPercent = percent
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeTrackerSection(
    animeId: Int,
    totalEpisodes: Int?,
    effectiveEpisode: Int,
    watchedEpisodeKeys: Set<String>,
    onToggleWatchedEpisodeNum: (Int) -> Unit
) {
    val s = LocalAppStrings.current
    val isGlass = LocalIsLiquidGlass.current
    val metrics = remember(animeId, totalEpisodes, effectiveEpisode, watchedEpisodeKeys) {
        calculateEpisodeTrackerMetrics(
            totalEpisodes = totalEpisodes,
            effectiveEpisode = effectiveEpisode,
            animeId = animeId,
            watchedEpisodeKeys = watchedEpisodeKeys
        )
    }
    val totalEpCount = metrics.displayEpisodeCount
    val watchedCount = metrics.watchedCount
    val progress = metrics.progress

    Surface(
        shape = RoundedCornerShape(adaptiveCardCorner(14.dp, 20.dp)),
        color = adaptiveCardColor(),
        border = glassCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail_episode_tracker")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.detailEpisodeTrackerTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$watchedCount / $totalEpCount (${metrics.progressPercent}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AniAiringGreenBright,
                    modifier = Modifier.testTag("detail_episode_progress_label")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = AniAiringGreenBright,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("detail_episode_progress_bar")
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (totalEpCount <= 18) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..totalEpCount).forEach { epNum ->
                        val isWatched = watchedEpisodeKeys.contains("${animeId}_$epNum")
                        EpisodeGridChip(
                            episodeNumber = epNum,
                            isWatched = isWatched,
                            onClick = { onToggleWatchedEpisodeNum(epNum) }
                        )
                    }
                }
            } else {
                val gridState = rememberLazyGridState()
                val coroutineScope = rememberCoroutineScope()

                if (totalEpCount > 48) {
                    val blockSize = 50
                    val ranges = remember(totalEpCount) {
                        (1..totalEpCount step blockSize).map { start ->
                            start to minOf(start + blockSize - 1, totalEpCount)
                        }
                    }
                    val activeRangeIndex by remember(ranges) {
                        derivedStateOf {
                            val firstVisibleEp = gridState.firstVisibleItemIndex + 1
                            ranges.indexOfFirst { (start, end) -> firstVisibleEp in start..end }
                                .coerceAtLeast(0)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_episode_range_row")
                    ) {
                        items(ranges.size) { index ->
                            val (start, end) = ranges[index]
                            val isActive = index == activeRangeIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            gridState.animateScrollToItem(start - 1)
                                        }
                                    }
                                    .testTag("episode_range_${start}_$end")
                            ) {
                                Text(
                                    text = "$start–$end",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = if (isGlass) 0.25f else 0.45f),
                    border = if (isGlass) glassCardBorder(glassWidth = 0.7.dp) else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 88.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(252.dp)
                            .testTag("detail_episode_scroll_grid")
                    ) {
                        items(totalEpCount, key = { it + 1 }) { index ->
                            val epNum = index + 1
                            val isWatched = watchedEpisodeKeys.contains("${animeId}_$epNum")
                            EpisodeGridChip(
                                episodeNumber = epNum,
                                isWatched = isWatched,
                                onClick = { onToggleWatchedEpisodeNum(epNum) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s.detailSwipeEpisodesHint(totalEpCount),
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EpisodeGridChip(
    episodeNumber: Int,
    isWatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isWatched) AniAiringGreenBright.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isWatched) AniAiringGreenBright else Color.Transparent
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("episode_chip_$episodeNumber")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isWatched) AniAiringGreenBright else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${s.episodeShortPrefix} $episodeNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isWatched) AniAiringGreenBright else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BasicInfoCol(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ReviewsSheetContent(
    reviews: List<AnimeReview>,
    isLoading: Boolean
) {
    val s = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = s.detailReviewsModalTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading && reviews.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            reviews.isEmpty() -> {
                Text(
                    text = s.detailNoReviews,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    items(reviews, key = { it.id }) { rev ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rev.userName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AniGoldBg
                                    ) {
                                        Text(
                                            text = "★ ${rev.score}/100",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AniGold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (rev.summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rev.summary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (rev.body.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rev.body,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun CharactersSheetContent(
    characters: List<AnimeCharacter>,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val s = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = s.detailCharactersModalTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading && characters.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            characters.isEmpty() -> {
                Text(
                    text = s.detailNoCharacters,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    items(characters, key = { it.id }) { ch ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(ch.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = ch.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (ch.role == "MAIN") s.detailRoleMain else s.detailRoleSupporting,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    ch.voiceActorName?.let { va ->
                                        Text(
                                            text = "CV: $va",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
