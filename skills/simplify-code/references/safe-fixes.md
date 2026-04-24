# Safe Fixes Reference

## What is safe to apply

Apply only high-confidence, behavior-preserving improvements such as:

- Replacing duplicated code with an existing helper
- Removing dead code
- Removing redundant derived state
- Simplifying control flow without changing semantics
- Narrowing overly broad reads or scans
- Renaming unclear local variables in contained scope

---

## What to leave alone

Do not apply automatically:

- Architectural refactors
- Public API redesign
- Changes requiring product judgment
- Refactors that cross too many files
- Anything that depends on speculative intent

---

## Scope discipline

Keep edits scoped to reviewed files unless a small adjacent change is required to complete the fix correctly.

Respect local patterns when they are clearly intentional or documented.
