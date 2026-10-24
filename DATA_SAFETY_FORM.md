# CredStow — Google Play Console Data Safety Form Answers

> **Step 6 of "Finish setting up your app" → Data Safety**
>
> This document provides every answer and selection needed to complete the Data Safety section.
> Navigate the form **top to bottom**, making each selection exactly as described below.

---

## Section 1: Overview

### Q: Do you have a privacy policy?
**Selection**: ✅ **Yes** (link the hosted privacy policy URL)
- If you haven't hosted it yet, you can use the offline HTML asset at `app/src/main/assets/privacy_policy.html` — upload it to a web URL (e.g., GitHub Pages, your domain, or use a service like [privacypolicies.com](https://www.privacypolicies.com)).

### Q: Does your app collect any personal or sensitive user data?
**Selection**: ✅ **Yes**
- Even though we don't collect personal data directly, we share some data with third parties (AdMob, Google Play Billing, Firebase Crashlytics).

---

## Section 2: Data Types Collected

For each data type, select **which types are collected**, then for each collected type, specify the **purposes** and **sharing** status.

### 2.1 Personal Info

| Data Type | Collected? | Shared with 3rd Parties? | Purpose |
|---|---|---|---|
| Name | **No** | N/A | Not collected |
| Email address | **No** | N/A | Not collected |
| User ID | **No** | N/A | Not collected |
| Phone number | **No** | N/A | Not collected |
| Address | **No** | N/A | Not collected |
| Birth date | **No** | N/A | Not collected |
| Gender | **No** | N/A | Not collected |
| Place of birth | **No** | N/A | Not collected |
| Country | **No** | N/A | Not collected |

> ✅ **None of the above** — Select this option for Personal Info.

---

### 2.2 Financial Info

| Data Type | Collected? | Shared with 3rd Parties? | Purpose |
|---|---|---|---|
| Payment info | **No (directly)** | See note | Google Play Billing handles payment; Google collects this |
| Purchase history | **No (directly)** | See note | Google Play Billing manages purchase state |
| Tax ID | No | N/A | Not collected |
| Salary | No | N/A | Not collected |
| Credit/debit card | No | N/A | Not collected |
| Bank account | No | N/A | Not collected |
| Investment info | No | N/A | Not collected |

> **Important**: While the app itself doesn't collect financial data, users who purchase Pro via Google Play Billing will have financial data processed by Google's payment systems. This is captured under "Data shared with third parties" below.

> ✅ **None of the above** — Select this for data *collected by the app*.
>
> When the form asks about *sharing* financial data with third parties, see Section 3 below.

---

### 2.3 Location Data

| Data Type | Collected? | Purpose |
|---|---|---|
| Precise location | **No** | Not collected |
| Approximate location | **No** | Not collected |

> ✅ **None of the above** — Select this option.

---

### 2.4 Sensory Data

| Data Type | Collected? | Purpose |
|---|---|---|
| Camera | No | Not collected |
| Photos | No | Not collected |
| Audio | No | Not collected |
| Video | No | Not collected |
| Microphone | No | Not collected |

> ✅ **None of the above** — Select this option.

---

### 2.5 Contacts

| Data Type | Collected? | Purpose |
|---|---|---|
| Contacts list | No | Not collected |
| Message history | No | Not collected |
| Email messages | No | Not collected |
| SMS/MMS | No | Not collected |

> ✅ **None of the above** — Select this option.

---

### 2.6 User Activity & Search History

| Data Type | Collected? | Purpose |
|---|---|---|
| Search history | No | Not collected |
| Browsing history | No | Not collected |
| User activity | No | Not collected |
| Content interactions | No | Not collected |

> ✅ **None of the above** — Select this option.

---

### 2.7 App Information & Device IDs

| Data Type | Collected? | Shared? | Purpose |
|---|---|---|---|
| App interactions | No | N/A | Not collected |
| Advertising ID | **No (directly)** | See 3.1 | AdMob may access via Google Play services |
| Page views | No | N/A | Not collected |
| In-app purchases | No (directly) | See 3.2 | Google Play Billing handles |
| Crash data (stack traces) | No (no key data) | See 3.3 | Firebase Crashlytics |
| Diagnostics | No | N/A | Not collected |
| Device IDs | No (directly) | See 3.1 | AdMob uses Google's advertising ID |
| Browser/web history | No | N/A | Not collected |
| IP address | No (directly) | See 3.1 | AdMob logs may include this |
| Device logs | No | N/A | Not collected |

