package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CallerRule::class, AutomationLog::class, RecentCall::class, FavoriteContact::class, SpamNumber::class, IgnoredContact::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "telecom_dialer_db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).appDao()
                            dao.insertRule(
                                CallerRule(
                                    name = "Apartment Gate Intercom",
                                    phoneNumberPattern = "5550199",
                                    isEnabled = true,
                                    autoAnswer = true,
                                    answerDelaySec = 1,
                                    dtmfSequence = "9#",
                                    dtmfDelayMs = 800,
                                    sendSms = true,
                                    smsMessage = "Gate buzzer triggered via Auto-Dialer automation.",
                                    autoHangup = true,
                                    hangupDelaySec = 2
                                )
                            )
                            dao.insertRule(
                                CallerRule(
                                    name = "Office Extension Direct Dial",
                                    phoneNumberPattern = "18005550100",
                                    isEnabled = true,
                                    autoAnswer = true,
                                    answerDelaySec = 2,
                                    dtmfSequence = "104#",
                                    dtmfDelayMs = 1200,
                                    sendSms = false,
                                    smsMessage = "",
                                    autoHangup = false,
                                    hangupDelaySec = 0
                                )
                            )
                            dao.insertRecentCall(
                                RecentCall(
                                    phoneNumber = "555-0199",
                                    callType = 1,
                                    timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                                    durationSeconds = 14,
                                    ruleMatched = "Apartment Gate Intercom"
                                )
                            )
                            dao.insertRecentCall(
                                RecentCall(
                                    phoneNumber = "+1 (800) 555-0100",
                                    callType = 2,
                                    timestamp = System.currentTimeMillis() - 1000 * 60 * 90,
                                    durationSeconds = 85,
                                    ruleMatched = "Office Extension Direct Dial"
                                )
                            )
                            dao.insertRecentCall(
                                RecentCall(
                                    phoneNumber = "+1 (555) 234-5678",
                                    callType = 3,
                                    timestamp = System.currentTimeMillis() - 1000 * 60 * 360,
                                    durationSeconds = 0,
                                    ruleMatched = null
                                )
                            )
                            // Initial Seed Favorites shown immediately when app opens
                            dao.insertFavorite(
                                FavoriteContact(
                                    name = "Apartment Gate",
                                    phoneNumber = "5550199",
                                    label = "Intercom",
                                    avatarColor = 0xFF16A34AL,
                                    speedDialSlot = 4
                                )
                            )
                            dao.insertFavorite(
                                FavoriteContact(
                                    name = "Mom",
                                    phoneNumber = "+1 (555) 234-5678",
                                    label = "Family",
                                    avatarColor = 0xFFDC2626L,
                                    speedDialSlot = 2
                                )
                            )
                            dao.insertFavorite(
                                FavoriteContact(
                                    name = "Sarah Jenkins",
                                    phoneNumber = "+1 (555) 890-1234",
                                    label = "Mobile",
                                    avatarColor = 0xFF2563EBL,
                                    speedDialSlot = 3
                                )
                            )
                            dao.insertFavorite(
                                FavoriteContact(
                                    name = "Office IVR",
                                    phoneNumber = "18005550100",
                                    label = "Work",
                                    avatarColor = 0xFFD97706L,
                                    speedDialSlot = 5
                                )
                            )
                            // Seed Offline Spam Database
                            dao.insertSpamNumber(
                                SpamNumber(
                                    phoneNumber = "+18005550199",
                                    label = "Robocall / Auto-dialer",
                                    reportCount = 428,
                                    isBlocked = true
                                )
                            )
                            dao.insertSpamNumber(
                                SpamNumber(
                                    phoneNumber = "+18885550144",
                                    label = "Suspected Fraud / IRS Scam",
                                    reportCount = 890,
                                    isBlocked = true
                                )
                            )
                            dao.insertSpamNumber(
                                SpamNumber(
                                    phoneNumber = "+19005550123",
                                    label = "High Risk Telemarketer",
                                    reportCount = 312,
                                    isBlocked = true
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
