package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SadhanaRepository
import com.example.domain.ChantingSlot
import com.example.domain.DailySadhanaLog
import com.example.domain.DayRestSlot
import com.example.domain.OfflineTimerState
import com.example.domain.ReminderSettings
import com.example.domain.SadhanaScoringEngine
import com.example.domain.ToBedSlot
import com.example.domain.UserProfile
import com.example.domain.UserRole
import com.example.domain.WakeUpSlot
import com.example.domain.WeeklyFeedback
import com.example.domain.WeeklySadhanaSummary
import com.example.reminders.SadhanaReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class NeedsOnboarding(val uid: String, val defaultName: String, val email: String) : SessionUiState
    data class Ready(val profile: UserProfile) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

class SessionViewModel(
    private val uid: String,
    private val displayNameHint: String,
    private val emailHint: String,
    private val repository: SadhanaRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        loadProfile()
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = SessionUiState.Loading
            // Offline-first: immediately check local cache so app works with zero internet & zero reads
            val cached = repository.getCachedProfile(uid)
            if (cached != null) {
                _state.value = SessionUiState.Ready(cached)
                if (repository.isProfileCacheFresh(uid)) {
                    return@launch
                }
            }
            // One-shot cloud check only on first login or when cache is >12h old (no realtime listener)
            repository.fetchUserProfileOnce(uid)
                .onSuccess { remote ->
                    if (remote != null) {
                        _state.value = SessionUiState.Ready(remote)
                    } else if (cached == null) {
                        _state.value = SessionUiState.NeedsOnboarding(
                            uid = uid,
                            defaultName = displayNameHint.ifBlank { "Devotee" },
                            email = emailHint
                        )
                    }
                }
                .onFailure { err ->
                    if (cached == null) {
                        _state.value = SessionUiState.Error(
                            err.localizedMessage ?: "Unable to load profile. Please check your connection."
                        )
                    }
                }
        }
    }

    fun completeOnboarding(
        displayName: String,
        role: UserRole,
        whatsappPhone: String,
        inviteCodeToJoin: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.createUserProfile(
                displayName = displayName,
                email = emailHint,
                role = role,
                whatsappPhone = whatsappPhone,
                initialInviteCodeToJoin = inviteCodeToJoin
            ).onSuccess { created ->
                _state.value = SessionUiState.Ready(created)
                _actionMessage.value = if (role == UserRole.COUNSELLOR) {
                    "Counsellor profile ready! Share code ${created.inviteCode} with your devotees."
                } else {
                    "Hare Krishna, ${created.displayName}! Your Sadhana Card is ready."
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not create profile."
            }
            _busy.value = false
        }
    }

    fun updateProfileLocally(updated: UserProfile) {
        _state.value = SessionUiState.Ready(updated)
    }

    fun updateContactInfo(newName: String, newWhatsapp: String) {
        val current = (_state.value as? SessionUiState.Ready)?.profile ?: return
        viewModelScope.launch {
            _busy.value = true
            repository.updateOwnContactInfo(current, newName, newWhatsapp)
                .onSuccess { updated ->
                    _state.value = SessionUiState.Ready(updated)
                    _actionMessage.value = "Profile details updated."
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update profile."
                }
            _busy.value = false
        }
    }

    fun linkCounsellor(code: String) {
        val current = (_state.value as? SessionUiState.Ready)?.profile ?: return
        viewModelScope.launch {
            _busy.value = true
            repository.linkCounsellorByInviteCode(current, code)
                .onSuccess { updated ->
                    _state.value = SessionUiState.Ready(updated)
                    _actionMessage.value = "Linked with ${updated.counsellorName}! Your counsellor can now view your sadhana and set your targets."
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not link counsellor code."
                }
            _busy.value = false
        }
    }

    class Factory(
        private val uid: String,
        private val displayNameHint: String,
        private val emailHint: String,
        private val repository: SadhanaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionViewModel(uid, displayNameHint, emailHint, repository) as T
    }
}

