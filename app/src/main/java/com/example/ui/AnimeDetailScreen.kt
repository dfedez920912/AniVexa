package com.example.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.model.AnimeCharacter
import com.example.model.AnimeRelease
import com.example.model.AnimeReview
import com.example.model.CountdownStatus
import com.example.model.SynopsisLanguage
import com.example.ui.theme.AniAiringGreen
import com.example.ui.theme.AniAiringGreenBright
import com.example.ui.theme.AniAzureBright
import com.example.ui.theme.AniGlassSheetSurface
import com.example.ui.theme.AniGold
import com.example.ui.theme.AniGoldBg
import com.example.ui.theme.AniNavyAction
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.adaptiveCardColor
import com.example.ui.theme.adaptiveCardCorner
import com.example.ui.theme.glassCardBorder
import com.example.ui.theme.screenBackdrop
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
    isLoadingDetail: Boolean,
    watchedEpisodeKeys: Set<String>,
    defaultReminderMinutes: Int,
    zoneId: ZoneId,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onToggleReminder: (Boolean, Int) -> Unit,
    onTestNotification: () -> Unit,
    onToggleWatchedEpisodeNum: (Int) -> Unit,
    onLanguageSelected: (SynopsisLanguage) -> Unit,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
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
    val countdown = anime.getCountdownStatus()
    val popularityFormatted = remember(anime.popularity) {
        if (anime.popularity > 0) NumberFormat.getNumberInstance(Locale.US).format(anime.popularity) else null
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
                                contentDescription = "Volver",
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
                                    contentDescription = "Activar alerta",
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
                                    val shareText = "${anime.displayTitle} - Ep ${anime.effectiveEpisodeNumber()} (${anime.getFormattedDateTimeShort(zoneId)}) en AniVexa"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir anime"))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir",
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
                            val popSuffix = popularityFormatted?.let { " ($it fans)" } ?: ""
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
                            text = anime.statusLabelEs,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (anime.isCurrentlyAiringStatus) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Synopsis with ES/EN pill & expand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sinopsis",
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

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SynopsisLanguage.entries.forEach { lang ->
                            val selected = anime.activeLanguage == lang
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { onLanguageSelected(lang) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (lang == SynopsisLanguage.SPANISH) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = lang.shortLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                        text = if (isSynopsisExpanded) "Mostrar menos" else "Leer más...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Blue CTA: "+ Add to My List" matching Screenshot 1
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
                        text = if (anime.isInWatchlist) "En Mi Lista (Tocar para quitar)" else "Añadir a Mi Lista",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Two Dark Navy Secondary Buttons: Reviews | Characters matching Screenshot 1
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
                                text = "Reseñas" + if (reviews.isNotEmpty()) " (${reviews.size})" else "",
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
                                text = "Personajes" + if (characters.isNotEmpty()) " (${characters.size})" else "",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Basic Info 3-column grid matching Screenshot 1
                Text(
                    text = "Información Básica",
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
                        label = "Tipo",
                        value = anime.format,
                        modifier = Modifier.weight(1f)
                    )
                    BasicInfoCol(
                        label = "Episodios",
                        value = anime.totalEpisodes?.toString() ?: "Desconocido",
                        modifier = Modifier.weight(1f)
                    )
                    BasicInfoCol(
                        label = "Periodo de Emisión",
                        value = anime.broadcastPeriodDisplay,
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
                                    text = "Estudio: $st",
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
                                    text = genre,
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
                                        text = "Episodio ${anime.effectiveEpisodeNumber()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (anime.effectiveAiringSeconds() > 0L) {
                                            "${anime.getFormattedDateTimeShort(zoneId)} • ${countdown.text}"
                                        } else {
                                            "Sin próximo horario programado"
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
                                listOf(15 to "15m", 30 to "30m", 60 to "1h", 0 to "Al emitir").forEach { (mins, label) ->
                                    FilterChip(
                                        selected = reminderMinutes == mins,
                                        onClick = {
                                            reminderMinutes = mins
                                            onToggleReminder(true, mins)
                                        },
                                        label = { Text(label, fontSize = 10.5.sp) },
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
                                Text("Probar alerta ahora", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Episode Tracker Grid
                val totalEpCount = (anime.totalEpisodes ?: anime.effectiveEpisodeNumber().coerceAtLeast(12)).coerceAtMost(48)
                val watchedCount = (1..totalEpCount).count { ep ->
                    watchedEpisodeKeys.contains("${anime.id}_$ep")
                }
                val progress = if (totalEpCount > 0) watchedCount.toFloat() / totalEpCount.toFloat() else 0f

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
                            Text(
                                text = "Seguimiento de Episodios",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$watchedCount / ${anime.totalEpisodes ?: totalEpCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AniAiringGreenBright
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
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..totalEpCount).forEach { epNum ->
                                val isWatched = watchedEpisodeKeys.contains("${anime.id}_$epNum")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isWatched) AniAiringGreenBright.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isWatched) AniAiringGreenBright else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleWatchedEpisodeNum(epNum) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isWatched) AniAiringGreenBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Ep $epNum",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isWatched) AniAiringGreenBright else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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
                            text = "Tu Puntuación",
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
                                        contentDescription = "Calificar $star estrellas",
                                        tint = if (active) AniGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Reseñas de la Comunidad",
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
                    text = "Todavía no hay reseñas publicadas para este anime en AniList.",
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Personajes y Voces (JP)",
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
                    text = "No se encontró información de personajes para este título.",
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
                                        text = if (ch.role == "MAIN") "Protagonista" else "Secundario",
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
