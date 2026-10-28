# CredStow — Complete Play Store Deployment Guide

> **Complete checklist for publishing CredStow to the Google Play Store**
> App: `com.credstow.app` | Version: 1.0 (code 2) | Last commit: `baf4c4e`
> Build command: `.\gradlew.bat --no-daemon assembleDebug testDebugUnitTest bundleRelease`

---

## Prerequisites

| Item | Status | Location / Notes |
|---|---|---|
| Google Play Developer account | ✅ Active | https://play.google.com/console |
| **One-time Pro IAP product** (`pro_unlock`) | ⏳ TODO | Must be created in Play Console → Setup → In-app products |
| Production AdMob App ID | ⏳ TODO | Replace test ID in `strings.xml` |
| Production AdMob Banner ID | ⏳ TODO | Replace test ID in `strings.xml` |
| Production AdMob Interstitial ID | ⏳ TODO | Replace test ID in `strings.xml` |
| Hosted privacy policy URL | ⏳ TODO | Upload `app/src/main/assets/privacy_policy.html` to a web URL |
| **Signed release AAB** | ✅ Built | `app/build/outputs/bundle/release/app-release.aab` |
| Production keystore | ✅ Verified | `credstow-upload.keystore` |

---

## Phase 1: Swap AdMob Test IDs → Production IDs

