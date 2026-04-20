# Wear OS Quick Entry Architecture

This document describes the first Wear OS integration for Dunio and focuses on the `quick entry`
flow that lets a user add a lightweight income or expense from the watch.

---

## Goal

The Wear OS experience is intentionally narrow in V1.

Supported flow:

1. choose `expense` or `income`
2. enter amount
3. choose category
4. confirm

Not included in V1:

- description editing
- date editing
- direct Firebase access from the watch
- standalone authentication on the watch
- voice input

---

## Product decision

The watch app is implemented as a **non-standalone Wear OS app**.

That means:

- the watch UI can run on the watch
- the watch depends on the paired phone app for core functionality
- the final write to Firestore happens on the phone

Why this was chosen:

- it avoids duplicating Firebase auth and household resolution on the watch
- it reuses the existing phone-side repositories and business rules
- it keeps the first release small, understandable, and cheap to maintain

---

## Module layout

The feature spans two places:

```text
mobile-app/
├── app/   # phone app
└── wear/  # dedicated Wear OS app
```

### Phone-side files

```text
app/src/main/java/com/davideagostini/summ/wear/
├── WearQuickEntryProtocol.kt
└── WearQuickEntryListenerService.kt
```

### Watch-side files

```text
wear/src/main/java/com/davideagostini/summ/wearapp/
├── data/
│   ├── WearQuickEntryLocalStore.kt
│   └── WearQuickEntryRepository.kt
├── model/
│   └── WearQuickEntryModels.kt
├── navigation/
│   └── WearQuickEntryNavigation.kt
├── presentation/
│   ├── WearQuickEntryActivity.kt
│   └── WearQuickEntryViewModel.kt
├── protocol/
│   └── WearQuickEntryProtocol.kt
├── theme/
│   └── WearQuickEntryTheme.kt
└── ui/
    ├── WearQuickEntryComponents.kt
    └── steps/
        ├── AmountStep.kt
        ├── CategoryStep.kt
        ├── ConfirmStep.kt
        ├── SuccessStep.kt
        └── TypeStep.kt
```

---

## Communication model

The watch and phone communicate through the **Wear Data Layer** in two modes:

- `MessageClient.sendRequest(...)` for immediate request/response operations
- `DataClient` `DataItem`s for offline-safe pending entry synchronization

### RPC paths

Defined in both modules:

- `/wear/quick-entry/categories`
- `/wear/quick-entry/save`
- `/wear/quick-entry/pending/{requestId}`

### Capability

The phone advertises:

- `summ_phone_app`

The watch uses that capability to find the best reachable phone node before sending requests.

---

## Why RPC instead of direct Firebase on watch

The watch does **not** write to Firestore directly.

Instead:

1. watch collects the small payload
2. watch sends payload to phone
3. phone validates household/session context
4. phone writes through existing repositories
5. phone refreshes downstream consumers such as widgets

This keeps the source of truth concentrated on the phone app in V1.

---

## Category loading

Category loading happens on the phone because the phone already knows:

- the authenticated Firebase session
- the active household
- the current category list
- the local category-usage ranking

When the watch asks for categories:

1. the phone reads all categories for the requested type
2. the phone computes the ordered list:
   - most used first
   - then all remaining categories
3. the phone returns:
   - household currency
   - ordered categories

The watch caches that response locally per type so the user can still complete a quick entry if the
phone becomes temporarily unavailable after categories were loaded at least once.

---

## Save flow

### Online path

If the phone is reachable:

1. watch sends `type + amount + category`
2. phone saves an `Entry` through `EntryRepository`
3. phone updates category usage best-effort
4. watch shows success

### Offline / temporarily unavailable path

If the phone is not reachable:

1. the watch persists the payload as a pending `DataItem`
2. the watch shows success as `queued`
3. the Wear Data Layer synchronizes that item automatically when the devices reconnect
4. the phone consumes the item and deletes it only after saving the entry successfully

---

## Local persistence on watch

The watch uses two small persistence layers:

- `SharedPreferences` for cached categories
- Wear Data Layer `DataItem`s for queued pending entries

Stored data:

- cached categories by type
- queued pending quick entries as `DataItem`s under `/wear/quick-entry/pending/...`

The queue payload stores only:

- type
- amount
- category name
- category emoji
- category key

This is intentionally minimal.

---

## Automatic retry model

Offline retry no longer depends on a watch-side reconnect service.

Instead:

