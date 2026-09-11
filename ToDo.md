# Kishan Dialer — Master Feature Roadmap & ToDo

## 📐 Structural Architecture Rules & Engineering Guidelines
1. **Platform Standards**: Follow clean, platform-standard Android architecture (MVVM, Jetpack Compose, Material Design 3, Telecom `InCallService`, Room SQLite). Rely on standard public Android framework APIs without prescribing undocumented internal classes.
2. **Conflict Resolution**: If any requirement conflicts with core Android telecom lifecycle rules or Material Design standards, maintain telecom safety, OS lifecycle compatibility, and user experience as top priorities.
3. **Multi-Format Cross-Device Fluidity**: Ensure all layouts, view hierarchies, and padding constraints adapt dynamically across diverse form factors (standard devices like Pixel 10a, foldables, and tablets), orientation changes, split-window multi-tasking ratios, and hardware display cutouts. Avoid hardcoded pixel values (`px`); use relative constraints, weight ratios, and system window insets.

---

## 🎯 Phase & Sprint Tracking

### Phase 1: High-Priority Data & Stability Features
- [x] **1. Duplicate Call Log Tracking Prevention**
  - *Requirement*: Implement strict deduplication logic. Ensure shifting call states (e.g., dialing to active) do not trigger multiple database write events for a single unique call session in History and Recents.
  - *Implementation*: Added in-memory session tracking (`loggedCallSessionIds`) and a 4-second deduplication time-window in `CallManager.kt`. Call state transitions update or reuse the existing session record rather than firing redundant database insertions into Room.
- [x] **2. Nickname / Contact Editing Persistence**
  - *Requirement*: Fix the contact editor data pipeline so that changing a nickname correctly propagates down to the underlying local repository layer and saves permanently to persistent storage.
  - *Implementation*: Linked the Contact Details bottom sheet and dialog editor through `MainViewModel.updateContact()`. Changes directly update the local database (`AppRepository.updateFavorite`) and sync to system contacts via `ContactHelper.updateContactNickname`.
- [x] **3. Call History Deletion Implementation**
  - *Requirement*: Add user interaction workflows (long-press context menu / overflow menu) allowing users to delete specific records completely from both the Recents view and detail history logs.
  - *Implementation*: Added single-call deletion and batch deletion ("Clear All for Number") into `CallLogScreen`'s overflow menu via `MainViewModel` and `AppRepository`.

---

### Phase 2: Screen Real Estate & Layout Adjustments (Zero Scroll Keypad)
- [x] **4. Dial-Pad Layout Scroll Removal & Element Repositioning**
  - *Requirement*: Completely eliminate vertical scrolling on the dial-pad view. Redesign screen real-estate using weight-based spatial distribution and consolidate layout-heavy horizontal options into concise drop-downs.
  - *Implementation*: Restructured `DialerScreen.kt` using flex weights (`weight(1f)`), compact headers, and consolidated quick action rows, keeping all keys within the viewport without vertical scrolling.
- [x] **5. Pixel 10a Camera Cutout Shielding & Layout Constraints**
  - *Requirement*: Adjust top-positioned view elements, such as incoming call notification banners and in-call headers, to securely respect system window insets so layouts draw cleanly below center-aligned hardware camera cutouts.
  - *Implementation*: Added `statusBarsPadding()` and `displayCutoutPadding()` to `InCallScreen.kt` and active call surfaces so all top banners and caller headers draw cleanly below punch-hole cutouts.
- [x] **6. UI Branding and List Asset Consistency**
  - *Requirement*: Update WhatsApp brand icon asset to fully mirror official geometry. Ensure all communication channel icons match standard voice call icons at a unified 1:1 dimension ratio. Synchronize contact lists and standalone profile views.
  - *Implementation*: Refactored `WhatsAppIcon.kt` to an exact 1:1 circular badge with official speech bubble geometry, bottom-left callout tail, and phone glyph; unified action button sizing (16-24dp) across screens.
