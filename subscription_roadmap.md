# Summ — Subscription Roadmap

## Goal

Define a monetization model that is:

- coherent with the product
- simple to explain
- household-based, not single-user
- compatible with Android + Google Play billing
- realistic to implement incrementally

The recommended model for Summ is:

- free app download
- one premium subscription per household
- one active subscription unlocks premium features for all household members

This is a better fit than:

- paid app upfront
- per-user subscription
- multiple plans at launch

---

## Current Status

### Already done

- Subscription strategy has been defined as household-based.
- Entitlement rules are documented around:
  - `1 household = 1 subscription`
  - any household member can buy
  - premium state is household-scoped, not user-scoped
- A backend foundation now exists in [functions](/Users/davideagostini/Documents/networth-app/functions).
- Monthly household email reports are already implemented server-side.
- Report delivery already supports premium gating through:
  - `subscriptionStatus == "active"`
  - or `monthlyReportTestMode == true` for local/manual testing
- Resend integration is already in place for email sending.
- Scheduled monthly report delivery is already implemented.
- Manual trigger for report testing is already implemented.
- The monthly report email template is already implemented and previewable.
- Monthly reports are now the strongest premium feature candidate already in progress.

### Still to do

- Define the actual premium UI in the Android app:
  - paywall
  - premium badges/status
  - gated feature states
- Add Firestore household subscription fields to the real data model if they are not already present everywhere.
- Implement Google Play Billing in the Android client.
- Create monthly and yearly subscription products in Play Console.
- Implement purchase restore flow.
- Build backend verification for Google Play purchases.
- Update Firestore entitlement state from verified purchases.
- Handle renewals, grace period, expiration, revocation, and duplicate protection.
- Reflect premium state live inside the Android app for all household members.
- Finish widgets.
- Decide whether quick tile is fully premium or bundled with premium quick access features.
- Actually gate the launch premium features in the app.

---

## Product Decision

### Recommended model

`1 household = 1 subscription`

Rules:

- Any authenticated member of a household can purchase the subscription.
- The first successful purchase activates premium for the whole household.
- Premium is evaluated at the household level, not the user level.
- All members of the same household see the same entitlement state.

### Why this model fits Summ

- The app data model is already household-scoped.
- The product is explicitly shared-finance oriented.
- A couple should not need two separate subscriptions for the same workspace.
- It keeps the value proposition simple:
  - "Unlock premium for your household."

---

## What Should Stay Free

The free tier must keep the app useful.

Do not put the core finance workflow behind the paywall.

Free features:

- Google sign-in
- create household / join household
- dashboard base metrics
- entries CRUD
- categories CRUD
- assets CRUD
- recurring transactions basic flow
- month close basic flow
- household members basic sharing

Reason:

- The app must remain valuable before asking for payment.
- Premium should feel like an upgrade, not a ransom.

---

## What Premium Should Unlock

Premium should focus on ongoing value, insights, convenience, and advanced tooling.

### Recommended premium features at launch

- monthly reports / summaries
- widgets
- quick tile / quick entry shortcuts

### Recommended premium features in more detail

#### 1. Widgets

Unlock:

- net worth widget
- monthly cash flow widget
- quick entry widget
- assets summary widget

Reason:

- strong ongoing value
- easy to communicate
- premium-feeling feature

Status:

- planned as a first-launch premium feature
- not implemented yet

#### 2. Import / Export

Unlock:

- CSV export
- JSON export
- CSV import
- backup / restore flow

Reason:

- extremely valuable for serious users
- common expectation in finance products

Status:

- post-launch premium candidate
- not needed for the first premium release

#### 3. AI Features

Unlock:

- automatic category suggestions
- unusual spending explanations
- monthly summary generation
- budget/risk insights
- natural language summaries

Reason:

- strong differentiation for Summ
- good fit for recurring value

Status:

- post-launch premium candidate

#### 4. Advanced Insights

Unlock:

- richer monthly comparisons
- trend explanations
- category anomaly cards beyond the basic unusual spending card
- deeper dashboard intelligence

Reason:

- fits the product direction
- keeps core dashboard free while premium deepens value

Status:

- post-launch premium candidate beyond the first launch bundle

#### 5. Monthly Reports

Unlock:

- auto-generated month report
- exportable summary
- trend snapshot
- household summary share flow

Status:

- email report backend is already implemented
- premium gating logic for reports is already implemented
- still missing: real subscription verification and app-side premium UX

#### 6. Quick Tile / Quick Access

Unlock:

- Quick Settings tile
- faster entry access from the system UI
- premium quick-access bundle together with widgets

Reason:

- strong Android-specific value
- feels premium without blocking the core app flow
- pairs naturally with widgets

Status:

- feature already exists in the app
- product decision is to consider it part of the first premium bundle

#### 7. Wear OS

Unlock:

- glanceable metrics
- quick entry from watch
- simple habit reminders

#### 8. Advanced Recurring / Reminder System

Unlock:

- automatic due reminders
- advanced recurrence handling
- recurring health checks
- month-close reminders

---

## What Should Not Be Premium Initially

Avoid paywalling these features at launch:

- sign in
- household creation
- basic entries
- basic assets
- dashboard basics
- categories
- shared household access

Reason:

- blocking these would make the free tier too weak
- onboarding would become much harder
- product trust would drop

---

## Recommended Plan Structure

Start with one plan only:

- `Summ Plus`

Do not launch with:

- monthly + yearly + family + pro + ultra
- multiple premium tiers
- lifetime unlock

Reason:

- one plan is simpler to explain
- easier to implement
- lower product complexity
- easier paywall messaging

### Suggested pricing structure

Start with:

- monthly plan
- yearly plan

Both unlock the same features.

Recommended messaging:

- monthly for easy trial
- yearly for better value

---

## Entitlement Rules

### Household entitlement

A household is premium when:

- there is an active verified subscription linked to that household

Suggested household fields:

```text
households/{householdId}
  subscriptionStatus
  subscriptionPlan
  subscriptionPlatform
  subscriptionPurchasedByUid
  subscriptionStartedAt
  subscriptionExpiryAt
  subscriptionLastVerifiedAt
```

Suggested values:

- `subscriptionStatus`: `inactive | active | grace_period | expired | revoked`
- `subscriptionPlan`: `summ_plus_monthly | summ_plus_yearly`
- `subscriptionPlatform`: `google_play`

### User entitlement

Do not make premium user-based.

A user is treated as premium only because:

- the user's current household is premium

This avoids:

- entitlement mismatches
- confusion in shared households
- duplicate purchases per couple

---

## Who Can Purchase

Recommended rule:

- both `owner` and `member` can purchase

Reason:

- real households do not always have the owner as payer
- blocking members would create friction
- the first successful purchase should unlock the entire household

### Rule summary

- Any member can buy.
- The first successful verified purchase activates premium for the household.
- If premium is already active, the paywall should not ask for another purchase.

---

## Edge Cases and Exact Rules

### 1. Household already premium

If premium is already active:

- hide purchase CTA
- show status instead:
  - `Premium is active for this household`

If needed, show:

- plan name
- expiry/renewal date

### 2. Two members try to purchase

If two members try to buy around the same time:

- backend verification must enforce one household entitlement state
- the paywall should re-check household premium state before final purchase confirmation

Expected app behavior:

- once household becomes premium, other members should see premium unlocked after sync

### 3. Member who purchased leaves the household

Recommended rule:

- the active subscription remains linked to the household until it expires or is canceled

Reason:

- simplest model
- avoids punitive behavior
- avoids weird entitlement flips

### 4. User changes household

If a user leaves household A and joins household B:

- entitlement follows the household, not the user
- user loses premium if household B is not premium
- user keeps premium only when joining another premium household

### 5. Restore purchase

Restore is not just "premium for the user".

Restore must:

- re-verify the purchase on backend
- identify the current household relationship
- reapply entitlement to the correct household

### 6. Household deleted

If household is deleted:

- subscription linkage must be handled carefully
- do not silently reassign premium to a different household

Recommended initial rule:

- prevent household deletion while subscription is active
- or require explicit support/admin migration logic later

### 7. Multiple active subscriptions accidentally created

Do not rely on client-side prevention only.

Backend should:

- verify purchase tokens
- reject duplicates
- keep one source of truth for entitlement

---

## Technical Architecture

## Recommended implementation approach

Do not ship premium logic as client-only state.

Use:

- Google Play Billing on Android
- backend verification
- Firestore entitlement state

### Source of truth

The source of truth should be backend-verified subscription state.

Do not trust:

- local flags
- cached purchase success only
- UI-only state

### Suggested architecture

