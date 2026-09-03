# Our Home changelog

## 0.2.0 — two-phone test milestone
- Added durable local task and completion storage.
- Added one-off, daily, weekly, and monthly task recurrence.
- Organized the Today view by room with Matt, Jessie, and shared assignments.
- Added persistent completion history and a weekly/all-time Matt vs Jessie scoreboard.
- Added first-run Matt/Jessie device identity selection.
- Added six-character household create/join pairing.
- Added Firebase Anonymous Authentication and Cloud Firestore synchronization.
- Added production Firestore security rules restricting households to two authenticated phone identities.
- Added Android notification permission handling and the task-completion notification channel.
- Added free-tier completion catch-up so a missed completion can boop when the other phone reconnects after being fully closed.
- Kept all sync services compatible with Firebase Spark; no Blaze-only Cloud Functions are deployed.

## 0.1.0 — initial foundation
- Initial premium Our Home interface.
- Task creation, assignment, priority, and room organization.
- Premium warm forest/cream/mint/peach visual system.
- Progress dashboard, filters, animated task cards, and launcher icon.