- [x] **7. Layout Clashing & Long Text Management**
  - *Requirement*: Implement dynamic name handling inside compact slots (Favorites cards and Dial-pad grids). Use end-truncation (`...`) or multi-stage text auto-scaling on long contact names.
  - *Implementation*: Added `TextOverflow.Ellipsis`, `maxLines = 1`, and flexible font sizing (`13.sp` down to `11.sp` for secondary labels) across `FavoriteGridCard`, dial-pad suggestion pills, and speed-dial slots.
- [x] **8. Dark Theme Overhaul for Spam Flags**
  - *Requirement*: Modify dark mode layouts to handle flagged spam records cleanly. Remove stark, bright light-colored block backgrounds; leave the background natively dark and use red textual and icon accents.
  - *Implementation*: Replaced harsh light-colored background blocks with dark-native surfaces (`MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)`) paired with high-contrast red iconography (`MaterialTheme.colorScheme.error`) and tinted text.

---

### Phase 3: Communication Routing & Workflow Preferences
- [x] **9. Contextual Name Splitting Policy**
  - *Requirement*: Root directory Contacts list view must ALWAYS show a contact's official Full Name. User-configured compact Nicknames must only be rendered inside space-limited spots: speed-dial keypad slot labels and Favorites grid cards.
  - *Implementation*: Enforced strict separation where `ContactsScreen` root directory items consistently render `contact.name` (official Full Name), while space-constrained surfaces (`FavoriteGridCard` and Speed-Dial pads) render `nickname ?: name`.
- [x] **10. Adaptive Channel Routing via Usage Analysis**
  - *Requirement*: Build lightweight usage tracking to count outbound communication events. Dynamically monitor whether a contact inside Favorites is primarily reached via cellular call versus WhatsApp, updating the default primary tap icon to match the most frequent channel.
  - *Implementation*: Built dynamic channel tracking in `FavoritesScreen.kt` and `DialerScreen.kt` evaluating historical call records for each contact; primary action button dynamically adapts to the most frequently used channel.
- [x] **11. Outbound Call Screen Overlay Clean Up**
  - *Requirement*: Suppress and mask floating call-state notification header overlays as long as the application's primary full-screen dialing activity layer is actively in focus.
  - *Implementation*: Added `CallManager.isCallUiForegrounded` detection in `OngoingCallNotificationHelper.kt` to drop notification priority and disable full-screen intent when full-screen call UI is actively foregrounded.
- [x] **12. External Redirection for Complex Contacts**
  - *Requirement*: Include a fallback utility button labeled "Edit in System Contacts" inside contact overview layouts to launch the platform's native external contacts manager via Android Intent.
  - *Implementation*: Added "Edit in System Contacts" button in `ContactDetailsBottomSheet.kt` firing standard `Intent.ACTION_VIEW` or `Intent.ACTION_INSERT_OR_EDIT` intent.
- [x] **13. Note-Taking Timer Optimization**
  - *Requirement*: Decrease the post-call execution delay timer responsible for displaying the quick brief note entry sheet from 6 seconds down to exactly 3 seconds for a snappier response.
  - *Implementation*: Reduced post-call note dismissal countdown timer to exactly 3 seconds (`autoCloseRemainingSeconds = 3`) in `InCallScreen.kt`.

---

### Phase 4: Spam Interception & Forward-Looking VoIP Abstraction
- [x] **14. Carrier-Level Automation Rule for Spam Filtering**
  - *Requirement*: Implement incoming call evaluation logic to parse metadata string markers passed down by the carrier network. If inbound telecom parameters signal spam/spoofing, programmatically drop the connection before it rings.
  - *Implementation*: Implemented carrier STIR/SHAKEN verification inspection in `CallManager.kt` (`callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED`), rejecting spam calls immediately (`Call.REJECT_REASON_DECLINED`).
- [x] **15. Extensible VoIP Endpoint Abstraction Layer (WhatsApp Business Integration)**
  - *Requirement*: Decouple outbound dial loops from rigid single-channel cellular networks. Refactor dialing engine to route calls through an extensible channel abstraction layer detecting package availability for WhatsApp Business alongside standard WhatsApp.
  - *Implementation*: Refactored VoIP call launching in `ContactHelper.kt` to dynamically query and detect both standard WhatsApp (`com.whatsapp`) and WhatsApp Business (`com.whatsapp.w4b`) MIME types and package intents.

