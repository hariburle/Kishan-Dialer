# Kishan Dialer — Sprint Roadmap & ToDo (Phase 3 Milestone)

## 📋 Architectural Principles
1. **Manageable Execution Sprints**: Work is divided into 5 targeted, self-contained phases to allow incremental code checkpoints, rapid compilation verification, and zero quota exhaustion.
2. **Platform & Design Consistency**: Every enhancement adheres strictly to Material Design 3 guidelines, fluid cross-device layout principles, and Android Telecom framework lifecycles.
3. **Continuous Tracking**: Each task has an explicit status indicator (`[ ]` Pending, `[🔄]` In Progress, `[x]` Completed) to be updated as code checkpoints are committed and verified.
4. **Once features are implemented move them to "ToDo.md" file which is a master file for tracking enhancements and implementation status.
5. Fav card order should also be persisted.
6. When in 'Ask and Learn' mode even after calls were placed i don't see the preferred calling option displayed on the fav cards
7. In contact list when message and calling options are shown, i see the whatsApp calling icon is smaller than Phone icon, these should be same and the preferred icon should be highlighted (without increasing the icon size)
8. In configure mode of fav panel, unable to x popular call cards to be ignored, i thought this was already implemented.
9. afer using the app for a few times and i change the fav card formats, the material style cards get a border unable to figure out why that happens, investigate and fix it.
---
## Uncategorized Bugs/Requests - review and move these items to relevant phases - keep this section
1. When a whatsapp call is initated from the app, even though the call actually happens over WhatsApp, can we still keep a log of the call in recents ? 
2. Do we need a 'search' option in recents ? if so what is the best way to implement it.
## 🚀 Phase Breakdown

### Phase 1: Visual Refinements & Keypad Stability
*Focus: UI polish, touch stability, and search UX consistency.*

- [x] **Task 1: Favorite Card Style Refinements — Concise Naming & Thin Light Theme Borders**
  - **User Item**: #1 ("Quick Action style borders are too wide and looks awful in light theme, make them thin") & #2 ("For Fav card designs, let us give shorter names for the three styles, for the Material You Experience style the border looks very wide in Light theme, need this border to look smaller.")
  - **Analysis**:
    - Favorite Card style labels in Settings/Rules are currently long ("Bento Grid Layout", "Compact Action Grid", "Material You Experience").
    - In Light Theme, both the "Material You Experience" style and "Quick-Action" style render borders that appear too thick, stark, or wide compared to clean modern Material 3 surfaces.
  - **Implementation Plan**:
    - Shorten card style labels in `RulesScreen.kt` / `FavCardDesign` to clean, concise names: `"Bento"`, `"Grid"`, and `"Material You"`.
    - Refine border strokes for `FavCardDesign.MATERIAL_YOU` and `FavCardDesign.QUICK_ACTION` in `FavoritesScreen.kt` and `RulesScreen.kt`:
      - Apply a subtle, thinner border stroke (e.g. 0.5.dp or 0.75.dp hairline) using `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)` in light theme instead of wide/heavy outlines.
      - Synchronize selection preview cards in Rules/Settings so they reflect the new thin-border styling and shorter labels.
  - **Status**: [x] Completed

- [x] **Task 2: Universal Search Placeholder & Default Favorites Panel**
  - **User Item**: #2 ("In Search bar say 'search by name or number' in all search fields; default panel on opening is Favorites (update settings)")
  - **Analysis**:
    - Placeholder strings across search bars currently vary ("Search contacts...", "Search...").
    - Default startup destination may currently default to Keypad/Dialer or Recents instead of Favorites.
  - **Implementation Plan**:
    - Update all search field placeholder texts in `FavoritesScreen.kt`, `ContactsScreen.kt`, and `DialerScreen.kt` to: `"Search by name or number"`.
    - Set default startup navigation tab to `Favorites` (index 0) in `MainActivity.kt` and `MainViewModel.kt`.
    - Provide a setting under App Settings for "Default Startup Panel" (Favorites, Recents, Contacts, Keypad), with "Favorites" as the default.
  - **Status**: [x] Completed

