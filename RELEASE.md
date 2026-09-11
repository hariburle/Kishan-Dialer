# SmartDialer – Release & Versioning Management

## 1. Versioning Scheme
SmartDialer uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- `versionCode` (Integer in `app/build.gradle.kts`): Increments monotonically with each build (e.g., `1`, `2`, `3`).
- `versionName` (String in `app/build.gradle.kts`): Semantic representation (e.g., `1.0.0`).

| Version | Version Code | Release Date | Summary |
|---------|--------------|--------------|---------|
| `1.0.0` | `1` | Sep 2026 | Initial beta release for test group with Rules, T9, and Flip-to-Shhh. |

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
3. Copy the output APK to `docs/SmartDialer.apk` and `SmartDialer.apk`:
   ```bash
   cp app/build/outputs/apk/release/app-release-unsigned.apk docs/SmartDialer.apk
   ```
4. Update the changelog on `docs/index.html` and push to GitHub.
