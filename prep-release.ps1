# Production release preparation script.
# Run AFTER you have obtained your real AdMob app ID, banner unit ID,
# interstitial unit ID, and confirmed the Play Billing product ID "pro_unlock".
#
# Usage:
#   1. Edit the four PROD_* variables below.
#   2. Run this script from the project root:  powershell -File prep-release.ps1
#   3. Build & sign the release AAB:  .\gradlew.bat --no-daemon bundleRelease
#   4. Upload to Play Console Internal Testing track.

$PROD_ADMOB_APP_ID      = "ca-app-pub-YOUR-REAL-APP-ID~YOUR-REAL-APP-NUMBER"
$PROD_ADMOB_BANNER       = "ca-app-pub-YOUR-REAL-APP-ID/YOUR-REAL-BANNER-ID"
$PROD_ADMOB_INTERSTITIAL = "ca-app-pub-YOUR-REAL-APP-ID/YOUR-REAL-INTERSTITIAL-ID"

$files = @(
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/credstow/app/di/ServiceLocator.kt",
    "app/src/main/java/com/credstow/app/ui/screens/KeyListScreen.kt"
)

foreach ($f in $files) {
    if (-not (Test-Path $f)) { Write-Error "Not found: $f"; exit 1 }
}

# --- strings.xml: replace test AdMob IDs ---
$xml = Get-Content "app/src/main/res/values/strings.xml" -Raw
$xml = $xml -replace 'ca-app-pub-3940256099942544~3347511713', $PROD_ADMOB_APP_ID
$xml = $xml -replace 'ca-app-pub-3940256099942544/6300978111', $PROD_ADMOB_BANNER
$xml = $xml -replace 'ca-app-pub-3940256099942544/1033600233', $PROD_ADMOB_INTERSTITIAL
$xml = $xml -replace '<!-- AdMob: Google-provided demo.*?-->', '<!-- AdMob: production IDs -->'
$xml = $xml -replace '<!-- Full-page.*?-->', '<!-- Full-page interstitial (production ID) -->'
Set-Content "app/src/main/res/values/strings.xml" $xml

# --- rename string keys from _test_ to production naming ---
$xml = Get-Content "app/src/main/res/values/strings.xml" -Raw
$xml = $xml -replace 'admob_test_app_id', 'admob_app_id'
$xml = $xml -replace 'admob_test_banner', 'admob_banner'
$xml = $xml -replace 'admob_test_interstitial', 'admob_interstitial'
Set-Content "app/src/main/res/values/strings.xml" $xml

# --- AndroidManifest.xml: update string reference ---
$manifest = Get-Content "app/src/main/AndroidManifest.xml" -Raw
$manifest = $manifest -replace '@string/admob_test_app_id', '@string/admob_app_id'
Set-Content "app/src/main/AndroidManifest.xml" $manifest

# --- ServiceLocator.kt: update string reference ---
$sl = Get-Content "app/src/main/java/com/credstow/app/di/ServiceLocator.kt" -Raw
$sl = $sl -replace 'R.string.admob_test_interstitial', 'R.string.admob_interstitial'
Set-Content "app/src/main/java/com/credstow/app/di/ServiceLocator.kt" $sl

# --- KeyListScreen.kt: update string reference ---
$kls = Get-Content "app/src/main/java/com/credstow/app/ui/screens/KeyListScreen.kt" -Raw
$kls = $kls -replace 'R.string.admob_test_banner', 'R.string.admob_banner'
Set-Content "app/src/main/java/com/credstow/app/ui/screens/KeyListScreen.kt" $kls

Write-Host "Production AdMob IDs applied. Build with: .\gradlew.bat --no-daemon bundleRelease"
Write-Host "Play Billing product ID 'pro_unlock' is already configured in AppConst.kt - no change needed."
