package com.example.ui.devotee

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.ReminderSettings
import com.example.domain.SadhanaScoringEngine
import com.example.domain.WeeklySadhanaSummary
import com.example.ui.components.ServerAndUpdateSettingsCard
import com.example.ui.components.WeeklySadhanaCardView
import com.example.ui.components.scoreColorForPercent
import com.example.ui.theme.DevotionalSuccess
import com.example.viewmodel.DevoteeUiState
import java.util.Locale

@Composable
fun DevoteeWeeklyTabScreen(
    state: DevoteeUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRefreshCloud: () -> Unit,
    onOpenWhatsAppWeekly: () -> Unit
) {
    val summary = state.selectedWeekSummary
    val feedback = state.feedbacksByWeek[summary.weekStartKey]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Weekly Sadhana Card",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Tap Refresh for latest counsellor targets & notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshCloud,
                        enabled = !state.refreshing,
                        modifier = Modifier.testTag("weekly_refresh_button")
                    ) {
                        if (state.refreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh targets and feedback")
                        }
                    }
                }
            }

            item {
                WeeklySadhanaCardView(
                    summary = summary,
                    feedback = feedback,
                    onPreviousWeek = onPreviousWeek,
                    onNextWeek = onNextWeek
                )
            }

            item {
                Button(
                    onClick = onOpenWhatsAppWeekly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("whatsapp_share_weekly_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DevotionalSuccess)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Weekly Card to Counsellor on WhatsApp",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DevoteeHistoryScreen(
    state: DevoteeUiState,
    onSelectWeekAndOpenCard: (String) -> Unit,
    onRefreshCloud: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val weeks = state.recentWeekSummaries
    val journalLogs = remember(state.allLogs) {
        state.allLogs.filter { it.shlokaLearned || it.shlokaNote.isNotBlank() || it.sevaMinutes > 0 || it.sevaNote.isNotBlank() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sadhana History",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${state.allLogs.size} total daily check-ins recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshCloud,
                        enabled = !state.refreshing,
                        modifier = Modifier.testTag("history_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh history from cloud")
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("history_trends_tab")
                    ) {
                        Text("Weekly Trends")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("history_journal_tab")
                    ) {
                        Text("Shloka & Seva Journal")
                    }
                }
            }

            if (selectedTab == 0) {
                // Bar Chart Card
                item {
                    WeeklyTrendBarChartCard(weeks = weeks.take(6).reversed())
                }

                // Past Weeks List
                items(weeks, key = { it.weekStartKey }) { wk ->
                    val hasFeedback = state.feedbacksByWeek.containsKey(wk.weekStartKey)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWeekAndOpenCard(wk.weekStartKey) }
                            .testTag("history_week_${wk.weekStartKey}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = wk.weekRangeLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${wk.daysLoggedCount}/7 days • Spiritual ${wk.spiritualTotal} • Discipline ${wk.disciplineTotal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hasFeedback) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "★ Counsellor feedback attached",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scoreColorForPercent(wk.grandPercent).copy(alpha = 0.14f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", wk.grandPercent)}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColorForPercent(wk.grandPercent)
                                    )
                                    Text(
                                        text = "${wk.grandTotal}/3500",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (journalLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolunteerActivism,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your Shloka & Seva Journal is Empty",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Whenever you log a memorized shloka, Vaishnava song, or devotional seva in Today's check-in, it will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(journalLogs, key = { it.dateKey }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = SadhanaScoringEngine.formatReadableDate(log.dateKey),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (log.shlokaLearned || log.shlokaNote.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "Shloka",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = log.shlokaNote.ifBlank { "Memorized a new Shloka / Vaishnava Song (+20)" },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                if (log.sevaMinutes > 0 || log.sevaNote.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Filled.VolunteerActivism,
                                            contentDescription = "Seva",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Seva (${SadhanaScoringEngine.formatMinutesAsHrsMin(log.sevaMinutes)}): ${log.sevaNote.ifBlank { "Devotional service offered" }}",
                                            style = MaterialTheme.typography.bodyMedium
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
private fun WeeklyTrendBarChartCard(weeks: List<WeeklySadhanaSummary>) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "6-Week Consistency Trend (% of 3,500)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (weeks.isEmpty()) return@Canvas
                    val slotWidth = size.width / weeks.size
                    val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                    val maxBarHeight = size.height - 8.dp.toPx()

                    weeks.forEachIndexed { idx, wk ->
                        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                        // Background track bar
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, maxBarHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                        val fillRatio = (wk.grandPercent / 100.0).toFloat().coerceIn(0f, 1f)
                        val barH = (maxBarHeight * fillRatio).coerceAtLeast(if (wk.grandTotal > 0) 6.dp.toPx() else 0f)
                        if (barH > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, maxBarHeight - barH),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeks.forEach { wk ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${wk.grandPercent.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = wk.weekRangeLabel.substringBefore(" –"),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevoteeProfileScreen(
    state: DevoteeUiState,
    busy: Boolean,
    onLinkCounsellor: (String) -> Unit,
    onSaveReminders: (ReminderSettings) -> Unit,
    onSyncNow: () -> Unit,
    onRefreshCloud: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var inviteInput by remember { mutableStateOf("") }
    var reminderDraft by remember(state.reminderSettings) { mutableStateOf(state.reminderSettings) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val updated = reminderDraft.copy(enabled = true)
            reminderDraft = updated
            onSaveReminders(updated)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Account Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = state.profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.profile.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = state.profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Role: Devotee",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Linked Counsellor Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assigned Counsellor", style = MaterialTheme.typography.titleMedium)
                        }
                        if (state.profile.counsellorUid.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = state.profile.counsellorName.ifBlank { "Counsellor Linked" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (state.profile.counsellorWhatsapp.isNotBlank()) {
                                            "WhatsApp: ${state.profile.counsellorWhatsapp}"
                                        } else {
                                            "WhatsApp number can be entered in composer"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Not linked yet. Enter your counsellor's invite code so they can view your sadhana and personalize your targets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inviteInput,
                                onValueChange = { inviteInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
                                label = { Text(if (state.profile.counsellorUid.isBlank()) "Counsellor Invite Code" else "Change Invite Code") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_invite_code_input")
                            )
                            Button(
                                onClick = {
                                    onLinkCounsellor(inviteInput)
                                    inviteInput = ""
                                },
                                enabled = !busy && inviteInput.length >= 4,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("profile_link_counsellor_button")
                            ) {
                                Text("Link")
                            }
                        }
                    }
                }
            }

            // Read-Only Counsellor-Set Targets Card
            item {
                val t = state.profile.targets
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("My Personalized Sadhana Targets", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Set exclusively by your Counsellor according to your spiritual stage so scores stay uniform out of 3,500.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TargetInfoRow("Book Reading (Weekly)", "${SadhanaScoringEngine.formatTargetHours(t.bookReadingTargetMinutes)} (${t.readingTierLabel}) → 490 pts")
                        TargetInfoRow("Personal Hearing (Weekly)", "${SadhanaScoringEngine.formatTargetHours(t.personalHearingTargetMinutes)} → 210 pts")
                        TargetInfoRow("Study / Prep (Daily)", "${SadhanaScoringEngine.formatTargetHours(t.studyDailyTargetMinutes)} → 30 pts/day")
                        TargetInfoRow("Daily Japa Target", "${t.japaRoundsTarget} rounds/day")
                    }
                }
            }

            // Daily & Follow-Up Reminders Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Daily & Follow-Up Reminders", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Automatically skipped once today's card is saved",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminderDraft.enabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val updated = reminderDraft.copy(enabled = enable)
                                        reminderDraft = updated
                                        onSaveReminders(updated)
                                    }
                                },
                                modifier = Modifier.testTag("reminder_enable_switch")
                            )
                        }

                        if (reminderDraft.enabled) {
                            // Primary Time Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Primary Check-In Time", style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val totalMin = (reminderDraft.primaryHour * 60 + reminderDraft.primaryMinute - 15 + 1440) % 1440
                                        reminderDraft = reminderDraft.copy(primaryHour = totalMin / 60, primaryMinute = totalMin % 60)
                                    }) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Earlier by 15 minutes")
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        val h12 = when {
                                            reminderDraft.primaryHour == 0 -> 12
                                            reminderDraft.primaryHour > 12 -> reminderDraft.primaryHour - 12
                                            else -> reminderDraft.primaryHour
                                        }
                                        val amPm = if (reminderDraft.primaryHour < 12) "AM" else "PM"
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d %s", h12, reminderDraft.primaryMinute, amPm),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        val totalMin = (reminderDraft.primaryHour * 60 + reminderDraft.primaryMinute + 15) % 1440
                                        reminderDraft = reminderDraft.copy(primaryHour = totalMin / 60, primaryMinute = totalMin % 60)
                                    }) {
                                        Icon(Icons.Filled.Add, contentDescription = "Later by 15 minutes")
                                    }
                                }
                            }

                            // Follow-up Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Follow-Up Reminders if Pending", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Re-notify if you still haven't logged today's sadhana",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = reminderDraft.followUpEnabled,
                                    onCheckedChange = { v -> reminderDraft = reminderDraft.copy(followUpEnabled = v) }
                                )
                            }

                            if (reminderDraft.followUpEnabled) {
                                Text("Follow-Up Interval", style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(30, 45, 60).forEach { mins ->
                                        FilterChip(
                                            selected = reminderDraft.followUpIntervalMinutes == mins,
                                            onClick = { reminderDraft = reminderDraft.copy(followUpIntervalMinutes = mins) },
                                            label = { Text("Every ${mins}m") }
                                        )
                                    }
                                }
                                Text("Max Follow-Up Nudges", style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2, 3).forEach { count ->
                                        FilterChip(
                                            selected = reminderDraft.maxFollowUps == count,
                                            onClick = { reminderDraft = reminderDraft.copy(maxFollowUps = count) },
                                            label = { Text("$count follow-up${if (count > 1) "s" else ""}") }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onSaveReminders(reminderDraft) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_reminders_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Apply Reminder Schedule")
                            }
                        }
                    }
                }
            }

            // Server & App Update Settings Card
            item {
                ServerAndUpdateSettingsCard()
            }

            // Sync & Sign Out
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_sync_now_button")
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.unsyncedCount > 0) "Sync (${state.unsyncedCount})" else "Sync Cloud")
                    }
                    OutlinedButton(
                        onClick = onRefreshCloud,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_refresh_cloud_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pull Targets")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("devotee_sign_out_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TargetInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
