package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

object SadhanaScoringEngine {

    const val MAX_MORNING_PROGRAM_WEEKLY = 210
    const val MAX_CHANTING_WEEKLY = 490
    const val MAX_BOOK_READING_WEEKLY = 490
    const val MAX_MORNING_CLASS_WEEKLY = 210
    const val MAX_PERSONAL_HEARING_WEEKLY = 210
    const val MAX_SHLOKA_WEEKLY = 140
    const val MAX_SPIRITUAL_WEEKLY = 1750

    const val MAX_STUDY_WEEKLY = 210
    const val MAX_TO_BED_WEEKLY = 490
    const val MAX_WAKE_UP_WEEKLY = 490
    const val MAX_DAY_REST_WEEKLY = 490
    const val MAX_FILLING_CARD_WEEKLY = 70
    const val MAX_DISCIPLINE_WEEKLY = 1750

    const val MAX_GRAND_TOTAL = 3500

    private val isoFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    }
    private val dayNameFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE", Locale.US)
    }
    private val dayNumFormat = ThreadLocal.withInitial {
        SimpleDateFormat("d", Locale.US)
    }
    private val monthYearFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM yyyy", Locale.US)
    }
    private val shortMonthDayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("d MMM", Locale.US)
    }

    fun logDocId(devoteeUid: String, dateKey: String): String =
        "${devoteeUid}_${dateKey.replace("-", "")}"

    fun feedbackDocId(devoteeUid: String, weekStartKey: String): String =
        "${devoteeUid}_${weekStartKey}"

    fun todayDateKey(): String = isoFormat.get()!!.format(Calendar.getInstance().time)

    fun parseCalendar(dateKey: String): Calendar {
        val cal = Calendar.getInstance()
        val parsed = runCatching { isoFormat.get()!!.parse(dateKey) }.getOrNull()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun weekStartFor(dateKey: String): String {
        val cal = parseCalendar(dateKey)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return isoFormat.get()!!.format(cal.time)
    }

    fun datesOfWeek(weekStartKey: String): List<String> {
        val cal = parseCalendar(weekStartKey)
        return (0..6).map { offset ->
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, offset)
            isoFormat.get()!!.format(dayCal.time)
        }
    }

    fun shiftDate(dateKey: String, deltaDays: Int): String {
        val cal = parseCalendar(dateKey)
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return isoFormat.get()!!.format(cal.time)
    }

    fun shiftWeek(weekStartKey: String, deltaWeeks: Int): String =
        shiftDate(weekStartKey, deltaWeeks * 7)

    fun formatWeekRange(weekStartKey: String): String {
        val startCal = parseCalendar(weekStartKey)
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        return "${shortMonthDayFormat.get()!!.format(startCal.time)} – ${shortMonthDayFormat.get()!!.format(endCal.time)}"
    }

    fun formatMonthYear(weekStartKey: String): String {
        val cal = parseCalendar(weekStartKey)
        return monthYearFormat.get()!!.format(cal.time)
    }

    fun formatReadableDate(dateKey: String): String {
        val cal = parseCalendar(dateKey)
        val today = todayDateKey()
        val yesterday = shiftDate(today, -1)
        val prefix = when (dateKey) {
            today -> "Today • "
            yesterday -> "Yesterday • "
            else -> ""
        }
        return "$prefix${dayNameFormat.get()!!.format(cal.time)}, ${shortMonthDayFormat.get()!!.format(cal.time)}"
    }

    fun chantingSlotForCurrentTime(): ChantingSlot {
        val now = Calendar.getInstance()
        val totalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return when {
            totalMinutes < 7 * 60 + 30 -> ChantingSlot.BEFORE_730
            totalMinutes < 9 * 60 + 30 -> ChantingSlot.BEFORE_930
            totalMinutes < 12 * 60 -> ChantingSlot.BEFORE_1200
            totalMinutes < 16 * 60 -> ChantingSlot.BEFORE_1600
            totalMinutes < 19 * 60 -> ChantingSlot.BEFORE_1900
            else -> ChantingSlot.AFTER_1900
        }
    }

    /**
     * Evaluates whether filling the card for [dateKey] right now is before next day's morning program ending (~9:00 AM).
     */
    fun isFillingOnTimeNow(dateKey: String): Boolean {
        val today = todayDateKey()
        if (dateKey >= today) return true
        val yesterday = shiftDate(today, -1)
        if (dateKey == yesterday) {
            val now = Calendar.getInstance()
            val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            return minutes <= 9 * 60 // Before next day's morning program Prasadam (~9:00 AM)
        }
        return false
    }

    /**
     * Computes daily study score relative to the counsellor-configured daily Study target.
     * Default target is 120 min (2 hrs): >= target => 30, >= 50% (1-2h) => 20, >= 25% (30m-1h) => 10, else 0.
     */
    fun computeStudyPoints(studyMinutes: Int, dailyTargetMinutes: Int): Int {
        val safeTarget = dailyTargetMinutes.coerceAtLeast(15)
        if (studyMinutes <= 0) return 0
        val ratio = studyMinutes.toDouble() / safeTarget.toDouble()
        return when {
            ratio >= 1.0 -> 30
            ratio >= 0.5 -> 20
            ratio >= 0.25 -> 10
            else -> 0
        }
    }

    /**
     * Weekly proportional Book Reading score capped at 490 pts,
     * preserving individual daily minutes while ensuring uniformity across counsellor-set targets.
     */
    fun computeWeeklyBookReadingPoints(totalMinutes: Int, weeklyTargetMinutes: Int): Int {
        val safeTarget = weeklyTargetMinutes.coerceAtLeast(30)
        if (totalMinutes <= 0) return 0
        val proportional = ((totalMinutes.toDouble() / safeTarget.toDouble()) * MAX_BOOK_READING_WEEKLY).roundToInt()
        return min(MAX_BOOK_READING_WEEKLY, proportional.coerceAtLeast(0))
    }

    /**
     * Weekly proportional Personal Hearing score capped at 210 pts,
     * preserving individual daily minutes while ensuring uniformity across counsellor-set targets.
     */
    fun computeWeeklyPersonalHearingPoints(totalMinutes: Int, weeklyTargetMinutes: Int): Int {
        val safeTarget = weeklyTargetMinutes.coerceAtLeast(30)
        if (totalMinutes <= 0) return 0
        val proportional = ((totalMinutes.toDouble() / safeTarget.toDouble()) * MAX_PERSONAL_HEARING_WEEKLY).roundToInt()
        return min(MAX_PERSONAL_HEARING_WEEKLY, proportional.coerceAtLeast(0))
    }

    fun computeDailyBreakdown(
        dateKey: String,
        log: DailySadhanaLog?,
        targets: SadhanaTargets
    ): DailyScoreBreakdown {
        val cal = parseCalendar(dateKey)
        val dayShort = dayNameFormat.get()!!.format(cal.time)
        val dayNum = dayNumFormat.get()!!.format(cal.time)

        if (log == null) {
            return DailyScoreBreakdown(
                dateKey = dateKey,
                dayShortLabel = dayShort,
                dayOfMonth = dayNum,
                logged = false,
                morningProgramPts = 0,
                chantingPts = 0,
                bookReadingMinutes = 0,
                morningClassPts = 0,
                personalHearingMinutes = 0,
                shlokaPts = 0,
                studyPts = 0,
                studyMinutes = 0,
                toBedPts = 0,
                wakeUpPts = 0,
                dayRestPts = 0,
                cardFilledPts = 0,
                sevaMinutes = 0,
                japaRounds = 0,
                shlokaNote = "",
                sevaNote = "",
                bookTitle = ""
            )
        }

        return DailyScoreBreakdown(
            dateKey = dateKey,
            dayShortLabel = dayShort,
            dayOfMonth = dayNum,
            logged = true,
            morningProgramPts = if (log.morningProgramAttended) 30 else 0,
            chantingPts = log.chantingEnum.points,
            bookReadingMinutes = log.bookReadingMinutes.coerceAtLeast(0),
            morningClassPts = if (log.morningClassAttended) 30 else 0,
            personalHearingMinutes = log.personalHearingMinutes.coerceAtLeast(0),
            shlokaPts = if (log.shlokaLearned) 20 else 0,
            studyPts = computeStudyPoints(log.studyMinutes, targets.studyDailyTargetMinutes),
            studyMinutes = log.studyMinutes.coerceAtLeast(0),
            toBedPts = log.toBedEnum.points,
            wakeUpPts = log.wakeUpEnum.points,
            dayRestPts = log.dayRestEnum.points,
            cardFilledPts = if (log.cardFilledOnTime) 10 else 0,
            sevaMinutes = log.sevaMinutes.coerceAtLeast(0),
            japaRounds = log.japaRounds.coerceAtLeast(0),
            shlokaNote = log.shlokaNote,
            sevaNote = log.sevaNote,
            bookTitle = log.bookTitle
        )
    }

    fun summarizeWeek(
        weekStartKey: String,
        logsByDate: Map<String, DailySadhanaLog>,
        targets: SadhanaTargets
    ): WeeklySadhanaSummary {
        val weekDates = datesOfWeek(weekStartKey)
        val days = weekDates.map { dateKey ->
            computeDailyBreakdown(dateKey, logsByDate[dateKey], targets)
        }

        val morningProgramTotal = days.sumOf { it.morningProgramPts }
        val chantingTotal = days.sumOf { it.chantingPts }
        val bookReadingTotalMinutes = days.sumOf { it.bookReadingMinutes }
        val bookReadingPoints = computeWeeklyBookReadingPoints(bookReadingTotalMinutes, targets.bookReadingTargetMinutes)
        val morningClassTotal = days.sumOf { it.morningClassPts }
        val personalHearingTotalMinutes = days.sumOf { it.personalHearingMinutes }
        val personalHearingPoints = computeWeeklyPersonalHearingPoints(personalHearingTotalMinutes, targets.personalHearingTargetMinutes)
        val shlokaTotal = days.sumOf { it.shlokaPts }

        val spiritualTotal = morningProgramTotal + chantingTotal + bookReadingPoints +
            morningClassTotal + personalHearingPoints + shlokaTotal
        val spiritualPercent = (spiritualTotal.toDouble() / MAX_SPIRITUAL_WEEKLY.toDouble()) * 100.0

        val studyTotal = days.sumOf { it.studyPts }
        val studyTotalMinutes = days.sumOf { it.studyMinutes }
        val toBedTotal = days.sumOf { it.toBedPts }
        val wakeUpTotal = days.sumOf { it.wakeUpPts }
        val dayRestTotal = days.sumOf { it.dayRestPts }
        val cardFilledTotal = days.sumOf { it.cardFilledPts }
        val sevaTotalMinutes = days.sumOf { it.sevaMinutes }

        val disciplineTotal = studyTotal + toBedTotal + wakeUpTotal + dayRestTotal + cardFilledTotal
        val disciplinePercent = (disciplineTotal.toDouble() / MAX_DISCIPLINE_WEEKLY.toDouble()) * 100.0

        val grandTotal = spiritualTotal + disciplineTotal
        val grandPercent = (grandTotal.toDouble() / MAX_GRAND_TOTAL.toDouble()) * 100.0

        return WeeklySadhanaSummary(
            weekStartKey = weekStartKey,
            weekRangeLabel = formatWeekRange(weekStartKey),
            monthYearLabel = formatMonthYear(weekStartKey),
            targets = targets,
            days = days,
            daysLoggedCount = days.count { it.logged },
            morningProgramTotal = morningProgramTotal,
            chantingTotal = chantingTotal,
            bookReadingTotalMinutes = bookReadingTotalMinutes,
            bookReadingPoints = bookReadingPoints,
            morningClassTotal = morningClassTotal,
            personalHearingTotalMinutes = personalHearingTotalMinutes,
            personalHearingPoints = personalHearingPoints,
            shlokaTotal = shlokaTotal,
            spiritualTotal = spiritualTotal,
            spiritualPercent = spiritualPercent,
            studyTotal = studyTotal,
            studyTotalMinutes = studyTotalMinutes,
            toBedTotal = toBedTotal,
            wakeUpTotal = wakeUpTotal,
            dayRestTotal = dayRestTotal,
            cardFilledTotal = cardFilledTotal,
            sevaTotalMinutes = sevaTotalMinutes,
            disciplineTotal = disciplineTotal,
            disciplinePercent = disciplinePercent,
            grandTotal = grandTotal,
            grandPercent = grandPercent
        )
    }

    fun formatMinutesAsHrsMin(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hrs = safe / 60
        val mins = safe % 60
        return when {
            hrs == 0 -> "${mins}m"
            mins == 0 -> "${hrs}h"
            else -> "${hrs}h ${mins}m"
        }
    }

    fun formatTargetHours(minutes: Int): String {
        val hrs = minutes / 60.0
        return if (minutes % 60 == 0) "${minutes / 60} hrs" else String.format(Locale.US, "%.1f hrs", hrs)
    }

    fun computeCurrentStreak(loggedDates: Set<String>): Int {
        var streak = 0
        var cursor = todayDateKey()
        if (!loggedDates.contains(cursor)) {
            cursor = shiftDate(cursor, -1)
        }
        while (loggedDates.contains(cursor)) {
            streak++
            cursor = shiftDate(cursor, -1)
        }
        return streak
    }
}
