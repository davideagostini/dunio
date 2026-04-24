# Scope and Diff Reference

## Scope priority

Choose scope in this order:

1. Files or paths explicitly named by the user
2. Current git changes
3. Files edited earlier in the same Codex turn
4. Most recently modified tracked files, only if the user asked for a review and no diff exists

If there is still no clear scope, stop and say so briefly.

---

## Correct git diff target

Prefer the smallest diff that matches the actual repo state:

- Unstaged changes: `git diff`
- Staged changes: `git diff --cached`
- Explicit branch or commit comparison: use exactly what the user requested
- Mixed staged and unstaged work: review both

Do not default to `git diff HEAD` when a smaller diff is available.

---

## Local instructions first

Before reviewing or editing, look for the closest relevant local guidance:

- `AGENTS.md`
- repo workflow docs
- architecture docs
- style guides for the touched module

Use these to distinguish real issues from intentional local patterns.
