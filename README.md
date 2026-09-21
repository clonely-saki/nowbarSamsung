# Sports Now Bar Lab — V0.1

A minimal Android proof-of-concept for testing whether a third-party sports app can appear as a Samsung Galaxy Now Bar / Live Update on a China-region device.

## Scope

No real sports API yet. This build intentionally isolates the platform question first.

Three simulated match types:
- Football: Arsenal vs Chelsea
- Basketball: Lakers vs Celtics
- LoL: BLG vs TES

Each match posts one ongoing notification and updates the same notification ID. It requests promoted-ongoing treatment, sets short critical text for the status-bar chip, uses a standard BigTextStyle, and exposes Update / End actions.

## Why this PoC exists

Android Live Updates require a properly formatted ongoing notification and the user/system must allow promoted notifications. Samsung/OEM software can still apply additional eligibility rules. Therefore the only trustworthy answer for Now Bar support is a real-device test.

## Device test checklist

1. Install the debug APK on the target Galaxy.
2. Open the app and grant normal notification permission.
3. Tap `打开 Live Updates / 提升通知设置` and enable promoted/live notifications if that screen exists.
4. Confirm the app shows `可发布 Promoted/Live Update: ✅`.
5. Start Football.
6. Lock the phone and check whether the activity appears in Now Bar.
7. Unlock the phone and check whether a compact chip appears in the status bar.
8. Expand the notification/Now Bar and record which text/actions Samsung renders.
9. Trigger several updates and confirm the existing Now Bar item updates instead of creating new cards.
10. Repeat with Basketball and LoL.
11. Tap End and confirm the live item disappears.

Record the exact model, One UI version, Android version, and screenshots for each surface.

## Important limitations

- This is a platform capability test, not a finished sports app.
- No custom RemoteViews are used because promoted Live Updates require supported standard notification styles.
- Whether Samsung renders the update in Now Bar is ultimately controlled by Samsung firmware and user settings.
- Android 16/QPR platform behavior may differ by device software release.

## Cloud build

The GitHub Actions workflow builds a debug APK on each push to `main`, or manually through `workflow_dispatch`.

Expected artifact:
`app/build/outputs/apk/debug/app-debug.apk`