- [x] **Task 12: Keypad Dial Button Internal Highlight Geometry**
  - **User Item**: #12 ("In key pad panel when a preferred dialing option is known the highlight around the dial button is making the keys move up... make the highlight within the circle of the button and not outside.. so the size of the button does not change")
  - **Analysis**: When a preferred dialing method (Phone vs WhatsApp) is detected, an outer border or dynamic padding on the dial button expands its outer bounds, pushing the dialpad grid upward and shifting keys.
  - **Implementation Plan**:
    - Lock the outer dimensions of the dial button container to a fixed size (e.g., `56.dp` x `56.dp` or matching height).
    - Render any preference highlight ring *inside* the button boundary (e.g., using `Modifier.border(...)` with inner padding, or an inner radial canvas ring) without increasing external margins or layout height.
    - Ensure zero layout jitter occurs when toggling between numbers or preferred dialing states.
  - **Status**: [x] Completed

---

### Phase 2: Contact Creation & Visual Pop-up Quality
*Focus: Elevating dialog design, avatar actions, and contact management workflows.*

- [x] **Task 4: Comprehensive Pop-up & Dialog Overhaul**
  - **User Item**: #4 ("Also review all pop-up panels and make sure the have all the necessary actions and make the panels looks decent and professional - i remember seeing the add contact panel to be pretty pale and dry - you know what i mean.")
  - **Analysis**: Dialogs (`CreateContactDialog`, `ContactDetailsBottomSheet`, `AddFavoriteDialog`, `EditFavoriteDialog`, `AudioOutputSelectorDialog`, `MultiNumberCallDialog`) need visual elevation: pleasant surface tints, rounded corners (28dp), clear iconography, section headers, avatar previews, and responsive primary buttons.
  - **Implementation Plan**:
    - `CreateContactDialog`: Add a clean avatar preview monogram with dynamic tint, clear input fields with leading icons (Person, Phone, Label, Account), pill selectors for destination (Google vs Device vs App-Only), and a vibrant primary "Save Contact" button.
    - `ContactDetailsBottomSheet`: Polish header typography, quick-action chips, and history section cards with subtle tonal container styling.
    - `AddFavoriteDialog` & `EditFavoriteDialog`: Add speed-dial slot chips, channel preference toggles, and clear confirmation actions.
    - Ensure standard 48dp touch targets and high-contrast accessibility compliance across all popups.
  - **Status**: [x] Completed

- [x] **Task 8: Recents List Unknown Number '+' Action & Plural Naming**
  - **User Item**: #8 ("In the recents list, when i see a number's details showing there is a large + sign in place of picture , what is the intent ? is there a way to add that number to contacts by providing other details and saving it ? should the panel be called Recent or Recents or something else ?")
  - **Analysis**:
    - Unknown numbers display a '+' monogram, but tapping it does not launch a save workflow.
    - Tab naming should follow standard mobile convention: "Recents" (plural).
  - **Implementation Plan**:
    - Rename tab and screen header to `"Recents"`.
    - Make the large '+' avatar interactive with a tooltip/content description ("Add to Contacts").
    - Tapping the '+' avatar (or tapping an "Add to Contacts" action button in the item details) opens `CreateContactDialog` pre-filled with that phone number.
    - After saving, dynamically update the call log display to show the newly created contact name.
  - **Status**: [x] Completed

---

### Phase 3: Data Freshness, Initial Experience & Deep-Linking
*Focus: First-run experience, database sync, and notification interactivity.*

