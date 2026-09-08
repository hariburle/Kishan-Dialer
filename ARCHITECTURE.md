# Kishan Dialer — System Architecture & Technical Documentation

## 1. Executive Summary

**Kishan Dialer** is a native Android Telecom dialer and call management application built with Jetpack Compose, Kotlin Coroutines/StateFlow, Room SQLite database, and Android Telecom framework (`InCallService`).

The app unifies phone contacts, app-created local contacts, T9 smart dialing, automated call screening/rules, carrier STIR/SHAKEN spam detection, and persistent drag-and-drop VIP favorite shortcuts into a clean Material 3 design.

---

## 2. Core Components & Architectural Overview

```
+---------------------------------------------------------------------------------+
|                                 MainActivity                                    |
|   (Hosts Bottom Navigation, Tab Bar, Floating In-Call Pill, and Root Insets)    |
+---------------------------------------------------------------------------------+
        |
        +---> MainViewModel (StateFlow, Repositories, Preferences Sync)
                |
                +---> AppRepository & AppDatabase (Room DB v10)
                |       ├── LocalContact
                |       ├── RecentCall
                |       ├── FavoriteContact
                |       ├── AutomationRule
                |       ├── IgnoredContact
                |       └── OfflineSpamNumber
                |
                +---> ContactHelper (System Contacts Provider & CallLog Merging)
                |
                +---> CallManager (Telecom InCallService & Automation Rule Pipeline)
```

### Key UI Screens & Components
- **`FavoritesScreen.kt`**: VIP grid hub featuring:
  - Configure Mode for drag-and-drop reordering.
  - Local state tracking (`localFavorites`) for zero-latency, continuous multi-row drag-and-drop.
  - Hit-testing against real-time physical screen bounds (`activeItemBounds[contact.id]`).
  - Top-layer `Box` overlay (`zIndex = 10000f`) ensuring dragged cards never draw under other cards.
  - Direct positional swapping (`Collections.swap`) for natural grid reordering.
  - Persistent sort order synced to Room and `SharedPreferences` (`favorite_sort_orders`).
- **`DialerScreen.kt`**: Ergo-width T9 smart keypad featuring:
  - Quick Recents bar shown when the keypad field is empty.
  - Overflow menu `⋮` on number bar providing "Add 2-sec pause (,)" and "Add wait (;)".
  - Long-press `*` -> `,` (Pause) and `#` -> `;` (Wait).
  - Long-press Backspace button clearing the entire number field with haptic feedback.
  - Flex-weight keys filling the horizontal width without vertical scrolling.
- **`CallLogScreen.kt`**: Rich call history merging system `CallLog.Calls` and Room `recent_calls` using unique composite keys (`"${group.primaryCall.id}_${group.primaryCall.timestamp}_$index"`), preventing LazyColumn key duplication crashes.
- **`ContactsScreen.kt`**: Unified directory aggregating system and local contacts into single person rows with right-thumb ergonomic action buttons on expanded rows.
- **`InCallScreen.kt`**: Active call UI with automatic soft keyboard dismissal upon connect, active call controls (Mute, Speaker, Hold, Audio Output Selector, Keypad), and post-call notes.
- **`MainActivity.kt`**: Hosts `FloatingCallPill` when an active call is minimized (`activeCall != null && activeCall.state != STATE_DISCONNECTED`), offering status bar inset shielding and a red hangup button.

---

## 3. Data Persistence & Migration

- **Database Version**: Bumped to Version 10 in `AppDatabase.kt`.
- **Migration Strategy**: `fallbackToDestructiveMigration(dropAllTables = true)` used during development to purge obsolete schema tables. Fake test seed data completely removed.
- **Local Contacts Table (`local_contacts`)**: Stores app-created contacts locally without requiring Google Account synchronization.
- **Favorites Sort Order (`favorite_sort_orders`)**: `SharedPreferences` string set storing `phoneNumber:sortOrder:name` mappings, ensuring custom grid order is restored immediately on app launch or reinstall.

---

## 4. Call Management & Automation Priority

```
Incoming Call Arrives
        │
        ▼
Evaluate Automation Rules (CallManager.kt)
  ├── Match rule 10-digit pattern (matchesRulePattern)
  └── Execute Rule Actions (e.g. Auto-Answer, DTMF sequence, Auto-Hangup, Auto-SMS)
        │
        ├── Rule Matched ──> Execute Action & Finish
        │
        └── No Rule Matched
                │
                ▼
      Evaluate Spam Blocklist & STIR/SHAKEN
        ├── Check local offline spam database
        └── Check Connection.VERIFICATION_STATUS_FAILED
                │
                ├── Is Spam ──> Reject / Silence
                └── Is Safe ──> Ring & Display Incoming Screen
```

---

## 5. Recent Fixes & Quality Upgrades

1. **Unified Contact Creation**: `CreateContactDialog` allows selecting between "Phone Contacts" and "App Only" local storage.
2. **Keypad UX**: Added Quick Recents bar, Pause/Wait overflow menu, long-press `,` / `;` T9 subtext, and long-press Backspace clear.
3. **In-Call Screen Keyboard Overlap**: Automatically hides soft keyboard when call connects (`LocalSoftwareKeyboardController.current?.hide()`).
4. **Recents Panel Crash Fix**: Resolved duplicate key exceptions in `LazyColumn` by generating unique composite keys for merged system/local call logs.
6. **Gate Buzzer & User Rule/Contact Whitelisting over Carrier Spam Filter**:
   - `CallManager.isWhitelistedOrRuleMatched` checks if an incoming number matches an active Automation Rule (e.g. Gate / Intercom Buzzer), Starred Favorites, or Saved Contacts BEFORE applying carrier STIR/SHAKEN or carrier spam checks.
   - Prevents legitimate gate buzzers or user contacts from being wrongly auto-rejected or flagged as carrier spam threats.

7. **Directional Grid Reordering Controls**:
   - In Configure Mode, every card provides explicit directional arrow buttons (`▲` Up Row, `▼` Down Row, `◄` Left Column, `►` Right Column).
   - Each directional button is dynamically enabled **only if there is space to move in that direction** (e.g., `▲` is enabled only if `index >= 2` in a 2-column grid).