### Step 1: Create AdMob ad units
1. Go to [Google AdMob](https://admob.google.com/) → **Ads** → **Ad units**
2. Create a **Banner** ad unit → copy the **Ad unit ID**          ca-app-pub-1096022160212922/2887667496
3. Create an **Interstitial** ad unit → copy the **Ad unit ID**   ca-app-pub-1096022160212922/4836008138
4. Go to **Settings** → **App ID** → copy the **App ID**          ca-app-pub-1096022160212922~9281961123

### Step 2: Run the prep-release script
```powershell
.\prep-release.ps1
```
This script will:
- Read `keystore.properties` for AdMob credentials (or prompt for production IDs)
- Replace the three test IDs in `app/src/main/res/values/strings.xml`:
  - App ID: `ca-app-pub-3940256099942544~3347511713` → your production App ID
  - Banner: `ca-app-pub-3940256099942544/6300978111` → your production Banner ID
  - Interstitial: `ca-app-pub-3940256099942544/1033600233` → your production Interstitial ID
- Build the release AAB automatically

### Step 3: Restore test IDs (IMPORTANT)
After building:
```bash
git checkout app/src/main/res/values/strings.xml
```
This restores the test IDs in your source tree. **Never commit production AdMob IDs to git.**

---

## Phase 2: Set Up In-App Products

### Step 1: Create the Pro unlock product
1. Go to [Google Play Console](https://play.google.com/console) → your app
2. Navigate to **Monetize > Products > In-app products**
3. Click **Create product**
4. Fill in:
   - **Product ID**: `pro_unlock`
   - **Name**: `CredStow Pro`
   - **Description**: `Unlock all features: unlimited keys, test pings, encrypted backup/restore, biometric lock, auto-lock, remove all ads`
   - **Pricing**: $4.99 USD → **One-time purchase** (not subscription)
   - **Tax classification**: Select appropriate option for your region
5. Click **Save** → **Activate**

### Step 2: Generate promo codes
1. Navigate to **Monetize > Products > In-app products** → `pro_unlock`
2. Click **Promo codes** → **Create promo code**
3. Generate 5–10 codes for testing
4. Distribute to testers (see Phase 5)

---

## Phase 3: Build & Sign the Release AAB

### Option A: Using the prep-release script (recommended)
```powershell
.\prep-release.ps1
# The script handles build + signing with credstow-upload.keystore
```

### Option B: Manual build
```bash
# 1. Swap AdMob IDs first (see Phase 1)
.\gradlew.bat --no-daemon bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Verify the AAB
```bash
# Check file exists and size ~26MB
# AAB is signed with credstow-upload.keystore automatically (configured in build.gradle.kts)
```

---

## Phase 4: Play Console App Setup

### Step 1: App info
1. **Play Console** → your app → **Setup > App info**
2. Fill:
   - **App name**: `CredStow`
   - **App icon**: `ic_launcher.png` (512×512, 32-bit PNG with transparency)
   - **Default language**: English
   - **App type**: `Application`
   - **Device type**: `Phone/Tablet`
   - **Category**: `Productivity`
   - **App title**: `CredStow`
   - **Short description**: `Store & manage AI API keys securely. AES-256-GCM encrypted, biometric lock, one-tap copy.`
   - **Full description**: *(Use the description from HANDOFF.md / the conversation output)*
   - **App content rating**: Run questionnaire → expect **Teen** rating (due to location data for ads)

### Step 2: Data Safety (Step 6)
See `DATA_SAFETY_FORM.md` in this directory for the complete answer guide.

Key selections:
- **Data encrypted in transit**: ✅ Yes
- **Data encrypted at rest**: ✅ Yes (AES-256-GCM)
- **Users can request data deletion**: ✅ Yes
- **Data shared with 3rd parties**: ✅ Yes (AdMob, Google Play Billing, Firebase Crashlytics)
- **Privacy policy**: Enter your hosted URL
- **Ads ID declaration**: ✅ Yes (Google's Advertising ID collected by AdMob for free-tier users)

### Step 3: Content rating
- **Rating scope**: `All ages` → No (contains ads)
- **Ads present**: ✅ Yes — "Ads are only shown to free-tier users. Pro users see no ads."
- **Content ratings questionnaire**: Select:
  - Violence: None
  - Sexual content: None
  - Alcohol/tobacco: None
  - Gambling: None
  - Expected rating: **Teen (13+)**

### Step 4: Pricing & distribution
- **Free**: ✅ Yes
- **Countries**: Select all (or your target markets)
- **Devices**: Phone, Tablet, Foldable

### Step 5: Ads declaration
- **Does your app show ads?**: ✅ Yes
- **Is the ad source Google AdMob?**: ✅ Yes
- **Ads-ID (Android advertising ID)**: ✅ Collected (for free-tier users only)

---

## Phase 5: Upload & Test

### Step 1: Upload AAB to Internal Testing
1. **Play Console** → **Testing > Internal testing**
2. Click **Create new release**
3. Select **`pro_unlock` IAP** → it will be attached to this release
4. Upload `app-release.aab`
5. Click **Next** → **Review** → **Start rollout to Internal**

### Step 2: Opt-in link
1. After rollout completes, copy the **opt-in URL**
2. Distribute to testers: `https://play.google.com/apps/internal/test/[your-app-slug]`

### Step 3: Add testers
- Add the Google account emails of 12+ testers
- Testers must use the **same Google account** on their Android device
- Testers install from the opt-in link (NOT from Play Store search)
- Testers test for **14 days** before production access can be applied for

### Step 4: Required screenshots (per device type)
Upload to the store listing in **All apps > Store presence > Main store listing > Screenshots**:

| Device | Size | Orientation | Count |
|---|---|---|---|
| Phone (small) | 1080×1920 (or 1080×2340) | Portrait | 2+ |
| 7-inch tablet | 1080×1920 | Portrait | 2+ |
| 10-inch tablet | 1920×1200 | Landscape | 2+ |
| Foldable | 1080×2520 | Portrait | 2+ |

**Suggested screenshots**:
1. Key list with locked keys + padlock toggle
2. Add/Edit key screen with provider selector + light blue accent
3. Key test result (green "Valid" status chip)
4. Settings screen showing AES-256-GCM encryption notice
5. FAB (light blue) pressed → Add Key screen

---

## Phase 6: Feature Graphic (1024×500)

Create a feature graphic at `1024×500 px` (landscape PNG/jpeg):

**Design brief**:
- Background: `#102A43` (dark blue splash color)
- Large title: "CredStow" in white, bold
- Subtitle: "Secure AI Key Manager" in `#60A5FA` (light blue)
- Visual: Stylized key/AES icon or vault-style key fob icon
- Accent: subtle teal (`#2EE5C2`) highlights

**Canva template dimensions**: 1024×500 px, landscape

---

## Phase 7: Production Access Application

After 14 days of internal testing:

1. **Play Console** → **All apps > Setup > Production access**
2. Click **Apply for production access**
3. Fill in:
   - **Application type**: New application
   - **App category**: Productivity
   - **App description**: *(Full description)*
   - **Promotional text**: Empty
   - **App screenshot**: Upload 2+ screenshots
   - **Feature graphic**: Upload 1024×500
   - **App icon**: 512×512
   - **Why are you distributing?**: I am the rights holder
   - **Is this app for children?**: No
   - **Is this app ad-supported?**: Yes (free tier only)
   - **Is this app directed at children?**: No
   - **Content rating**: Teen
   - **Privacy policy**: Yes, with URL
   - **Declaration of accessibility**: N/A
   - **User data**: *(See Data Safety form)*
   - **Target audience & kids**: No users under 13
   - **Ads declaration**: Yes, AdMob (free tier)
   - **Comments**: "I have tested the app for 14 days with 12+ testers. All critical bugs resolved. APK/AAB signed with production keystore."

4. Submit → wait 3–7 business days for review

---

## Phase 8: Go Live

After approval:

1. **Play Console** → **Release > Releases > Production**
2. Copy the Internal Testing release → **Create new release in Production**
3. Select same AAB + same IAP product
4. Review → **Rollout to Production**

**Rollout strategy**: 10% → monitor Crashlytics for 24h → 50% → 100%

---

## Quick Reference: AdMob Test IDs

| Item | Test ID | Production ID |
|---|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` | ⬅️ Replace via `prep-release.ps1` |
| Banner | `ca-app-pub-3940256099942544/6300978111` | ⬅️ Replace via `prep-release.ps1` |
| Interstitial | `ca-app-pub-3940256099942544/1033600233` | ⬅️ Replace via `prep-release.ps1` |

> **Never ship with test IDs** — Google will permanently ban your AdMob account if production traffic uses test ad units.

---

## Quick Reference: Build & Release Commands

```powershell
# 1. Swap AdMob IDs for production
.\prep-release.ps1

# 2. Build + test + sign AAB (runs during prep-release.ps1)
#    Or manually:
.\gradlew.bat --no-daemon assembleDebug testDebugUnitTest bundleRelease

# 3. Find the AAB
#    app/build/outputs/bundle/release/app-release.aab

# 4. Restore test IDs in source (IMPORTANT)
git checkout app/src/main/res/values/strings.xml

# 5. Commit the prod-ready AAB metadata (not the strings.xml override)
git add app/
git commit -m "build: production release AAB for Play Store submission"
git push origin main
```

---

## Troubleshooting

| Issue | Solution |
|---|---|
| "AdMob test ads on production" | You forgot to run `prep-release.ps1` or shipped without swapping IDs. Stop the release immediately. |
| "Upload failed: version code X already used" | Bump `versionCode` in `build.gradle.kts` (currently `versionCode = 2`) |
| "IAP product not found" | Ensure `pro_unlock` is **activated** in Play Console before uploading AAB |
| "App not available in country" | Check **Pricing & distribution** → country selection |
| "Feature graphic rejected" | Ensure 1024×500, 32-bit PNG, no text in safe-area margins |
| "Privacy policy missing" | Host the HTML at a real URL (not a local file path) |
| "Data safety form rejected" | Use `DATA_SAFETY_FORM.md` for exact answers |
```
