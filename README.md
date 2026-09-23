# Our Home

Our Home is a small Android household task app originally built for two phones. It keeps cleaning and household work simple: schedule tasks, assign them, check them off, keep a friendly points scoreboard, and sync activity between household members.

The project is public because it was made for fun. If it is useful to you, feel free to fork it, change it, or repurpose it under the MIT license.

## Current milestone — 0.4.0

- Kotlin + Jetpack Compose Android app
- Premium room-organized Today screen
- Matt / Jessie / shared task assignments
- Normal, Important, and Urgent priorities
- One-off, daily, weekly, monthly, and custom every-X-days recurrence
- Gentle carry-over for unfinished tasks
- Weekly planning view
- Completion undo
- Durable local storage
- Completion history and points scoreboard
- Separate reward catalogues and custom rewards
- Reward wallet and coupon redemption
- Coupon-use approval from the other household phone
- Six-character household pairing
- Anonymous Firebase Authentication
- Cloud Firestore synchronization
- Small household completion/reward notifications while sync is active
- Signed GitHub release APKs

## Downloads

Signed builds are published on the repository's **Releases** page as:

`OurHome-vX.Y.Z.apk`

Each release also includes a SHA-256 checksum file.

## Firebase

Our Home is deliberately designed around Firebase's no-cost Spark plan.

The app uses Firebase Authentication and Cloud Firestore only. It does **not** require Cloud Functions. Firestore listeners can update the other phone quickly while its app process is alive; a fully closed receiving app does not get guaranteed server-triggered push under this design, but completion and reward-request checkpoints allow catch-up when it reconnects.

The Android Firebase client configuration in `app/google-services.json` is client configuration, not an admin/server credential. Security depends on the published Firestore rules. Signing keys, passwords, and service-account credentials must never be committed.

The Firebase project needs:

1. Anonymous Authentication enabled.
2. Cloud Firestore created.
3. The repository's `firestore.rules` published whenever the data model changes.

## Build

Requirements:

- JDK 17
- Android SDK 35
- Gradle 8.11+

Debug build:

```bash
gradle :app:assembleDebug
```

Release builds are produced by the GitHub Actions signed-release workflow. The signing keystore itself is stored outside the repository and restored only from GitHub Actions Secrets.

## Privacy

The app does not require email addresses, phone numbers, analytics, advertising, or a general user account. Household data is scoped by Firestore rules to authenticated members of the paired household.

## License

MIT. See [LICENSE](LICENSE).
