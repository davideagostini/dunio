# Android Compose Review Validation Reference

## When to validate

Run validation only when the mode requires it or the user explicitly asks.

---

## Preferred validation order

Choose the smallest relevant check:

1. Targeted tests for the touched module or feature
2. Lint or static analysis if it is a real project gate
3. Compile or assemble for the touched target
4. Broader validation only if the scope justifies it

Avoid broad full-suite runs when a narrower check covers the risk.

---

## Reporting

Always state:

- What validation ran
- What was intentionally skipped
- Whether anything was blocked by tooling or environment limits
