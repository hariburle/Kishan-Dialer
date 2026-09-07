package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caller_rules")
data class CallerRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumberPattern: String,
    val isEnabled: Boolean = true,
    val autoAnswer: Boolean = true,
    val answerDelaySec: Int = 1,
    val dtmfSequence: String = "",
    val dtmfDelayMs: Long = 800L,
    val sendSms: Boolean = false,
    val smsMessage: String = "",
    val autoHangup: Boolean = false,
    val hangupDelaySec: Int = 2
)

@Entity(tableName = "automation_logs")
data class AutomationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val phoneNumber: String,
    val ruleName: String,
    val actionsSummary: String,
    val status: String // "SUCCESS", "EXECUTING", "FAILED"
)

@Entity(tableName = "recent_calls")
data class RecentCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val photoUri: String? = null,
    val callType: Int, // 1: Incoming, 2: Outgoing, 3: Missed
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val ruleMatched: String? = null,
    val simSlot: Int = 1,
    val isSpam: Boolean = false,
    val note: String? = null,
    val reminderTime: Long? = null,
    val callReason: String? = null,
    val communityTag: String? = null
)

@Entity(tableName = "favorite_contacts")
data class FavoriteContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nickname: String? = null,
    val phoneNumber: String,
    val label: String = "Mobile",
    val avatarColor: Long = 0xFF2563EB,
    val photoUri: String? = null,
    val speedDialSlot: Int? = null, // 1 to 9
    val sortOrder: Int = 0
)

@Entity(tableName = "offline_spam_numbers")
data class SpamNumber(
    @PrimaryKey val phoneNumber: String,
    val label: String = "Suspected Spam",
    val reportCount: Int = 1,
    val isBlocked: Boolean = true
)

@Entity(tableName = "ignored_contacts")
data class IgnoredContact(
    @PrimaryKey val phoneNumber: String,
    val name: String = "",
    val category: String = "",
    val tag: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

