package com.example.data

import android.content.Context
import com.example.data.local.LocalPreferencesStore
import com.example.data.local.LocalSadhanaLogEntity
import com.example.data.local.SadhanaDatabase
import com.example.data.local.ServerConfigStore
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.DailySadhanaLog
import com.example.domain.InviteCodeRecord
import com.example.domain.OfflineTimerState
import com.example.domain.ReminderSettings
import com.example.domain.SadhanaScoringEngine
import com.example.domain.SadhanaTargets
import com.example.domain.UserProfile
import com.example.domain.UserRole
import com.example.domain.WeeklyFeedback
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class SyncOutcome(
    val syncedCount: Int,
    val remainingUnsynced: Int,
    val errorMessage: String? = null
)

class SadhanaRepository(
    context: Context,
    private val overrideDb: FirebaseFirestore? = null
) {
    private val appContext = context.applicationContext
    private val auth = Firebase.auth
    private val dao = SadhanaDatabase.getInstance(appContext).sadhanaLogDao()
    val prefs = LocalPreferencesStore(appContext)

    private val db: FirebaseFirestore
        get() = overrideDb ?: FirebaseFirestore.getInstance(
            ServerConfigStore.getActiveDatabaseId(appContext)
        )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing cloud data.")

    // --- OFFLINE-FIRST LOCAL OBSERVATION (ROOM + DATASTORE, ZERO CLOUD LISTENERS) ---

    fun observeLocalLogs(devoteeUid: String): Flow<List<DailySadhanaLog>> =
        dao.observeLogsForDevotee(devoteeUid).map { list -> list.map { it.toDomain() } }

    fun observeUnsyncedCount(devoteeUid: String): Flow<Int> =
        dao.observeUnsyncedCount(devoteeUid)

    val offlineTimerState: Flow<OfflineTimerState> = prefs.offlineTimerState
    val reminderSettings: Flow<ReminderSettings> = prefs.reminderSettings

    suspend fun getCachedProfile(uid: String): UserProfile? = prefs.getCachedUserProfile(uid)

    suspend fun isProfileCacheFresh(uid: String, maxAgeMillis: Long = 12L * 60L * 60L * 1000L): Boolean =
        prefs.isProfileCacheFresh(uid, maxAgeMillis)

    suspend fun clearLocalSessionCache() {
        prefs.clearCachedUser()
    }

    // --- ONE-SHOT PROFILE & ONBOARDING OPERATIONS ---

    suspend fun fetchUserProfileOnce(uid: String): Result<UserProfile?> {
        val path = "users/$uid"
        return try {
            require(uid.isNotBlank()) { "User ID cannot be blank" }
            val snap = db.collection("users").document(uid).get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (profile != null) {
                    prefs.cacheUserProfile(profile)
                }
                Result.success(profile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(
        displayName: String,
        email: String,
        role: UserRole,
        whatsappPhone: String = "",
        initialInviteCodeToJoin: String = ""
    ): Result<UserProfile> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "Devotee" }.take(80)
            val safeEmail = email.trim().take(120)
            val safeWa = whatsappPhone.trim().take(24)

            var counsellorUid = ""
            var counsellorName = ""
            var counsellorWhatsapp = ""
            var generatedCode = ""

            if (role == UserRole.DEVOTEE && initialInviteCodeToJoin.isNotBlank()) {
                val normalized = initialInviteCodeToJoin.trim().uppercase().filter { it.isLetterOrDigit() }
                if (normalized.length < 4 || normalized.length > 12) {
                    return Result.failure(IllegalArgumentException("Invite code must be 4–12 letters/numbers."))
                }
                val codeSnap = db.collection("inviteCodes").document(normalized).get().await()
                if (!codeSnap.exists()) {
                    return Result.failure(IllegalArgumentException("Invite code '$normalized' was not found. Check with your counsellor or skip for now."))
                }
                val record = codeSnap.toObject(InviteCodeRecord::class.java)
                if (record != null && record.counsellorUid.isNotBlank()) {
                    counsellorUid = record.counsellorUid
                    counsellorName = record.counsellorName.take(80)
                    counsellorWhatsapp = record.counsellorWhatsapp.take(24)
                }
            }

            if (role == UserRole.COUNSELLOR) {
                generatedCode = allocateUniqueInviteCode()
                path = "inviteCodes/$generatedCode"
                val codePayload = mapOf(
                    "code" to generatedCode,
                    "counsellorUid" to uid,
                    "counsellorName" to safeName,
                    "counsellorWhatsapp" to safeWa,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("inviteCodes").document(generatedCode).set(codePayload).await()
            }

            path = "users/$uid"
            val userPayload = mapOf(
                "uid" to uid,
                "displayName" to safeName,
                "email" to safeEmail,
                "role" to role.name,
                "inviteCode" to generatedCode,
                "whatsappPhone" to safeWa,
                "counsellorUid" to counsellorUid,
                "counsellorName" to counsellorName,
                "counsellorWhatsapp" to counsellorWhatsapp,
                "bookReadingTargetMinutes" to 360,
                "personalHearingTargetMinutes" to 180,
                "studyDailyTargetMinutes" to 120,
                "japaRoundsTarget" to 16,
                "readingTierLabel" to "Bhima (6h)",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userPayload).await()

            val created = UserProfile(
                uid = uid,
                displayName = safeName,
                email = safeEmail,
                role = role.name,
                inviteCode = generatedCode,
                whatsappPhone = safeWa,
                counsellorUid = counsellorUid,
                counsellorName = counsellorName,
                counsellorWhatsapp = counsellorWhatsapp,
                bookReadingTargetMinutes = 360,
                personalHearingTargetMinutes = 180,
                studyDailyTargetMinutes = 120,
                japaRoundsTarget = 16,
                readingTierLabel = "Bhima (6h)"
            )
            prefs.cacheUserProfile(created)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun allocateUniqueInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(6) {
            val candidate = (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
            val exists = db.collection("inviteCodes").document(candidate).get().await().exists()
            if (!exists) return candidate
        }
        return "HK" + (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    suspend fun updateOwnContactInfo(
        profile: UserProfile,
        newDisplayName: String,
        newWhatsappPhone: String
    ): Result<UserProfile> {
        var path = "users/${profile.uid}"
        return try {
            val uid = requireUserId()
            val safeName = newDisplayName.trim().ifBlank { profile.displayName }.take(80)
            val safeWa = newWhatsappPhone.trim().take(24)

            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to safeName,
                    "whatsappPhone" to safeWa,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (profile.userRole == UserRole.COUNSELLOR && profile.inviteCode.isNotBlank()) {
                path = "inviteCodes/${profile.inviteCode}"
                db.collection("inviteCodes").document(profile.inviteCode).update(
                    mapOf(
                        "counsellorName" to safeName,
                        "counsellorWhatsapp" to safeWa,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val updated = profile.copy(displayName = safeName, whatsappPhone = safeWa)
            prefs.cacheUserProfile(updated)
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun linkCounsellorByInviteCode(currentProfile: UserProfile, rawCode: String): Result<UserProfile> {
        val code = rawCode.trim().uppercase().filter { it.isLetterOrDigit() }
        if (code.length < 4 || code.length > 12) {
            return Result.failure(IllegalArgumentException("Enter a valid 4–12 character invite code."))
        }
        var path = "inviteCodes/$code"
        return try {
            val uid = requireUserId()
            val snap = db.collection("inviteCodes").document(code).get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("No counsellor found with code '$code'."))
            }
            val record = snap.toObject(InviteCodeRecord::class.java)
                ?: return Result.failure(IllegalStateException("Invalid invite code record."))

            if (record.counsellorUid == uid) {
                return Result.failure(IllegalArgumentException("You cannot link your own account as counsellor."))
            }

            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "counsellorUid" to record.counsellorUid,
                    "counsellorName" to record.counsellorName.take(80),
                    "counsellorWhatsapp" to record.counsellorWhatsapp.take(24),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update local Room logs so next sync carries the new counsellorUid
            dao.updateCounsellorForDevotee(uid, record.counsellorUid)

            val updated = currentProfile.copy(
                counsellorUid = record.counsellorUid,
                counsellorName = record.counsellorName.take(80),
                counsellorWhatsapp = record.counsellorWhatsapp.take(24)
            )
            prefs.cacheUserProfile(updated)

            // Best-effort sync of local logs so counsellor immediately sees history
            syncPendingLogs(uid)

            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- OFFLINE-FIRST DAILY LOG SAVING & EXPLICIT ONE-SHOT SYNC ---

    /**
     * Saves immediately to local Room DB (guaranteed offline success), then optionally pushes
     * unsynced logs via one-shot Firestore write when [attemptCloudSync] is true.
     */
    suspend fun saveDailyLogOfflineFirst(
        log: DailySadhanaLog,
        attemptCloudSync: Boolean = true
    ): SyncOutcome {
        val existingLocal = dao.getLogByDate(log.devoteeUid, log.dateKey)
        val entity = LocalSadhanaLogEntity.fromDomain(
            log = log,
            syncedToCloud = false,
            existsOnCloud = existingLocal?.existsOnCloud ?: false,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsert(entity)
        return if (attemptCloudSync && auth.currentUser?.uid == log.devoteeUid) {
            syncPendingLogs(log.devoteeUid)
        } else {
            val remaining = dao.getUnsyncedLogs(log.devoteeUid).size
            SyncOutcome(syncedCount = 0, remainingUnsynced = remaining)
        }
    }

    /**
     * Pushes ONLY locally modified logs (syncedToCloud == false) in one-shot Firestore writes.
     */
    suspend fun syncPendingLogs(devoteeUid: String): SyncOutcome {
        val unsynced = dao.getUnsyncedLogs(devoteeUid)
        if (unsynced.isEmpty()) {
            return SyncOutcome(syncedCount = 0, remainingUnsynced = 0)
        }
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != devoteeUid) {
            return SyncOutcome(syncedCount = 0, remainingUnsynced = unsynced.size, errorMessage = "Sign in to sync to cloud.")
        }

        var synced = 0
        var lastError: String? = null
        for (entity in unsynced) {
            val docPath = "sadhanaLogs/${entity.docId}"
            try {
                val docRef = db.collection("sadhanaLogs").document(entity.docId)
                val shouldUpdate = if (entity.existsOnCloud) {
                    true
                } else {
                    docRef.get().await().exists()
                }

                if (shouldUpdate) {
                    docRef.update(buildLogUpdateMap(entity)).await()
                } else {
                    docRef.set(buildLogCreateMap(entity)).await()
                }
                dao.markSynced(entity.docId)
                synced++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docPath)
                lastError = e.localizedMessage ?: "Offline — saved locally"
            }
        }
        val remaining = dao.getUnsyncedLogs(devoteeUid).size
        return SyncOutcome(syncedCount = synced, remainingUnsynced = remaining, errorMessage = lastError)
    }

    /**
     * Ultra-low-cost startup sync for Devotee:
     * - Always pushes pending unsynced local logs (0 ops if none pending).
     * - ONLY pulls full cloud history automatically when local Room DB is empty (fresh install / new device).
     * - Otherwise skips reading historical sadhanaLogs on app launch (0 reads!) and only refreshes
     *   the 1-document user profile if cache is older than 12 hours.
     */
    suspend fun syncDevoteeOnStartup(uid: String): Result<Pair<UserProfile?, List<WeeklyFeedback>?>> {
        return try {
            requireUserId()
            // 1. Push any offline pending check-ins first (0 ops if none pending)
            syncPendingLogs(uid)

            // 2. Fresh install / switched device? Pull cloud history once into local Room DB
            val localCount = dao.countLogsForDevotee(uid)
            if (localCount == 0) {
                return refreshDevoteeFromCloud(uid).map { (profile, feedbacks) -> profile to feedbacks }
            }

            // 3. Normal daily app open: 0 reads if profile cache is fresh (<12h), else 1 read for profile
            val freshProfile = if (prefs.isProfileCacheFresh(uid)) {
                null
            } else {
                fetchUserProfileOnce(uid).getOrNull()
            }
            Result.success(freshProfile to null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Explicit on-demand cloud refresh for Devotee (one-shot reads only — ZERO realtime listeners).
     * Refreshes profile (to pick up Counsellor target updates/WhatsApp), pulls cloud logs without
     * overwriting unsynced local edits, and fetches Counsellor weekly feedback notes.
     */
    suspend fun refreshDevoteeFromCloud(uid: String): Result<Pair<UserProfile?, List<WeeklyFeedback>>> {
        var path = "users/$uid"
        return try {
            requireUserId()
            // 1. Push local pending changes first so cloud stays up to date
            syncPendingLogs(uid)

            // 2. Refresh profile (picks up Counsellor-updated Reading/Hearing/Study targets & WhatsApp)
            val profile = fetchUserProfileOnce(uid).getOrNull()

            // 3. One-shot fetch of devotee's cloud sadhana logs
            path = "sadhanaLogs"
            val logSnap = db.collection("sadhanaLogs")
                .whereEqualTo("devoteeUid", uid)
                .get()
                .await()

            val unsyncedIds = dao.getUnsyncedLogs(uid).map { it.docId }.toSet()
            val merged = logSnap.documents.mapNotNull { doc ->
                val item = doc.toObject(DailySadhanaLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (item != null && doc.id !in unsyncedIds) {
                    LocalSadhanaLogEntity.fromDomain(
                        log = item.copy(id = doc.id),
                        syncedToCloud = true,
                        existsOnCloud = true,
                        updatedAtMillis = item.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                } else {
                    null
                }
            }
            if (merged.isNotEmpty()) {
                dao.upsertAll(merged)
            }

            // 4. One-shot fetch of Counsellor weekly feedback notes
            path = "weeklyFeedback"
            val fbSnap = db.collection("weeklyFeedback")
                .whereEqualTo("devoteeUid", uid)
                .get()
                .await()
            val feedbacks = fbSnap.toObjects(WeeklyFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.weekStartKey }

            Result.success(profile to feedbacks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    // --- COUNSELLOR ON-DEMAND QUERIES & MUTATIONS (ZERO REALTIME LISTENERS) ---

    suspend fun fetchAssignedDevoteesOnce(counsellorUid: String): Result<List<UserProfile>> {
        val path = "users"
        return try {
            requireUserId()
            val snap = db.collection("users")
                .whereEqualTo("counsellorUid", counsellorUid)
                .get()
                .await()
            val devotees = snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.displayName.lowercase() }
            Result.success(devotees)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchCounsellorLogsOnce(counsellorUid: String): Result<List<DailySadhanaLog>> {
        val path = "sadhanaLogs"
        return try {
            requireUserId()
            val snap = db.collection("sadhanaLogs")
                .whereEqualTo("counsellorUid", counsellorUid)
                .get()
                .await()
            val logs = snap.toObjects(DailySadhanaLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(logs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchCounsellorFeedbackOnce(counsellorUid: String): Result<List<WeeklyFeedback>> {
        val path = "weeklyFeedback"
        return try {
            requireUserId()
            val snap = db.collection("weeklyFeedback")
                .whereEqualTo("counsellorUid", counsellorUid)
                .get()
                .await()
            val items = snap.toObjects(WeeklyFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Counsellor-exclusive mutation updating a devotee's spiritual targets.
     * Matches Action 2 in firestore.rules strictly.
     */
    suspend fun updateDevoteeTargets(
        devoteeUid: String,
        targets: SadhanaTargets
    ): Result<Unit> {
        val path = "users/$devoteeUid"
        return try {
            requireUserId()
            require(devoteeUid.isNotBlank()) { "Devotee UID required" }
            val safeBook = targets.bookReadingTargetMinutes.coerceIn(30, 2400)
            val safeHearing = targets.personalHearingTargetMinutes.coerceIn(30, 1800)
            val safeStudy = targets.studyDailyTargetMinutes.coerceIn(15, 720)
            val safeJapa = targets.japaRoundsTarget.coerceIn(1, 64)
            val safeLabel = targets.readingTierLabel.trim().ifBlank { "Custom" }.take(40)

            db.collection("users").document(devoteeUid).update(
                mapOf(
                    "bookReadingTargetMinutes" to safeBook,
                    "personalHearingTargetMinutes" to safeHearing,
                    "studyDailyTargetMinutes" to safeStudy,
                    "japaRoundsTarget" to safeJapa,
                    "readingTierLabel" to safeLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWeeklyFeedback(
        devoteeUid: String,
        counsellorName: String,
        weekStartKey: String,
        blessingTag: String,
        note: String
    ): Result<WeeklyFeedback> {
        val docId = SadhanaScoringEngine.feedbackDocId(devoteeUid, weekStartKey)
        val path = "weeklyFeedback/$docId"
        return try {
            val counsellorUid = requireUserId()
            val safeNote = note.trim().take(1000)
            if (safeNote.isEmpty()) {
                return Result.failure(IllegalArgumentException("Please enter a short feedback note."))
            }
            val safeTag = blessingTag.trim().take(60)
            val safeCounsellorName = counsellorName.trim().ifBlank { "Counsellor" }.take(80)

            val docRef = db.collection("weeklyFeedback").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "counsellorName" to safeCounsellorName,
                        "blessingTag" to safeTag,
                        "note" to safeNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "devoteeUid" to devoteeUid,
                        "counsellorUid" to counsellorUid,
                        "counsellorName" to safeCounsellorName,
                        "weekStartKey" to weekStartKey,
                        "blessingTag" to safeTag,
                        "note" to safeNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                WeeklyFeedback(
                    id = docId,
                    devoteeUid = devoteeUid,
                    counsellorUid = counsellorUid,
                    counsellorName = safeCounsellorName,
                    weekStartKey = weekStartKey,
                    blessingTag = safeTag,
                    note = safeNote
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PAYLOAD BUILDERS (1:1 PARITY WITH firestore.rules) ---

    private fun buildLogCreateMap(e: LocalSadhanaLogEntity): Map<String, Any> = mapOf(
        "devoteeUid" to e.devoteeUid,
        "counsellorUid" to e.counsellorUid,
        "dateKey" to e.dateKey,
        "weekStartKey" to e.weekStartKey,
        "morningProgramAttended" to e.morningProgramAttended,
        "chantingSlot" to e.chantingSlot,
        "japaRounds" to e.japaRounds.coerceIn(0, 128),
        "japaBeads" to e.japaBeads.coerceIn(0, 108),
        "bookReadingMinutes" to e.bookReadingMinutes.coerceIn(0, 1440),
        "bookTitle" to e.bookTitle.take(120),
        "morningClassAttended" to e.morningClassAttended,
        "personalHearingMinutes" to e.personalHearingMinutes.coerceIn(0, 1440),
        "shlokaLearned" to e.shlokaLearned,
        "shlokaNote" to e.shlokaNote.take(500),
        "studyMinutes" to e.studyMinutes.coerceIn(0, 1440),
        "toBedSlot" to e.toBedSlot,
        "wakeUpSlot" to e.wakeUpSlot,
        "dayRestSlot" to e.dayRestSlot,
        "cardFilledOnTime" to e.cardFilledOnTime,
        "sevaMinutes" to e.sevaMinutes.coerceIn(0, 1440),
        "sevaNote" to e.sevaNote.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun buildLogUpdateMap(e: LocalSadhanaLogEntity): Map<String, Any> = mapOf(
        "counsellorUid" to e.counsellorUid,
        "morningProgramAttended" to e.morningProgramAttended,
        "chantingSlot" to e.chantingSlot,
        "japaRounds" to e.japaRounds.coerceIn(0, 128),
        "japaBeads" to e.japaBeads.coerceIn(0, 108),
        "bookReadingMinutes" to e.bookReadingMinutes.coerceIn(0, 1440),
        "bookTitle" to e.bookTitle.take(120),
        "morningClassAttended" to e.morningClassAttended,
        "personalHearingMinutes" to e.personalHearingMinutes.coerceIn(0, 1440),
        "shlokaLearned" to e.shlokaLearned,
        "shlokaNote" to e.shlokaNote.take(500),
        "studyMinutes" to e.studyMinutes.coerceIn(0, 1440),
        "toBedSlot" to e.toBedSlot,
        "wakeUpSlot" to e.wakeUpSlot,
        "dayRestSlot" to e.dayRestSlot,
        "cardFilledOnTime" to e.cardFilledOnTime,
        "sevaMinutes" to e.sevaMinutes.coerceIn(0, 1440),
        "sevaNote" to e.sevaNote.take(500),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
