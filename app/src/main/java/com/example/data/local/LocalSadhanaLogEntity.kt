package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.DailySadhanaLog
import com.example.domain.SadhanaScoringEngine

@Entity(tableName = "local_sadhana_logs")
data class LocalSadhanaLogEntity(
    @PrimaryKey val docId: String,
    val devoteeUid: String,
    val counsellorUid: String,
    val dateKey: String,
    val weekStartKey: String,
    val morningProgramAttended: Boolean,
    val chantingSlot: String,
    val japaRounds: Int,
    val japaBeads: Int,
    val bookReadingMinutes: Int,
    val bookTitle: String,
    val morningClassAttended: Boolean,
    val personalHearingMinutes: Int,
    val shlokaLearned: Boolean,
    val shlokaNote: String,
    val studyMinutes: Int,
    val toBedSlot: String,
    val wakeUpSlot: String,
    val dayRestSlot: String,
    val cardFilledOnTime: Boolean,
    val sevaMinutes: Int,
    val sevaNote: String,
    val syncedToCloud: Boolean,
    val existsOnCloud: Boolean,
    val updatedAtMillis: Long
) {
    fun toDomain(): DailySadhanaLog = DailySadhanaLog(
        id = docId,
        devoteeUid = devoteeUid,
        counsellorUid = counsellorUid,
        dateKey = dateKey,
        weekStartKey = weekStartKey,
        morningProgramAttended = morningProgramAttended,
        chantingSlot = chantingSlot,
        japaRounds = japaRounds,
        japaBeads = japaBeads,
        bookReadingMinutes = bookReadingMinutes,
        bookTitle = bookTitle,
        morningClassAttended = morningClassAttended,
        personalHearingMinutes = personalHearingMinutes,
        shlokaLearned = shlokaLearned,
        shlokaNote = shlokaNote,
        studyMinutes = studyMinutes,
        toBedSlot = toBedSlot,
        wakeUpSlot = wakeUpSlot,
        dayRestSlot = dayRestSlot,
        cardFilledOnTime = cardFilledOnTime,
        sevaMinutes = sevaMinutes,
        sevaNote = sevaNote
    )

    companion object {
        fun fromDomain(
            log: DailySadhanaLog,
            syncedToCloud: Boolean,
            existsOnCloud: Boolean,
            updatedAtMillis: Long = System.currentTimeMillis()
        ): LocalSadhanaLogEntity {
            val resolvedId = log.id.ifBlank { SadhanaScoringEngine.logDocId(log.devoteeUid, log.dateKey) }
            val resolvedWeek = log.weekStartKey.ifBlank { SadhanaScoringEngine.weekStartFor(log.dateKey) }
            return LocalSadhanaLogEntity(
                docId = resolvedId,
                devoteeUid = log.devoteeUid,
                counsellorUid = log.counsellorUid,
                dateKey = log.dateKey,
                weekStartKey = resolvedWeek,
                morningProgramAttended = log.morningProgramAttended,
                chantingSlot = log.chantingSlot,
                japaRounds = log.japaRounds.coerceIn(0, 128),
                japaBeads = log.japaBeads.coerceIn(0, 108),
                bookReadingMinutes = log.bookReadingMinutes.coerceIn(0, 1440),
                bookTitle = log.bookTitle.take(120),
                morningClassAttended = log.morningClassAttended,
                personalHearingMinutes = log.personalHearingMinutes.coerceIn(0, 1440),
                shlokaLearned = log.shlokaLearned,
                shlokaNote = log.shlokaNote.take(500),
                studyMinutes = log.studyMinutes.coerceIn(0, 1440),
                toBedSlot = log.toBedSlot,
                wakeUpSlot = log.wakeUpSlot,
                dayRestSlot = log.dayRestSlot,
                cardFilledOnTime = log.cardFilledOnTime,
                sevaMinutes = log.sevaMinutes.coerceIn(0, 1440),
                sevaNote = log.sevaNote.take(500),
                syncedToCloud = syncedToCloud,
                existsOnCloud = existsOnCloud,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}
