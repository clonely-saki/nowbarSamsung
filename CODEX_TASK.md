# Codex task: Sports Now Bar Lab V0.1

Work only on the proof-of-concept. Do not add real sports APIs, login, backend, database, AI, analytics, ads, or unrelated UI.

Goals:
1. Ensure the project builds with AGP 9.4 / Gradle 9.6 / compileSdk 36.1.
2. Preserve Android promoted ongoing notification requirements:
   - POST_NOTIFICATIONS
   - POST_PROMOTED_NOTIFICATIONS
   - ongoing=true
   - contentTitle set
   - standard notification style only
   - request promoted ongoing
   - short critical text <= 7 chars
3. Keep one notification ID so score changes update the same live item.
4. Keep the three mock sports: football, basketball, LoL.
5. Make the debug screen clearly show notification permission and `canPostPromotedNotifications()` state.
6. Keep an explicit button that opens the app's notification promotion settings, with a safe fallback.
7. Keep Update and End notification actions.
8. Add/adjust tests where useful, but prioritize a buildable APK and real-device verification.
9. Do not claim Samsung Now Bar support from emulator results. The acceptance test is a real China-region Galaxy device.

Acceptance criteria:
- `:app:assembleDebug` succeeds.
- APK installs.
- Starting any mock match posts exactly one ongoing notification.
- Updating changes the same notification.
- Ending removes it.
- The app exposes enough diagnostic state to determine whether the OS permits promoted notifications.
