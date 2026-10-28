# Orvane — Round 2 · Phase D Device Verification Checklist

Scratch file: every on-device acceptance check discovered while coding Phases A–C.
Executed at Phase D with the device attached; evidence → `qa/r2_<nn>_*.png`.

## Phase A acceptance checks (collected while coding)

- [ ] A1 Title derives from destination: open drawer → open any other smart list → switch to Habits/Calendar/Focus tabs → title shows tab name; back to Today → shows the list/view name.
- [ ] A2 Onboarding: titles readable in Dark + Light (needs fresh onboarded=false state or restore-triggered check — can verify visually only on a wiped profile OR during install-skip prompt on existing profile… plan: temporarily clear onboarded pref via reinstall? NO destructive paths — verify via screenshots only on fresh install from r2 install; if impossible on device, verify in both uimodes via onboarding of a second install package not possible → verify light/dark via theme toggle before onboarded? — resolve at Phase D).
- [ ] A3 Settings → First day of week: all 7 chips on one line; Sun not wrapped; tap Sun → selection persists (dump value).
- [ ] A4 Quick add: type draft → swipe sheet down → reopen → field EMPTY (was stale before). Add a task → reopen → empty.
- [ ] A5 AI assist button invisible with no endpoint/key configured; visible after configuring a fake endpoint+key in Settings.
- [ ] A6 Voice: deny mic permission → message "Microphone permission is off — enable it in Settings to add tasks by voice" (not "Didn't catch that"); grant → recognizer opens.
- [ ] A7 Habit skip: chip shows "» Skipped" text; unskip returns to plain ».

## Phase B acceptance checks (collected while coding)

- [ ] B1a Nag cutoff custom: set cutoff to 23:47 via Custom chip → chip row shows custom value; schedule an unchecked habit nag 2 min ahead → fires at exact time; checked habit does not nag.
- [ ] B1b Snooze presets: custom minutes entry in Settings → presets used by notifications reflect it.
- [ ] B1c Habit reminder custom hour: arbitrary time selectable in habit editor.
- [ ] B2a Pomodoro honors config: Settings Focus=10 → sheet shows 10:00 → run counts from 10:00.
- [ ] B2b Sheet ±5 stepper on focus/short/long → persists to Settings and next session honors it.
- [ ] B2c Long break after configured cycle count; sessions logged in Focus Stats.
- [ ] B3a Matrix is 2×2 grid with axis labels; no horizontal scroll.
- [ ] B3b Drag task between quadrants ~1s hold → quadrant mapping updates task (check priority/due in task detail).
- [ ] B3c Empty quadrant shows dashed drop zone; expand quadrant on header tap.
- [ ] B4a 12 combinations: {System,Light,Dark} × {dynamic on, off} for all main screens — via `adb shell cmd uimode night yes|no` + in-app toggles; screenshots.
- [ ] B4b Toggle dynamic color → instant change, no restart.
- [ ] B4c Zero hardcoded Color(0xFF…) outside theme/data files (grep gate stays passing); light mode coherent everywhere.

## Phase C visual evidence list (collected while coding)

- [ ] r2 evidence for: tasks screen (greeting header, overdue banner, cards, swipes, sections), quick-add redesigned sheet, habits (hero, cards, weekday dots, stats panel with aligned charts, heatmap), drawer (branded header, groups, pills, manage entries), matrix, focus (hero timer, chips, stats charts), settings (grouped cards), calendar (dots, today ring, time indicator, agenda), onboarding, lock overlay, empty states, light-mode set.

## Regression flows (end of session)

- [ ] Quick add NL parse → save
- [ ] Complete + undo snackbar
- [ ] Habit check-in + skip + stats expand
- [ ] Pomodoro run/pause/stop + notification present
- [ ] Backup export JSON (SAF) → restore round trip
- [ ] Back gesture pops overlays (r1 regression)
- [ ] FAB still centered on icon row (r1 regression)
- [ ] Instrumented suites connectedDebugAndroidTest green
