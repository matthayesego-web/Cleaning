# Our Home changelog

## 0.3.0 — calm daily planning
- Today now shows only tasks scheduled for the current day instead of carrying every overdue item forward.
- Added an automatic midnight date rollover so the Today screen refreshes itself for the new day.
- Added a Weekly control panel with Monday–Sunday navigation, previous/next week controls, and quick task creation for a selected day.
- Added room-grouped task organization to both Today and Week views.
- Added day selection when creating or editing a task, including weekly schedule anchoring.
- Made the top notification bell open recent household activity/history.
- Made each task's three-dot menu functional with Edit and Delete actions plus delete confirmation.
- Added persistent local and Firestore task deletion.
- Updated Household status copy to reflect the working Spark/free-tier sync setup.

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
