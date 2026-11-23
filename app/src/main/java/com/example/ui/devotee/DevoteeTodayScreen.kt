package com.example.ui.devotee

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ChantingSlot
import com.example.domain.DailySadhanaLog
import com.example.domain.DayRestSlot
import com.example.domain.SadhanaScoringEngine
import com.example.domain.ToBedSlot
import com.example.domain.WakeUpSlot
import com.example.ui.components.MinutesStepperRow
import com.example.ui.theme.DevotionalSuccess
import com.example.viewmodel.DevoteeUiState
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevoteeTodayScreen(
    state: DevoteeUiState,
    onSelectDate: (String) -> Unit,
    onUpdateDraft: ((DailySadhanaLog) -> DailySadhanaLog) -> Unit,
    onOpenFocusJapa: () -> Unit,
    onAdjustJapaRounds: (Int) -> Unit,
    onToggleReadingTimer: () -> Unit,
    onResetReadingTimer: () -> Unit,
    onCommitReadingTimer: () -> Unit,
    onSaveCheckIn: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenWhatsAppDaily: () -> Unit
) {
    val draft = state.draft
    val targets = state.profile.targets
    val weekSummary = state.currentWeekSummary
    val todayBreakdown = SadhanaScoringEngine.computeDailyBreakdown(state.selectedDateKey, draft, targets)

    // Live offline Book Reading stopwatch ticker (zero cloud calls)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.offlineTimer.readingTimerRunning) {
        while (state.offlineTimer.readingTimerRunning) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val liveReadingSeconds = state.offlineTimer.readingAccumulatedSeconds + if (
        state.offlineTimer.readingTimerRunning && state.offlineTimer.readingTimerStartMillis > 0L
    ) {
        ((nowMillis - state.offlineTimer.readingTimerStartMillis) / 1000L).coerceAtLeast(0L)
    } else {
        0L
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
            // 1. Header & Sync / Reminder Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hare Krishna, ${state.profile.displayName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Offline-ready daily sadhana check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onSyncNow,
                            shape = RoundedCornerShape(50),
                            color = if (state.unsyncedCount > 0) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.testTag("today_sync_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.unsyncedCount > 0) Icons.Filled.CloudUpload else Icons.Filled.CloudDone,
                                    contentDescription = "Cloud sync status",
                                    tint = if (state.unsyncedCount > 0) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (state.unsyncedCount > 0) "${state.unsyncedCount} Sync" else "Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenReminders,
                            modifier = Modifier.testTag("today_reminders_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Configure daily check-in reminders",
                                tint = if (state.reminderSettings.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // 2. Date Switcher & Daily/Weekly Score Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { onSelectDate(SadhanaScoringEngine.shiftDate(state.selectedDateKey, -1)) },
                                    modifier = Modifier.testTag("date_prev_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                                }
                                Text(
                                    text = SadhanaScoringEngine.formatReadableDate(state.selectedDateKey),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        val next = SadhanaScoringEngine.shiftDate(state.selectedDateKey, +1)
                                        if (next <= SadhanaScoringEngine.todayDateKey()) {
                                            onSelectDate(next)
                                        }
                                    },
                                    enabled = state.selectedDateKey < SadhanaScoringEngine.todayDateKey(),
                                    modifier = Modifier.testTag("date_next_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "WEEKLY PACE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${weekSummary.grandTotal} / 3500 (${String.format(Locale.US, "%.1f", weekSummary.grandPercent)}%)",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Today's fixed pts: ${todayBreakdown.fixedDailyScore} / 400",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${state.currentStreak}d",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SPIRITUAL SECTION HEADER
            item {
                SectionBanner(
                    title = "SPIRITUAL SADHANA",
                    subtitle = "${weekSummary.spiritualTotal} / 1750 weekly pts",
                    accent = MaterialTheme.colorScheme.primary
                )
            }

            // 4. Japa Chanting Card with "Open Focus Japa" Dedicated Window Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.SelfImprovement,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Japa Chanting",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${draft.japaRounds} / ${targets.japaRoundsTarget} rounds • Bead ${draft.japaBeads}/108",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "+${draft.chantingEnum.points} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenFocusJapa,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("open_focus_japa_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.OpenInFull, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Focus Japa")
                            }
                            OutlinedButton(
                                onClick = { onAdjustJapaRounds(+1) },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp)
                                    .testTag("quick_plus_round_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add one round", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("1 Rnd")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Completion Time Bracket",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChantingSlot.entries.forEach { slot ->
                                FilterChip(
                                    selected = draft.chantingEnum == slot,
                                    onClick = { onUpdateDraft { it.copy(chantingSlot = slot.name) } },
                                    label = { Text("${slot.shortLabel} (${slot.points})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Offline Book Reading Stopwatch & Daily Minutes Card
            item {
                val readProgress = (weekSummary.bookReadingTotalMinutes.toFloat() /
                    targets.bookReadingTargetMinutes.coerceAtLeast(30).toFloat()).coerceIn(0f, 1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Book Reading (Offline Timer)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Target: ${SadhanaScoringEngine.formatTargetHours(targets.bookReadingTargetMinutes)}/wk (${targets.readingTierLabel}) • Set by Counsellor",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weekly proportional progress bar
                        LinearProgressIndicator(
                            progress = { readProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This week: ${SadhanaScoringEngine.formatMinutesAsHrsMin(weekSummary.bookReadingTotalMinutes)} / ${SadhanaScoringEngine.formatTargetHours(targets.bookReadingTargetMinutes)} → ${weekSummary.bookReadingPoints} / 490 pts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Offline Live Stopwatch Strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Reading timer",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val mins = liveReadingSeconds / 60
                                    val secs = liveReadingSeconds % 60
                                    Text(
                                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = onToggleReadingTimer,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("reading_timer_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (state.offlineTimer.readingTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (state.offlineTimer.readingTimerRunning) "Pause timer" else "Start timer",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (state.offlineTimer.readingTimerRunning) "Pause" else "Start")
                                    }
                                    if (liveReadingSeconds > 0L) {
                                        OutlinedButton(
                                            onClick = onCommitReadingTimer,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("reading_timer_commit_button")
                                        ) {
                                            Text("+Add")
                                        }
                                        IconButton(onClick = onResetReadingTimer) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "Reset reading timer")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        MinutesStepperRow(
                            label = "Today's Reading",
                            minutes = draft.bookReadingMinutes,
                            stepMinutes = 10,
                            onMinutesChange = { m -> onUpdateDraft { it.copy(bookReadingMinutes = m) } }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draft.bookTitle,
                            onValueChange = { txt -> onUpdateDraft { it.copy(bookTitle = txt.take(120)) } },
                            label = { Text("Book Name (Optional)") },
                            placeholder = { Text("e.g., Bhagavad-gita As It Is / Srimad-Bhagavatam") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 6. Morning Program, Morning Class, Personal Hearing & Shloka Card
            item {
                val hearProgress = (weekSummary.personalHearingTotalMinutes.toFloat() /
                    targets.personalHearingTargetMinutes.coerceAtLeast(30).toFloat()).coerceIn(0f, 1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToggleRuleRow(
                            title = "Morning Program (+30 pts)",
                            subtitle = "Before 4:30 AM • MA (before 3rd conch), NA & TA",
                            checked = draft.morningProgramAttended,
                            onCheckedChange = { v -> onUpdateDraft { it.copy(morningProgramAttended = v) } },
                            testTag = "morning_program_switch"
                        )
                        ToggleRuleRow(
                            title = "Morning Class (+30 pts)",
                            subtitle = "Attended scheduled Srimad-Bhagavatam class",
                            checked = draft.morningClassAttended,
                            onCheckedChange = { v -> onUpdateDraft { it.copy(morningClassAttended = v) } },
                            testTag = "morning_class_switch"
                        )

                        Column {
                            MinutesStepperRow(
                                label = "Personal Hearing (Daily)",
                                minutes = draft.personalHearingMinutes,
                                stepMinutes = 15,
                                onMinutesChange = { m -> onUpdateDraft { it.copy(personalHearingMinutes = m) } }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { hearProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weekly: ${SadhanaScoringEngine.formatMinutesAsHrsMin(weekSummary.personalHearingTotalMinutes)} / ${SadhanaScoringEngine.formatTargetHours(targets.personalHearingTargetMinutes)} (Counsellor target) → ${weekSummary.personalHearingPoints} / 210 pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ToggleRuleRow(
                            title = "New Shloka / Vaishnava Song (+20 pts)",
                            subtitle = "Memorized a verse or Vaishnava bhajan today",
                            checked = draft.shlokaLearned,
                            onCheckedChange = { v -> onUpdateDraft { it.copy(shlokaLearned = v) } },
                            testTag = "shloka_switch"
                        )
                        if (draft.shlokaLearned || draft.shlokaNote.isNotBlank()) {
                            OutlinedTextField(
                                value = draft.shlokaNote,
                                onValueChange = { txt -> onUpdateDraft { it.copy(shlokaNote = txt.take(500)) } },
                                label = { Text("Shloka / Song Journal Note") },
                                placeholder = { Text("e.g., BG 2.13 or Gurvastakam verse 3...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 7. DISCIPLINE & STUDY SECTION HEADER
            item {
                SectionBanner(
                    title = "DISCIPLINE, REST & SEVA",
                    subtitle = "${weekSummary.disciplineTotal} / 1750 weekly pts",
                    accent = MaterialTheme.colorScheme.secondary
                )
            }

            // 8. Study, Sleep, Wake Up, Day Rest, Card On Time & Seva Card
            item {
                val studyPts = SadhanaScoringEngine.computeStudyPoints(draft.studyMinutes, targets.studyDailyTargetMinutes)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            MinutesStepperRow(
                                label = "Study / Prep (+$studyPts / 30 pts)",
                                minutes = draft.studyMinutes,
                                stepMinutes = 15,
                                onMinutesChange = { m -> onUpdateDraft { it.copy(studyMinutes = m) } }
                            )
                            Text(
                                text = "Daily target: ${SadhanaScoringEngine.formatTargetHours(targets.studyDailyTargetMinutes)} (set by Counsellor) • 100% = 30, 50%+ = 20, 25%+ = 10",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        BracketChipGroup(
                            title = "To Bed (+${draft.toBedEnum.points} pts)",
                            options = ToBedSlot.entries.map { it to "${it.shortLabel} (${it.points})" },
                            selected = draft.toBedEnum,
                            onSelect = { slot -> onUpdateDraft { it.copy(toBedSlot = slot.name) } }
                        )

                        BracketChipGroup(
                            title = "Wake Up (+${draft.wakeUpEnum.points} pts)",
                            options = WakeUpSlot.entries.map { it to "${it.shortLabel} (${it.points})" },
                            selected = draft.wakeUpEnum,
                            onSelect = { slot -> onUpdateDraft { it.copy(wakeUpSlot = slot.name) } }
                        )

                        BracketChipGroup(
                            title = "Day Rest (+${draft.dayRestEnum.points} pts)",
                            options = DayRestSlot.entries.map { it to "${it.shortLabel} (${it.points})" },
                            selected = draft.dayRestEnum,
                            onSelect = { slot -> onUpdateDraft { it.copy(dayRestSlot = slot.name) } }
                        )

                        ToggleRuleRow(
                            title = "Filled Sadhana Card On Time (+10 pts)",
                            subtitle = "Filled before next day's morning program ends (Prasadam)",
                            checked = draft.cardFilledOnTime,
                            onCheckedChange = { v -> onUpdateDraft { it.copy(cardFilledOnTime = v) } },
                            testTag = "card_on_time_switch"
                        )

                        Column {
                            MinutesStepperRow(
                                label = "Seva Time (Devotional Service)",
                                minutes = draft.sevaMinutes,
                                stepMinutes = 15,
                                onMinutesChange = { m -> onUpdateDraft { it.copy(sevaMinutes = m) } }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = draft.sevaNote,
                                onValueChange = { txt -> onUpdateDraft { it.copy(sevaNote = txt.take(500)) } },
                                label = { Text("Seva Journal Note (Optional)") },
                                placeholder = { Text("e.g., Prasadam distribution, Deity seva, book stall...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 9. Primary Save & WhatsApp Share Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSaveCheckIn,
                        enabled = !state.syncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_daily_sadhana_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (state.syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Sadhana Check-In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenWhatsAppDaily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("whatsapp_share_daily_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, DevotionalSuccess),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DevotionalSuccess)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.profile.counsellorName.isNotBlank()) {
                                "Message ${state.profile.counsellorName} on WhatsApp"
                            } else {
                                "Compose WhatsApp Message to Counsellor"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionBanner(title: String, subtitle: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleRuleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> BracketChipGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}
