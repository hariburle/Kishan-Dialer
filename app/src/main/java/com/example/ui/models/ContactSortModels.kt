package com.example.ui.models

enum class ContactSortBy { FIRST_NAME, LAST_NAME }
enum class ContactSortOrder { ASCENDING, DESCENDING }
enum class ContactSourceFilter { ALL, APP_ONLY, DEVICE }
enum class SmartContactSort(val label: String, val emoji: String) {
    A_Z("All (A-Z)", "🔤"),
    RECENT("Recent", "🕒"),
    FREQUENT("Frequent", "🔥"),
    LONG_TIME("Long Time No Talk", "🕰️"),
    REDISCOVER("Rediscover", "🎲")
}
