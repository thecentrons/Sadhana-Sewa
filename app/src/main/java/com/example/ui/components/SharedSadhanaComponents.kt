package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.SadhanaScoringEngine
import com.example.domain.WeeklyFeedback
import com.example.domain.WeeklySadhanaSummary
import com.example.ui.theme.DevotionalSuccess
import com.example.ui.theme.DevotionalWarning
import java.util.Locale

@Composable
fun WeeklySadhanaCardView(
    summary: WeeklySadhanaSummary,
    feedback: WeeklyFeedback?,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Week Navigation & Grand Score Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onPreviousWeek,
                        modifier = Modifier.testTag("week_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous week"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = summary.monthYearLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = summary.weekRangeLabel,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.testTag("week_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next week"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Final Score Hero Row
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "FINAL WEEKLY SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${summary.grandTotal} / 3500",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.daysLoggedCount} of 7 days logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = scoreColorForPercent(summary.grandPercent),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", summary.grandPercent)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two-Half Breakdown Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionSubtotalPill(
                        label = "Spiritual",
                        pointsText = "${summary.spiritualTotal} / 1750",
                        percent = summary.spiritualPercent,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SectionSubtotalPill(
                        label = "Discipline & Study",
                        pointsText = "${summary.disciplineTotal} / 1750",
                        percent = summary.disciplinePercent,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Counsellor-Set Targets Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Counsellor targets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Targets: Read ${SadhanaScoringEngine.formatTargetHours(summary.targets.bookReadingTargetMinutes)}/wk • Hear ${SadhanaScoringEngine.formatTargetHours(summary.targets.personalHearingTargetMinutes)}/wk • Study ${SadhanaScoringEngine.formatTargetHours(summary.targets.studyDailyTargetMinutes)}/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Scrollable Mon-Sun Matrix (faithfully mirrors the ISKCON Sadhana Card spreadsheet)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Mon – Sun Sadhana Card (swipe horizontally)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(136.dp)
                        )
                        summary.days.forEach { day ->
                            Column(
                                modifier = Modifier.width(44.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.dayShortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.background
                                )
                                Text(
                                    text = day.dayOfMonth,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(74.dp)
                        )
                    }

                    // Spiritual Section Rows
                    MatrixSectionDivider("SPIRITUAL SADHANA (1750 MAX)", MaterialTheme.colorScheme.primary)
                    MatrixRow(
                        title = "Morning Program",
                        values = summary.days.map { if (it.logged) "${it.morningProgramPts}" else "–" },
                        total = "${summary.morningProgramTotal}/210"
                    )
                    MatrixRow(
                        title = "Chanting",
                        values = summary.days.map { if (it.logged) "${it.chantingPts}" else "–" },
                        total = "${summary.chantingTotal}/490"
                    )
                    MatrixRow(
                        title = "Book Reading (m)",
                        values = summary.days.map { if (it.logged) "${it.bookReadingMinutes}m" else "–" },
                        total = "${summary.bookReadingPoints}/490",
                        highlight = true
                    )
                    MatrixRow(
                        title = "Morning Class",
                        values = summary.days.map { if (it.logged) "${it.morningClassPts}" else "–" },
                        total = "${summary.morningClassTotal}/210"
                    )
                    MatrixRow(
                        title = "Personal Hear (m)",
                        values = summary.days.map { if (it.logged) "${it.personalHearingMinutes}m" else "–" },
                        total = "${summary.personalHearingPoints}/210",
                        highlight = true
                    )
                    MatrixRow(
                        title = "Shloka / Song",
                        values = summary.days.map { if (it.logged) "${it.shlokaPts}" else "–" },
                        total = "${summary.shlokaTotal}/140"
                    )

                    // Discipline & Study Section Rows
                    MatrixSectionDivider("DISCIPLINE & STUDY (1750 MAX)", MaterialTheme.colorScheme.secondary)
                    MatrixRow(
                        title = "Study / Prep",
                        values = summary.days.map { if (it.logged) "${it.studyPts}" else "–" },
                        total = "${summary.studyTotal}/210"
                    )
                    MatrixRow(
                        title = "Study Minutes",
                        values = summary.days.map { if (it.logged) "${it.studyMinutes}m" else "–" },
                        total = SadhanaScoringEngine.formatMinutesAsHrsMin(summary.studyTotalMinutes)
                    )
                    MatrixRow(
                        title = "To Bed",
                        values = summary.days.map { if (it.logged) "${it.toBedPts}" else "–" },
                        total = "${summary.toBedTotal}/490"
                    )
                    MatrixRow(
                        title = "Wake Up",
                        values = summary.days.map { if (it.logged) "${it.wakeUpPts}" else "–" },
                        total = "${summary.wakeUpTotal}/490"
                    )
                    MatrixRow(
                        title = "Day Rest",
                        values = summary.days.map { if (it.logged) "${it.dayRestPts}" else "–" },
                        total = "${summary.dayRestTotal}/490"
                    )
                    MatrixRow(
                        title = "Card On Time",
                        values = summary.days.map { if (it.logged) "${it.cardFilledPts}" else "–" },
                        total = "${summary.cardFilledTotal}/70"
                    )
                    MatrixRow(
                        title = "Seva (bonus)",
                        values = summary.days.map { if (it.logged && it.sevaMinutes > 0) "${it.sevaMinutes}m" else "–" },
                        total = SadhanaScoringEngine.formatMinutesAsHrsMin(summary.sevaTotalMinutes),
                        highlight = true
                    )
                }
            }
        }

        // Counsellor Weekly Feedback Card (if present)
        if (feedback != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Counsellor guidance",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Counsellor Guidance • ${feedback.counsellorName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    if (feedback.blessingTag.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = feedback.blessingTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = feedback.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionSubtotalPill(
    label: String,
    pointsText: String,
    percent: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pointsText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.16f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", percent)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MatrixSectionDivider(title: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatrixRow(
    title: String,
    values: List<String>,
    total: String,
    highlight: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .background(
                    if (highlight) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    else Color.Transparent
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(136.dp)
            )
            values.forEach { cell ->
                Text(
                    text = cell,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (cell == "–") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(44.dp)
                )
            }
            Text(
                text = total,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(74.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

fun scoreColorForPercent(percent: Double): Color = when {
    percent >= 75.0 -> DevotionalSuccess
    percent >= 50.0 -> DevotionalWarning
    else -> Color(0xFFC85A17)
}

@Composable
fun MinutesStepperRow(
    label: String,
    minutes: Int,
    stepMinutes: Int = 15,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${minutes} min (${SadhanaScoringEngine.formatMinutesAsHrsMin(minutes)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onMinutesChange((minutes - stepMinutes).coerceAtLeast(0)) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            IconButton(
                onClick = { onMinutesChange((minutes + stepMinutes).coerceAtMost(1440)) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppComposerSheet(
    counsellorName: String,
    counsellorWhatsapp: String,
    initialMessage: String,
    onDismiss: () -> Unit,
    onSend: (phone: String, message: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }
    var phoneOverride by remember(counsellorWhatsapp) { mutableStateOf(counsellorWhatsapp) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = DevotionalSuccess,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Message Counsellor on WhatsApp",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (counsellorName.isNotBlank()) "To: $counsellorName" else "Direct WhatsApp / Share",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = phoneOverride,
                onValueChange = { phoneOverride = it.take(24) },
                label = { Text("Counsellor WhatsApp Number (with country code)") },
                placeholder = { Text("e.g., +919876543210") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(2000) },
                label = { Text("Editable Message") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSend(phoneOverride, message) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("send_whatsapp_confirm_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DevotionalSuccess)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (phoneOverride.filter { it.isDigit() }.length >= 7) {
                        "Open in WhatsApp"
                    } else {
                        "Share via Apps"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
