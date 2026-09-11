# Kishan Dialer — Sprint Roadmap & ToDo (Phase 3 Milestone)

## 📋 Architectural Principles
1. **Manageable Execution Sprints**: Work is divided into targeted, self-contained phases to allow incremental code checkpoints, rapid compilation verification, and zero quota exhaustion.
2. **Platform & Design Consistency**: Every enhancement adheres strictly to Material Design 3 guidelines, fluid cross-device layout principles, and Android Telecom framework lifecycles.
3. **Continuous Tracking**: Each task has an explicit status indicator (`[ ]` Pending, `[🔄]` In Progress, `[x]` Completed) to be updated as code checkpoints are committed and verified.
4. **Once features are implemented move them to "ToDo.md" file which is a master file for tracking enhancements and implementation status.

---
## Uncategorized Bugs/Requests - review and move these items to relevant phases - keep this section
1. Now that we have dragging working for fav pane configure area, we can get rid of the arrows for moving the fav cards up/down/left/right we can get rid of those arrows
---

## 🚀 Phase Breakdown

### Phase 1: Visual Refinements & Keypad Stability
*Focus: UI polish, touch stability, and search UX consistency.*

- [x] **Task 1: Favorite Card Style Refinements — Concise Naming & Thin Light Theme Borders**
  - **User Item**: #1 ("Quick Action style borders are too wide and looks awful in light theme, make them thin") & #2 ("For Fav card designs, let us give shorter names for the three styles, for the Material You Experience style the border looks very wide in Light theme, need this border to look smaller.")
  - **Status**: [x] Completed

- [x] **Task 2: Universal Search Placeholder & Default Favorites Panel**
  - **User Item**: #2 ("In Search bar say 'search by name or number' in all search fields; default panel on opening is Favorites (update settings)")
  - **Status**: [x] Completed

- [x] **Task 12: Keypad Dial Button Internal Highlight Geometry**
  - **User Item**: #12 ("In key pad panel when a preferred dialing option is known the highlight around the dial button is making the keys move up... make the highlight within the circle of the button and not outside.. so the size of the button does not change")
  - **Status**: [x] Completed

---

### Phase 2: Contact Creation & Visual Pop-up Quality
*Focus: Elevating dialog design, avatar actions, and contact management workflows.*

- [x] **Task 4: Comprehensive Pop-up & Dialog Overhaul**
  - **User Item**: #4 ("Also review all pop-up panels and make sure the have all the necessary actions and make the panels looks decent and professional - i remember seeing the add contact panel to be pretty pale and dry - you know what i mean.")
  - **Status**: [x] Completed

- [x] **Task 8: Recents List Unknown Number '+' Action & Plural Naming**
  - **User Item**: #8 ("In the recents list, when i see a number's details showing there is a large + sign in place of picture , what is the intent ? is there a way to add that number to contacts by providing other details and saving it ? should the panel be called Recent or Recents or something else ?")
  - **Status**: [x] Completed

---

### Phase 3: Data Freshness, Initial Experience & Deep-Linking
*Focus: First-run experience, database sync, and notification interactivity.*

- [x] **Task 3: Eliminate Initial 4-Card Flash in Favorites**
  - **User Item**: #3 ("Favorites panel still flashes 4 cards before showing my actual favorites")
  - **Status**: [x] Completed

- [x] **Task 5: Fresh Install Call Log Import**
  - **User Item**: #5 ("on a fresh install why are the recent calls panel empty ?")
  - **Status**: [x] Completed

- [x] **Task 7: Missed Call Notification Deep-Link & Highlighting**
  - **User Item**: #7 ("I saw a missed call notification but when i touched it nothing happened, i was expecting it to open the recent calls with that call highlighted.")
  - **Status**: [x] Completed

- [x] **Task 13: Persist Favorite Card Deletion in Configure Mode**
  - **User Item**: #13 ("When a favoutire card id deleted in the configure mode of fav panel it reappears during next refresh, the idead of delete a favorite card is to unmark that favourite.")
  - **Status**: [x] Completed

---

### Phase 4: Adaptive Channel Routing & Dedicated Spam Center
*Focus: Smart communication preferences and clean settings modularity.*

- [x] **Task 6: Bento Style 'Phone' Labeling & 'Ask and Learn' Dual Dialers**
  - **User Item**: #6 ("for Bento Style, call it 'Phone' if the WhatsApp calling preference is 'Ask and Learn' , will it make sense to show both Phone and WhatsApp dialers when we don't know the preference ? and only the preferred mode when we know.")
  - **Status**: [x] Completed

