# Our Home changelog

## 0.3.2 — flexible repeats & completion undo
- Added custom day-based recurrence so a task can repeat every 2–365 days instead of only daily, weekly or monthly.
- Custom repeat schedules stay anchored to the selected start day and advance to the next future occurrence after completion.
- Added Undo beside tasks in Finished today so accidental completions can be reversed immediately.
- Added Undo for completed tasks in the Week view.
- Completion records now preserve the scheduled occurrence date so undoing a recurring task restores the correct occurrence instead of disturbing its future cadence.
- Custom recurrence and undo metadata sync through Firestore and persist locally on both phones.

## 0.3.1 — premium polish & gentle carry-over
- Refined the full visual system with deeper forest tones, softer premium surfaces, sharper typography, hairline borders and more intentional elevation.
- Reworked Today, Week, Activity and Household screens for a cleaner, more polished hierarchy.
- Added room-specific visual accents and icons while preserving room-grouped task organization.
- Added a richer Today progress card and more polished task, history and navigation treatments.
- Added gentle task carry-over: unfinished work from an earlier scheduled date can surface on Today with a clear “Carried over” treatment instead of silently disappearing.
- Weekly planning still preserves the original schedule so Today can be actionable without rewriting the weekly plan.
- Past unfinished tasks are visually identified in the Week view as carried/missed work.
- Household now surfaces connection status and the pairing code more cleanly.
- Documented the future family-account architecture: adult accounts can manage the full household; child accounts will see only directly assigned tasks, can complete them, will not receive household notifications, and can still send completion activity to adult devices.

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
