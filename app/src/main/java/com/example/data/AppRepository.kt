package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allRules: Flow<List<CallerRule>> = appDao.getAllRules()
    val recentLogs: Flow<List<AutomationLog>> = appDao.getRecentAutomationLogs()
    val recentCalls: Flow<List<RecentCall>> = appDao.getRecentCalls()
    val favorites: Flow<List<FavoriteContact>> = appDao.getAllFavorites()
    val spamNumbers: Flow<List<SpamNumber>> = appDao.getAllSpamNumbers()

    suspend fun getEnabledRules(): List<CallerRule> = appDao.getEnabledRules()

    suspend fun insertRule(rule: CallerRule): Long = appDao.insertRule(rule)

    suspend fun updateRule(rule: CallerRule) = appDao.updateRule(rule)

    suspend fun deleteRule(rule: CallerRule) = appDao.deleteRule(rule)

    suspend fun insertAutomationLog(log: AutomationLog): Long = appDao.insertAutomationLog(log)

    suspend fun clearAutomationLogs() = appDao.clearAutomationLogs()

    suspend fun insertRecentCall(call: RecentCall): Long = appDao.insertRecentCall(call)

    fun getCallHistoryForContact(numbers: List<String>, name: String): Flow<List<RecentCall>> =
        appDao.getCallHistoryForContact(numbers, name)

    suspend fun getCallHistoryForContactList(numbers: List<String>, name: String): List<RecentCall> =
        appDao.getCallHistoryForContactList(numbers, name)

    suspend fun getLatestRecentCallForNumber(phoneNumber: String): RecentCall? = appDao.getLatestRecentCallForNumber(phoneNumber)

    suspend fun updateRecentCall(call: RecentCall) = appDao.updateRecentCall(call)

    suspend fun getAllFavoritesList(): List<FavoriteContact> = appDao.getAllFavoritesList()

    suspend fun insertFavorite(contact: FavoriteContact): Long = appDao.insertFavorite(contact)

    suspend fun updateFavorite(contact: FavoriteContact) = appDao.updateFavorite(contact)

    suspend fun updateFavorites(contacts: List<FavoriteContact>) = appDao.updateFavorites(contacts)

    suspend fun deleteFavorite(contact: FavoriteContact) = appDao.deleteFavorite(contact)

    suspend fun getSpamByNumber(number: String): SpamNumber? = appDao.getSpamByNumber(number)

    suspend fun insertSpamNumber(spam: SpamNumber): Long = appDao.insertSpamNumber(spam)

    suspend fun deleteSpamNumber(spam: SpamNumber) = appDao.deleteSpamNumber(spam)

    suspend fun deleteSpamByNumber(number: String) = appDao.deleteSpamByNumber(number)

    suspend fun getIgnoredContactByNumber(number: String): IgnoredContact? = appDao.getIgnoredContactByNumber(number)

    val ignoredContacts: Flow<List<IgnoredContact>> = appDao.getAllIgnoredContacts()

    suspend fun insertIgnoredContact(ignored: IgnoredContact) = appDao.insertIgnoredContact(ignored)

    suspend fun deleteIgnoredContact(ignored: IgnoredContact) = appDao.deleteIgnoredContact(ignored)

    suspend fun deleteIgnoredContactByNumber(number: String) = appDao.deleteIgnoredContactByNumber(number)
}