- [x] **Task 11: Dedicated Spam & Blocked Screen / Window**
  - **User Item**: #11 ("Show spam in a different window instead of hogging the settings panel")
  - **Status**: [x] Completed

---

### Phase 5: Gesture Navigation & Secondary Gesture Actions
*Focus: Seamless navigation and user-configurable gestures.*

- [x] **Task 9: Swipe to Switch Panels & Sub-Tabs with Enable/Disable Setting**
  - **User Item**: #9 ("Allow me to swipe on the screen to switch between panels and if the panel has tabs then a swipe should switch the tab or else switch the panel if its the last tab in that direction, make that a setting to enable/disable.")
  - **Status**: [x] Completed

- [x] **Task 10: Secondary Gestures When Panel Swipe Is Disabled**
  - **User Item**: #10 ("If swipe to switch panels is disabled, use the swipe for anything else useful while not confusing it with any Android gestures (i tend to swipe to close the panels at times.")
  - **Status**: [x] Completed

---

### Phase 6: Roadmap Polish & Final UX Refinements
*Focus: Recents search, reorder persistence, icon sizing, popular contact ignore, and pager smoothness.*

- [x] **Task 14: Recents Search & Live Filtering**
  - **User Item**: #2 ("Do we need a 'search' option in recents ? if so what is the best way to implement it.")
  - **Analysis**: Users need to quickly find past call records by caller name, phone number, or note.
  - **Implementation Plan**:
    - Add a top search bar in `CallLogScreen.kt` ("Search by name or number") matching the search UX in Favorites and Contacts.
    - Dynamically filter call log items and grouped calls by query in real time.
  - **Status**: [x] Completed

- [x] **Task 15: Favorites Reorder Persistence & Material Border Consistency**
  - **User Items**: #5 ("Fav card order should also be persisted") & #9 ("Material style cards get a border after changing formats, investigate and fix")
  - **Analysis**:
    - Drag-and-drop sort orders in configure mode must be written directly to Room DB (`sortOrder` column) and `SharedPreferences` `favorite_sort_orders` set to prevent resetting on recomposition.
    - Material style card borders must remain strictly borderless or hairline (`0.5.dp` alpha `0.15f`) across format switches.
  - **Implementation Plan**:
    - Update `reorderFavorites()` in `MainViewModel.kt` to persist custom orders to Room DB & SharedPreferences.
    - Clean up `cardBorder` in `FavoritesScreen.kt` so Material style never renders unwanted heavy outlines.
  - **Status**: [x] Completed

- [x] **Task 16: Ask & Learn Preferred Channel Recomposition & Equal Icon Sizing**
  - **User Items**: #6 ("When in 'Ask and Learn' mode even after calls were placed i don't see the preferred calling option displayed on the fav cards") & #7 ("In contact list, WhatsApp calling icon is smaller than Phone icon, these should be same and preferred icon highlighted without increasing size")
  - **Analysis**:
    - Favorites cards need reactive state updates when `learnedCallModes` map changes so preferred channel badges update immediately upon placing calls.
    - In `ContactsScreen.kt`, Phone and WhatsApp call buttons must have identical 1:1 button dimensions (`36.dp`) and icon sizes (`18.dp`), with the preferred option highlighted via a subtle container background tint.
  - **Implementation Plan**:
    - Observe `learnedCallModes` in `FavoritesScreen.kt` to trigger instant card updates.
    - Unify Phone and WhatsApp action button sizing in `ContactsScreen.kt` and apply container highlight to the preferred option.
  - **Status**: [x] Completed

- [x] **Task 17: Configure Mode Popular Contact Ignore [X] Action**
  - **User Item**: #8 ("In configure mode of fav panel, unable to x popular call cards to be ignored, i thought this was already implemented.")
  - **Analysis**: In configure mode on the Favorites tab, frequently contacted popular cards need a visible `[X]` ignore icon button that triggers `viewModel.ignorePopularContact(phoneNumber)`.
  - **Implementation Plan**:
    - Add an explicit `[X]` ignore button overlay to popular cards in `FavoritesScreen.kt` configure mode that calls `onIgnoreContact(phoneNumber)` and removes the card from the popular list immediately.
  - **Status**: [x] Completed

