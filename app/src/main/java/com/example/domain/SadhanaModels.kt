package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    DEVOTEE,
    COUNSELLOR;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: DEVOTEE
    }
}

enum class ChantingSlot(val label: String, val shortLabel: String, val points: Int) {
    BEFORE_730("Before 7:30 AM", "<7:30 AM", 70),
    BEFORE_930("Before 9:30 AM", "<9:30 AM", 50),
    BEFORE_1200("Before 12:00 PM", "<12 PM", 30),
    BEFORE_1600("Before 4:00 PM", "<4 PM", 20),
    BEFORE_1900("Before 7:00 PM", "<7 PM", 10),
    AFTER_1900("After 7:00 PM / Pending", "After 7 PM", 0);

    companion object {
        fun fromRaw(raw: String?): ChantingSlot =
            entries.firstOrNull { it.name == raw } ?: AFTER_1900
    }
}

enum class ToBedSlot(val label: String, val shortLabel: String, val points: Int) {
    BEFORE_2115("Before 9:15 PM", "<9:15 PM", 70),
    BEFORE_2130("9:15 – 9:30 PM", "9:15–9:30", 50),
    BEFORE_2145("9:30 – 9:45 PM", "9:30–9:45", 30),
    BEFORE_2200("9:45 – 10:00 PM", "9:45–10 PM", 10),
    AFTER_2200("After 10:00 PM", ">10 PM", 0);

    companion object {
        fun fromRaw(raw: String?): ToBedSlot =
            entries.firstOrNull { it.name == raw } ?: AFTER_2200
    }
}

enum class WakeUpSlot(val label: String, val shortLabel: String, val points: Int) {
    BEFORE_0340("Before 3:40 AM", "<3:40 AM", 70),
    BEFORE_0350("3:40 – 3:50 AM", "3:40–3:50", 50),
    BEFORE_0400("3:50 – 4:00 AM", "3:50–4:00", 30),
    BEFORE_0415("4:00 – 4:15 AM", "4:00–4:15", 10),
    AFTER_0415("After 4:15 AM", ">4:15 AM", 0);

    companion object {
        fun fromRaw(raw: String?): WakeUpSlot =
            entries.firstOrNull { it.name == raw } ?: AFTER_0415
    }
}

enum class DayRestSlot(val label: String, val shortLabel: String, val points: Int) {
    UNDER_30("Under 30 min", "<30 min", 70),
    FROM_30_TO_45("30 – 45 min", "30–45 min", 50),
    FROM_45_TO_60("45 – 60 min", "45–60 min", 30),
    OVER_60("Over 60 min", ">60 min", 0);

    companion object {
        fun fromRaw(raw: String?): DayRestSlot =
            entries.firstOrNull { it.name == raw } ?: OVER_60
    }
}

data class SadhanaTargets(
    val bookReadingTargetMinutes: Int = 360,     // Default Bhima 6 hrs/week
    val personalHearingTargetMinutes: Int = 180, // Default 3 hrs/week
    val studyDailyTargetMinutes: Int = 120,      // Default 2 hrs/day
    val japaRoundsTarget: Int = 16,              // Default 16 rounds/day
    val readingTierLabel: String = "Bhima (6h)"
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.DEVOTEE.name,
    val inviteCode: String = "",
    val whatsappPhone: String = "",
    val counsellorUid: String = "",
    val counsellorName: String = "",
    val counsellorWhatsapp: String = "",
    val bookReadingTargetMinutes: Int = 360,
    val personalHearingTargetMinutes: Int = 180,
    val studyDailyTargetMinutes: Int = 120,
    val japaRoundsTarget: Int = 16,
    val readingTierLabel: String = "Bhima (6h)",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val userRole: UserRole
        get() = UserRole.fromRaw(role)

    val targets: SadhanaTargets
        get() = SadhanaTargets(
            bookReadingTargetMinutes = bookReadingTargetMinutes.coerceIn(30, 2400),
            personalHearingTargetMinutes = personalHearingTargetMinutes.coerceIn(30, 1800),
            studyDailyTargetMinutes = studyDailyTargetMinutes.coerceIn(15, 720),
            japaRoundsTarget = japaRoundsTarget.coerceIn(1, 64),
            readingTierLabel = readingTierLabel.ifBlank { "Custom" }
        )
}