data class DevoteeUiState(
    val profile: UserProfile,
    val selectedDateKey: String = SadhanaScoringEngine.todayDateKey(),
    val selectedWeekStartKey: String = SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey()),
    val draft: DailySadhanaLog = DailySadhanaLog(),
    val allLogs: List<DailySadhanaLog> = emptyList(),
    val feedbacksByWeek: Map<String, WeeklyFeedback> = emptyMap(),
    val unsyncedCount: Int = 0,
    val offlineTimer: OfflineTimerState = OfflineTimerState(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val syncing: Boolean = false,
    val refreshing: Boolean = false,
    val bannerMessage: String? = null
) {
    val logsByDate: Map<String, DailySadhanaLog>
        get() = allLogs.associateBy { it.dateKey }

    val selectedWeekSummary: WeeklySadhanaSummary
        get() = SadhanaScoringEngine.summarizeWeek(selectedWeekStartKey, logsByDate, profile.targets)

    val currentWeekSummary: WeeklySadhanaSummary
        get() {
            val weekForDraft = SadhanaScoringEngine.weekStartFor(selectedDateKey)
            val merged = logsByDate.toMutableMap()
            merged[selectedDateKey] = draft
            return SadhanaScoringEngine.summarizeWeek(weekForDraft, merged, profile.targets)
        }

    val currentStreak: Int
        get() = SadhanaScoringEngine.computeCurrentStreak(logsByDate.keys)

    val recentWeekSummaries: List<WeeklySadhanaSummary>
        get() {
            val currentWeek = SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey())
            val loggedWeeks = allLogs.map { it.weekStartKey }.toSet()
            val baseWeeks = (0..7).map { SadhanaScoringEngine.shiftWeek(currentWeek, -it) }
            return (baseWeeks + loggedWeeks).distinct().sortedDescending().take(12).map { wk ->
                SadhanaScoringEngine.summarizeWeek(wk, logsByDate, profile.targets)
            }
        }
}

