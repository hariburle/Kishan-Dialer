package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM caller_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<CallerRule>>

    @Query("SELECT * FROM caller_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<CallerRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CallerRule): Long

    @Update
    suspend fun updateRule(rule: CallerRule)

    @Delete
    suspend fun deleteRule(rule: CallerRule)

    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAutomationLogs(): Flow<List<AutomationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationLog(log: AutomationLog): Long

    @Query("DELETE FROM automation_logs")
    suspend fun clearAutomationLogs()

    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC LIMIT 100")
    fun getRecentCalls(): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber IN (:numbers) OR callerName = :name ORDER BY timestamp DESC LIMIT 100")
    fun getCallHistoryForContact(numbers: List<String>, name: String): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber IN (:numbers) OR callerName = :name ORDER BY timestamp DESC LIMIT 100")
    suspend fun getCallHistoryForContactList(numbers: List<String>, name: String): List<RecentCall>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecentCallForNumber(phoneNumber: String): RecentCall?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentCall(call: RecentCall): Long

    @Update
    suspend fun updateRecentCall(call: RecentCall)

    @Query("SELECT * FROM favorite_contacts ORDER BY sortOrder ASC, id ASC")
    fun getAllFavorites(): Flow<List<FavoriteContact>>

    @Query("SELECT * FROM favorite_contacts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllFavoritesList(): List<FavoriteContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(contact: FavoriteContact): Long

    @Update
    suspend fun updateFavorite(contact: FavoriteContact)

    @Update
    suspend fun updateFavorites(contacts: List<FavoriteContact>)

    @Delete
    suspend fun deleteFavorite(contact: FavoriteContact)

    @Query("SELECT * FROM offline_spam_numbers ORDER BY reportCount DESC")
    fun getAllSpamNumbers(): Flow<List<SpamNumber>>

    @Query("SELECT * FROM offline_spam_numbers WHERE phoneNumber = :number LIMIT 1")
    suspend fun getSpamByNumber(number: String): SpamNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumber(spam: SpamNumber): Long

    @Delete
    suspend fun deleteSpamNumber(spam: SpamNumber)

    @Query("DELETE FROM offline_spam_numbers WHERE phoneNumber = :number")
    suspend fun deleteSpamByNumber(number: String)
}
