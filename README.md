# Android Automation App

A native Android automation app (Kotlin + Jetpack Compose) that runs three always-on background rules, each independently toggleable from the notification shade without restarting the service.

---

## Overview

| Rule | Trigger | Action |
|------|---------|--------|
| **App Audio Suppressor** | Target app enters foreground | Silences that app's video/reel audio; background music unaffected |
| **Battery Saver** | Battery drops below threshold | Applies a configurable set of power-saving actions |
| **DND on Media + Bluetooth** | Selected app plays audio over selected BT device | Enables Do Not Disturb |

---

## Architecture

```
AutomationForegroundService  (START_STICKY foreground service)
├── RuleEngine               (AND-logic multi-trigger evaluation)
│   ├── Rule 1 → ForegroundAppTrigger        → SuppressMediaSessionAction
│   ├── Rule 2 → BatteryLevelTrigger         → [configurable action list]
│   └── Rule 3 → MediaSessionActiveTrigger
│               + BluetoothAudioTrigger      → DndModeAction
├── StateManager             (save/restore per-action state, stack-based)
└── BroadcastReceiver        (per-rule toggle from notification or UI)

AutomationAccessibilityService   (foreground app detection + key events)
AutomationNotificationListenerService  (MediaSession monitoring)
```

**Key design decisions:**
- Rules evaluate AND logic across all their triggers — all must be active simultaneously for the rule to fire.
- Actions save their pre-activation state and restore it on revert, so the device returns to exactly the state it was in before the rule fired.
- Per-rule enabled state persists to `SharedPreferences` and survives service restarts.

---

## Rules

### Rule 1 — App Audio Suppressor

**Trigger:** The configured target app enters the foreground (detected via `AccessibilityService` `TYPE_WINDOW_STATE_CHANGED`).

**Action:** A 300 ms polling loop checks the app's `MediaSession`. Whenever audio is playing, it calls `transportControls.pause()`. This silences reels, stories, and auto-playing videos on entry *and* as they start while the app is open — without touching the user's background music, which has a separate session.

**Configuration:**
- Target app — type a package name or browse installed apps via a searchable picker.

**Volume key shortcuts (while suppression is active):**
- **Volume Up** — temporarily pauses suppression; the current video resumes and audio plays freely.
- **Volume Down** — re-activates suppression; the next video is silenced within 300 ms.
- Both keys still change the system volume normally as a side effect.
- Suppression resets automatically the next time the target app comes back to the foreground.

---

### Rule 2 — Battery Saver

**Trigger:** Battery level drops at or below the configured threshold (5–80 %, slider).

**Actions (each independently enabled/disabled with inline sub-configuration):**

| Action | Sub-configuration |
|--------|------------------|
| Disable auto-sync | — |
| Set screen brightness to manual mode | — |
| Set screen brightness level | Slider 5–50 % of full brightness |
| Shorten screen timeout | Buttons: 30 s / 1 min / 2 min / 5 min |
| Enable Do Not Disturb | — |
| Lower media volume | Slider 0–50 % of max |

All actions save the current device state before applying changes and restore it when the battery recovers above the threshold.

---

### Rule 3 — DND on Media + Bluetooth

**Triggers (both must be active simultaneously):**
1. A selected media app has an active, playing `MediaSession`.
2. A selected Bluetooth audio device is connected via A2DP.

**Action:** Enables Do Not Disturb (`INTERRUPTION_FILTER_PRIORITY`), restoring the previous filter when either trigger deactivates.

**Configuration:**
- **Media apps** — multi-select checkbox picker from installed apps; defaults to YouTube Music.
- **Bluetooth devices** — multi-select checkbox picker from paired devices, with an "Any audio device" option that matches all connections.

**Initial state:** Both triggers check their state at service start, so DND activates immediately if music is already playing over Bluetooth when the service starts.

---

## Service Controls

### Start / Stop
Managed from the main screen. The service runs as a foreground service (`START_STICKY`) and shows a persistent notification.

### Restart / Apply Changes
A **Restart** button is shown when the service is running. It turns red and relabels to **Apply Changes** whenever a configuration value is modified while the service is running — a visual reminder that config changes (target app, BT devices, battery actions, etc.) require a restart to take effect, since the rule engine reads preferences at startup.

### Per-rule toggles
Each rule can be enabled or disabled independently, **without restarting the service**, from two places:
- **Notification action buttons** — `App Audio ●`, `Battery ●`, `BT DND ●` (● = on, ○ = off). Always accessible from the notification shade or lock screen.
- **In-app switches** — the toggle on each rule card in the main screen.

Both are in sync. The enabled state persists across service restarts.

---

## Permissions

| Permission | Why |
|-----------|-----|
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Run the background service |
| `POST_NOTIFICATIONS` | Show the persistent notification |
| `BIND_ACCESSIBILITY_SERVICE` | Detect foreground app changes and intercept volume keys |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Monitor `MediaSession` state across all apps |
| `BLUETOOTH_CONNECT` (runtime, API 31+) | Read paired and connected Bluetooth devices |
| `ACCESS_NOTIFICATION_POLICY` | Set / restore Do Not Disturb mode |
| `WRITE_SETTINGS` | Set screen brightness level and screen timeout |
| `PACKAGE_USAGE_STATS` | (Future use) |
| `QUERY_ALL_PACKAGES` | List all installed apps in the app picker on Android 11+ |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep service alive |

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 26 (Android 8.0) · **Target SDK:** 34
- **Persistence:** `SharedPreferences` via `AppPreferences` object
- **Concurrency:** Kotlin coroutines (`SupervisorJob` + `Dispatchers.IO`)
- **Logging:** Timber
- **Build:** Gradle (Kotlin DSL) + version catalog (`libs.versions.toml`)

---

## Project Structure

```
app/src/main/java/com/herchenmx/automationapp/
├── actions/
│   ├── DndModeAction.kt              Save/restore DND filter
│   ├── PauseMediaSessionAction.kt    One-shot MediaSession pause
│   ├── SetSystemSettingAction.kt     Save/restore Settings.System integers
│   ├── SetVolumeAction.kt            Save/restore AudioManager stream volume
│   ├── SuppressMediaSessionAction.kt Continuous polling suppressor + paused flag
│   └── SystemToggleAction.kt        Save/restore binary system features (auto-sync, brightness mode)
├── engine/
│   ├── Action.kt                    Interface: execute() / revert()
│   ├── Condition.kt                 Interface: isMet()
│   ├── Rule.kt                      Data class: triggers + conditions + actions + isEnabled
│   ├── RuleEngine.kt                AND-logic evaluator; setRuleEnabled() for live toggling
│   ├── StateManager.kt              Stack-based save/restore per key
│   └── Trigger.kt                   Interface: start(onStateChange) / stop()
├── rules/
│   └── HardcodedRules.kt            Builds the three rules from SharedPreferences
├── service/
│   ├── AutomationAccessibilityService.kt    Foreground app + volume key events
│   ├── AutomationForegroundService.kt       Main service, notification, toggle receiver
│   └── AutomationNotificationListenerService.kt  MediaSession monitoring
├── triggers/
│   ├── BatteryLevelTrigger.kt
│   ├── BluetoothAudioTrigger.kt     Supports Set<String> of target addresses
│   ├── ForegroundAppTrigger.kt
│   └── MediaSessionActiveTrigger.kt Supports Set<String> of target packages; initial state check
├── ui/
│   ├── PermissionScreen.kt
│   └── RuleListScreen.kt            Main config screen
└── util/
    ├── AppPreferences.kt            All SharedPreferences access; BtDevice data class
    └── PermissionChecker.kt
```
