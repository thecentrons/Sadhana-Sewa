package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object WhatsAppShareHelper {

    fun buildDailyMessage(
        devoteeName: String,
        counsellorName: String,
        log: DailySadhanaLog,
        targets: SadhanaTargets
    ): String {
        val greetingName = counsellorName.ifBlank { "Prabhuji" }
        val daily = SadhanaScoringEngine.computeDailyBreakdown(log.dateKey, log, targets)
        val sb = StringBuilder()
        sb.appendLine("Hare Krishna $greetingName,")
        sb.appendLine("Please accept my humble obeisances. All glories to Srila Prabhupada!")
        sb.appendLine()
        sb.appendLine("Daily Sadhana Report (${SadhanaScoringEngine.formatReadableDate(log.dateKey)})")
        sb.appendLine("Devotee: $devoteeName")
        sb.appendLine("• Morning Program: ${if (log.morningProgramAttended) "Yes (30)" else "Missed (0)"}")
        sb.appendLine("• Japa: ${log.japaRounds}/${targets.japaRoundsTarget} rounds (${log.chantingEnum.shortLabel} = ${daily.chantingPts})")
        sb.appendLine("• Book Reading: ${SadhanaScoringEngine.formatMinutesAsHrsMin(log.bookReadingMinutes)}${if (log.bookTitle.isNotBlank()) " (${log.bookTitle})" else ""}")
        sb.appendLine("• Morning Class: ${if (log.morningClassAttended) "Yes (30)" else "No (0)"}")
        sb.appendLine("• Personal Hearing: ${SadhanaScoringEngine.formatMinutesAsHrsMin(log.personalHearingMinutes)}")
        if (log.shlokaLearned) {
            sb.appendLine("• Shloka/Song: Yes (20)${if (log.shlokaNote.isNotBlank()) " – ${log.shlokaNote}" else ""}")
        }
        sb.appendLine("• Study: ${SadhanaScoringEngine.formatMinutesAsHrsMin(log.studyMinutes)} (${daily.studyPts} pts)")
        sb.appendLine("• Rest Schedule: Bed ${log.toBedEnum.shortLabel} | Wake ${log.wakeUpEnum.shortLabel} | Day Rest ${log.dayRestEnum.shortLabel}")
        if (log.sevaMinutes > 0) {
            sb.appendLine("• Seva: ${SadhanaScoringEngine.formatMinutesAsHrsMin(log.sevaMinutes)}${if (log.sevaNote.isNotBlank()) " – ${log.sevaNote}" else ""}")
        }
        sb.appendLine()
        sb.append("Your servant, $devoteeName")
        return sb.toString()
    }

    fun buildWeeklyMessage(
        devoteeName: String,
        counsellorName: String,
        summary: WeeklySadhanaSummary
    ): String {
        val greetingName = counsellorName.ifBlank { "Prabhuji" }
        val sb = StringBuilder()
        sb.appendLine("Hare Krishna $greetingName,")
        sb.appendLine("Please accept my humble obeisances. All glories to Srila Prabhupada!")
        sb.appendLine()
        sb.appendLine("Weekly Sadhana Card (${summary.weekRangeLabel})")
        sb.appendLine("Devotee: $devoteeName (${summary.daysLoggedCount}/7 days logged)")
        sb.appendLine("• Final Score: ${summary.grandTotal}/3500 (${String.format(Locale.US, "%.1f", summary.grandPercent)}%)")
        sb.appendLine("• Spiritual: ${summary.spiritualTotal}/1750 (${String.format(Locale.US, "%.1f", summary.spiritualPercent)}%)")
        sb.appendLine("• Discipline: ${summary.disciplineTotal}/1750 (${String.format(Locale.US, "%.1f", summary.disciplinePercent)}%)")
        sb.appendLine("• Book Reading: ${SadhanaScoringEngine.formatMinutesAsHrsMin(summary.bookReadingTotalMinutes)} / ${SadhanaScoringEngine.formatTargetHours(summary.targets.bookReadingTargetMinutes)} (${summary.bookReadingPoints}/490)")
        sb.appendLine("• Personal Hearing: ${SadhanaScoringEngine.formatMinutesAsHrsMin(summary.personalHearingTotalMinutes)} / ${SadhanaScoringEngine.formatTargetHours(summary.targets.personalHearingTargetMinutes)} (${summary.personalHearingPoints}/210)")
        if (summary.sevaTotalMinutes > 0) {
            sb.appendLine("• Seva Logged: ${SadhanaScoringEngine.formatMinutesAsHrsMin(summary.sevaTotalMinutes)}")
        }
        sb.appendLine()
        sb.append("Seeking your guidance and blessings, $devoteeName")
        return sb.toString()
    }

    /**
     * Opens WhatsApp directly to [rawPhone] with [message] prefilled.
     * Falls back cleanly to Android system share sheet if WhatsApp is not installed or phone is blank.
     */
    fun launchWhatsAppOrShare(context: Context, rawPhone: String, message: String) {
        val cleanDigits = rawPhone.filter { it.isDigit() }
        if (cleanDigits.length >= 7) {
            val uri = Uri.parse("https://wa.me/$cleanDigits?text=${Uri.encode(message)}")
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = waIntent.resolveActivity(context.packageManager)
            if (resolved != null) {
                context.startActivity(waIntent)
                return
            }
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share Sadhana Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
