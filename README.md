# 📞 Kishan Dialer — Smart Android Phone & Call Manager

[![Android](https://img.shields.io/badge/Android-11%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-purple.svg)](https://developer.android.com/jetpack/compose)

**Kishan Dialer** is an intelligent, feature-rich native Android Telecom phone dialer designed for modern Android devices. Built with **Jetpack Compose (Material 3)**, **Kotlin Coroutines/StateFlow**, and Android's native **Telecom `InCallService`**, it combines T9 smart search, automated call screening/gate buzzer rules, persistent drag-and-drop VIP favorite grid reordering, carrier STIR/SHAKEN spam protection, and dual-SIM management into a fast, private, dark-mode-first experience.

---

## 🚀 Key Features & Capabilities

### 1. 🎹 Smart T9 Dialer & Keypad
- **Ergonomic Flex Keypad**: Responsive 3x4 dialpad tiles designed to fill screen width with high touch targets.
- **T9 Search**: Instant T9 matching on digits that searches contact names, nicknames, and phone numbers as you type.
- **Pause (`,`) & Wait (`;`) Support**:
  - Long-press `*` key to enter a 2-second Pause (`,`).
  - Long-press `#` key to enter a Wait (`;`).
  - Dedicated overflow menu (`⋮`) on the number bar to insert Pause or Wait directly.
- **Quick Recents Bar**: Displays your most recent caller avatars right above the keypad when the input field is empty for instant redialing.
- **Long-Press Backspace Clear**: Long-press the backspace button to clear the entire number field with haptic feedback.

---

### 2. ⭐ VIP Favorites Hub & Grid Management
- **2-Column Responsive Grid**: Beautiful horizontal favorite cards showing contact avatars, full names, nicknames, phone numbers, and custom labels.
- **Continuous Multi-Row Drag-and-Drop Reordering**:
  - Unlock Configure Mode via the top-right checkmark button.
  - Touch and drag any card smoothly across 1, 2, 5, or 10 rows.
  - Cards stay 100% pinned under your finger on top of all grid items (`zIndex(10000f)`).
  - Surrounding cards automatically slide out of the way (`animateItem()`).
  - Custom grid order is saved to Room DB & `SharedPreferences` for persistence across reinstalls.
- **Large Touch-Friendly Reorder Arrows**:
  - Accessible **34x34dp directional buttons** (`▲` Up Row, `▼` Down Row, `◄` Left Col, `►` Right Col) designed for easy tapping with large thumbs.
  - Directional buttons are dynamically enabled **only if there is space to move in that direction**.
- **1-Tap Speed Dial Shortcuts**: Assign numbers 2–9 to VIP favorites for instant long-press dialing on the keypad.

---

### 3. 📜 Call History & Recent Logs
- **Merged Call Logging**: Merges system call logs (`CallLog.Calls`) and local app call history with zero duplicate entries.
- **Grouped Recents**: Automatically groups consecutive calls from the same caller with expandable call history lists and duration stats.
- **Post-Call Notes & Callback Reminders**: Add timestamped notes to recent call records immediately after hanging up.
- **Call History Search & Deletion**: Delete individual calls or clear all call logs for a specific contact from the overflow menu.

---

### 4. 📇 Unified Contact Directory
- **Phone Contacts + App Local Contacts**: Integrates both your device/Google Contacts and private app-only contacts (`local_contacts` Room table) into a single clean list.
- **A-Z Alphabet Scroller**: Quick-jump indexed alphabet bar for fast navigation through large contact books.
- **WhatsApp Integration**: Detects WhatsApp availability and displays instant WhatsApp call buttons alongside cellular call buttons.
- **Right-Thumb Ergonomic Actions**: Expanded contact detail rows arrange action buttons (Call, WhatsApp, SMS, Favorite, Edit) for comfortable one-handed right-thumb reach.

---

### 5. 🤖 Automated Call Screening & Gate Buzzer Rules
- **Carrier Spam Filter Bypass**: Automatically whitelists incoming calls that match an active **User Automation Rule**, **Starred Favorite**, or **Saved Contact**, ensuring gate buzzers and VIP contacts are NEVER auto-rejected by carrier STIR/SHAKEN filters.
- **Gate / Intercom Buzzer Recipe**:
  - *Trigger*: Incoming call from Gate / Lobby number (e.g., `+1 469-731-3343` or `5550199`).
  - *Action*: Auto-answers after 1 second $\rightarrow$ Sends in-band DTMF `9#` $\rightarrow$ Auto-hangs up after 2 seconds $\rightarrow$ Sends optional confirmation SMS.
- **Custom Rule Builder**: Create rules triggered by exact numbers or 10-digit patterns with customizable auto-answer delays, DTMF key sequences, auto-hangup delays, and auto-reply SMS messages.

---

### 6. 📞 In-Call Experience & Floating Status Pill
- **Soft Keyboard Auto-Dismissal**: Automatically hides the soft keyboard when an outbound or inbound call connects, ensuring in-call controls are never obscured.
- **Active Call Controls**: Mute, Speaker, Hold, Audio Route Selector (Earpiece, Speaker, Bluetooth), and In-Call Keypad for DTMF navigation.
- **Floating Call Pill**: When an active call is minimized, a floating status pill appears at the top of the screen displaying call status, duration, and an instant red Hangup button.

---

## 🛠️ How to Use

### Setting Up a Gate / Intercom Buzzer Rule
1. Open the **Rules** tab from the bottom navigation bar.
2. Tap **+ Add New Rule**.
3. Enter the Gate / Intercom phone number (e.g. `4697313343`).
4. Enable **Auto Answer** (set delay to 1 sec).
5. Enter the DTMF sequence required to unlock the door (e.g. `9#` or `4#`).
6. Enable **Auto Hangup** (set delay to 2 sec).
7. Tap **Save Rule**.
8. *Result*: When your gate rings, the dialer automatically picks up, presses `9#` to open the gate, and hangs up without you touching your phone!

### Reordering Favorites
1. Go to the **Favorites** tab.
2. Tap the top-right **Configure Mode** checkmark button (`✓`).
3. **Option A (Touch Drag)**: Press and hold any favorite card, then drag it up, down, left, or right across rows. Surrounding cards will slide out of the way. Release to drop.
4. **Option B (Directional Arrow Buttons)**: Use the large high-contrast arrow buttons on each card (`▲` Up Row, `▼` Down Row, `◄` Left Col, `►` Right Col) to shift the card one step at a time.
5. Tap the top-right checkmark button again to lock in the new layout. Your custom order is automatically saved and persisted across app reinstalls.

### Dialing Numbers with Pause (`,`) or Wait (`;`)
1. Open the **Keypad** tab.
2. Type the base phone number (e.g. `18005550100`).
3. **To add a 2-second Pause**: Long-press the `*` key or tap `⋮` (overflow menu) $\rightarrow$ **Add 2-sec pause (,)**.
4. **To add a Wait (pause until confirmed)**: Long-press the `#` key or tap `⋮` $\rightarrow$ **Add wait (;)**.
5. Type the extension or PIN code (e.g. `104#`).
6. Tap the green **Call** button.

---

## 📦 Download & Installation

The latest pre-built signed debug executable is available directly in the project root repository:

- **[SmartDialer.apk](SmartDialer.apk)** — Ready to install on any Android 11+ (API 30+) device.

---

## 💻 Building from Source

To compile the project using Android Studio or Gradle command line:

```bash
# Clone the repository
git clone https://github.com/your-username/Kishan-Dialer.git
cd Kishan-Dialer

# Build Debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be output to: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Permissions & Security

Kishan Dialer requires standard telephony permissions to operate as your default phone handler:
- `READ_CONTACTS` & `WRITE_CONTACTS`: Sync contacts, nicknames, and starred favorites.
- `CALL_PHONE` & `MANAGE_OWN_CALLS`: Initiate and manage phone calls via Android Telecom.
- `READ_CALL_LOG` & `WRITE_CALL_LOG`: Display and manage recent call history.
- `ANSWER_PHONE_CALLS`: Auto-answer calls for user-configured automation rules.
- `SEND_SMS`: Send optional auto-response SMS messages for busy/gate rules.

*All user data, call logs, contacts, and automation rules remain 100% private and stored locally on your device in Room SQLite database.*
