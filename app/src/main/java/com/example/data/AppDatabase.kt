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
    entities = [CallerRule::class, AutomationLog::class, RecentCall::class, FavoriteContact::class, SpamNumber::class, IgnoredContact::class, LocalContact::class],
    version = 10,
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
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).appDao()
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
