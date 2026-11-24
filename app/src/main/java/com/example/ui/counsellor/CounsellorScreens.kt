package com.example.ui.counsellor

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SadhanaScoringEngine
import com.example.domain.SadhanaTargets
import com.example.domain.UserProfile
import com.example.ui.components.ServerAndUpdateSettingsCard
import com.example.ui.components.WeeklySadhanaCardView
import com.example.ui.components.scoreColorForPercent
import com.example.viewmodel.CounsellorUiState
import java.util.Locale

@Composable
fun CounsellorDashboardScreen(
    state: CounsellorUiState,
    onRefresh: () -> Unit,
    onSelectDevotee: (String) -> Unit,
    onCopiedInviteCode: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val roster = state.rosterEntries
    val avgPercent = if (roster.isNotEmpty()) {
        roster.map { it.currentWeekSummary.grandPercent }.average()
    } else 0.0

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
            // Top Header with Manual Refresh (zero realtime listeners)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Counsellor Dashboard",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "On-demand refresh • Zero idle cloud cost",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.loading,
                        modifier = Modifier.testTag("counsellor_refresh_button")
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh devotee roster")
                        }
                    }
                }
            }

            // Shareable Invite Code Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR COUNSELLOR INVITE CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.counsellorProfile.inviteCode.ifBlank { "—" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Share this code with devotees so they can join your care group.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(state.counsellorProfile.inviteCode))
                                onCopiedInviteCode()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("copy_invite_code_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy invite code", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // Group Overview Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChipCard(
                        title = "Devotees",
                        value = "${roster.size}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricChipCard(
                        title = "This Week Avg",
                        value = "${String.format(Locale.US, "%.1f", avgPercent)}%",
                        modifier = Modifier.weight(1f)
                    )
                    MetricChipCard(
                        title = "Active Today",
                        value = "${roster.count { it.currentWeekSummary.daysLoggedCount > 0 }}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Assigned Devotees (This Week)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (roster.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Devotees Linked Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share your invite code (${state.counsellorProfile.inviteCode}) with devotees. Once they enter it in Sadhana Seva, tap Refresh to view their cards and set their personalized targets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(roster, key = { it.profile.uid }) { entry ->
                    val wk = entry.currentWeekSummary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDevotee(entry.profile.uid) }
                            .testTag("devotee_roster_card_${entry.profile.uid}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = entry.profile.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = entry.profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${wk.daysLoggedCount}/7 days • Read ${SadhanaScoringEngine.formatMinutesAsHrsMin(wk.bookReadingTotalMinutes)}/${SadhanaScoringEngine.formatTargetHours(entry.profile.bookReadingTargetMinutes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Targets: ${entry.profile.readingTierLabel} • Hear ${SadhanaScoringEngine.formatTargetHours(entry.profile.personalHearingTargetMinutes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
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
            }
        }
    }
}

