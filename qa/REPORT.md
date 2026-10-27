# Tasky — On-Device QA Report

> **N.B. (2026-10-28):** after this sweep the app was renamed **Orvane** — applicationId `com.tasky.app` ->
> `com.orvane.app` (full package rename, R-package, tests, DB/datastore/work names), visible name, widget
> labels, notifications and in-app copy rebranded; legacy `"app": "Tasky"` marker backups still restore.
> The evidence and findings below predate the rename.

**Date:** 2026-10-27 (session ended 2026-10-28 IST) · **Build:** debug APK `app/build/outputs/apk/debug/app-debug.apk` (29,404,523 B)
**Device:** Google Pixel 10 `57311FDCR0063F`, Android 17 (SDK 37), 1080×2424, dark theme, gesture nav, IST (UTC+5:30)
**Scope:** ~35 interactive acceptance scenarios driven via adb/uiautomator with screenshots in `qa/`. All data operations verified against Room DB state, datastore files, dumpsys, and logcat — not just pixels.

## Verdict

**PASS with notes.** All core flows (tasks, lists, tags, calendar, habits, focus, backup, lock, offline) work; two real bugs found during the sweep were fixed and re-verified; quality gates green. Widget coverage is partial (see Findings — user deferred/skipped widget placement in this version).

## Quality Gates

| Gate | Status |
|---|---|
| `./gradlew lintDebug` | **0 errors**, 47 warnings (warnings acceptable) |
| `./gradlew testDebugUnitTest` | **55 tests, 0 failed, 0 skipped** |
| `assembleDebug` | successful (APK installed & exercised on device) |
| 4 instrumented suites on device | 4 passed / 0 failed (run before this sweep; APK on device includes those fixes) |
| Crash watchdog (`logcat *:E` grep FATAL/ANR) | **zero fatal events** across the whole session; killed at end |

## Scenario Matrix

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 01–03 | First launch, onboarding pages | PASS | `01_launch_first_run.png`, `02_onboarding_p2.png`, `03_onboarding_p3.png` |
| 04–05 | Dashboard after onboarding; drawer lists/tags | PASS | `04_dashboard_after_onboarding.png`, `05_drawer*.png` |
| 06 | Sample tasks seeded | PASS | `06_inbox_sample_tasks.png` |
| 07–08 | Quick add natural-language parse ("buy milk tomorrow 5pm #errands") + add | PASS | `07_quickadd_nl_parse.png`, `08_quickadd_added.png` |
| 09–10 | Complete row + undo snackbar; Completed list (no duplicate rows after fix) | PASS | `09_complete_undo_snackbar.png`, `10_completed_list.png`, `49_completed_no_duplicates.png` |
| 11–13 | Task detail; subtask add/complete | PASS | `11_task_detail.png`, `12_subtask_added.png`, `13_subtask_*.png` |
| 14–16 | Priority bar (High red); tag add with "+" chip + Remove ✕; detail exit shows row progress `1/1` | PASS | `14_priority_high.png`, `15_tag_added.png`, `16_listrow_progress_priority.png` |
| 17 | Move task to "Work" list via detail 🗂 dropdown | PASS | `17_list_work_moved_task.png` |
| 18–20 | Calendar Month / Week / Agenda + hour grid + event chips | PASS | `18_calendar_month.png`, `19_calendar_week.png`, `20_calendar_agenda.png` |
| 21 | Device-calendar read sync | PASS — denied shows "Calendar permission needed", granted syncs "467 device events" | `21_calendar_sync.png` |
| 22 | ICS feed add (local `http.server 8765` + `adb reverse`) | PASS — "Fetched 2 events from feeds" | `22_ics_feed_added.png` |
| 23–25 | Habit check-in (1/2, streak), stats panel + heatmap (via card body tap), create habit "Read 20 pages" 📚 Daily | PASS | `23_habit_checkin.png`, `24_habit_*.png`, `25_habit_created.png` |
| 26–28 | Pomodoro setup sheet / running 24:53 pause-skip-stop / pause freeze & resume / FGS + shade notification scope | PASS — FGS `isForeground=true`, id 4242, `channel_focus`; timer survives backgrounding; Stop action kills service | `26_pomodoro_running.png`, `27_pomodoro_paused.png`, `28_pomodoro_notification.png` |
| 29–30 | Focus Stats (30-day totals, top tasks) + Eisenhower matrix buckets | PASS | `29_focus_stats.png`, `30_focus_matrix.png` |
| 31–32 | Habits widget placed on home; tap-toggle checks in from widget (⚠ covered before widget work was deferred) | PASS (partial widget coverage) | `31_widget_habits_home.png`, `32_widget_habit_tap.png` |
| 33–34 | Backup JSON via SAF → `/sdcard/Download/tasky-backup.json` (5,624 B) → "Backup saved." | PASS | `33_backup_saf.png`, `34_backup_saved.png` |
| 35 | Destructive restore: uninstall + reinstall + skip-samples + Restore… → "Restored 7 tasks." — lists/tags/tasks/habits/checks fully back | PASS | `35_restore_done.png` |
| 36 | Post-restore habit skip-fidelity: SKIPPED record survives restore and renders (tinted » chip) | PASS | `36_habit_skip_restored.png` |
| 37–43 | App lock: set PIN 8-digit, relaunch → lock overlay, wrong PIN rejected, correct PIN unlocks, biometric enable (UI switches to "Change PIN"), face-auth auto-unlock (BiometricPrompt), "Use PIN" fallback after fingerprint prompt → keypad → unlock | PASS | `37_applock_on.png`, `38_pin_lock_overlay.png`, `39_wrong_pin_rejected.png`, `40_pin_unlock_success.png`, `41_biometric_enabled.png`, `42_biometric_prompt.png`, `43_biometric_pin_fallback.png` |
| 44–48 | Airplane mode ON: app start, quick-add → Inbox save, Habits render, no crash; airplane OFF restore | PASS | `44_offline_today.png`, `45_ai_assist_no_endpoint.png`, `46_voice_denied_graceful.png`, `47_offline_*.png`, `48_offline_habits.png` |
| — | Auto backup staging: fresh-install run writes valid Tasky JSON (13 keys) to private `files/backups/` | PASS (prune by code review, see notes) | JSON pulled & parsed |

