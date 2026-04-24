# Classification Reference

## Main buckets

Classify candidate items into:

- `feature`: new capability or meaningful expansion
- `improvement`: better UX, flow, discoverability, or content
- `fix`: resolved user-facing bug
- `performance`: speed, stability, battery, sync, or reliability gain
- `internal-only`: refactor, tooling, tests, migration, cleanup

---

## Rewrite rules

Translate engineering language into user-facing outcomes.

Examples:

| Raw input | Classification | Better wording |
|---|---|---|
| `add pull-to-refresh to orders list` | feature | Added pull-to-refresh on the orders list |
| `fix stale cache after logout` | fix | Fixed an issue where some data could remain outdated after logout |
| `reduce database contention during sync` | performance | Improved sync reliability and responsiveness |
| `migrate room entities` | internal-only | Exclude from public notes unless it fixed a visible issue |

---

## What belongs in production notes

Include:

- New features
- Improvements users can notice
- Fixes users may have encountered
- Stability or performance gains with clear value

Exclude:

- Framework migrations
- Build and CI changes
- Architecture cleanup
- Internal monitoring improvements with no user impact
- Purely preparatory work for future releases

---

## Deduplication rules

Multiple commits may map to one release note bullet.

Examples:

- `fix crash on startup`
- `add null guard for session token`
- `handle missing profile image`

These may collapse into:

- Fixed a startup issue affecting some users after login

Always summarize at the outcome level, not the commit level.
