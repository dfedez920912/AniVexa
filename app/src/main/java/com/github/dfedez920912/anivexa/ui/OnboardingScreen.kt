package com.github.dfedez920912.anivexa.ui

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.dfedez920912.anivexa.backup.BackupOperationMode
import com.github.dfedez920912.anivexa.backup.GoogleDriveBackupClient
import com.github.dfedez920912.anivexa.backup.LocalBackupStorage
import com.github.dfedez920912.anivexa.i18n.LocalAppStrings
import com.github.dfedez920912.anivexa.ui.components.BackupRestoreDialog
import com.github.dfedez920912.anivexa.ui.theme.LocalIsLiquidGlass
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardColor
import com.github.dfedez920912.anivexa.ui.theme.adaptiveCardCorner
import com.github.dfedez920912.anivexa.ui.theme.glassCardBorder
import com.github.dfedez920912.anivexa.R
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    uiState: AnimeUiState = AnimeUiState(),
    onLocalTreeGranted: (Uri?, () -> Unit) -> Unit = { _, _ -> },
    onLoadLocalBackups: () -> Unit = {},
    onRestoreLocalBackup: (Uri) -> Unit = {},
    onRequestDriveAccess: (Context, () -> Unit, (IntentSender) -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _ -> },
    onDriveResolutionResult: (Context, Int, Intent?, () -> Unit, (IntentSender) -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onDriveActivityResult: (Context, Int, Intent?, () -> Unit, (Intent) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onLoadDriveBackups: () -> Unit = {},
    onRestoreDriveBackup: (String) -> Unit = {},
    onClearBackupMessages: () -> Unit = {},
    onOnboardingFinished: () -> Unit
) {
    val isGlass = LocalIsLiquidGlass.current
    val s = LocalAppStrings.current
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var showRestoreDialog by remember { mutableStateOf(false) }

    val backdropModifier = if (isGlass) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0E1525),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    }

    Box(
        modifier = backdropModifier
            .safeDrawingPadding()
            .testTag("onboarding_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: logo pill + Skip button on pages 0..2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = if (isGlass) glassCardBorder(highContrast = true) else null,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = "AniVexa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Text(
                        text = s.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (pagerState.currentPage < ONBOARDING_PAGE_COUNT - 1) {
                    TextButton(
                        onClick = onOnboardingFinished,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = s.onboardingSkipBtn,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Multi-page Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("onboarding_pager")
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0 -> OnboardingFeatureSlide(
                            tag = "onboarding_slide_0",
                            iconRes = R.drawable.ic_anime_notifications_active_filled,
                            headerTitle = s.onboardingTitle,
                            headerSubtitle = s.onboardingSubtitle,
                            featureTitle = s.onboardingFeature1Title,
                            featureDescription = s.onboardingFeature1Desc,
                            highlights = listOf(s.settingsJstTitle, s.calendarTitle, s.badgeAiring)
                        )

                        1 -> OnboardingFeatureSlide(
                            tag = "onboarding_slide_1",
                            iconRes = R.drawable.ic_anime_bookmark_filled,
                            headerTitle = s.onboardingFeature2Title,
                            headerSubtitle = s.myListEmptyBody,
                            featureTitle = s.navMyList,
                            featureDescription = s.onboardingFeature2Desc,
                            highlights = listOf(s.detailEpisodeTrackerTitle, s.detailYourRatingTitle, s.navSearch)
                        )

                        2 -> OnboardingFeatureSlide(
                            tag = "onboarding_slide_2",
                            iconRes = R.drawable.ic_anime_translate_filled,
                            headerTitle = s.onboardingFeature3Title,
                            headerSubtitle = s.settingsLanguageSubtitle,
                            featureTitle = s.settingsAppearanceSection,
                            featureDescription = s.onboardingFeature3Desc,
                            highlights = listOf("ES • EN • FR • 日本語", s.themeDarkLabel, s.themeGlassLabel)
                        )

                        else -> OnboardingBackupSlide(
                            onOpenRestore = {
                                onClearBackupMessages()
                                showRestoreDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                repeat(ONBOARDING_PAGE_COUNT) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        label = "onboarding_dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .testTag("onboarding_page_indicator_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .heightIn(min = 52.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anime_arrow_back_automirrored_filled),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = s.dialogBackBtn, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (pagerState.currentPage < ONBOARDING_PAGE_COUNT - 1) {
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(if (pagerState.currentPage > 0) 0.58f else 1f)
                            .heightIn(min = 52.dp)
                            .testTag("onboarding_next_button")
                    ) {
                        Text(
                            text = s.onboardingNextBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anime_keyboard_arrow_right_automirrored_filled),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onOnboardingFinished,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .heightIn(min = 52.dp)
                            .testTag("onboarding_continue_button")
                    ) {
                        Text(
                            text = s.onboardingStartBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anime_keyboard_arrow_right_automirrored_filled),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        BackupRestoreDialog(
            mode = BackupOperationMode.RESTORE,
            uiState = uiState,
            onDismiss = {
                showRestoreDialog = false
                onClearBackupMessages()
            },
            onLocalTreeGranted = onLocalTreeGranted,
            onCreateLocalBackup = {},
            onLoadLocalBackups = onLoadLocalBackups,
            onRestoreLocalBackup = onRestoreLocalBackup,
            onRequestDriveAccess = onRequestDriveAccess,
            onDriveResolutionResult = onDriveResolutionResult,
            onDriveActivityResult = onDriveActivityResult,
            onCreateDriveBackup = {},
            onLoadDriveBackups = onLoadDriveBackups,
            onRestoreDriveBackup = onRestoreDriveBackup,
            onClearMessages = onClearBackupMessages,
            onCompletedContinue = {
                showRestoreDialog = false
                onClearBackupMessages()
                onOnboardingFinished()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingFeatureSlide(
    tag: String,
    iconRes: Int,
    headerTitle: String,
    headerSubtitle: String,
    featureTitle: String,
    featureDescription: String,
    highlights: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            border = glassCardBorder(highContrast = true),
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Text(
            text = headerTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = headerSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(adaptiveCardCorner(18.dp, 22.dp)),
            color = adaptiveCardColor(elevated = true),
            border = glassCardBorder(),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = featureTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = featureDescription,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    highlights.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackupSlide(
    onOpenRestore: () -> Unit
) {
    val isGlass = LocalIsLiquidGlass.current
    val s = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("onboarding_slide_3"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            border = glassCardBorder(highContrast = true),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(adaptiveCardCorner(20.dp, 24.dp)),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isGlass) 0.28f else 0.22f),
            border = glassCardBorder(highContrast = true),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_backup_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = s.splashNewBadge,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = s.splashBackupTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = s.splashBackupDesc,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BackupDestinationPill(
                        icon = Icons.Default.PhoneAndroid,
                        text = s.backupLocalSubtitle(LocalBackupStorage.BACKUP_FOLDER_NAME)
                    )
                    BackupDestinationPill(
                        icon = Icons.Default.CloudUpload,
                        text = "${s.backupDriveTitle} • ${GoogleDriveBackupClient.DRIVE_FOLDER_NAME} (.avb)"
                    )
                }

                OutlinedButton(
                    onClick = onOpenRestore,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_restore_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.splashRestoreBtn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupDestinationPill(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = adaptiveCardColor(),
        border = glassCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
