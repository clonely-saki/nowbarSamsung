# Samsung package-allowlist diagnostic build

This is an isolated personal-device compatibility experiment. It is not a production or distribution build.

## Build configuration

Normal builds remain unchanged:

```text
applicationId: com.nowbarsports.poc
```

Build the diagnostic APK only by passing the temporary Gradle property:

```text
gradle :app:assembleDebug -PsamsungDiagnostic=true
```

The diagnostic build uses:

```text
applicationId: com.nhn.android.nmap
versionName: 0.1.0-samsung-diagnostic
```

The same Samsung metadata and notification extras used by Experiment A remain enabled. The diagnostic UI contains only one football mock Live Update with Start, Update, and End controls.

## Signing and package collision

This is an `assembleDebug` build and is signed with the project's local Android debug keystore. It is not signed with Kakao's or any official application's certificate. Android therefore will not treat it as an update to an installed official `com.nhn.android.nmap` application.

Because the package name is reused, the diagnostic APK cannot coexist with another installed `com.nhn.android.nmap` package. Installing it may require removing the existing package first, which can remove that application's local data. Do not distribute this APK or publish it to an app store.

## Cleanup

After testing, uninstall the diagnostic package completely:

```text
adb uninstall com.nhn.android.nmap
```

If the official application was removed before testing, reinstall it from its official source afterward. Rebuild without `-PsamsungDiagnostic=true` to restore the normal project identity for future builds.
