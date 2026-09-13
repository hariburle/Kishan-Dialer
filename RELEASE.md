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

### Enhancements & New Features:
- **Keypad 2x2 Call Action Grid**:
  - Replaced scrolling carousels with an elegant, non-interfering 2x2 action grid (`Text Message`, `Phone`, `WhatsApp - Msg`, `WhatsApp - Voice`).
  - **Adaptive Preference Highlighting**: Dynamically highlights preferred communication channels using clean, high-contrast borders and subtle background tints based on learned caller intelligence, avoiding clutter from intrusive badge tags.
- **Dark Mode WhatsApp Icon Contrast**:
  - Enhanced vector rendering with a clean, high-contrast outer contour ring around the bubble.
  - Prevents the green bubble from blending with dark backgrounds in dark mode, maintaining balanced icon sizing across all screens.
- **External Outgoing Call Redirection Service**:
  - Intercepts outgoing calls initiated from Bluetooth car head units, smartwatches, and third-party dialers (`OmniCallRedirectionService`) to automatically route calls over WhatsApp VoIP when preferred.
- **Favorites UX & Default Number Selection**:
  - Explicit per-number selection for contacts with multiple phone numbers, allowing users to designate the specific number for instant dialing.
  - Handled back button navigation via `BackHandler` on Favorites and Contacts screens.
- **Contact Nickname Bi-directional Sync**:
  - Full two-way synchronization of nicknames between Android device contacts (`ContactsContract.CommonDataKinds.Nickname`) and the local database.
  - Displays contact nicknames prominently across favorites with official names subtitled.

### Bug Fixes:
- **WhatsApp Call Confirmation**: Fixed an issue where clicking the WhatsApp call button on favorite cards bypassed the confirmation dialog, and fixed the confirmation dialog confirming cellular calls even when WhatsApp was requested.
- **External Hands-Free / Bluetooth Routing**: Solved outgoing calls initiated via vehicle head units bypassing app preferences by implementing the system `CallRedirectionService`.

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