> ✅ **None of the above** — Select this for data *collected by the app*.

---

### 2.8 Stored Data (on device / in-app)

> ⚠️ **Important**: The Data Safety form asks about data stored in the app, including local storage and backups.

| Data Type | Collected? | Stored on device? | Purpose |
|---|---|---|---|
| **API keys** (user-entered) | Yes (user-provided) | ✅ Yes | Core app functionality (AES-256-GCM encrypted) |
| Provider selection | Yes | ✅ Yes | Key management |
| Key labels & statuses | Yes | ✅ Yes | UI display |
| **Purchased product IDs** | Yes (local only) | ✅ Yes | Track Pro status |
| **Pro trial start/end timestamps** | Yes (local only) | ✅ Yes | Enforce 5-day free trial period |
| Settings (sort order, lock settings, font scale) | Yes | ✅ Yes | User preferences |
| Clipboard auto-clear counter | Yes | ✅ Yes | Ad frequency capping |
| Exported backup file | User-initiated | User-chosen location | Backup/restore |

**Purposes selected**:
- ✅ **App functionality** (all data types)
- ❌ Personalization
- ❌ Advertising
- ❌ Analytics
- ❌ Health (N/A)

**Sharing**: None of this stored data is shared with third parties. It remains on the device.

---

## Section 3: Data Shared with Third Parties

### 3.1 AdMob (Google) — Free Tier Users Only

