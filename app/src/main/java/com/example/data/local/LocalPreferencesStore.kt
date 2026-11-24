package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.OfflineTimerState
import com.example.domain.ReminderSettings
import com.example.domain.UserProfile
import com.example.domain.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sadhanaDataStore by preferencesDataStore(name = "sadhana_seva_prefs")

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val ACTIVE_UID = stringPreferencesKey("active_uid")
        val CACHED_NAME = stringPreferencesKey("cached_name")
        val CACHED_EMAIL = stringPreferencesKey("cached_email")
        val CACHED_ROLE = stringPreferencesKey("cached_role")
        val CACHED_INVITE_CODE = stringPreferencesKey("cached_invite_code")
        val CACHED_WHATSAPP = stringPreferencesKey("cached_whatsapp")
        val CACHED_COUNSELLOR_UID = stringPreferencesKey("cached_counsellor_uid")
        val CACHED_COUNSELLOR_NAME = stringPreferencesKey("cached_counsellor_name")
        val CACHED_COUNSELLOR_WA = stringPreferencesKey("cached_counsellor_wa")
        val TARGET_BOOK_MIN = intPreferencesKey("target_book_min")
        val TARGET_HEARING_MIN = intPreferencesKey("target_hearing_min")
        val TARGET_STUDY_MIN = intPreferencesKey("target_study_min")
        val TARGET_JAPA_ROUNDS = intPreferencesKey("target_japa_rounds")
        val TARGET_READING_LABEL = stringPreferencesKey("target_reading_label")
        val LAST_PROFILE_SYNC_MILLIS = longPreferencesKey("last_profile_sync_millis")

        // Offline Japa & Book Reading Timer State
        val JAPA_DATE_KEY = stringPreferencesKey("japa_date_key")
        val JAPA_BEADS = intPreferencesKey("japa_beads")
        val JAPA_ROUNDS = intPreferencesKey("japa_rounds")
        val JAPA_START_MILLIS = longPreferencesKey("japa_start_millis")
        val READING_RUNNING = booleanPreferencesKey("reading_running")
        val READING_START_MILLIS = longPreferencesKey("reading_start_millis")
        val READING_ACCUM_SEC = longPreferencesKey("reading_accum_sec")
        val READING_BOOK_TITLE = stringPreferencesKey("reading_book_title")

        // Reminder Settings
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val FOLLOWUP_ENABLED = booleanPreferencesKey("followup_enabled")
        val FOLLOWUP_INTERVAL = intPreferencesKey("followup_interval")
        val FOLLOWUP_MAX = intPreferencesKey("followup_max")
    }

    val offlineTimerState: Flow<OfflineTimerState> = context.sadhanaDataStore.data.map { prefs ->
        OfflineTimerState(
            japaDateKey = prefs[Keys.JAPA_DATE_KEY].orEmpty(),
            japaBeads = prefs[Keys.JAPA_BEADS] ?: 0,
            japaRounds = prefs[Keys.JAPA_ROUNDS] ?: 0,
            japaSessionStartMillis = prefs[Keys.JAPA_START_MILLIS] ?: 0L,
            readingTimerRunning = prefs[Keys.READING_RUNNING] ?: false,
            readingTimerStartMillis = prefs[Keys.READING_START_MILLIS] ?: 0L,
            readingAccumulatedSeconds = prefs[Keys.READING_ACCUM_SEC] ?: 0L,
            readingBookTitle = prefs[Keys.READING_BOOK_TITLE].orEmpty()
        )
    }

    val reminderSettings: Flow<ReminderSettings> = context.sadhanaDataStore.data.map { prefs ->
        parseReminderSettings(prefs)
    }

    suspend fun getReminderSettingsOnce(): ReminderSettings =
        parseReminderSettings(context.sadhanaDataStore.data.first())

    suspend fun getActiveUidOnce(): String =
        context.sadhanaDataStore.data.first()[Keys.ACTIVE_UID].orEmpty()

    private fun parseReminderSettings(prefs: Preferences): ReminderSettings = ReminderSettings(
        enabled = prefs[Keys.REMINDER_ENABLED] ?: false,
        primaryHour = prefs[Keys.REMINDER_HOUR] ?: 20,
        primaryMinute = prefs[Keys.REMINDER_MINUTE] ?: 30,
        followUpEnabled = prefs[Keys.FOLLOWUP_ENABLED] ?: true,
        followUpIntervalMinutes = prefs[Keys.FOLLOWUP_INTERVAL] ?: 45,
        maxFollowUps = prefs[Keys.FOLLOWUP_MAX] ?: 2
    )

    suspend fun cacheUserProfile(profile: UserProfile) {
        context.sadhanaDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_UID] = profile.uid
            prefs[Keys.CACHED_NAME] = profile.displayName
            prefs[Keys.CACHED_EMAIL] = profile.email
            prefs[Keys.CACHED_ROLE] = profile.role
            prefs[Keys.CACHED_INVITE_CODE] = profile.inviteCode
            prefs[Keys.CACHED_WHATSAPP] = profile.whatsappPhone
            prefs[Keys.CACHED_COUNSELLOR_UID] = profile.counsellorUid
            prefs[Keys.CACHED_COUNSELLOR_NAME] = profile.counsellorName
            prefs[Keys.CACHED_COUNSELLOR_WA] = profile.counsellorWhatsapp
            prefs[Keys.TARGET_BOOK_MIN] = profile.bookReadingTargetMinutes
            prefs[Keys.TARGET_HEARING_MIN] = profile.personalHearingTargetMinutes
            prefs[Keys.TARGET_STUDY_MIN] = profile.studyDailyTargetMinutes
            prefs[Keys.TARGET_JAPA_ROUNDS] = profile.japaRoundsTarget
            prefs[Keys.TARGET_READING_LABEL] = profile.readingTierLabel
            prefs[Keys.LAST_PROFILE_SYNC_MILLIS] = System.currentTimeMillis()
        }
    }

    suspend fun isProfileCacheFresh(uid: String, maxAgeMillis: Long = 12L * 60L * 60L * 1000L): Boolean {
        val prefs = context.sadhanaDataStore.data.first()
        if (prefs[Keys.ACTIVE_UID] != uid) return false
        val lastSync = prefs[Keys.LAST_PROFILE_SYNC_MILLIS] ?: 0L
        return lastSync > 0L && (System.currentTimeMillis() - lastSync) < maxAgeMillis
    }

    suspend fun getCachedUserProfile(uid: String): UserProfile? {
        val prefs = context.sadhanaDataStore.data.first()
        val cachedUid = prefs[Keys.ACTIVE_UID]
        if (cachedUid != uid || cachedUid.isBlank()) return null
        return UserProfile(
            uid = cachedUid,
            displayName = prefs[Keys.CACHED_NAME].orEmpty(),
            email = prefs[Keys.CACHED_EMAIL].orEmpty(),
            role = prefs[Keys.CACHED_ROLE] ?: UserRole.DEVOTEE.name,
            inviteCode = prefs[Keys.CACHED_INVITE_CODE].orEmpty(),
            whatsappPhone = prefs[Keys.CACHED_WHATSAPP].orEmpty(),
            counsellorUid = prefs[Keys.CACHED_COUNSELLOR_UID].orEmpty(),
            counsellorName = prefs[Keys.CACHED_COUNSELLOR_NAME].orEmpty(),
            counsellorWhatsapp = prefs[Keys.CACHED_COUNSELLOR_WA].orEmpty(),
            bookReadingTargetMinutes = prefs[Keys.TARGET_BOOK_MIN] ?: 360,
            personalHearingTargetMinutes = prefs[Keys.TARGET_HEARING_MIN] ?: 180,
            studyDailyTargetMinutes = prefs[Keys.TARGET_STUDY_MIN] ?: 120,
            japaRoundsTarget = prefs[Keys.TARGET_JAPA_ROUNDS] ?: 16,
            readingTierLabel = prefs[Keys.TARGET_READING_LABEL] ?: "Bhima (6h)"
        )
    }

    suspend fun clearCachedUser() {
        context.sadhanaDataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_UID)
            prefs.remove(Keys.CACHED_ROLE)
            prefs.remove(Keys.LAST_PROFILE_SYNC_MILLIS)
        }
    }

    suspend fun saveJapaState(dateKey: String, beads: Int, rounds: Int, startMillis: Long) {
        context.sadhanaDataStore.edit { prefs ->
            prefs[Keys.JAPA_DATE_KEY] = dateKey
            prefs[Keys.JAPA_BEADS] = beads.coerceIn(0, 108)
            prefs[Keys.JAPA_ROUNDS] = rounds.coerceIn(0, 128)
            prefs[Keys.JAPA_START_MILLIS] = startMillis
        }
    }

    suspend fun saveReadingTimerState(
        running: Boolean,
        startMillis: Long,
        accumulatedSeconds: Long,
        bookTitle: String
    ) {
        context.sadhanaDataStore.edit { prefs ->
            prefs[Keys.READING_RUNNING] = running
            prefs[Keys.READING_START_MILLIS] = startMillis
            prefs[Keys.READING_ACCUM_SEC] = accumulatedSeconds.coerceAtLeast(0L)
            prefs[Keys.READING_BOOK_TITLE] = bookTitle.take(120)
        }
    }

    suspend fun saveReminderSettings(settings: ReminderSettings) {
        context.sadhanaDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = settings.enabled
            prefs[Keys.REMINDER_HOUR] = settings.primaryHour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = settings.primaryMinute.coerceIn(0, 59)
            prefs[Keys.FOLLOWUP_ENABLED] = settings.followUpEnabled
            prefs[Keys.FOLLOWUP_INTERVAL] = settings.followUpIntervalMinutes.coerceIn(15, 180)
            prefs[Keys.FOLLOWUP_MAX] = settings.maxFollowUps.coerceIn(1, 3)
        }
    }
}