- [x] **Task 3: Eliminate Initial 4-Card Flash in Favorites**
  - **User Item**: #3 ("Favorites panel still flashes 4 cards before showing my actual favorites")
  - **Analysis**: The favorites list currently initializes with a fallback list of 4 dummy/sample cards before Room emits the real persisted favorites, causing an unwanted visual flash.
  - **Implementation Plan**:
    - Remove hardcoded default dummy favorite items from ViewModel initial state.
    - Use `StateFlow<List<FavoriteContact>>` initialized to `emptyList()` or a clean shimmer/loading skeleton that only displays if data is loading.
    - Transition smoothly into real favorites or the empty "No favorites yet" layout without flashing dummy cards.
  - **Status**: [x] Completed

- [x] **Task 5: Fresh Install Call Log Import**
  - **User Item**: #5 ("on a fresh install why are the recent calls panel empty ?")
  - **Analysis**: The app currently only stores calls initiated or received within the app itself in Room. On a fresh install, prior system call history from Android's `CallLog.Calls` provider is not imported, leaving the screen empty.
  - **Implementation Plan**:
    - Implement a system call log synchronization helper (`CallLogSyncHelper.kt`) that queries `android.provider.CallLog.Calls` when `READ_CALL_LOG` permission is granted.
    - Seed Room with the latest system call records on first run or permission grant.
    - Ensure deduplication prevents re-importing existing entries.
  - **Status**: [x] Completed

- [x] **Task 7: Missed Call Notification Deep-Link & Highlighting**
  - **User Item**: #7 ("I saw a missed call notification but when i touched it nothing happened, i was expecting it to open the recent calls with that call highlighted.")
  - **Analysis**: The missed call notification pending intent either lacked intent extras or was not intercepted by `MainActivity` to switch tabs and highlight the corresponding call.
  - **Implementation Plan**:
    - Update `OngoingCallNotificationHelper` / `CallNotificationReceiver` to attach `EXTRA_NAV_TAB = "RECENTS"` and `EXTRA_HIGHLIGHT_NUMBER = number` to the missed call notification content intent.
    - In `MainActivity.kt` (`onCreate` and `onNewIntent`), observe intent extras, switch selected tab to `Recents`, and notify `MainViewModel`.
    - In `CallLogScreen.kt`, auto-scroll to and pulse/highlight the missed call item.
  - **Status**: [x] Completed

- [x] **Task 13: Persist Favorite Card Deletion in Configure Mode**
  - **User Item**: #13 ("When a favoutire card id deleted in the configure mode of fav panel it reappears during next refresh, the idead of delete a favorite card is to unmark that favourite.")
  - **Analysis**: In `FavoritesScreen.kt`, deleting a card while in configure mode only updates temporary local UI state (`localFavorites`) without invoking `viewModel.deleteFavorite(contact)` to unmark or delete it from the Room database. On state refresh or recomposition, Room emits the persisted favorites list again and the card reappears.
  - **Implementation Plan**:
    - Update the configure-mode card deletion handler in `FavoritesScreen.kt` to trigger `onDeleteFavorite(contact)`, calling `MainViewModel.deleteFavorite(contact)` to unmark `isFavorite = false` / delete the item from the Room database.
    - Keep `localFavorites` synchronized with Room's `Flow<List<FavoriteContact>>` so deleted cards stay permanently removed across refreshes.
  - **Status**: [x] Completed

---

### Phase 4: Adaptive Channel Routing & Dedicated Spam Center
*Focus: Smart communication preferences and clean settings modularity.*

- [x] **Task 6: Bento Style 'Phone' Labeling & 'Ask and Learn' Dual Dialers**
  - **User Item**: #6 ("for Bento Style, call it 'Phone' if the WhatsApp calling preference is 'Ask and Learn' , will it make sense to show both Phone and WhatsApp dialers when we don't know the preference ? and only the preferred mode when we know.")
  - **Analysis**: When the preference is "Ask and Learn" (undetermined), showing only one generic button or ambiguous label causes confusion.
  - **Implementation Plan**:
    - In Bento style favorite cards:
      - If calling preference is "Ask and Learn" (undetermined/equal frequency), show dual action buttons: Phone (labeled "Phone") and WhatsApp (labeled "WhatsApp").
      - Once the preferred mode is learned through user actions (e.g. user predominantly makes Phone or WhatsApp calls), collapse or highlight the preferred mode as the primary prominent dialer, keeping a secondary mini-action for the alternate.
  - **Status**: [x] Completed

