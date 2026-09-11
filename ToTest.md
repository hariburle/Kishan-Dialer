# Kishan Dialer — Interactive Testing Checklist (`ToTest.md`)

Mark `[x]` on the checkbox for each task once you verify it. Each section below includes the exact test steps and expected results.

## 📋 Quick Checkpoints
- [ ] **Task 1 (Phase 1)** — Bento/Grid/Material card names & thin Light theme borders
- [ ] **Task 2 (Phase 1)** — Search placeholder `"Search by name or number"` & Bento/Slide defaults
- [ ] **Task 12 (Phase 1)** — Keypad dial button highlight ring inside circle (no key movement)
- [ ] **Task 4 (Phase 2)** — Dialog overhaul (rounded corners, avatar monograms, clean styling)
- [ ] **Task 8 (Phase 2)** — Recents `"Recents"` naming, `PersonAdd (+)` avatar & `"Add to Contacts"` workflow
- [ ] **Task 3 (Phase 3)** — Zero initial 4-card flash in Favorites on app launch
- [ ] **Task 5 (Phase 3)** — Fresh install call log import & Auto-Backup persistence across reinstalls
- [ ] **Task 7 (Phase 3)** — Missed call notification deep-link & auto-scroll highlight
- [ ] **Task 13 (Phase 3)** — Instant configure-mode favorite deletion persistence
- [ ] **Task 6 (Phase 4)** — Bento `"Phone"` label, `"Ask and Learn"` dual dialers & WhatsApp call logging in Recents
- [ ] **Task 11 (Phase 4)** — Dedicated Spam & Blocked window + `"International Numbers"` setting label
- [ ] **Task 9 (Phase 5)** — Swipe left/right to switch between main panels
- [ ] **Task 10 (Phase 5)** — Secondary item-level swipe gestures when panel swiping is disabled

---

## 🧪 Detailed Test Scripts & Verification Steps

### 1. Task 1 (Phase 1): Favorite Card Style Refinements
- [ ] **Verified**
- **Test Steps**:
  1. Open the app and navigate to **Rules / Settings** (Tab 4).
  2. Scroll down to the **Favorite Card Style** section.
  3. Verify style names are concise: **Bento**, **Grid**, and **Material**.
  4. Select **Material** or **Grid** style in Light Theme and switch to the **Favorites** tab.
- **Expected Result**:
  - Style names in Settings/Rules are short and clean (**Bento**, **Grid**, **Material**).
  - Card borders in Light Theme display an ultra-thin `0.5.dp` hairline outline without heavy/thick dark borders.

### 2. Task 2 (Phase 1): Universal Search Placeholder & Default Settings
- [ ] **Verified**
- **Test Steps**:
  1. Inspect search fields in **Favorites** (Tab 0), **Contacts** (Tab 3), and **Contact Picker Dialog** (when adding a favorite/rule).
  2. Clear app data or inspect default settings in **Settings** (Tab 4).
  3. Force-close and re-open the app.
- **Expected Result**:
  - Search field placeholders consistently read: `"Search by name or number"`.
  - Default settings are pre-configured:
    - **Favorite Card Theme**: Bento
    - **WhatsApp Calling**: Ask & Learn
    - **Call Answering Style**: Horizontal Slide
  - App opens directly to the **Favorites** panel (Tab 0) by default.

### 3. Task 12 (Phase 1): Keypad Dial Button Internal Highlight Geometry
- [ ] **Verified**
- **Test Steps**:
  1. Navigate to the **Keypad** (Tab 2).
  2. Enter a phone number for which a preferred channel (Phone vs WhatsApp) is detected.
- **Expected Result**:
  - The preference highlight ring renders *inside* the circular dial button container.
  - The dial button outer dimensions stay locked to `56.dp`, and zero vertical jitter or key shifting occurs when entering digits.

---

### 4. Task 4 (Phase 2): Comprehensive Pop-up & Dialog Overhaul
- [ ] **Verified**
- **Test Steps**:
  1. Open **Contacts** (Tab 3) and tap **+ New Contact**.
  2. Open **Recents** (Tab 1) or **Favorites** (Tab 0) and tap a contact row to open **Contact Details Bottom Sheet**.
  3. Edit a favorite contact or trigger the Audio Output Selector during a call.
- **Expected Result**:
  - Dialogs feature modern `24.dp`/`28.dp` rounded corners, pleasant surface container tints, leading icons (`Person`, `Phone`, `Label`), monogram avatar previews, and responsive primary buttons.

### 5. Task 8 (Phase 2): Recents List Unknown Number '+' Action & "Recents" Naming
- [ ] **Verified**
- **Test Steps**:
  1. Check bottom navigation bar label and header for Tab 1.
  2. Locate an unknown/unsaved number in **Recents** (Tab 1) and tap the row to view details.
  3. Tap the **PersonAdd (+)** avatar or the **"Add to Contacts"** primary button.
  4. Save the contact with a name in `CreateContactDialog`.
- **Expected Result**:
  - Navigation tab is named **"Recents"** (plural).
  - Unknown number details view displays a `PersonAdd` avatar with a `+` badge and a prominent `"Add to Contacts"` button.
  - Tapping `"Add to Contacts"` opens `CreateContactDialog` pre-filled with the phone number, updating the caller name across the app immediately upon saving.

