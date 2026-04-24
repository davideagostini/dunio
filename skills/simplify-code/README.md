# simplify-code

A skill for reviewing changed code for reuse, quality, efficiency, and clarity issues, then optionally applying only high-confidence, behavior-preserving simplifications.

It is designed for change-focused review rather than broad architectural redesign, and it supports three operating modes: `review-only`, `safe-fixes`, and `fix-and-validate`.

---

## What it covers

The skill ships with 5 reference files:

| Reference             | Topics |
| --------------------- | ------ |
| `scope-and-diff.md`   | How to determine the smallest correct review scope and git diff target |
| `parallel-review.md`  | When to review locally versus when to spawn four sub-agents in parallel |
| `finding-rubric.md`   | How to normalize, filter, and report findings |
| `safe-fixes.md`       | Which fixes are safe to apply and which ones should be left alone |
| `validation.md`       | How to choose the smallest relevant validation after edits |

---

## Typical use cases

- Review the current diff for maintainability issues
- Simplify a changed module without changing behavior
- Remove duplication or redundant state in recently edited code
- Safely clean up a PR and then run minimal validation

---

## How it works

The skill follows a 6-step workflow:

1. **Determine the scope** — choose the smallest correct review target
2. **Decide whether to parallelize** — review locally or use four sub-agents
3. **Aggregate findings** — normalize and filter for signal
4. **Apply safe fixes when requested** — only high-confidence, behavior-preserving edits
5. **Validate when required** — run the smallest relevant check
6. **Summarize the outcome** — report what was reviewed, fixed, and left alone

The agent uses local instructions and module conventions to separate real issues from intentional patterns.

---

## Expected structure

```text
simplify-code/
├── SKILL.md
├── README.md
└── references/
    ├── finding-rubric.md
    ├── parallel-review.md
    ├── safe-fixes.md
    ├── scope-and-diff.md
    └── validation.md
```

---

## Example prompts

```text
/simplify-code review the current diff
```

```text
/simplify-code simplify these changes in app/src/main/java without changing behavior
```

```text
/simplify-code clean up the staged changes and run the smallest relevant validation
```

---

## Authoring rules

- Scope review to the smallest correct diff
- Prefer local standards over generic style preferences
- Apply only high-confidence, behavior-preserving fixes
- Avoid refactor churn that needs product or architectural judgment
