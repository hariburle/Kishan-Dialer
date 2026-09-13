# OmniDial – Release & Versioning Management

## 1. Versioning Scheme
OmniDial uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- `versionCode` (Integer in `app/build.gradle.kts`): Increments monotonically with each build (e.g., `1`, `2`, `3`).
- `versionName` (String in `app/build.gradle.kts`): Semantic representation (e.g., `1.0.0`).

| Version | Version Code | Release Date | Summary |
|---------|--------------|--------------|---------|
| `1.1.0` | `2` | Sep 2026 | Keypad 2x2 action buttons, adaptive channel highlights, WhatsApp dark-mode icon contrast, Bluetooth/car call redirection, Favorites per-number selection, and Nickname sync. |
| `1.0.0` | `1` | Sep 2026 | Initial release with Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh. |

---

## Release 1.1.0 Change Log

### What's New:
- **Cleaner 4-Button Dial Pad**: Quickly reach anyone with 4 organized buttons on your dial pad: Send Text, Regular Phone Call, WhatsApp Message, or WhatsApp Voice Call.
- **Smart Call Suggestions**: The app gently highlights how you usually contact each person, so you never have to remember whether to call on WhatsApp or regular mobile.
- **Car Bluetooth & Hands-Free Calling**: Making calls from your car dashboard, smartwatch, or Bluetooth headset now automatically routes through WhatsApp when that's your preferred channel for that contact.
- **Choose Default Numbers for Favorites**: When starring a contact who has multiple numbers (like home, work, and mobile), you can easily choose the exact number to dial by default.
- **Easier to See in Dark Mode**: WhatsApp icons and buttons now feature crisp, high-contrast outlines so they stand out clearly on dark backgrounds.
- **Friendlier Nicknames**: Add personal nicknames that appear front-and-center on your favorites grid, with official legal names neatly subtitled below.

### Improvements & Fixes:
- **Reliable WhatsApp Call Confirmations**: Fixed an issue where tapping WhatsApp call on favorite cards could accidentally place a regular cellular call or skip confirmation.
- **Smoother Navigation**: Pressing the Android back button when searching favorites or contacts now cleanly dismisses the search bar rather than exiting the app.

---

## 2. Versioned APK Download Scheme
OmniDial maintains both versioned and latest APK artifacts:
- Latest Version: `OmniDial-v1.1.0.apk` (and alias `OmniDial.apk`)
- Prior Release: `OmniDial-v1.0.0.apk`
Hosted directly on GitHub Pages under the `/docs` directory.

---

## 2. GitHub Pages Deployment Steps
1. Push repository changes to GitHub (`git push origin main`).
2. On GitHub, navigate to **Settings > Pages**.
3. Under **Build and deployment > Source**, select **Deploy from a branch**.
4. Set branch to `main` and folder to `/docs`, then click **Save**.
5. Your download webpage will be live at: `https://<username>.github.io/<repo-name>/`

---

## 3. Creating New Releases
1. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build optimized release APK:
   ```bash
   gradle :app:assembleRelease
   ```
3. Copy the output APK to root `OmniDial.apk`:
   ```bash
   cp app/build/outputs/apk/release/app-release-unsigned.apk OmniDial.apk
   ```
4. Update the changelog on `docs/index.html` and push to GitHub.