@Composable
private fun MetricChipCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CounsellorDevoteeDetailScreen(
    state: CounsellorUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSaveTargets: (devoteeUid: String, targets: SadhanaTargets) -> Unit,
    onSaveFeedback: (devoteeUid: String, weekStartKey: String, tag: String, note: String) -> Unit
) {
    BackHandler(onBack = onBack)

    val devotee = state.selectedDevotee ?: return
    val summary = state.selectedDevoteeWeekSummary ?: return
    val existingFeedback = state.selectedWeekFeedback

    var showTargetsSheet by remember { mutableStateOf(false) }
    var selectedTag by remember(existingFeedback?.blessingTag, state.selectedWeekStartKey) {
        mutableStateOf(existingFeedback?.blessingTag ?: "Steady Sadhana")
    }
    var feedbackNote by remember(existingFeedback?.note, state.selectedWeekStartKey) {
        mutableStateOf(existingFeedback?.note ?: "")
    }

    val blessingTags = listOf(
        "Steady Sadhana",
        "Wonderful Japa",
        "Great Book Reading",
        "Focus on Early Rest",
        "Morning Program Focus"
    )

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
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("counsellor_detail_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to roster")
                        }
                        Column {
                            Text(devotee.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Assigned Devotee • ${devotee.readingTierLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh devotee card")
                        }
                        Button(
                            onClick = { showTargetsSheet = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("edit_devotee_targets_button")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Targets")
                        }
                    }
                }
            }

            // Full Mon-Sun 3500-Point Weekly Card
            item {
                WeeklySadhanaCardView(
                    summary = summary,
                    feedback = existingFeedback,
                    onPreviousWeek = onPreviousWeek,
                    onNextWeek = onNextWeek
                )
            }

            // Counsellor Weekly Feedback Composer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weekly Counsellor Note (${summary.weekRangeLabel})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blessingTags.forEach { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = feedbackNote,
                            onValueChange = { feedbackNote = it.take(1000) },
                            label = { Text("Encouragement & Guidance Note") },
                            placeholder = { Text("Haribol! Appreciate your steady early japa this week...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("counsellor_feedback_input")
                        )
                        Button(
                            onClick = {
                                onSaveFeedback(devotee.uid, summary.weekStartKey, selectedTag, feedbackNote)
                            },
                            enabled = !state.saving && feedbackNote.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_counsellor_feedback_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (existingFeedback != null) "Update Weekly Note" else "Save Weekly Note")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTargetsSheet) {
        CounsellorTargetEditorSheet(
            devotee = devotee,
            saving = state.saving,
            onDismiss = { showTargetsSheet = false },
            onSave = { newTargets ->
                onSaveTargets(devotee.uid, newTargets)
                showTargetsSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CounsellorTargetEditorSheet(
    devotee: UserProfile,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (SadhanaTargets) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bookMin by remember(devotee) { mutableIntStateOf(devotee.bookReadingTargetMinutes) }
    var hearMin by remember(devotee) { mutableIntStateOf(devotee.personalHearingTargetMinutes) }
    var studyMin by remember(devotee) { mutableIntStateOf(devotee.studyDailyTargetMinutes) }
    var japaRounds by remember(devotee) { mutableIntStateOf(devotee.japaRoundsTarget) }
    var tierLabel by remember(devotee) { mutableStateOf(devotee.readingTierLabel) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Set Sadhana Targets for ${devotee.displayName}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Counsellor-only control: proportional scoring scales weekly Book Reading (490), Personal Hearing (210), and daily Study (30/day) uniformly to 3,500 points based on these targets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Quick Reading Tier Presets", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Starter (3h)", 180, "Starter (3h)"),
                    Triple("Arjuna (5h)", 300, "Arjuna (5h)"),
                    Triple("Bhima (6h)", 360, "Bhima (6h)"),
                    Triple("Dedicated (7h)", 420, "Dedicated (7h)")
                ).forEach { (label, mins, tag) ->
                    FilterChip(
                        selected = bookMin == mins,
                        onClick = {
                            bookMin = mins
                            tierLabel = tag
                        },
                        label = { Text(label) }
                    )
                }
            }

            TargetStepperRow(
                label = "Weekly Book Reading",
                valueText = SadhanaScoringEngine.formatTargetHours(bookMin),
                onMinus = {
                    bookMin = (bookMin - 30).coerceAtLeast(30)
                    tierLabel = "Custom (${SadhanaScoringEngine.formatTargetHours(bookMin)})"
                },
                onPlus = {
                    bookMin = (bookMin + 30).coerceAtMost(2400)
                    tierLabel = "Custom (${SadhanaScoringEngine.formatTargetHours(bookMin)})"
                }
            )

            TargetStepperRow(
                label = "Weekly Personal Hearing",
                valueText = SadhanaScoringEngine.formatTargetHours(hearMin),
                onMinus = { hearMin = (hearMin - 30).coerceAtLeast(30) },
                onPlus = { hearMin = (hearMin + 30).coerceAtMost(1800) }
            )

            TargetStepperRow(
                label = "Daily Study / Prep Target",
                valueText = SadhanaScoringEngine.formatTargetHours(studyMin),
                onMinus = { studyMin = (studyMin - 30).coerceAtLeast(30) },
                onPlus = { studyMin = (studyMin + 30).coerceAtMost(720) }
            )

            TargetStepperRow(
                label = "Daily Japa Rounds Target",
                valueText = "$japaRounds rounds",
                onMinus = { japaRounds = (japaRounds - 1).coerceAtLeast(1) },
                onPlus = { japaRounds = (japaRounds + 1).coerceAtMost(64) }
            )

            Button(
                onClick = {
                    onSave(
                        SadhanaTargets(
                            bookReadingTargetMinutes = bookMin,
                            personalHearingTargetMinutes = hearMin,
                            studyDailyTargetMinutes = studyMin,
                            japaRoundsTarget = japaRounds,
                            readingTierLabel = tierLabel
                        )
                    )
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_devotee_targets_confirm_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Personalized Targets")
            }
        }
    }
}

@Composable
private fun TargetStepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(86.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            IconButton(onClick = onPlus) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
fun CounsellorProfileScreen(
    profile: UserProfile,
    busy: Boolean,
    onSaveContact: (displayName: String, whatsappPhone: String) -> Unit,
    onCopiedInviteCode: () -> Unit,
    onSignOut: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var whatsapp by remember(profile.whatsappPhone) { mutableStateOf(profile.whatsappPhone) }

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
                Text("Counsellor Profile", style = MaterialTheme.typography.headlineSmall)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Invite Code: ${profile.inviteCode}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(profile.inviteCode))
                                onCopiedInviteCode()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Invite Code")
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(80) },
                            label = { Text("Counsellor Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it.take(24) },
                            label = { Text("WhatsApp Number (with country code)") },
                            placeholder = { Text("+919876543210") },
                            supportingText = {
                                Text("Automatically shared with new devotees who join via your invite code so they can send sadhana updates directly to your WhatsApp.")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("counsellor_whatsapp_input")
                        )

                        Button(
                            onClick = { onSaveContact(name, whatsapp) },
                            enabled = !busy && name.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("counsellor_save_profile_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Save Profile & WhatsApp")
                        }
                    }
                }
            }

            item {
                ServerAndUpdateSettingsCard()
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("counsellor_sign_out_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
