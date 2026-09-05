ement# Kishan Dialer — Feature Roadmap & To-Do

## Pending
i am able to assign same speed dial number to multiple contacts and how do the speed dials work ? its not very clear
when calling a number why am i having to select my google account first ? can we make that default selection once or use what the contact application users?
can you initiate the whatsapp call directly why are you showing me the contact and expecting me to hit the audio call button ? 
showing the edit and delete on fav cards is making it look very cluttered lets group edit, delete, reorder and any other management into the Reorder panel and may be change it to configure icon.


## 1. Advanced Caller ID & Spam Protection (Truecaller Essentials)
- [ ] **Live Community-Sourced Caller ID**: Identify unknown numbers in real time using a crowd-sourced / cloud database, even when numbers are not saved in local device contacts.
- [x] **Spam & Telemarketer Tagging**: Visually flag known spam, fraud, or telemarketing numbers using color-coded alerts (e.g., high-visibility red badge for high-risk spam).
- [x] **Auto-Block List & True Silence**: Automatically reject or silently drop top-reported spam callers, hidden/private numbers, or foreign prefixes before the phone rings or vibrates.
- [ ] **Contextual Caller ID ("Call Reason")**: Allow callers or users to attach a short call reason / subject tag so recipients know the context before answering.
- [x] **Local Offline Spam Database**: Cache top regional/global spam numbers locally in Room SQLite for zero-latency, offline spam blocking.

---

## 2. Intelligent Automation & Call Management
- [x] **Smart Call Automation & Auto-Responder**: Automated responder service that screens incoming calls, prompts callers for their identity/purpose, and posts actions.
- [ ] **AI-Powered Call Screening**: On-device / cloud AI assistant to handle unknown callers, filtering bots and telemarketers with live conversational transcriptions.
- [ ] **Quick-Decline SMS Chips**: Tap preset response chips (e.g., "In a meeting", "Can't talk right now") directly from the incoming call screen.
- [ ] **Post-Call Reminders & Notes**: Add timestamped notes to recent calls and set follow-up callback reminders.

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
- [ ] **Native Android Home Screen Widgets**: Interactive Glance/AppWidget components for recent missed calls, quick-dial favorite avatars, and one-tap responder toggle.

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