- [x] **Task 11: Dedicated Spam & Blocked Screen / Window**
  - **User Item**: #11 ("Show spam in a different window instead of hogging the settings panel")
  - **Analysis**: Spam settings, blocked number lists, and community spam scores currently occupy significant space inside the general Settings / Rules screen.
  - **Implementation Plan**:
    - Create a dedicated `SpamScreen.kt` or `SpamManagementDialog.kt` ("Spam & Blocked").
    - Move blocked numbers, community spam rules, auto-block sensitivity sliders, and spam activity logs into this dedicated view.
    - Replace the cluttered section in Settings with a clean entry row: "Spam & Blocked Calls" with an indicator badge (e.g., "12 blocked numbers").
  - **Status**: [x] Completed

---

### Phase 5: Gesture Navigation & Secondary Gesture Actions
*Focus: Seamless navigation and user-configurable gestures.*

- [x] **Task 9: Swipe to Switch Panels & Sub-Tabs with Enable/Disable Setting**
  - **User Item**: #9 ("Allow me to swipe on the screen to switch between panels and if the panel has tabs then a swipe should switch the tab or else switch the panel if its the last tab in that direction, make that a setting to enable/disable.")
  - **Analysis**: Users want intuitive horizontal swipe navigation between main tabs (Favorites <-> Recents <-> Contacts <-> Keypad <-> Settings) and sub-tabs.
  - **Implementation Plan**:
    - Implement a `HorizontalPager` or nested drag gesture handler at the root content level.
    - Add boundary detection: if a screen has internal sub-tabs (e.g. Contacts A-Z or All/App-Only), swiping moves through the sub-tabs first; at the first/last sub-tab, the swipe transitions to the neighboring main panel.
    - Add a toggle in App Settings: "Swipe to switch panels" (Default: Enabled).
  - **Status**: [x] Completed

- [x] **Task 10: Secondary Gestures When Panel Swipe Is Disabled**
  - **User Item**: #10 ("If swipe to switch panels is disabled, use the swipe for anything else useful while not confusing it with any Android gestures (i tend to swipe to close the panels at times.")
  - **Analysis**: When panel swiping is turned off, swipe gestures can be repurposed for productive list actions without conflicting with system back gestures.
  - **Implementation Plan**:
    - When "Swipe to switch panels" is disabled:
      - Recents & Contacts list items support swipe actions: swipe right to dial, swipe left to send SMS / open WhatsApp.
      - Bottom sheets and detail pop-ups support smooth pull/swipe down to close.
      - Keep edge margins safe (min 24dp from screen edge) to prevent interference with Android system predictive back gestures.
  - **Status**: [x] Completed

---

## 📊 Execution & Checkpoint Summary Table

| Phase | Tasks | Key Focus Area | Status |
| :--- | :--- | :--- | :--- |
| **Phase 1** | Tasks 1, 2, 12 | Shorter card style names, thin Light theme borders, Search text, Keypad dial geometry | ✅ Completed |
| **Phase 2** | Tasks 4, 8 | Dialog redesigns, Recents '+' contact creation, "Recents" naming | ✅ Completed |
| **Phase 3** | Tasks 3, 5, 7, 13 | Zero dummy flash, Fresh install call log sync, Missed call deep-link, Configure mode delete persistence | ✅ Completed |
| **Phase 4** | Tasks 6, 11 | Bento Ask & Learn dialers, Dedicated Spam window | ✅ Completed |
| **Phase 5** | Tasks 9, 10 | Horizontal panel/tab swipe navigation, Secondary gestures | ✅ Completed |