---

### 6. Task 3 (Phase 3): Eliminate Initial 4-Card Flash in Favorites
- [ ] **Verified**
- **Test Steps**:
  1. Force-close the app and re-launch it on the **Favorites** tab.
  2. Observe the initial screen render before database emissions complete.
- **Expected Result**:
  - No 4 dummy/popular cards flash before real favorites load.
  - If no favorites are starred, the screen displays a clean, static *"No Favorites Added Yet"* empty layout.

### 7. Task 5 (Phase 3): Fresh Install Call Log Import & Auto-Backup
- [ ] **Verified**
- **Test Steps**:
  1. Install a fresh build or clear app data with `READ_CALL_LOG` permission granted.
  2. Open the **Recents** tab immediately upon first launch.
  3. Reinstall or update the app on a device with Google Auto-Backup.
- **Expected Result**:
  - System call history from `CallLog.Calls` automatically seeds into Room SQLite, populating the Recents list immediately on fresh install.
  - Learned choices and ignored popular contacts persist across app updates/reinstalls via Android Auto-Backup rules (`backup_rules.xml` & `data_extraction_rules.xml`).

### 8. Task 7 (Phase 3): Missed Call Notification Deep-Link & Highlighting
- [ ] **Verified**
- **Test Steps**:
  1. Simulate an unanswered incoming call (or generate a missed call notification).
  2. Tap the missed call notification from the Android notification shade.
- **Expected Result**:
  - Tapping the notification opens the app directly to the **Recents** tab (Tab 1).
  - The screen auto-scrolls to the missed call item and highlights it with a primary container border.

### 9. Task 13 (Phase 3): Persist Favorite Card Deletion in Configure Mode
- [ ] **Verified**
- **Test Steps**:
  1. On the **Favorites** tab, tap the **Configure (Tune)** button to enter edit mode.
  2. Tap the **Delete (Trash)** icon on a favorite card.
  3. Refresh the screen or restart the app.
- **Expected Result**:
  - The card is removed instantly from local state and deleted/unmarked in Room SQLite without popping up blocking prompts or reappearing on refresh/restart.

---

### 10. Task 6 (Phase 4): Bento Style "Phone" Labeling, "Ask & Learn" Dual Dialers & WhatsApp Call Logging
- [ ] **Verified**
- **Test Steps**:
  1. Set **Favorite Card Style** to **Bento** in Settings/Rules.
  2. Set **WhatsApp Calling** to **Ask and Learn** in Settings/Rules.
  3. View a Bento favorite card for a contact with no learned channel preference.
  4. Trigger a WhatsApp call from the app (via Bento card, Contact sheet, or Dialer).
  5. Return to the app and check the **Recents** tab.
- **Expected Result**:
  - Cellular calling button label is **"Phone"** (not "Direct Call").
  - For contacts with undetermined preference ("Ask and Learn"), Bento cards display side-by-side **dual buttons** (**Phone** & **WhatsApp**).
  - Every WhatsApp call initiated from the app is immediately logged into **Recents** with caller name, number, timestamp, and a `"WhatsApp Call"` badge.

### 11. Task 11 (Phase 4): Dedicated Spam Window & International Numbers Option
- [ ] **Verified**
- **Test Steps**:
  1. Open **Rules / Settings** (Tab 4).
  2. Inspect the **WhatsApp Calling Integration Mode** options.
  3. Locate the **Protection & Spam** entry card and check the indicator badge (e.g. `"{X} blocked"`).
  4. Tap **Spam & Blocked Calls** to open the dedicated window.
  5. Test searching blocked numbers, tapping **Block** to add a number, and tapping **Unblock**.
- **Expected Result**:
  - International WhatsApp option in Settings is named **"International Numbers"** with the description: *"Directs numbers outside your country (+1 for US, +91 for India, etc.) to WhatsApp automatically."*
  - The long embedded spam list in Settings is replaced with a clean, single entry row.
  - Tapping the entry opens `SpamManagementDialog`, allowing complete management of blocked numbers, search, manual blocking, auto-block carrier spam toggles, and unblocking.

---

### 12. Task 9 (Phase 5): Swipe to Switch Main Panels
- [ ] **Verified**
- **Test Steps**:
  1. Ensure **"Swipe to switch panels"** is **Enabled** in **Rules / Settings** (Tab 4).
  2. Horizontally swipe left or right across the screen on any main panel (**Favorites** <-> **Recents** <-> **Keypad** <-> **Contacts** <-> **Settings**).
- **Expected Result**:
  - Smooth horizontal swipe navigation seamlessly switches between the 5 main app panels.

### 13. Task 10 (Phase 5): Secondary Gestures When Panel Swiping Is Disabled
- [ ] **Verified**
- **Test Steps**:
  1. Go to **Rules / Settings** (Tab 4) and disable **"Swipe to switch panels"**.
  2. Try swiping horizontally on main screens.
  3. Perform item-level swipes in **Recents** or **Contacts**.
- **Expected Result**:
  - Main panel horizontal swiping is disabled.
  - List items support item-level swipe gestures (e.g., swipe right to call, swipe left for details/WhatsApp) without triggering full-screen tab switches.

---

### 💬 Feedback & Notes (If any test fails or needs adjustment)
*(Write any notes here, or type them in our chat)*