---

## 📋 Core Features & User Request Resolutions

- [x] **Post-Call Notes Persistence**: Resolved. `savePostCallNote()` matches the latest call record for that phone number in Room and commits the note and reminder timestamp immediately.
- [x] **Recent Call UI & Label Wrapping**: Resolved. Enforced `softWrap = false`, `maxLines = 1`, and padded SIM pills; organized secondary actions (notes, spam reporting, rules) into a clean dropdown menu to prevent row cramping.
- [x] **Favorite Panel Nickname Priority**: Resolved. When a nickname exists, the card displays exclusively the nickname without parentheses or full name clutter.
- [x] **Ongoing Call Notification & Lockscreen Screen**: 
  - *Implementation Architecture*: Uses Android Telecom `InCallService` bound foreground service with `NotificationCompat.Builder` using `setOngoing(true)` and Category `CATEGORY_CALL`.
  - *Lockscreen Wakeup*: Activity manifest sets `showWhenLocked="true"`, `turnScreenOn="true"`, and notification includes a `FullScreenIntent` directing immediately to `InCallScreen` when ringing/active.
  - *Background / Home Navigation*: Ongoing notification displays caller name, elapsed call timer, and pending intent action buttons for "Mute", "Speaker", and "End Call".
- [x] **App-Only Contacts Visibility & Google Sync**: Resolved. Merged app-only contacts into `ContactsScreen`, added filter chips (`All`, `App Only`, `Google / Device`), "App Only" indicator chips, and one-tap "Sync to Google Contacts" button on rows and bottom sheets.
- [x] **Contact Creation Modal Destination**: Resolved. Switched contact creation on the Contacts screen to `CreateContactDialog` allowing explicit choice between saving to Google/Device or App-Only, with optional favorite starring.
- [x] **Bluetooth / Speaker / Handset Audio Routing**: Resolved. Added Telecom `setAudioRoute()` support (`ROUTE_EARPIECE`, `ROUTE_SPEAKER`, `ROUTE_BLUETOOTH`), dynamic audio button, and `AudioOutputSelectorDialog`.
- [x] **3x3 Speed Dial Keypad Matrix**: Resolved. Aligned the speed dial selection dialog into an intuitive 3x3 layout matching the physical dialpad (with Key 1 reserved for Voicemail).

---

