# Orvane

Orvane is an offline-first personal productivity app for Android: tasks, calendar,
habits and a focus timer in one place. Data lives entirely on-device in a local
Room database — no account, no sync, no telemetry.

The app was previously called **Tasky** (`com.tasky.app`); it was renamed to
**Orvane** (`com.orvane.app`) in October 2026. Backup files exported by the old
app still restore (see [Backup & restore](#backup--restore)).

## Features

- **Tasks** — custom lists with sort/collapse, smart views (Today, Tomorrow,
  Next 7 days, Overdue, All, Inbox, Completed, Trash), tags, priorities,
  recurring tasks, notes, due dates and reminders.
- **Quick add** — one text field with natural-language date parsing
  ("Pay rent tomorrow 10am" picks up the due date/time).
- **Reminders** — alarm-scheduled notifications with complete / snooze actions;
  uncompleted habits get an evening nag at a configurable cutoff.
- **Calendar** — month view combining tasks and device calendar events
  (read via CalendarContract), plus `.ics` file import.
- **Habits** — daily check-ins with streak calculation and progress display.
- **Focus** — Pomodoro timer (configurable focus/short/long lengths) running in
  a foreground service, with white-noise ambience, 30-day focus statistics and
  an Eisenhower-matrix overview of work.
- **Search** — global search across task titles and notes.
- **Widgets** — Glance app widgets: Today tasks, habit check-off, Pomodoro mini
  and Quick Add.
- **App lock** — optional PIN with device-kit biometric unlock when available.
- **Optional AI assist** — point Orvane at any OpenAI-compatible endpoint; the
  API key is stored locally and sent only to that endpoint.
- **Backup & restore** — full JSON export/import over SAF, task-list CSV export,
  and an automatic backup every 24 h that keeps the newest 7 files.
- **Material 3 UI** — light/dark themes with an optional Material You dynamic
  color mode; adaptive launcher icon (dark circle + teal check) with a
  monochrome/themed variant.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.0, JDK 17 toolchain |
| UI | Jetpack Compose, Material 3, view-model-flavored state (single-activity, state-based navigation) |
| Storage | Room 2.6 (`orvane.db`) + DataStore preferences (`orvane_settings`) |
| Background | WorkManager (automatic backup), AlarmManager exact alarms (reminders/nags), foreground service (Pomodoro + white noise) |
| DI | Hilt (incl. `hilt-work`, `hilt-navigation-compose`) |
| Widgets | Glance 1.1 (Material 3) |
| Misc | AndroidX Biometric, Media, kotlinx-serialization (backup JSON) |

Module layout (single `:app` module, package `com.orvane.app`):

```
app/src/main/java/com/orvane/app/
├── core/          # ai, alarms, backup, calendar (ICS), filter, focus,
│                  # nl (date parser), notifications, recurrence, streak, util
├── data/
│   ├── db/        # Room entities, DAOs, OrvaneDatabase, smart-view ids
│   ├── repo/      # Task / Habit / Calendar / Focus / Backup repositories
│   └── settings/  # DataStore-backed settings repository
├── di/            # Hilt modules
├── ui/            # calendar, drawer, focus, habits, lock, onboarding,
│                  # quickadd, search, settings, tasks, theme, common
└── widgets/       # Glance widgets (Today, Habits, Pomodoro, Quick Add)
```

## Building

Requirements: Android Studio (or plain Gradle) with JDK 17 and the Android SDK
platform for `compileSdk = 36`. Debug builds are unsigned-keystore free.

```bash
git clone <your-fork-url> && cd Tasky
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device/emulator (Android 8.0+, `minSdk = 26`):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing

```bash
./gradlew testDebugUnitTest            # JVM/Robolectric suite (55 tests)
./gradlew lintDebug                    # Android lint (0 errors expected; warnings only)
./gradlew connectedDebugAndroidTest    # instrumented suite (Hilt + Compose UI tests)
```

## Backup & restore

Backups are plain JSON at `/sdcard/…` via the system file picker (SAF), with a
header like:

```json
{
  "format": 1,
  "exportedAt": 1730000000000,
  "app": "Orvane"
}
```

Restore replaces matching entries (no merge dialog). Backups written by the
former Tasky build carry `"app": "Tasky"` and are still accepted, so a Tasky
→ Orvane migration is a pure *export → install Orvane → restore*.

> Changing the applicationId (as the rename did) makes Android treat the app as
> brand new: existing installs must restore from a backup, and home-screen
> widgets must be re-added.

## Quality assurance

`qa/` contains an on-device acceptance report (`qa/REPORT.md`) driven over adb/
uiautomator on a Pixel 10 with screenshots (`qa/NN_*.png`) covering tasks,
lists/tags, calendar, habits, focus/Pomodoro, widgets, search, backup/restore,
app lock and offline behavior.

## License

Internal/personal project — no license file yet;
