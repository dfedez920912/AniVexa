package com.github.dfedez920912.anivexa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.model.AnimeRelease
import com.github.dfedez920912.anivexa.ui.theme.AniAzure
import com.github.dfedez920912.anivexa.ui.theme.AniGold
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import java.time.ZoneId

@Composable
fun AnimePosterCard(
    anime: AnimeRelease,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAiringTime: Boolean = false,
    showEpisodeBadge: Boolean = false,
    zoneId: ZoneId = ZoneId.systemDefault(),
    onToggleReminder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isGlass = LocalIsLiquidGlass.current
    val s = LocalAppStrings.current
    val cardRadius = adaptiveCardCorner(standard = 12.dp, glass = 18.dp)
    val cardShape = RoundedCornerShape(cardRadius)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = adaptiveCardColor(elevated = false)
        ),
        border = glassCardBorder(standardWidth = 0.7.dp, glassWidth = 0.95.dp),
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick)
            .testTag("poster_card_${anime.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(anime.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom gradient for polish
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Top-right status pill ("Emisión" / "Pronto") matching reference screenshots
                val badgeText = when {
                    anime.isCurrentlyAiringStatus -> s.badgeAiring
                    anime.status.equals("NOT_YET_RELEASED", ignoreCase = true) -> s.badgeSoon
                    else -> null
                }
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(if (isGlass) 9.dp else 6.dp),
                        color = if (isGlass) Color(0xCC0EA5E9) else AniAzure.copy(alpha = 0.94f),
                        border = if (isGlass) glassCardBorder(glassWidth = 0.75.dp, highContrast = true) else null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Optional top-left episode pill for Calendar cards
                if (showEpisodeBadge) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${s.episodeShortPrefix} ${anime.effectiveEpisodeNumber()}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Interactive Bell Icon Button for toggling episode release reminders
                if (onToggleReminder != null) {
                    Surface(
                        shape = CircleShape,
                        color = if (anime.hasReminder) {
                            if (isGlass) Color(0xCC0EA5E9) else AniAzure.copy(alpha = 0.94f)
                        } else {
                            Color.Black.copy(alpha = 0.58f)
                        },
                        border = if (isGlass) glassCardBorder(glassWidth = 0.75.dp, highContrast = true) else null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleReminder)
                            .testTag("poster_card_reminder_bell_${anime.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (anime.hasReminder) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = s.calendarReminderDesc,
                                tint = if (anime.hasReminder) AniGold else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.height(34.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Rating • Year • Total episodes
                val yearText = anime.seasonYear?.toString()
                val epsText = anime.totalEpisodes?.let { "$it ${s.episodesUnitShort}" }
                    ?: if (anime.format.equals("MOVIE", ignoreCase = true)) s.formatMovie else null
                val metaParts = listOfNotNull(yearText, epsText).joinToString(" • ")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (anime.score != null) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AniGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", anime.score),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AniGold
                        )
                        if (metaParts.isNotEmpty()) {
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val synopsisPreview = anime.getActiveSynopsis()
                if (synopsisPreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = synopsisPreview,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("poster_card_synopsis_${anime.id}")
                    )
                }

                if (showAiringTime && anime.effectiveAiringSeconds() > 0L) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = anime.getFormattedDateTimeShort(zoneId),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
