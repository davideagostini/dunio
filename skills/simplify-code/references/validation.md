# Validation Reference

## When to validate

Run validation only in `fix-and-validate` mode, or when the user explicitly asks for it.

---

## Choose the smallest relevant check

Prefer:

- Targeted tests for the touched module
- Typecheck for the touched target
- Compile for the touched app or package
- Formatter or lint only if it is a real project safety gate

Avoid broad suites unless the change breadth justifies them.

---

## Reporting

Always say:

- What validation ran
- What was intentionally skipped
- Whether anything was blocked by environment limits or missing tooling
