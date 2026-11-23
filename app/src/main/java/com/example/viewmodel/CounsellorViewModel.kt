package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SadhanaRepository
import com.example.domain.DailySadhanaLog
import com.example.domain.SadhanaScoringEngine
import com.example.domain.SadhanaTargets
import com.example.domain.UserProfile
import com.example.domain.WeeklyFeedback
import com.example.domain.WeeklySadhanaSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevoteeRosterEntry(
    val profile: UserProfile,
    val currentWeekSummary: WeeklySadhanaSummary,
    val totalLogsCount: Int
)

data class CounsellorUiState(
    val counsellorProfile: UserProfile,
    val devotees: List<UserProfile> = emptyList(),
    val logsByDevotee: Map<String, List<DailySadhanaLog>> = emptyMap(),
    val feedbacksByKey: Map<String, WeeklyFeedback> = emptyMap(),
    val selectedDevoteeUid: String? = null,
    val selectedWeekStartKey: String = SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey()),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val bannerMessage: String? = null
) {
    val currentWeekStartKey: String
        get() = SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey())

    val rosterEntries: List<DevoteeRosterEntry>
        get() = devotees.map { dev ->
            val logs = logsByDevotee[dev.uid].orEmpty()
            val byDate = logs.associateBy { it.dateKey }
            DevoteeRosterEntry(
                profile = dev,
                currentWeekSummary = SadhanaScoringEngine.summarizeWeek(currentWeekStartKey, byDate, dev.targets),
                totalLogsCount = logs.size
            )
        }

    val selectedDevotee: UserProfile?
        get() = devotees.firstOrNull { it.uid == selectedDevoteeUid }

    val selectedDevoteeWeekSummary: WeeklySadhanaSummary?
        get() {
            val dev = selectedDevotee ?: return null
            val byDate = logsByDevotee[dev.uid].orEmpty().associateBy { it.dateKey }
            return SadhanaScoringEngine.summarizeWeek(selectedWeekStartKey, byDate, dev.targets)
        }

    val selectedDevoteeRecentWeeks: List<WeeklySadhanaSummary>
        get() {
            val dev = selectedDevotee ?: return emptyList()
            val logs = logsByDevotee[dev.uid].orEmpty()
            val byDate = logs.associateBy { it.dateKey }
            val currentWk = currentWeekStartKey
            val weeks = ((0..5).map { SadhanaScoringEngine.shiftWeek(currentWk, -it) } + logs.map { it.weekStartKey })
                .distinct()
                .sortedDescending()
                .take(8)
            return weeks.map { wk -> SadhanaScoringEngine.summarizeWeek(wk, byDate, dev.targets) }
        }

    val selectedWeekFeedback: WeeklyFeedback?
        get() {
            val devUid = selectedDevoteeUid ?: return null
            return feedbacksByKey[SadhanaScoringEngine.feedbackDocId(devUid, selectedWeekStartKey)]
        }
}