@Keep
@IgnoreExtraProperties
data class InviteCodeRecord(
    val code: String = "",
    val counsellorUid: String = "",
    val counsellorName: String = "",
    val counsellorWhatsapp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailySadhanaLog(
    @DocumentId val id: String = "",
    val devoteeUid: String = "",
    val counsellorUid: String = "",
    val dateKey: String = "",
    val weekStartKey: String = "",
    val morningProgramAttended: Boolean = false,
    val chantingSlot: String = ChantingSlot.AFTER_1900.name,
    val japaRounds: Int = 0,
    val japaBeads: Int = 0,
    val bookReadingMinutes: Int = 0,
    val bookTitle: String = "",
    val morningClassAttended: Boolean = false,
    val personalHearingMinutes: Int = 0,
    val shlokaLearned: Boolean = false,
    val shlokaNote: String = "",
    val studyMinutes: Int = 0,
    val toBedSlot: String = ToBedSlot.AFTER_2200.name,
    val wakeUpSlot: String = WakeUpSlot.AFTER_0415.name,
    val dayRestSlot: String = DayRestSlot.UNDER_30.name,
    val cardFilledOnTime: Boolean = true,
    val sevaMinutes: Int = 0,
    val sevaNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val chantingEnum: ChantingSlot get() = ChantingSlot.fromRaw(chantingSlot)
    val toBedEnum: ToBedSlot get() = ToBedSlot.fromRaw(toBedSlot)
    val wakeUpEnum: WakeUpSlot get() = WakeUpSlot.fromRaw(wakeUpSlot)
    val dayRestEnum: DayRestSlot get() = DayRestSlot.fromRaw(dayRestSlot)
}

@Keep
@IgnoreExtraProperties
data class WeeklyFeedback(
    @DocumentId val id: String = "",
    val devoteeUid: String = "",
    val counsellorUid: String = "",
    val counsellorName: String = "",
    val weekStartKey: String = "",
    val blessingTag: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DailyScoreBreakdown(
    val dateKey: String,
    val dayShortLabel: String,
    val dayOfMonth: String,
    val logged: Boolean,
    val morningProgramPts: Int,
    val chantingPts: Int,
    val bookReadingMinutes: Int,
    val morningClassPts: Int,
    val personalHearingMinutes: Int,
    val shlokaPts: Int,
    val studyPts: Int,
    val studyMinutes: Int,
    val toBedPts: Int,
    val wakeUpPts: Int,
    val dayRestPts: Int,
    val cardFilledPts: Int,
    val sevaMinutes: Int,
    val japaRounds: Int,
    val shlokaNote: String,
    val sevaNote: String,
    val bookTitle: String
) {
    val fixedDailyScore: Int
        get() = morningProgramPts + chantingPts + morningClassPts + shlokaPts +
            studyPts + toBedPts + wakeUpPts + dayRestPts + cardFilledPts
}

data class WeeklySadhanaSummary(
    val weekStartKey: String,
    val weekRangeLabel: String,
    val monthYearLabel: String,
    val targets: SadhanaTargets,
    val days: List<DailyScoreBreakdown>,
    val daysLoggedCount: Int,
    val morningProgramTotal: Int,
    val chantingTotal: Int,
    val bookReadingTotalMinutes: Int,
    val bookReadingPoints: Int,
    val morningClassTotal: Int,
    val personalHearingTotalMinutes: Int,
    val personalHearingPoints: Int,
    val shlokaTotal: Int,
    val spiritualTotal: Int,
    val spiritualPercent: Double,
    val studyTotal: Int,
    val studyTotalMinutes: Int,
    val toBedTotal: Int,
    val wakeUpTotal: Int,
    val dayRestTotal: Int,
    val cardFilledTotal: Int,
    val sevaTotalMinutes: Int,
    val disciplineTotal: Int,
    val disciplinePercent: Double,
    val grandTotal: Int,
    val grandPercent: Double
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val primaryHour: Int = 20,
    val primaryMinute: Int = 30,
    val followUpEnabled: Boolean = true,
    val followUpIntervalMinutes: Int = 45,
    val maxFollowUps: Int = 2
)

data class OfflineTimerState(
    val japaDateKey: String = "",
    val japaBeads: Int = 0,
    val japaRounds: Int = 0,
    val japaSessionStartMillis: Long = 0L,
    val readingTimerRunning: Boolean = false,
    val readingTimerStartMillis: Long = 0L,
    val readingAccumulatedSeconds: Long = 0L,
    val readingBookTitle: String = ""
)
