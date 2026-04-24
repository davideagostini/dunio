# android-compose-review

A skill for reviewing and incrementally improving an existing Android Jetpack Compose codebase.

It is designed for real projects that already exist, where the goal is to audit, modernize, simplify, or safely improve Compose code without turning the task into a greenfield rewrite.

---

## What it covers

The skill ships with 15 reference files:

| Reference                  | Topics |
| -------------------------- | ------ |
| `review-scope.md`          | How to choose the smallest correct scope in an existing codebase |
| `modernization-checklist.md` | How to prioritize incremental improvements and safe cleanups |
| `validation.md`            | How to choose the smallest relevant validation after edits |
| `architecture.md`          | Feature-first MVVM, state ownership, navigation, and dependency structure |
| `principles.md`            | Core architectural guardrails and implementation rules |
| `state-management.md`      | Compose state, hoisting, derived values, and recomposition triggers |
| `side-effects.md`          | Lifecycle-aware effects, coroutines, listeners, and cleanup |
| `view-composition.md`      | Composable structure, slots, and stateless/stateful boundaries |
| `composition-locals.md`    | Custom locals and implicit dependency concerns |
| `lists-and-scrolling.md`   | Lazy lists, grids, keys, paging, and scroll handling |
| `performance.md`           | Recomposition, allocations, and hot-path performance review |
| `modifiers.md`             | Modifier ordering, custom modifiers, and rendering behavior |
| `accessibility.md`         | Semantics, touch targets, and accessibility review points |
| `deprecated-migrations.md` | Outdated patterns and modern Compose migrations |
| `common-bugs.md`           | Recurring Compose and ViewModel mistakes worth checking quickly |

---

## Typical use cases

- Review a Compose screen or module in an existing app
- Modernize outdated Compose patterns without a full rewrite
- Audit state management, side effects, recomposition, and lifecycle issues
- Clean up a recent diff in a Compose-heavy code path

---

## How it works

The skill follows a 6-step workflow:

1. **Determine scope and baseline** — identify the smallest correct review target
2. **Review architecture and state ownership** — check boundaries and responsibility
3. **Review the relevant Compose concern** — load the reference that matches the touched area
4. **Review modernization opportunities** — prioritize deprecations, bug risks, and high-value cleanup
5. **Apply only safe fixes when requested** — keep edits incremental and behavior-preserving
6. **Validate when required** — run the smallest relevant check

The agent uses the same Compose references as the starter skill, but reorients them toward audit and improvement of existing code rather than project setup from scratch.

---

## Expected structure

```text
android-compose-review/
├── SKILL.md
├── README.md
└── references/
    ├── accessibility.md
    ├── architecture.md
    ├── common-bugs.md
    ├── composition-locals.md
    ├── deprecated-migrations.md
    ├── lists-and-scrolling.md
    ├── modernization-checklist.md
    ├── modifiers.md
    ├── performance.md
    ├── principles.md
    ├── review-scope.md
    ├── side-effects.md
    ├── state-management.md
    ├── validation.md
    └── view-composition.md
```

---

## Example prompts

```text
/android-compose-review review the current diff in the app module
```

```text
/android-compose-review modernize this screen without changing behavior
```

```text
/android-compose-review audit state management and side effects in feature/profile
```

---

## Authoring rules

- Prefer incremental improvement over greenfield replacement
- Use the most relevant Compose reference before reviewing a concern
- Prioritize lifecycle, state, and performance correctness over cosmetic cleanup
- Apply only high-confidence, behavior-preserving fixes unless the user asks for broader refactoring
