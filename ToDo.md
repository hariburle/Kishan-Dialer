# Kishan Dialer — Feature Roadmap & ToDo

## User Request Status & Resolutions

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


## Completed in this update
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



## 1. Advanced Caller ID & Spam Protection (Truecaller Essentials)
- [x] **Live Community-Sourced Caller ID**: Identify unknown numbers in real time using a crowd-sourced / cloud database, even when numbers are not saved in local device contacts.
- [x] **Spam & Telemarketer Tagging**: Visually flag known spam, fraud, or telemarketing numbers using color-coded alerts (e.g., high-visibility red badge for high-risk spam).
- [x] **Auto-Block List & True Silence**: Automatically reject or silently drop top-reported spam callers, hidden/private numbers, or foreign prefixes before the phone rings or vibrates.
- [x] **Contextual Caller ID ("Call Reason")**: Allow callers or users to attach a short call reason / subject tag so recipients know the context before answering.
- [x] **Local Offline Spam Database**: Cache top regional/global spam numbers locally in Room SQLite for zero-latency, offline spam blocking.

---

## 2. Intelligent Automation & Call Management
- [x] **Smart Call Automation & Auto-Responder**: Automated responder service that screens incoming calls, prompts callers for their identity/purpose, and posts actions.
- [x] **AI-Powered Call Screening**: On-device / cloud AI assistant to handle unknown callers, filtering bots and telemarketers with live conversational transcriptions. Included interactive prompt chips, real-time speech transcription, confidence verdict scoring, spam auto-detection, and one-tap call connect or spam blocking.
- [x] **Quick-Decline SMS Chips**: Tap preset response chips (e.g., "In a meeting", "Can't talk right now") directly from the incoming call screen.
- [x] **Post-Call Reminders & Notes**: Add timestamped notes to recent calls and set follow-up callback reminders.

---

## 3. Enhanced Dialer & Contact UX
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

## 4. Voice Recording Feasibility & Recommendation
- [x] **Documented Feasibility & Constraints**:
  - Since Android 10 (API 29+), Android strictly blocks third-party apps from recording remote in-call audio (`AudioSource.VOICE_CALL` and `VOICE_COMMUNICATION` are restricted to system apps or require root/accessibility hacks).
  - Google Play policy strictly prohibits using Accessibility APIs for call audio recording.
  - Recording phone calls also faces strict legal constraints (two-party vs. one-party consent laws across jurisdictions).
- [x] **Architectural Guardrails Applied**:
  - Dialer automation focuses on native Telecom flows (auto-answer, DTMF keystroke transmission, automated SMS, and call logging) without risking Play Store bans.

---

## 5. Standard Automation Rule Recipes
- [x] **Recipe 1: Gate / Intercom Buzzer**
  - Trigger: Incoming call from Gate Number (e.g., `5550199`)
  - Actions: Auto-Answer (1s delay) -> In-band DTMF `9#` -> Auto-hangup (2s delay)
- [x] **Recipe 2: Office Extension Direct Routing**
  - Trigger: Calls to/from corporate number (e.g., `18005550100`)
  - Actions: Auto-Answer (2s delay) -> In-band DTMF sequence `104#`
- [x] **Recipe 3: Meeting / Focus SMS Auto-Responder**
  - Trigger: Any caller during busy periods (`*`)
  - Actions: Send background SMS: *"I am currently busy. I will call you back shortly."* -> Auto-hangup
- [x] **Recipe 4: Delivery Access Auto-Grant**
  - Trigger: Delivery driver / lobby box number
  - Actions: Auto-Answer -> DTMF `4#` -> Auto-SMS: *"Lobby gate opened automatically."* -> Auto-hangup
- [x] **Recipe 5: Voicemail Automated Access Sequence**
  - Trigger: Carrier voicemail dial
  - Actions: Answer -> Delay 1000ms -> In-band DTMF PIN sequence `1234#`