#### Android client

Responsibilities:

- show paywall
- launch billing flow
- receive purchase token
- send purchase token to backend
- listen to household premium status in Firestore

#### Backend

Recommended:

- Firebase Cloud Functions

Responsibilities:

- verify Play purchase/subscription
- map purchase to household
- update Firestore entitlement
- handle renewals / expiration / revocation

#### Firestore

Responsibilities:

- store current household entitlement state
- expose premium status to all members

---

## Suggested Firestore Model

### Household-level fields

```text
households/{householdId}
  subscriptionStatus: "inactive" | "active" | "grace_period" | "expired" | "revoked"
  subscriptionPlan: "summ_plus_monthly" | "summ_plus_yearly" | null
  subscriptionPlatform: "google_play" | null
  subscriptionPurchasedByUid: string | null
  subscriptionStartedAt: timestamp | null
  subscriptionExpiryAt: timestamp | null
  subscriptionLastVerifiedAt: timestamp | null
```

### Optional audit document

```text
households/{householdId}/subscriptionEvents/{eventId}
```

Useful for:

- renewal tracking
- restore operations
- debugging entitlement issues

### Temporary testing mode already used

For monthly reports, the backend already supports:

```text
households/{householdId}
  monthlyReportEnabled: true
  monthlyReportTestMode: true
```

This is intentionally a testing bypass so premium-only report flows can be developed before Billing is implemented.

---

## Paywall UX Rules

### Messaging

Use household-based messaging.

Recommended:

- `Unlock Summ Plus for your household`
- `One subscription unlocks premium features for everyone in this household`

Avoid:

- user-centric wording that implies per-user premium

### If member opens paywall

Allowed behavior:

- member can subscribe
- copy should still explain household effect

### If household is already premium

Show:

- premium active state
- no purchase CTA

### If user is signed out

Do not show subscription flow.

Require:

- sign in
- household membership

before purchase.

---

## Rollout Roadmap

### Phase 1 — Product definition

Deliverables:

- premium feature list finalized
- free vs premium scope finalized
- paywall messaging draft
- household entitlement rules finalized

### Phase 2 — App gating without billing

Deliverables:

- premium feature flag in app
- Firestore household premium state
- gated premium features in UI
- internal test using fake premium values

Reason:

- validate UX before integrating Billing

Status:

- partially done on backend for monthly reports via `monthlyReportTestMode`
- still not implemented in the Android app UI

### Phase 3 — Play Billing integration

Deliverables:

- Billing client integration
- monthly and yearly products in Play Console
- purchase flow
- restore flow on client

### Phase 4 — Backend verification

Deliverables:

- Cloud Function to verify purchase
- Firestore entitlement updater
- purchase token handling
- duplicate protection

Status:

- not started yet
- current functions backend is ready for premium-related server logic, but it does not verify Play purchases yet

### Phase 5 — Entitlement sync

Deliverables:

- household premium reflected across all members
- premium status refresh on app open
- proper handling of grace period / expired / canceled

### Phase 6 — Launch

Deliverables:

- production paywall
- Play Console subscription config
- support/help copy
- analytics around conversion and usage

---

## Recommended First Premium Launch

If launching soon, the simplest realistic first premium package would be:

- monthly reports
- widgets
- quick tile / quick access

This set is:

- understandable
- premium-feeling
- not core-blocking
- aligned with the long-term vision

Practical status today:

- `monthly reports` is the most advanced premium candidate because the backend already exists
- `widgets` is the main missing feature before the first premium launch makes sense
- `quick tile` already exists and can be moved into the premium bundle
- `import/export` and `AI insights` still need product and implementation work and should stay post-launch

---

## Recommendations Summary

- Use `free + household subscription`
- Do not use paid-app upfront
- Do not use per-user premium
- Allow both owner and member to purchase
- First valid purchase unlocks the full household
- Keep the source of truth server-verified
- Start with one premium plan family: `Summ Plus`
- Launch premium only after defining a strong premium bundle

---

## Open Questions

- Which feature should be the first actually gated premium feature inside the Android app?
- Should premium unlock immediately after purchase on the same device before backend confirmation, or only after backend confirmation?
- Should a household with active premium be deletable?
- Should yearly be the primary default plan in the paywall?
- Is `quick tile` fully premium, or should there be a limited free version?
- Should AI be included in the base premium plan or become a later add-on?
