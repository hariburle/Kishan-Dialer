# OmniDial — Ongoing Changelog & Running Work Log

> **Purpose**: This file serves as our real-time running log of every enhancement, UI refinement, and bug fix. As new changes are made, append them directly under `[Unreleased]` so nothing is ever forgotten when publishing future release notes and website updates.

---

## 📌 [Unreleased] (Upcoming Release — v1.2.0 / Build 3)

### 🌟 Enhancements (User-Facing)
- **Prominent Speed Dial Calling**: In the speed dial shortcut pop-up, the **Call** option is now framed as a prominent, elevated call button with instant contact name recognition, while secondary actions (`Reassign`, `Clear`, `Cancel`) are styled cleanly as subtle text links to keep dialing fast and focused.
- **Speed Dial Confirmation Setting**: Added an accidental touch protection toggle in Settings allowing users to choose whether long-pressing keys (2–9) opens a confirmation popup or dials instantly.
- **Unassigned Key Assignment Prompt**: Added a dedicated setting and prompt dialog when long-pressing unassigned keypad keys, asking whether you would like to assign a contact to that speed dial number.

### 🐛 Fixes & Polish (User-Facing)
- **Speed Dial UI Polish**: Added dedicated number badge `#X` and formatted phone number labels inside the speed dial dialog for improved readability.

### 🔧 Technical / Architecture Notes
- Added `confirmSpeedDialCall` and `askToAssignUnassignedSpeedDial` StateFlows in `MainViewModel.kt` backed by `SharedPreferences`.
- Added serialization and restoration support in `BackupManager.kt`.
- Updated `DialerScreen.kt` with `promptAssignSlotTarget` and prominent button styling.

---

## 🚀 [v1.1.0] — Build 2 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Cleaner 4-Button Dial Pad**: Quickly reach anyone with 4 organized buttons on the keypad: Send Text, Regular Phone Call, WhatsApp Message, or WhatsApp Voice Call (replaces scrolling carousels).
- **Smart Call Suggestions**: The app gently highlights how you usually contact each person using subtle high-contrast borders, eliminating guesswork.
- **Car Bluetooth & Hands-Free Calling**: Outgoing calls initiated from vehicle infotainment systems, smartwatches, or Bluetooth headsets automatically route through WhatsApp when preferred.
- **Default Phone Number Selection for Favorites**: Starred contacts with multiple numbers (work, home, mobile) allow explicitly choosing which line is dialed by default.
- **Crisp Dark Mode Contrast**: WhatsApp icons and action buttons feature high-contrast outlines to stay sharp on dark AMOLED screens.
- **Friendlier Nicknames**: Personal nicknames appear front-and-center on your favorites grid with legal names cleanly subtitled below.

### 🐛 Fixes & Polish
- **WhatsApp Call Confirmation Routing**: Resolved bug where tapping WhatsApp call on favorite cards could accidentally trigger cellular calls or bypass confirmation.
- **Back Navigation on Search**: Pressing the Android back button when searching favorites or contacts cleanly dismisses the search bar rather than closing the screen.
- **Bi-directional Nickname Sync**: Ensured nickname changes in device contacts and the in-app editor immediately stay in lockstep.

### 🔧 Technical Notes
- Implemented Android Telecom `CallRedirectionService` (`OmniCallRedirectionService`).
- Updated `DialerScreen.kt` call action area to a fixed 2x2 grid with adaptive `colorScheme.primary` container borders.
- Added white contour outer path stroke in `WhatsAppIcon.kt`.
- Added multi-number picker flow to `FavoritesScreen.kt`.

---

## 📦 [v1.0.0] — Build 1 (September 2026)

### 🌟 Enhancements (User-Facing)
- **Dual Calling in One App**: Seamlessly place regular carrier phone calls or WhatsApp voice calls from a single unified dialer.
- **Smart Gate & Intercom Buzzer**: Auto-answers entrance intercoms, transmits door DTMF tones (e.g. `9#`), and hangs up automatically.
- **Instant T9 Search**: Fast contact and recent call search as you type letters or numbers on the dial pad.
- **Flip to Silence**: Turn phone face-down to immediately silence incoming rings.
- **Customizable VIP Favorites Hub**: Keep priority contacts within thumb's reach with customizable grid cards and 1-tap speed dial shortcuts.

---

### 📝 How to Update This Running Log
Whenever making code changes:
1. Add new items to `[Unreleased]` under the appropriate subhead.
2. Phrase user-facing bullets in terms of real-world everyday benefits.
3. Keep technical notes under `🔧 Technical / Architecture Notes` for engineering context.
4. When releasing a new version, promote `[Unreleased]` into a tagged release section and update `RELEASE.md` + website.