## 🎨 Visual & UI Enhancements
- [x] **Favorite Card Style Refinements — Concise Naming & Thin Light Theme Borders**: Shortened card style names to `"Bento"`, `"Grid"`, and `"Material You"`. Applied ultra-thin `0.5.dp` hairline borders with subtle outline tint in Light Theme for `FavCardDesign.MATERIAL_YOU` and `FavCardDesign.QUICK_ACTION` across Favorites and Rules screens.
- [x] **Universal Search Placeholder & Default Favorites Panel**: Set default search bar placeholder across `FavoritesScreen`, `ContactsScreen`, and `ContactPickerDialog` to `"Search by name or number"`. Configured default startup panel to `Favorites` (tab 0).
- [x] **Keypad Dial Button Internal Highlight Geometry**: Fixed preference highlight ring inside the dial button boundary (`.padding(3.dp).border(2.dp, ...)`), locking button outer dimensions to `56.dp` and preventing key shifts on preferred dialer detection.
- [x] **Comprehensive Pop-up & Dialog Overhaul**: Redesigned dialogs (`CreateContactDialog`, `ContactDetailsBottomSheet`, `EditFavoriteDialog`, `AudioOutputSelectorDialog`, `MultiNumberCallDialog`) with rounded corners (24/28dp), leading icons, avatar preview monograms, label chips, and clear action buttons.
- [x] **Recents List Unknown Number '+' Action & Plural Naming**: Renamed tab and headers to `"Recents"`. Interactive `PersonAdd` avatar and `"Add to Contacts"` primary button in `ContactDetailsBottomSheet` and overflow menu open `CreateContactDialog` pre-filled with the number.
- [x] **Zero Initial Flash in Favorites**: Render clean empty state directly when favorites are empty, preventing initial 4-card popular flash on startup.
- [x] **Fresh Install Call Log Sync**: Automatically seed Room SQLite database with system call history logs on first run when permission is granted.
- [x] **Missed Call Notification Deep-Link & Highlighting**: Attached `EXTRA_NAV_TAB = "RECENTS"` and `EXTRA_HIGHLIGHT_NUMBER` extras to missed call notifications; `MainActivity` switches to Recents tab and `CallLogScreen` auto-scrolls to and highlights the missed call item.
- [x] **Instant Favorite Deletion Persistence**: Configure mode and card deletions immediately update Room database and local state, permanently unmarking favorites so cards never reappear on refresh.
- [x] **Bento Style 'Phone' Labeling & 'Ask and Learn' Dual Dialers**: Renamed cellular calling button label from "Direct Call" to "Phone". In "Ask and Learn" mode when channel preference is undetermined, Bento cards render side-by-side dual action buttons ("Phone" & "WhatsApp"), collapsing or highlighting the preferred mode once learned.
- [x] **Dedicated Spam & Blocked Window**: Created `SpamManagementDialog` with blocked numbers list, quick search, manual block additions, and auto-block toggles. Replaced the cluttered section in Settings/Rules with a clean entry row displaying a `"{X} blocked"` indicator badge.
- [x] **Recents Search & Live Filtering**: Added a top search bar in `CallLogScreen.kt` (`"Search by name or number"`) matching the search UX in Favorites and Contacts to filter call history by caller name, number, or note in real time.
- [x] **Favorites Reorder Persistence & Material Card Hairline Border**: Persisted custom drag-and-drop sort orders directly to Room DB (`sortOrder` column) & SharedPreferences (`favorite_sort_orders` set) so custom orders never reset on recomposition. Refined Material card borders in `FavoritesScreen.kt` to ensure clean, borderless/hairline outlines across format switches.
- [x] **Ask & Learn Preferred Channel Recomposition & Equal Icon Sizing**: Observed `learnedCallModes` state in `FavoritesScreen.kt` so Bento cards update their preferred channel button immediately after placing calls. Unified Phone and WhatsApp call action buttons in `ContactsScreen.kt` to equal 1:1 dimensions (`36.dp` button with `18.dp` icon) and highlighted the preferred channel button with a subtle container tint.
- [x] **Configure Mode Popular Contact Ignore [X] Action**: Added an explicit `[X]` ignore button overlay to popular cards in `FavoritesScreen.kt` configure mode that calls `viewModel.ignorePopularContact(phoneNumber)` and removes the card from the popular section immediately.
- [x] **Horizontal Pager Smoothness & Sub-Tab Navigation**: Added `beyondViewportPageCount = 2` to `HorizontalPager` in `MainActivity.kt` to pre-render adjacent pages and prevent transition sticking during drag gestures. Integrated nested sub-tab swiping in `RulesScreen.kt`.
- [x] **WhatsApp Outgoing Call Logging in Recents**: Every WhatsApp call initiated from the app is immediately logged into Room SQLite (`recent_calls`) with caller name, phone number, timestamp, and a `"WhatsApp Call"` badge, instantly refreshing the Recents tab upon returning to the app.
- [x] **Phone Contact vs Local Contact Edit Routing**: When a contact is synced from device/Google contacts (`contactId > 0 && !isAppOnly`), tapping Edit in `ContactDetailsBottomSheet.kt` directly launches the Android Phone Contacts app editor (`Intent.ACTION_EDIT`). When the contact is local/app-only (`isAppOnly = true`), tapping Edit opens the in-app `EditContactDialog`.
- [x] **Contact Details Sizing & Edge Padding**: Added status bar padding and top spacing to `ContactDetailsBottomSheet` so that long call history lists and scrolling headers respect screen bounds and do not scroll over the top edge of the screen.
- [x] **Ignore Popular Contacts**: Added option in favorite/popular screen edit mode to ignore/hide specific contacts from the popular section, with an "Ignored Popular Contacts" restoration section.
- [x] **Search Results Enhancement**: Updated search results in favorites to include "Add to Favorites" and dual-action Phone/WhatsApp call buttons for each contact number.
- [x] **Favorites Panel Behavior**: Tapping a favorite in the Contacts panel now opens the contact details sheet instead of single-touch dialing.
- [x] **WhatsApp Integration in Contacts**: Added WhatsApp call button alongside Phone call button for all contact phone numbers.
- [x] **Collapsible Contacts Favorites**: Wrapped the favorites section at the top of the Contacts panel in a collapsible container, closed by default, with an expand/collapse toggle header.
- [x] **WhatsApp Icon Replacement**: Replaced all "WA" text instances with the official WhatsApp chat icon across Dialer, Contacts, Favorites, Bottom Sheets, and Multi-Number dialogs.
- [x] **Dynamic Preferred Dialer Sizing**: Implemented dynamic sizing for Phone vs WhatsApp call buttons on the Dialer screen based on recent call history/frequency (preferred method is larger, non-preferred is smaller, and equal/undetermined are normalized to same size).
- [x] **Screen Rotation State Persistence**: Used `rememberSaveable` for tab selection state across the app to prevent resetting to Favorites when rotating the device.
- [x] **Removed Shhh Toggle**: Removed the explicit Shhh mode toggle and status indicator from the UI, relying natively on the system's Do Not Disturb mode.
- [x] **Expansive Dial Pad Keys**: Re-architected dial pad keys to use flex weights and aspect ratios, filling the entire horizontal width available instead of fixed dimensional sizes.
- [x] **Larger Dial Pad Keys**: Increased dial pad key size (`compact` sizes increased from 56dp to 68dp) to better utilize available screen space and improve tap targets.
- [x] **Refined Popular List**: Renamed the "POPULAR" section to "Popular" and limited the list to 4 items. Additionally, filtered out utility/system numbers (like Voicemail, Spam, Entry Gate, Intercom) to keep the list relevant.
- [x] **Separated Full Name and Nickname**: Decoupled "Contact Name" and "Nickname" into distinct input fields in the edit dialog. The contact's full name is preserved, while custom nicknames are stored independently, synced to device contacts, displayed on favorite cards, and prioritized on dialpad speed-dial keys.
- [x] **Label Wrapping & Chip Flow**: Converted the Edit Contact dialog label chips to dynamic `FlowRow` and enforced `softWrap = false` on chips and card badges, preventing awkward multi-line breaks like "Fam\nily".
- [x] **Phone Number Display on Favorite Cards**: Added contact phone numbers beneath the contact name on all favorite cards.
- [x] **Square Keypad Keys & Assignee Badges**: Upgraded dialpad buttons to modern rounded-square tiles displaying real-time speed dial assignee names without corner clipping.
- [x] **Compact Horizontal Favorite Cards**: Redesigned cards to a balanced horizontal layout utilizing the card's width.
- [x] **Concise Labels & `#number` Notation**: Simplified verbose labels ("Key #slot" is now `#slot`, "+ Speed Key" is "+ Speed").