**Data shared**: Device identifiers (Google's Advertising ID), IP address
**Purpose**: Serve banner and interstitial ads, frequency capping
**Legal basis**: User is free to upgrade to Pro ($4.99 one-time) to remove all ads
**Selection**:
- **Is the data you share limited to a relevant and approved purpose?** → ✅ Yes
- **Data type**: `Device or other identifiers` → `Advertising ID`
- **Shared with**: Google / AdMob

### 3.2 Google Play Billing (Google) — Pro Purchase

**Data shared**: Purchase-related info (name, email, payment method, purchase details)
**Purpose**: Process the one-time `pro_unlock` ($4.99) purchase
**Legal basis**: User-initiated purchase action
**Selection**:
- **Is the data you share limited to a relevant and approved purpose?** → ✅ Yes
- **Data type**: `Financial info` → `Purchase history`
- **Shared with**: Google / Google Play

### 3.3 Google Play Billing — Owned Product IDs

**Data shared**: Set of owned product IDs (e.g., `{"pro_unlock"}`)
**Purpose**: Verify Pro ownership on app startup (offline local copy)
**Note**: This data is stored **locally** in DataStore and synced via Google Play Billing. It is NOT sent to any CredStow server.
**Selection**:
- **Is the data you share limited to a relevant and approved purpose?** → ✅ Yes

### 3.4 Firebase Crashlytics (Google) — Crash Reports

**Data shared**: Crash stack traces, exception messages, device model, OS version, app version
**Purpose**: Diagnose and fix crashes, improve app stability
**Critical note**: **No API keys, key labels, or user-entered content are logged**. The `TestKeyUseCase` passes plaintext keys directly to the HTTP client and never logs them. `Log.d()` calls throughout the app only log flow control states (e.g., "SplashGate: dest=KEY_LIST"), never keys.
**Selection**:
- **Is the data you share limited to a relevant and approved purpose?** → ✅ Yes
- **Data type**: `App info and settings` → `Crash logs`
- **Shared with**: Google / Firebase

### 3.5 API Provider Endpoints (User-initiated)

**Data shared**: Plaintext API key (only the key being tested)
**Purpose**: Validate the API key by sending an authenticated HTTP request to the provider's endpoint
**Trigger**: Only when the user taps the "test" button on a key
**Selection**:
- ⚠️ This is **not a third-party sharing** in the Data Safety form sense, because the user is explicitly sending their own data to a service they control. The app acts as a pass-through client.
- **However**: You may optionally disclose this under "App info and settings" → `Other data shared with third parties` → `API endpoints` (OpenAI, Anthropic, Google, etc.)

> ✅ **Recommendation**: Include this transparency disclosure. Users should know that tapping "test" sends their key to the API provider.

---

## Section 4: Security Practices

The form will ask whether you implement security measures. Mark all that apply:

### ✅ Security measures implemented in CredStow

1. **Data encrypted in transit** — ✅ Yes
   - All API key test pings use HTTPS (TLS 1.2+)
   - No HTTP traffic anywhere in the app

2. **Data encrypted at rest** — ✅ Yes
   - Every API key is encrypted with AES-256-GCM (unique IV per key)
   - The AES master key (DEK) is wrapped and stored in Android Keystore
   - Backup exports use an additional layer: PBKDF2 password derivation → AES-256-GCM

3. **Can users request data deletion?** — ✅ Yes
   - Deleting a key removes it from the local database permanently
   - All DataStore preferences can be reset by uninstalling the app or clearing app data from system Settings

4. **Data backups** — ⚠️ Optional disclosure
   - The app provides an **encrypted export** feature (Pro only) that creates an AES-256-GCM encrypted backup file
   - Users can import this backup to restore their keys
   - The encryption password is NOT stored anywhere — if the user loses it, the backup is unrecoverable

---

## Section 5: Form Selection Flow (Step-by-Step)

### Step 1: Data you collect
Navigate through each data category and select:
- **Personal info**: ❌ None
- **Financial info**: ❌ None
- **Health & fitness**: ❌ None
- **Location**: ❌ None
- **Photos/Media**: ❌ None (no media access)
- **Storage**: ✅ Yes — local app data (API keys, settings, preferences)
  - Purpose: ✅ App functionality
  - Shared with 3rd parties: ❌ No
- **Device or other identifiers**: ❌ None
- **App info & device IDs**: ✅ Yes — crash data (via Crashlytics)
  - Purpose: ✅ App functionality (crash diagnosis)
  - Shared with 3rd parties: ✅ Yes (Google/Firebase)
- **Other**: ✅ Yes — API keys (user-entered)
  - Purpose: ✅ App functionality
  - Shared with 3rd parties: ❌ No (except user-initiated test pings to provider APIs)

### Step 2: Data shared with third parties
- **Financial info**: ✅ Yes — shared with Google Play (purchase processing)
- **Device or other identifiers**: ✅ Yes — Advertising ID shared with Google AdMob
- **App info & device IDs**: ✅ Yes — Crash data shared with Google/Firebase

### Step 3: Security practices
- ✅ Data encrypted in transit
- ✅ Data encrypted at rest
- ✅ Users can request data deletion

### Step 4: Description
Enter this description:

> **CredStow stores and manages AI API keys (OpenAI, Anthropic, Google, Hugging Face, etc.) entirely on-device. All API keys are encrypted with AES-256-GCM, backed by the Android Keystore. No API keys are ever sent to CredStow servers — the only network traffic is user-initiated test pings sent directly to the relevant provider's API (OpenAI, Anthropic, etc.).
>
> Free-tier users see AdMob banner and interstitial ads. AdMob (Google) may receive the device's advertising ID and IP address for ad serving and frequency capping. Upgrading to Pro ($4.99 one-time) removes all ads.
>
> Purchasing Pro uses Google Play Billing; Google processes the payment and purchase verification. The purchased product ID is stored locally.
>
> Crash reporting via Firebase Crashlytics collects stack traces, exception messages, device model, OS version, and app version to diagnose crashes. No API keys, key labels, or user-entered content are ever logged.
>
> All user data (API keys, settings, sort preferences, Pro status, trial timestamps) is stored locally on the device using encrypted Room and DataStore. No personal data is sent to any CredStow server.

---

## Key Takeaways for Form Completion

| Question | Answer |
|---|---|
| Is user data encrypted? | ✅ Yes (AES-256-GCM + Android Keystore) |
| Can users delete their data? | ✅ Yes (delete keys, clear app data, uninstall) |
| Is data shared with 3rd parties? | ✅ Yes, but only: AdMob (ads), Google Play (billing), Firebase (crashes) |
| Is any data sent to CredStow servers? | ❌ No — all data stays on-device |
| Is the user's API key logged in crash reports? | ❌ No — no keys are ever logged or included in Crashlytics reports |
| Is user-entered data used for advertising? | ❌ No — only Google's advertising ID is shared with AdMob for ad serving |