## Bugs Found & Fixed During This Sweep

1. **Splash screen missing** — app flashed its post-splash theme window on cold start (manifest declared `Theme.Tasky.Splash` but `installSplashScreen()` was never called). Fixed in `MainActivity.kt`; verified on the reinstalled APK.
2. **Smart list "Completed" rendered duplicate sections** — the view emits `completed` rows already (own header via `showCompletedAtBottom`), while the section list also produced a "Completed" section → double listing. Fixed in `TasksViewModel.kt` (sections = empty for `SmartList.COMPLETED`); re-verified on device with restored data (single header only, `qa/49_completed_no_duplicates.png`).

## Findings & Notes (no action / by-design / deferred)

- **Widget coverage partial.** Habits widget placement + tap-to-check verified (31–32). Today-widget drop, Pomodoro mini widget, and Quick-add widget placement were skipped entirely on explicit user instruction for this version. Widget drag needs `input motionevent DOWN/MOVE/UP` with ~1 s hold (plain `input swipe` fails).
- **Habit "skipped" state is color-only** — the » skip chip fills with `tertiaryContainer`, no text affordance on the row (and the checked-box area stays empty). Restore fidelity is correct (stat card "0 done · 1 skipped"); low-severity UX nit.
- **No UI to create lists/folders.** Post-restore data shows them; creation only possible via seeded/backup data — roadmap gap rather than defect.
- **TopAppBar title sticks** — after opening drawer and switching smart tabs, the title can stay on the previous list name (cosmetic).
- **Onboarding title contrast** and **Settings "Sun" chip wraps to 3 lines** — minor visual nits.
- **Quick-add drafts survive sheet dismissal** — reopening restores unsaved text; can surprise (observed with doubled title during repeated sheet opens).
- **AI assist button is always visible** in quick add (Settings blurb says "when configured") — with no endpoint it fails graceful: "Add an AI base URL and key in Settings first."
- **Voice quick-add does not request `RECORD_AUDIO` itself** — delegates to the system recognizer; with mic permission denied the Google sheet ends in "Didn't catch that. Try speaking again." — graceful, but the message misattributes a permission denial to a listening failure.
- **Auto-backup pruning (`keep newest 7`) not exercisable on-device** — the 24 h periodic job cannot be force-run on Android 17 (`jobscheduler run` rejects the WorkManager id); staging + JSON validity verified for the real run; prune logic reviewed only. Newest snapshot after restore captured the pre-restore empty DB — harmless since Restore reads user-picked files, but first-run restore window is briefly un-protected.
- **Pixel-10 face-auth auto-authenticates instantly**, so the fingerprint/Lock-overlay path is exercised mainly through its "Use PIN" fallback; both paths verified.
- **`pm clear` fails via adb shell on this device** — full data reset requires uninstall/reinstall (used for scenario 35).

## Device State Left After QA

Airplane mode OFF; App lock OFF (PIN hash retained; biometric pref stays enabled but inert); POST_NOTIFICATIONS + READ_CALENDAR granted; offline test task moved to Trash (Inbox back to 1); ICS test server killed; `adb reverse` entries cleared; logcat watchdog killed; `qa/` screenshots + this report are the only artifacts.

## Commits

- (previous) `4a5d672` — instrumented-test stack bump + test fixes
- (this) — splash fix + Completed-duplicate fix + `qa/` evidence pack