---

## 🛡️ Advanced Caller ID & Spam Protection (Truecaller Essentials)
- [x] **Live Community-Sourced Caller ID**: Identify unknown numbers in real time using a crowd-sourced / cloud database, even when numbers are not saved in local device contacts.
- [x] **Spam & Telemarketer Tagging**: Visually flag known spam, fraud, or telemarketing numbers using color-coded alerts (e.g., high-visibility red badge for high-risk spam).
- [x] **Auto-Block List & True Silence**: Automatically reject or silently drop top-reported spam callers, hidden/private numbers, or foreign prefixes before the phone rings or vibrates.
- [x] **Contextual Caller ID ("Call Reason")**: Allow callers or users to attach a short call reason / subject tag so recipients know the context before answering.
- [x] **Local Offline Spam Database**: Cache top regional/global spam numbers locally in Room SQLite for zero-latency, offline spam blocking.

---

## ⚡ Intelligent Automation & Call Management
- [x] **Smart Call Automation & Auto-Responder**: Automated responder service that screens incoming calls, prompts callers for their identity/purpose, and posts actions.
- [x] **AI-Powered Call Screening**: On-device / cloud AI assistant to handle unknown callers, filtering bots and telemarketers with live conversational transcriptions. Included interactive prompt chips, real-time speech transcription, confidence verdict scoring, spam auto-detection, and one-tap call connect or spam blocking.
- [x] **Quick-Decline SMS Chips**: Tap preset response chips (e.g., "In a meeting", "Can't talk right now") directly from the incoming call screen.
- [x] **Post-Call Reminders & Notes**: Add timestamped notes to recent calls and set follow-up callback reminders.

