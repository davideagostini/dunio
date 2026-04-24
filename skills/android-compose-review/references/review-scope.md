# Android Compose Review Scope Reference

## Scope priority

Choose scope in this order:

1. Files or paths explicitly named by the user
2. Current git changes
3. A single screen, feature, or module that the user described
4. Most recently modified relevant files, only if the user asked for a review and no better scope exists

If there is still no clear scope, stop and say so briefly.

---

## Baseline before judging

Before calling something a problem:

- Read the closest local instructions such as `AGENTS.md`
- Check whether the module already follows an established pattern
- Distinguish between intentional local conventions and accidental drift

Do not grade an existing app as if it were a blank new project.

---

## What to favor

Favor findings that improve:

- State ownership
- Lifecycle safety
- Recomposition behavior
- List stability and performance
- Readability and maintainability
- Migration away from clearly outdated patterns