class DevoteeViewModel(
    initialProfile: UserProfile,
    private val appContext: Context,
    private val repository: SadhanaRepository,
    private val onProfileRefreshed: (UserProfile) -> Unit
) : ViewModel() {

    private val uid = initialProfile.uid
    private val _profile = MutableStateFlow(initialProfile)
    private val _selectedDateKey = MutableStateFlow(SadhanaScoringEngine.todayDateKey())
    private val _selectedWeekStartKey = MutableStateFlow(
        SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey())
    )
    private val _draft = MutableStateFlow(emptyDraftFor(SadhanaScoringEngine.todayDateKey(), initialProfile))
    private val _feedbacks = MutableStateFlow<Map<String, WeeklyFeedback>>(emptyMap())
    private val _syncing = MutableStateFlow(false)
    private val _refreshing = MutableStateFlow(false)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private var draftLoadedForDate: String? = null

    private val localLogsFlow = repository.observeLocalLogs(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val unsyncedCountFlow = repository.observeUnsyncedCount(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val timerFlow = repository.offlineTimerState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfflineTimerState())

    private val reminderFlow = repository.reminderSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    val uiState: StateFlow<DevoteeUiState> = combine(
        combine(_profile, _selectedDateKey, _selectedWeekStartKey, _draft) { p, date, wk, dr ->
            Quad(p, date, wk, dr)
        },
        combine(localLogsFlow, _feedbacks, unsyncedCountFlow, timerFlow, reminderFlow) { logs, fb, unsynced, timer, rem ->
            Quint(logs, fb, unsynced, timer, rem)
        },
        combine(_syncing, _refreshing, _bannerMessage) { s, r, m -> Triple(s, r, m) }
    ) { left, mid, right ->
        DevoteeUiState(
            profile = left.first,
            selectedDateKey = left.second,
            selectedWeekStartKey = left.third,
            draft = left.fourth,
            allLogs = mid.first,
            feedbacksByWeek = mid.second,
            unsyncedCount = mid.third,
            offlineTimer = mid.fourth,
            reminderSettings = mid.fifth,
            syncing = right.first,
            refreshing = right.second,
            bannerMessage = right.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DevoteeUiState(profile = initialProfile, draft = _draft.value)
    )

    init {
        // Hydrate draft from local Room DB immediately (offline-first)
        viewModelScope.launch {
            localLogsFlow.collect { logs ->
                val dateKey = _selectedDateKey.value
                if (draftLoadedForDate != dateKey) {
                    val found = logs.firstOrNull { it.dateKey == dateKey }
                    if (found != null) {
                        _draft.value = found
                        draftLoadedForDate = dateKey
                    }
                }
            }
        }
        // Ultra-low-cost startup: pushes pending logs, pulls history ONLY on fresh install (0 reads normally!)
        syncOnStartup()
    }

    private fun syncOnStartup() {
        viewModelScope.launch {
            repository.syncDevoteeOnStartup(uid)
                .onSuccess { (freshProfile, freshFeedbacks) ->
                    if (freshProfile != null) {
                        _profile.value = freshProfile
                        onProfileRefreshed(freshProfile)
                    }
                    if (freshFeedbacks != null) {
                        _feedbacks.value = freshFeedbacks.associateBy { it.weekStartKey }
                    }
                }
        }
    }

    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        _draft.update { it.copy(counsellorUid = profile.counsellorUid) }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        _selectedWeekStartKey.value = SadhanaScoringEngine.weekStartFor(dateKey)
        val existing = localLogsFlow.value.firstOrNull { it.dateKey == dateKey }
        _draft.value = existing ?: emptyDraftFor(dateKey, _profile.value)
        draftLoadedForDate = dateKey
    }

    fun shiftSelectedWeek(deltaWeeks: Int) {
        _selectedWeekStartKey.value = SadhanaScoringEngine.shiftWeek(_selectedWeekStartKey.value, deltaWeeks)
    }

    fun selectWeek(weekStartKey: String) {
        _selectedWeekStartKey.value = weekStartKey
    }

    fun updateDraft(transform: (DailySadhanaLog) -> DailySadhanaLog) {
        _draft.update { transform(it) }
    }

    // --- OFFLINE FOCUS JAPA COUNTER (ZERO NETWORK CALLS) ---

    fun tapJapaBead() {
        val today = SadhanaScoringEngine.todayDateKey()
        val current = _draft.value
        val workingDate = _selectedDateKey.value
        val nextBeads = current.japaBeads + 1
        val completedRound = nextBeads >= 108
        val newBeads = if (completedRound) 0 else nextBeads
        val newRounds = if (completedRound) (current.japaRounds + 1).coerceAtMost(128) else current.japaRounds
        val targetRounds = _profile.value.targets.japaRoundsTarget
        val autoSlot = if (completedRound && newRounds == targetRounds && workingDate == today) {
            SadhanaScoringEngine.chantingSlotForCurrentTime().name
        } else {
            current.chantingSlot
        }
        val updated = current.copy(
            japaBeads = newBeads,
            japaRounds = newRounds,
            chantingSlot = autoSlot
        )
        _draft.value = updated
        persistOfflineCounterAndDraft(updated, startMillis = timerFlow.value.japaSessionStartMillis.takeIf { it > 0L } ?: System.currentTimeMillis())
        if (completedRound && newRounds == targetRounds) {
            _bannerMessage.value = "Haribol! $targetRounds rounds completed — Chanting time auto-recorded."
        }
    }

    fun adjustJapaRounds(delta: Int) {
        val today = SadhanaScoringEngine.todayDateKey()
        val current = _draft.value
        val newRounds = (current.japaRounds + delta).coerceIn(0, 128)
        val targetRounds = _profile.value.targets.japaRoundsTarget
        val autoSlot = if (delta > 0 && newRounds == targetRounds && _selectedDateKey.value == today) {
            SadhanaScoringEngine.chantingSlotForCurrentTime().name
        } else {
            current.chantingSlot
        }
        val updated = current.copy(japaRounds = newRounds, chantingSlot = autoSlot)
        _draft.value = updated
        persistOfflineCounterAndDraft(updated, startMillis = timerFlow.value.japaSessionStartMillis)
    }

    fun resetJapaBeadsOnly() {
        val updated = _draft.value.copy(japaBeads = 0)
        _draft.value = updated
        persistOfflineCounterAndDraft(updated, startMillis = 0L)
    }

    fun markRoundsDoneNow() {
        val slot = SadhanaScoringEngine.chantingSlotForCurrentTime()
        val targetRounds = _profile.value.targets.japaRoundsTarget
        val updated = _draft.value.copy(
            japaRounds = maxOf(_draft.value.japaRounds, targetRounds),
            japaBeads = 0,
            chantingSlot = slot.name
        )
        _draft.value = updated
        persistOfflineCounterAndDraft(updated, startMillis = 0L)
        _bannerMessage.value = "Chanting marked ${slot.label} (+${slot.points} pts)."
    }

    private fun persistOfflineCounterAndDraft(updated: DailySadhanaLog, startMillis: Long) {
        viewModelScope.launch {
            repository.prefs.saveJapaState(
                dateKey = updated.dateKey,
                beads = updated.japaBeads,
                rounds = updated.japaRounds,
                startMillis = startMillis
            )
            // Persist locally to Room with attemptCloudSync = false so zero network cost while chanting!
            repository.saveDailyLogOfflineFirst(updated, attemptCloudSync = false)
        }
    }

    // --- OFFLINE BOOK READING STOPWATCH (ZERO NETWORK CALLS) ---

    fun toggleReadingTimer() {
        viewModelScope.launch {
            val state = timerFlow.value
            val now = System.currentTimeMillis()
            if (state.readingTimerRunning) {
                val elapsed = ((now - state.readingTimerStartMillis) / 1000L).coerceAtLeast(0L)
                repository.prefs.saveReadingTimerState(
                    running = false,
                    startMillis = 0L,
                    accumulatedSeconds = state.readingAccumulatedSeconds + elapsed,
                    bookTitle = state.readingBookTitle.ifBlank { _draft.value.bookTitle }
                )
            } else {
                repository.prefs.saveReadingTimerState(
                    running = true,
                    startMillis = now,
                    accumulatedSeconds = state.readingAccumulatedSeconds,
                    bookTitle = state.readingBookTitle.ifBlank { _draft.value.bookTitle }
                )
            }
        }
    }

    fun resetReadingTimer() {
        viewModelScope.launch {
            repository.prefs.saveReadingTimerState(
                running = false,
                startMillis = 0L,
                accumulatedSeconds = 0L,
                bookTitle = _draft.value.bookTitle
            )
        }
    }

    fun commitReadingTimerToDraft() {
        viewModelScope.launch {
            val state = timerFlow.value
            val now = System.currentTimeMillis()
            val extraSeconds = if (state.readingTimerRunning && state.readingTimerStartMillis > 0L) {
                ((now - state.readingTimerStartMillis) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
            val totalSeconds = state.readingAccumulatedSeconds + extraSeconds
            val minutesToAdd = ((totalSeconds + 30L) / 60L).toInt().coerceAtLeast(if (totalSeconds > 0L) 1 else 0)
            if (minutesToAdd <= 0) {
                _bannerMessage.value = "Start the reading timer first to log reading minutes."
                return@launch
            }
            val updated = _draft.value.copy(
                bookReadingMinutes = (_draft.value.bookReadingMinutes + minutesToAdd).coerceAtMost(1440)
            )
            _draft.value = updated
            repository.prefs.saveReadingTimerState(
                running = false,
                startMillis = 0L,
                accumulatedSeconds = 0L,
                bookTitle = updated.bookTitle
            )
            // Save locally offline immediately (zero network cost until user saves/syncs)
            repository.saveDailyLogOfflineFirst(updated, attemptCloudSync = false)
            _bannerMessage.value = "Added $minutesToAdd min to Book Reading (saved offline)."
        }
    }

    // --- EXPLICIT SAVE & ON-DEMAND CLOUD SYNC ---

    fun saveDailyCheckIn() {
        viewModelScope.launch {
            _syncing.value = true
            val toSave = _draft.value.copy(
                devoteeUid = uid,
                counsellorUid = _profile.value.counsellorUid,
                dateKey = _selectedDateKey.value,
                weekStartKey = SadhanaScoringEngine.weekStartFor(_selectedDateKey.value)
            )
            val outcome = repository.saveDailyLogOfflineFirst(toSave, attemptCloudSync = true)
            // Cancel today's pending follow-up reminders once today's card is saved!
            if (toSave.dateKey == SadhanaScoringEngine.todayDateKey()) {
                SadhanaReminderScheduler.cancelFollowUpReminder(appContext)
            }
            _bannerMessage.value = when {
                outcome.remainingUnsynced == 0 -> "Sadhana saved & synced to cloud!"
                else -> "Saved offline on device. Tap Sync when internet returns."
            }
            _syncing.value = false
        }
    }

    fun syncPendingNow() {
        viewModelScope.launch {
            _syncing.value = true
            val outcome = repository.syncPendingLogs(uid)
            _bannerMessage.value = when {
                outcome.remainingUnsynced == 0 && outcome.syncedCount > 0 ->
                    "Synced ${outcome.syncedCount} sadhana log(s) to cloud."
                outcome.remainingUnsynced == 0 ->
                    "Everything is already synced."
                else ->
                    outcome.errorMessage ?: "Offline — ${outcome.remainingUnsynced} log(s) waiting to sync."
            }
            _syncing.value = false
        }
    }

    fun refreshFromCloud(silent: Boolean = false) {
        viewModelScope.launch {
            _refreshing.value = true
            repository.refreshDevoteeFromCloud(uid)
                .onSuccess { (freshProfile, freshFeedbacks) ->
                    if (freshProfile != null) {
                        _profile.value = freshProfile
                        onProfileRefreshed(freshProfile)
                    }
                    _feedbacks.value = freshFeedbacks.associateBy { it.weekStartKey }
                    if (!silent) {
                        _bannerMessage.value = "Synced latest targets & counsellor feedback."
                    }
                }
                .onFailure {
                    if (!silent) {
                        _bannerMessage.value = "Working offline — showing local sadhana records."
                    }
                }
            _refreshing.value = false
        }
    }

    // --- REMINDER SETTINGS ---

    fun updateReminderSettings(settings: ReminderSettings) {
        viewModelScope.launch {
            repository.prefs.saveReminderSettings(settings)
            if (settings.enabled) {
                SadhanaReminderScheduler.schedulePrimaryReminder(appContext, settings)
                _bannerMessage.value = String.format(
                    java.util.Locale.US,
                    "Daily reminder set for %02d:%02d%s",
                    settings.primaryHour,
                    settings.primaryMinute,
                    if (settings.followUpEnabled) " (+${settings.maxFollowUps} follow-ups every ${settings.followUpIntervalMinutes}m)" else ""
                )
            } else {
                SadhanaReminderScheduler.cancelAllReminders(appContext)
                _bannerMessage.value = "Daily sadhana reminders turned off."
            }
        }
    }

    private fun emptyDraftFor(dateKey: String, profile: UserProfile): DailySadhanaLog = DailySadhanaLog(
        id = SadhanaScoringEngine.logDocId(profile.uid, dateKey),
        devoteeUid = profile.uid,
        counsellorUid = profile.counsellorUid,
        dateKey = dateKey,
        weekStartKey = SadhanaScoringEngine.weekStartFor(dateKey),
        morningProgramAttended = false,
        chantingSlot = ChantingSlot.AFTER_1900.name,
        japaRounds = 0,
        japaBeads = 0,
        bookReadingMinutes = 0,
        bookTitle = "",
        morningClassAttended = false,
        personalHearingMinutes = 0,
        shlokaLearned = false,
        shlokaNote = "",
        studyMinutes = 0,
        toBedSlot = ToBedSlot.BEFORE_2130.name,
        wakeUpSlot = WakeUpSlot.BEFORE_0350.name,
        dayRestSlot = DayRestSlot.UNDER_30.name,
        cardFilledOnTime = SadhanaScoringEngine.isFillingOnTimeNow(dateKey),
        sevaMinutes = 0,
        sevaNote = ""
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    class Factory(
        private val initialProfile: UserProfile,
        private val appContext: Context,
        private val repository: SadhanaRepository,
        private val onProfileRefreshed: (UserProfile) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DevoteeViewModel(initialProfile, appContext, repository, onProfileRefreshed) as T
    }
}