---

## 📱 Enhanced Dialer & Contact UX
- [x] **Aggregated & Clean Contact Directory UX**: Group multiple phone numbers per contact into a single clean person row (eliminates duplicate rows and removes raw number clutter from the main view), with expandable detail view for multiple numbers, SMS, WhatsApp, and automation shortcuts.
- [x] **Device Contacts as Single Source of Truth**: Bi-directional sync with Android Contacts database for starred status, nicknames, and real-time edits.
- [x] **Dedicated Contacts Directory with A-Z Alphabet Scroller**: Clean iOS/Android-style alphabet indexed contacts list with real-time search, SMS & call triggers.
- [x] **VIP Favorites Hub & 2x2 Speed Dial Grid**: Quick-access favorite cards, visual photo monograms, custom labels, and speed dial slot assignments (Keys 2-9).
- [x] **One-Tap Star Favoriting**: Instant favorite toggling across Recents, Contacts, and Dialer screens with local Room persistence.
- [x] **T9 Smart Search Dialing**: Instant T9 matching on the dialpad that parses names, aliases, company titles, and phone numbers in real time as digits are pressed.
- [x] **Rich Call History**: Group consecutive calls from the same contact/number, show duration statistics, and display spam flags on past unknown numbers.
- [x] **Speed Dial & VIP Favorites**: Assign long-press actions on digits 1–9 to immediately dial designated priority contacts.
- [x] **Dual-SIM Optimization**: Seamless one-tap SIM switching (SIM 1 / SIM 2) with contact-specific preferred SIM routing rules.
- [x] **Native Android Home Screen Widgets**: Interactive Glance/AppWidget components for recent missed calls, quick-dial favorite avatars, and one-tap responder toggle.

---

## 🎙️ Voice Recording Feasibility & Recommendation
- [x] **Documented Feasibility & Constraints**:
  - Since Android 10 (API 29+), Android strictly blocks third-party apps from recording remote in-call audio (`AudioSource.VOICE_CALL` and `VOICE_COMMUNICATION` are restricted to system apps or require root/accessibility hacks).
  - Google Play policy strictly prohibits using Accessibility APIs for call audio recording.
  - Recording phone calls also faces strict legal constraints (two-party vs. one-party consent laws across jurisdictions).
- [x] **Architectural Guardrails Applied**:
  - Dialer automation focuses on native Telecom flows (auto-answer, DTMF keystroke transmission, automated SMS, and call logging) without risking Play Store bans.

---

## ⚙️ Standard Automation Rule Recipes
- [x] **Recipe 1: Gate / Intercom Buzzer**
  - *Trigger*: Incoming call from Gate Number (e.g., `5550199`)
  - *Actions*: Auto-Answer (1s delay) -> In-band DTMF `9#` -> Auto-hangup (2s delay)
- [x] **Recipe 2: Office Extension Direct Routing**
  - *Trigger*: Calls to/from corporate number (e.g., `18005550100`)
  - *Actions*: Auto-Answer (2s delay) -> In-band DTMF sequence `104#`
- [x] **Recipe 3: Meeting / Focus SMS Auto-Responder**
  - *Trigger*: Any caller during busy periods (`*`)
  - *Actions*: Send background SMS: *"I am currently busy. I will call you back shortly."* -> Auto-hangup
- [x] **Recipe 4: Delivery Access Auto-Grant**
  - *Trigger*: Delivery driver / lobby box number
  - *Actions*: Auto-Answer -> DTMF `4#` -> Auto-SMS: *"Lobby gate opened automatically."* -> Auto-hangup
- [x] **Recipe 5: Voicemail Automated Access Sequence**
  - *Trigger*: Carrier voicemail dial
  - *Actions*: Answer -> Delay 1000ms -> In-band DTMF PIN sequence `1234#`