class CounsellorViewModel(
    initialProfile: UserProfile,
    private val repository: SadhanaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounsellorUiState(counsellorProfile = initialProfile))
    val uiState: StateFlow<CounsellorUiState> = _uiState.asStateFlow()

    init {
        // One-shot initial fetch only (zero realtime listeners!)
        refreshRoster(silent = true)
    }

    fun updateCounsellorProfile(profile: UserProfile) {
        _uiState.update { it.copy(counsellorProfile = profile) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun selectDevotee(devoteeUid: String?) {
        _uiState.update {
            it.copy(
                selectedDevoteeUid = devoteeUid,
                selectedWeekStartKey = SadhanaScoringEngine.weekStartFor(SadhanaScoringEngine.todayDateKey())
            )
        }
    }

    fun shiftSelectedWeek(deltaWeeks: Int) {
        _uiState.update {
            it.copy(selectedWeekStartKey = SadhanaScoringEngine.shiftWeek(it.selectedWeekStartKey, deltaWeeks))
        }
    }

    fun selectWeek(weekStartKey: String) {
        _uiState.update { it.copy(selectedWeekStartKey = weekStartKey) }
    }

    /**
     * Explicit on-demand refresh fetching assigned devotees, logs, and feedback in 3 one-shot queries.
     */
    fun refreshRoster(silent: Boolean = false) {
        val counsellorUid = _uiState.value.counsellorProfile.uid
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val devoteesRes = repository.fetchAssignedDevoteesOnce(counsellorUid)
            val logsRes = repository.fetchCounsellorLogsOnce(counsellorUid)
            val fbRes = repository.fetchCounsellorFeedbackOnce(counsellorUid)

            val devotees = devoteesRes.getOrElse { _uiState.value.devotees }
            val logsByDev = logsRes.getOrNull()?.groupBy { it.devoteeUid } ?: _uiState.value.logsByDevotee
            val feedbacks = fbRes.getOrNull()?.associateBy {
                SadhanaScoringEngine.feedbackDocId(it.devoteeUid, it.weekStartKey)
            } ?: _uiState.value.feedbacksByKey

            val errorMsg = devoteesRes.exceptionOrNull()?.localizedMessage
                ?: logsRes.exceptionOrNull()?.localizedMessage

            _uiState.update {
                it.copy(
                    devotees = devotees,
                    logsByDevotee = logsByDev,
                    feedbacksByKey = feedbacks,
                    loading = false,
                    bannerMessage = when {
                        errorMsg != null -> "Could not refresh: $errorMsg"
                        !silent -> "Refreshed ${devotees.size} assigned devotee(s)."
                        else -> it.bannerMessage
                    }
                )
            }
        }
    }

    /**
     * Counsellor-exclusive action: set personalized weekly Reading, Hearing, Study, and Japa targets.
     */
    fun saveDevoteeTargets(devoteeUid: String, targets: SadhanaTargets) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            repository.updateDevoteeTargets(devoteeUid, targets)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedList = state.devotees.map { dev ->
                            if (dev.uid == devoteeUid) {
                                dev.copy(
                                    bookReadingTargetMinutes = targets.bookReadingTargetMinutes,
                                    personalHearingTargetMinutes = targets.personalHearingTargetMinutes,
                                    studyDailyTargetMinutes = targets.studyDailyTargetMinutes,
                                    japaRoundsTarget = targets.japaRoundsTarget,
                                    readingTierLabel = targets.readingTierLabel
                                )
                            } else {
                                dev
                            }
                        }
                        state.copy(
                            devotees = updatedList,
                            saving = false,
                            bannerMessage = "Updated sadhana targets — proportional weekly scores recalculated."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            saving = false,
                            bannerMessage = err.localizedMessage ?: "Failed to update devotee targets."
                        )
                    }
                }
        }
    }

    fun saveWeeklyFeedback(devoteeUid: String, weekStartKey: String, blessingTag: String, note: String) {
        val counsellorName = _uiState.value.counsellorProfile.displayName
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            repository.saveWeeklyFeedback(
                devoteeUid = devoteeUid,
                counsellorName = counsellorName,
                weekStartKey = weekStartKey,
                blessingTag = blessingTag,
                note = note
            ).onSuccess { saved ->
                _uiState.update { state ->
                    val key = SadhanaScoringEngine.feedbackDocId(devoteeUid, weekStartKey)
                    state.copy(
                        feedbacksByKey = state.feedbacksByKey + (key to saved),
                        saving = false,
                        bannerMessage = "Weekly guidance saved to devotee's card."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        saving = false,
                        bannerMessage = err.localizedMessage ?: "Failed to save feedback note."
                    )
                }
            }
        }
    }

    class Factory(
        private val initialProfile: UserProfile,
        private val repository: SadhanaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CounsellorViewModel(initialProfile, repository) as T
    }
}
