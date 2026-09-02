# Our Home

A private two-person Android cleaning app for Matt and Jessie. The project is being built around a shared household, quick task entry, assignments, priorities, completion history, recurring chores, offline support, and real-time synchronization.

## Current milestone — 0.1.0 foundation

- Native Android app written in Kotlin and Jetpack Compose
- Friendly Today screen with Matt/Jessie/Either assignments
- Premium warm visual system, progress hero, filters, polished task cards, and branded launcher icon
- Normal, Important, and Urgent priorities
- Quick task creation and undoable completion
- Repository abstraction ready for the Firestore implementation
- Firebase configuration deliberately excluded from source control

The current in-memory repository is a development fixture. Shared household pairing, durable local storage, Firestore synchronization, notifications, recurring schedules, and history are the next implementation stages.

## Build

The project requires JDK 17, Android SDK 35, and Gradle 8.11+.

```bash
gradle :app:assembleDebug
```

## Privacy

Household data will only be available to authenticated members of the paired household. Secrets, signing keys, and Firebase configuration files must never be committed.
