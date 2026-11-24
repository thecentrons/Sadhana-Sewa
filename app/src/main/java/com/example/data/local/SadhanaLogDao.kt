package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SadhanaLogDao {
    @Query("SELECT * FROM local_sadhana_logs WHERE devoteeUid = :uid ORDER BY dateKey DESC")
    fun observeLogsForDevotee(uid: String): Flow<List<LocalSadhanaLogEntity>>

    @Query("SELECT * FROM local_sadhana_logs WHERE devoteeUid = :uid AND dateKey = :dateKey LIMIT 1")
    suspend fun getLogByDate(uid: String, dateKey: String): LocalSadhanaLogEntity?

    @Query("SELECT * FROM local_sadhana_logs WHERE devoteeUid = :uid AND syncedToCloud = 0 ORDER BY dateKey ASC")
    suspend fun getUnsyncedLogs(uid: String): List<LocalSadhanaLogEntity>

    @Query("SELECT COUNT(*) FROM local_sadhana_logs WHERE devoteeUid = :uid AND syncedToCloud = 0")
    fun observeUnsyncedCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalSadhanaLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LocalSadhanaLogEntity>)

    @Query("UPDATE local_sadhana_logs SET syncedToCloud = 1, existsOnCloud = 1 WHERE docId = :docId")
    suspend fun markSynced(docId: String)

    @Query("UPDATE local_sadhana_logs SET counsellorUid = :counsellorUid, syncedToCloud = 0 WHERE devoteeUid = :uid")
    suspend fun updateCounsellorForDevotee(uid: String, counsellorUid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM local_sadhana_logs WHERE devoteeUid = :uid AND dateKey = :dateKey)")
    suspend fun hasLogForDate(uid: String, dateKey: String): Boolean

    @Query("SELECT COUNT(*) FROM local_sadhana_logs WHERE devoteeUid = :uid")
    suspend fun countLogsForDevotee(uid: String): Int
}