- [x] **Task 18: Horizontal Pager Smoothness & Sub-Tab Swipe Integration**
  - **User Item**: #10 ("while swiping between panels, sometimes the panels seem tobe stuck in transition... treat Rules and Settings tabs in Rules panel as two swipable panels")
  - **Analysis**:
    - Horizontal Pager needs `beyondViewportPageCount = 2` to pre-render adjacent pages and prevent transition sticking during drag gestures.
    - Inside `RulesScreen.kt`, horizontal swiping should switch between sub-tabs ("Rules" vs "Activity Logs" / "Settings") before transitioning to neighboring main panels.
  - **Implementation Plan**:
    - Add `beyondViewportPageCount = 2` to `HorizontalPager` in `MainActivity.kt`.
    - Integrate nested sub-tab pager in `RulesScreen.kt`.
  - **Status**: [x] Completed

- [x] **Task 19: WhatsApp Outgoing Call Logging in Recents**
  - **User Item**: #1 ("When a whatsapp call is initiated from the app, keep a log of the call in recents")
  - **Analysis**: Users want all WhatsApp calls placed from the app to appear in Recents history.
  - **Implementation Plan**:
    - Implemented in `MainViewModel.placeWhatsAppCall()`: inserts a `RecentCall` record (`callReason = "WhatsApp Call"`, `callType = OUTGOING_TYPE`) into Room DB and refreshes recent calls.
  - **Status**: [x] Completed

- [x] **Task 20: Phone Contact vs Local Contact Edit Routing**
  - **User Item**: ("If a contact is from Phone contacts, edit directly in Phone's contact book; if local to app, edit within our app")
  - **Analysis**:
    - Phone contacts synced from Android system (`contactId > 0`) should be opened directly in Android Phone Contacts editor via `Intent(Intent.ACTION_EDIT)`.
    - Local app-only contacts (`contactId <= 0` or `isAppOnly = true`) are edited using the in-app `EditContactDialog`.
  - **Implementation Plan**:
    - Added `ContactHelper.launchContactEditor(context, contact)` to launch `ACTION_EDIT` / `ACTION_INSERT_OR_EDIT` intent.
    - Updated `ContactDetailsBottomSheet.kt` header Edit icon button and bottom button to check `contactId` and route to Phone Contacts editor for device contacts or `EditContactDialog` for local contacts.
  - **Status**: [x] Completed

- [x] **Task 21: Popular Contact Favoriting Nickname Auto-Lookup**
  - **User Item**: ("i favorited a number from popular and even though that contact has a nickname in the phone contact, fav card for that number is not showing the nick name")
  - **Analysis**: Look up `deviceContacts` in `MainViewModel.addFavorite()` to attach any existing `nickname` when starring popular items or new numbers.
  - **Status**: [x] Completed

- [x] **Task 22: Dual Dialers Across All Favorite Card Styles (Bento, Grid, Material)**
  - **User Item**: ("Why not dual-dialers in other styles of fav cards ?")
  - **Analysis**: Render side-by-side dual dialers (`Phone` and `WhatsApp`) in Ask & Learn / Ask Always mode across Bento, Grid (Quick Action), and Material card designs in `FavoritesScreen.kt`.
  - **Status**: [x] Completed

- [x] **Task 23: Keypad Hybrid Dial Buttons Design Overhaul**
  - **User Item**: ("The two dial options in dial-pad both look green and icons look the same... put on your creative designer hat and come up with an option i will like")
  - **Analysis**: Redesigned Keypad action buttons in `DialerScreen.kt` into wide, high-visibility Hybrid Action Buttons (`[ 📞 Phone Call ]` in deep emerald `#059669` and `[ 💬 WhatsApp ]` in signature `#25D366`) with glowing preferred borders.
  - **Status**: [x] Completed

- [x] **Task 24: Rules Header Cleanup & "Never" Mode Choice Preservation**
  - **User Items**: ("No need to describe 'Smart Automation Engine' just move 'Create Rule' to bottom right") & ("By selecting 'Never' shall we persist preferences but override with phone only")
  - **Analysis**: Cleaned up `RulesScreen.kt` header and placed `+ Create Rule` at bottom-right FAB. Preserved learned choices in storage when mode is "Never" without wiping user data.
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
| **Phase 6** | Tasks 14–24 | Recents search, Reorder persistence, Icon sizing, Popular ignore, Keypad hybrid buttons, Nickname lookup | ✅ Completed |
