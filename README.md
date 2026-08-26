# MediReminder

A Kotlin Multiplatform medication reminder app — Android, iOS and Desktop from one
shared Compose Multiplatform codebase.

Package: `me.sandbad.medireminder` · Android applicationId: `me.sandbad.medireminder`

## What it does

- **Medications** — name, form, strength, colour, instructions, stock level.
- **Schedules** — every day, specific weekdays, every N days, or as-needed; one or more times per day.
- **Today** — the day's dose list with Take / Snooze / Skip, and per-day navigation.
- **Reminders** — a platform notification per pending dose, with Take and Snooze actions on Android.
- **History** — adherence rate, streak, per-day bars, and the full dose log over 7/30/90 days.
- **Refill alerts** — stock is decremented as doses are taken and flagged when it drops below a threshold.

Everything is stored locally in SQLite; there is no account and no network layer.

## Module layout

| Module | Contents |
| --- | --- |
| `shared` | Domain models, SQLDelight schema, repositories, services, all Compose UI, DI |
| `androidApp` | `Application` + `MainActivity`, manifest, notification permissions |
| `desktop` | Compose Desktop window entry point |
| `iosApp` | SwiftUI shell hosting `MainViewController()` from `shared` |

Inside `shared`:

```
core/model        domain types (Medication, Schedule, DoseLog, AppSettings)
core/adapter      SQLDelight column adapters
core/database     driver factory (expect/actual) + adapter wiring
core/repository   interfaces + SQLDelight implementations
core/service      DoseScheduler (pure calendar maths), MedicationService, AdherenceService
core/reminder     ReminderScheduler interface; Android/iOS/Desktop implementations
ui                theme, components, screens, ViewModels, Voyager navigation
di                Koin graph (platform modules add the driver + scheduler bindings)
```

## Key design points

- `DoseScheduler` is pure and has no dependencies, so schedule rules are unit tested
  directly (`shared/src/commonTest`).
- `MedicationService.syncUpcoming()` is the single entry point that materialises dose
  rows for the next 7 days, closes out missed doses, and re-arms platform reminders.
  It is called on app start, after every edit, and after a device reboot on Android.
- `ReminderScheduler` implementations must be idempotent — `scheduleAll` receives the
  complete set of upcoming doses and replaces whatever was pending.
- Boolean/Int/date columns are stored as INTEGER/TEXT with adapters in
  `core/adapter`; `AppSettings` uses `INSERT OR REPLACE` because upsert syntax needs
  SQLite 3.24, above the app's minSdk 26 baseline.

## Building

```bash
./gradlew :desktop:run                    # desktop app
./gradlew :androidApp:installDebug        # Android
./gradlew :shared:desktopTest             # unit tests
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode after a Gradle sync.
