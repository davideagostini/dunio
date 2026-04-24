# Release Tracks Reference

## Internal

Audience:

- QA
- Product
- Engineering
- Stakeholders who need operational detail

Style:

- Direct
- Explicit
- Can mention modules, flows, or known limitations
- Can include validation cues and rollout context

Good internal bullets:

- Added pull-to-refresh on the orders list and fixed stale cache after logout
- Fixed startup crash caused by missing session state on cold launch
- Migrated sync retry flow and need focused QA on poor-network scenarios

---

## Beta

Audience:

- Early testers
- Power users
- Internal dogfooding groups

Style:

- User-facing
- Slightly more detailed than production
- Can mention areas where feedback is useful

Good beta bullets:

- Added pull-to-refresh on the orders list for quicker manual updates
- Improved app stability during login and sync
- Refined onboarding and would like feedback on the new first-run flow

---

## Production

Audience:

- General users

Style:

- Short
- Polished
- User-facing only
- No rollout instructions or engineering detail

Good production bullets:

- Improved login reliability
- Faster and more stable syncing
- Refined onboarding experience

Avoid:

- Ticket IDs
- Commit-level details
- Internal module names
- “Refactor”, “migration”, “cleanup”, “tech debt”

---

## Suggested output shapes

### Internal

```text
Internal
- Added pull-to-refresh on the orders list
- Fixed stale cache after logout
- Improved sync retry handling
- Focus QA on login recovery and poor-network scenarios
```

### Beta

```text
Beta
- Added pull-to-refresh on the orders list
- Improved login and sync reliability
- Refined onboarding flow and welcome feedback
```

### Production

```text
Production
- Improved login reliability
- Faster, more stable syncing
- Refined onboarding experience
```
