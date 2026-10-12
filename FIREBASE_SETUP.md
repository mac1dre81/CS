# Firebase Crashlytics Setup

## 1. Create a Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project (or reuse an existing one).
2. Register your Android app with package name `com.credstow.app`.
3. Download the generated `google-services.json`.

## 2. Place the file

Copy `google-services.json` to:
```
app/google-services.json
```

This file is **git-ignored** (see `.gitignore`) and must never be committed.

## 3. What happens next

The `google-services` and `firebase-crashlytics` Gradle plugins are already applied.
Once the JSON file is present:

- Crashlytics initializes automatically on app startup.
- Crashes are reported to the Firebase Console within minutes.
- No code changes are required — the SDK is auto-initialized.

## 4. Verification

After placing the file, rebuild:
```
./gradlew :app:assembleDebug
```

Check `logcat` for `FirebaseCrashlytics` initialization logs.