1. the watch writes a pending `DataItem`
2. the Wear Data Layer keeps that item while devices are disconnected
3. the platform synchronizes it automatically when a connection becomes available
4. the phone-side listener saves the entry and deletes the item

This removes the need for deprecated background reconnect listeners on the watch.

---

## Failure semantics

There are two classes of failure in the watch repository:

### `PhoneUnavailableException`

Transport failure.

Examples:

- phone out of range
- no reachable node
- request transport failure

Behavior:

- save is queued in the Wear Data Layer
- categories fall back to cached copy if available

### `PhoneRejectedException`

Logical rejection returned by the phone app itself.

Examples:

- invalid session
- invalid payload
- phone-side validation failure

Behavior:

- request is **not** silently queued
- UI shows an error message

---

## Phone-side responsibilities

`WearQuickEntryListenerService` is the bridge from Data Layer RPC to the existing app stack.

It is responsible for:

- decoding watch requests
- processing pending quick-entry `DataItem`s
- resolving session / household state
- loading ordered category data
- saving entries through `EntryRepository`
- updating category usage best-effort

This service intentionally does not create a second business-logic path. It delegates into the same
repositories already used by the phone UI.

---

## Watch-side responsibilities

### `presentation/WearQuickEntryActivity`

- renders the wrist-first flow
- applies the Wear theme
- wires the shared `WearQuickEntryViewModel`
- passes state plus the shared `onAction(...)` entry point into the route host

### `presentation/WearQuickEntryViewModel`

- owns the state machine
- validates amount input
- coordinates category loading
- handles saved vs queued outcomes
- exposes a dedicated `pendingCount` flow for the first screen badge
- receives UI intent through a single `WearQuickEntryAction` channel instead of many callback
  methods

### `navigation/WearQuickEntryNavigation`

- owns the `SwipeDismissableNavHost`
- keeps routes separate for `type`, `amount`, `category`, `all_categories`, `confirm`, and `success`
- lets the first route collect queue badge state directly so the badge updates even while the
  screen stays mounted
- forwards a single UI action handler into the step composables

### `data/WearQuickEntryRepository`

- speaks to the phone over Data Layer RPC
- manages cached categories
- queues pending entries as `DataItem`s when the phone is unavailable
- exposes the pending queue count by reading the current pending `DataItem`s

### `data/WearQuickEntryLocalStore`

- persists cached category payloads by type
- does not own the pending-entry queue anymore

### `ui/`

- `WearQuickEntryComponents.kt` contains reusable buttons, chips, amount input, and summary UI
- `ui/steps/TypeStep.kt` contains the first screen
- `ui/steps/AmountStep.kt` contains the amount screen
- `ui/steps/CategoryStep.kt` contains both quick categories and all categories variants
- `ui/steps/ConfirmStep.kt` contains the confirm screen
- `ui/steps/SuccessStep.kt` contains the success screen

### `theme/`

- `WearQuickEntryTheme.kt` centralizes dynamic-color fallback and Wear-specific surface tokens used
  across the flow

---

## Queue badge update path

The queue badge on the first screen is intentionally handled as a dedicated state path.

Current update chain:

1. the watch creates or receives a pending-entry `DataItem` change
2. `WearQuickEntryRepository.observePendingCount()` reacts through a `DataClient` listener
3. `WearQuickEntryViewModel` updates `pendingCount`
4. the `type` route in `WearQuickEntryNavigation` collects that flow directly
5. `TypeStep` shows or hides the badge

This is more explicit than relying only on broad screen-level recomposition and is important
because the `type` route stays mounted inside the Wear navigation host while the phone may consume
pending items in background.

---

## Current limitations

The current Wear version is intentionally conservative.

Known limitations:

- not standalone
- no voice input
- no custom description
- no custom date
- no edit/delete on watch
- queued items still assume the phone can process the payload when it receives the `DataItem`
  event; if the phone app is in an invalid session state, the item remains pending until a future
  processing pass handles it

---

## Recommended next steps

If this V1 performs well, the best follow-ups are:

1. recent categories / favorite categories shortcuts
2. voice amount or voice quick entry
3. better watch-specific visual polish
4. optional quick templates such as groceries, fuel, coffee
5. add a dedicated phone-side recovery pass for stale pending `DataItem`s when the handheld app
   returns to foreground after a failed processing attempt

Not recommended yet:

- full standalone Firebase watch app
- AI on the watch
- large multi-screen finance dashboards on Wear OS
