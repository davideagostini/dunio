# GitHub Reporting Reference

## Good summary shape

When reporting back, include:

1. What you inspected
2. Current status
3. Failed job and step, if any
4. Likely cause, if debugging
5. Next useful action

---

## Example

Good:

- Checked PR `#55` in `owner/repo`
- Failing check: `android-build`
- Failed step: `Run unit tests`
- Observed error: test assertion failure in `LoginViewModelTest`
- Likely cause: regression in login state handling
- Next action: inspect the changed login flow and rerun tests locally

Weak:

- CI is broken
- Something failed in Actions

---

## Keep facts and inference separate

Observed facts should be stated directly.

Any inferred root cause should be labeled as likely or probable unless the logs make it explicit.
