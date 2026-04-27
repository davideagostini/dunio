# Mobile Offline Roadmap

This document tracks future offline-first improvements for the Android phone app.

It is intentionally a roadmap only: none of the items below are required for the
current production behavior, and they should be implemented incrementally to avoid
regressions in the shared household model.

## Current baseline

- Cloud Firestore is already used through `FirebaseFirestore.getInstance()`
- on Android, Firestore offline persistence is enabled by default
- cached data is therefore already available for previously loaded documents and queries
- the main opportunity is not "turning offline on", but improving how the UI uses the
  cached data that already exists

## Recurring transaction maintenance

- recurring templates are applied immediately after a save when the due date is already in the past
- the app also schedules a daily WorkManager check to apply any remaining due recurring transactions in the background
- this keeps the behavior reliable without depending only on app startup or a manual `Apply` tap

## Guiding principles

- prioritize offline-friendly behavior for high-frequency daily actions
- keep household membership and permission-sensitive flows conservative
- never leave the user in an infinite loading state when offline
- prefer explicit states such as `cached`, `refreshing`, `queued`, and `failed`
- keep phone and Wear quick-entry semantics aligned where possible

## Offline-safe candidates

These areas are good candidates for stronger offline-first behavior:

1. entries list
2. quick entry on phone
3. assets list
4. dashboard cached reads
5. categories cached reads

## Online-only or conservative flows

These flows should remain online-only or require stronger confirmation:

1. household creation and household join
2. invite acceptance and member management
3. role and permission changes
4. session-critical flows when household membership is uncertain
5. month-close flows or other household-wide state changes

## Roadmap

### 1. Entries list: exploit cache better

Goal:
- reduce perceived loading time on the entries screen, especially after the first visit

Why:
- Firestore already caches entry documents locally
- the screen should show cached content as early as possible instead of waiting on
  secondary sources

Potential follow-ups:
- keep full-screen loading limited to the first real entries response
- continue using the lighter month refresh indicator for month changes
- evaluate showing cached results immediately even when category or month-close data
  is still catching up
- optionally expose `fromCache` metadata for future polish

### 2. Quick Entry phone: eliminate infinite offline loading

Goal:
- ensure quick entry never gets stuck in a permanent loading state when the device is offline

Why:
- loading forever gives the user no clear answer about whether the feature is available

Potential follow-ups:
- if categories are not available locally, replace infinite loading with an explicit
  offline-unavailable state
- show a retry action and a close action instead of a blocking spinner
- only show the full-screen loading state while waiting for the first meaningful
  categories result

### 3. Quick Entry phone: cached categories fallback

Goal:
- allow the user to keep using quick entry offline if categories have already been loaded before

Why:
- this mirrors the practical value already achieved on Wear with cached category sets

Potential follow-ups:
- keep previously loaded categories available offline
- allow the step flow to proceed using cached categories
- avoid re-blocking the entire flow when the network is unavailable but cached data exists

### 4. Quick Entry phone: queued offline save

Goal:
- align phone quick entry behavior with Wear semantics when connectivity is missing

Why:
- today the desired product behavior is clearer if the app can say `saved offline` or
  `queued`, instead of leaving the user in a vague saving state

Potential follow-ups:
- treat an offline quick-entry save as `queued`
- surface an immediate success-style confirmation to the user
- synchronize automatically when connectivity returns
- use consistent wording with Wear where possible

Risks to manage:
- duplicate saves if retries are not idempotent
- unclear UX if queued / synced states are not explicit

### 5. Firestore metadata-aware UI

Goal:
- make cached and pending states explicit where it improves trust

Why:
- shared household data can be slightly stale when read from cache
- explicit metadata helps explain what the user is seeing

Potential follow-ups:
- inspect `SnapshotMetadata.isFromCache`
- inspect `SnapshotMetadata.hasPendingWrites`
- optionally show subtle `syncing` or `offline` indicators only where valuable

### 6. Local offline query performance

Goal:
- keep cached queries fast as the user accumulates more local Firestore data

Why:
- Firestore can cache data locally, but offline query performance can still degrade on
  larger local datasets

Potential follow-ups:
- evaluate local Firestore query indexes if month-based cached queries become slower
- measure before adding complexity

## Key risks

The shared household model makes offline behavior more sensitive than in a single-user app.

Important risks:

1. stale cached data while another household member changes the same records
2. last-write-wins conflicts on the same Firestore document
3. session or membership changes while a device is offline
4. delayed write failures caused by security rules or permission changes
5. duplicate sync behavior if quick-entry queue semantics are not carefully designed

## Recommended implementation order

1. improve entries-screen cache usage
2. remove infinite offline loading from phone quick entry
3. allow phone quick entry to use cached categories offline
4. decide whether phone quick entry should support explicit queued offline saves
5. only then add metadata-driven polish such as `fromCache` / `hasPendingWrites`

## Out of scope for now

- rewriting the app around Room as the primary source of truth
- turning every phone feature into a full offline-first sync system
- changing household membership flows to work offline
- changing Wear behavior as part of the phone roadmap unless explicitly planned
