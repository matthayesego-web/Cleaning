# Our Home

A private two-person Android cleaning app for Matt and Jessie. The goal is intentionally small: see what needs doing, assign it, check it off, keep a friendly scoreboard, and keep both phones in sync.

## Current development milestone — 0.2.0

- Native Android app written in Kotlin and Jetpack Compose
- Premium room-organized Today screen with Matt / Jessie / Either assignments
- Normal, Important, and Urgent priorities
- One-off, daily, weekly, and monthly tasks
- Durable local task and completion storage for offline use
- Completion history plus weekly and all-time Matt vs Jessie scoreboard
- First-run two-phone pairing with a six-character household code
- Anonymous Firebase Authentication for device identity
- Cloud Firestore synchronization for tasks and completion history
- Android task-complete notification channel (the small household "boop")
- Notification catch-up checkpoint so completions made while a phone was fully closed can be surfaced when that phone reconnects
- Firestore rules that restrict each household to two authenticated phone identities

## Firebase plan

Our Home is deliberately designed to remain on Firebase's no-cost Spark plan.

The app uses Firebase Authentication and Cloud Firestore only. It does **not** deploy Cloud Functions. That means Firestore listeners can update the other phone very quickly while its app process is alive, but a guaranteed server-triggered push while the receiving phone is fully shut down is not part of the Spark-only design. If the app was fully closed, the completion checkpoint lets it catch up and notify when it next reconnects.

The Android Firebase client configuration is included in this private repository so CI and test builds can compile. It contains Firebase client configuration, not a server/admin credential. Signing keys and service-account credentials must never be committed.

## Firebase setup

The Firebase project needs:

1. Anonymous Authentication enabled.
2. Cloud Firestore created in Production mode.
3. The repository's `firestore.rules` published to Firestore before pairing is tested.

The project configuration points at Firebase project `our-home-89f81` and Android application ID `ca.northstarappworks.cleaning`.

## Build

The project requires JDK 17, Android SDK 35, and Gradle 8.11+.

```bash
gradle :app:assembleDebug
```

GitHub Actions is intentionally configured to build `main`, while active development happens on the `development` branch so routine coding does not consume Actions minutes. Changes are moved to `main` only when a meaningful test build is ready.

## Privacy

Household task data is scoped to authenticated members of the paired household. The app does not require names, email addresses, phone numbers, analytics, advertising, or a general user account. Local task data remains available when the phone is offline.
