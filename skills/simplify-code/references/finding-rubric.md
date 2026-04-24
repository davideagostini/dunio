# Finding Rubric Reference

## Normalize findings

Represent each finding with:

1. File and line, or nearest symbol
2. Category: `reuse`, `quality`, `efficiency`, or `clarity`
3. Why it is a problem
4. Recommended fix
5. Confidence: `high`, `medium`, or `low`

---

## What to keep

Keep findings that materially improve:

- Maintainability
- Correctness
- Runtime or operational cost
- Consistency with local project standards

Discard:

- Duplicate findings
- Purely aesthetic preferences
- Findings contradicted by local instructions
- Low-signal churn with no meaningful payoff

---

## Output style

Prefer concise findings with clear actionability.

Good:

- `foo.ts:42` `quality` redundant cached value duplicates what is already derived from `items.length`; remove stored state and derive on read. `high`

Weak:

- This feels a bit messy and could probably be cleaned up.
