# Our Home — Family account architecture

This is the expansion plan for moving beyond the original two-adult Matt/Jessie household without making the everyday app more complicated.

## Product rules

### Adult account
- Can see the full household schedule and all rooms.
- Can create, edit, reschedule, delete and assign tasks.
- Can see household history and scoreboards.
- Receives completion activity from other household members when notifications are enabled.
- Can invite/manage additional household members.

### Child account
- Sees only tasks assigned directly to that child account.
- Can mark those assigned tasks complete.
- Cannot browse the full adult household schedule, history or management controls.
- Does not receive household completion notifications.
- Completing a task creates normal household completion activity so adult devices can receive the boop.

## Data model direction

Do not add hard-coded CHILD values to the existing `Assignee` enum. Before family accounts are enabled, migrate assignments to stable household-member IDs.

Planned member fields:
- `memberId` / Firebase auth UID
- `displayName`
- `accountType`: `ADULT` or `CHILD`
- `canManageHousehold`
- `receivesNotifications`
- `joinedAt`

Planned task assignment fields:
- `assigneeMemberId` for a direct assignment
- an explicit shared/adult-household assignment mode where needed

Completion records should preserve both the completing member ID and display name at completion time so history remains understandable if a member is renamed later.

## Firestore/security direction

The production rules currently intentionally limit a household to two authenticated phones. Family accounts will require a deliberate rules migration rather than silently weakening that protection.

When family accounts are enabled:
- Adult members may query/read the household task collection.
- Child clients must query only tasks assigned to their authenticated member ID.
- Firestore rules must independently enforce that a child can read only directly assigned tasks.
- Child writes should be limited to completion operations for tasks assigned to that child; schedule/assignment edits remain adult-only.
- Pairing/inviting a child should be initiated by an adult so a child cannot self-enrol as an adult through the normal household code.

## Notification direction

- Adult devices suppress notifications for their own completions and may receive completions from the other adult or child accounts.
- Child devices suppress completion notifications entirely.
- The existing Spark/free-tier listener/catch-up notification design remains compatible with this model; Blaze is not required for the core family-account design.
