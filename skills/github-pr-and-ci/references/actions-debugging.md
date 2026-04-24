# Actions Debugging Reference

## Standard debugging sequence

Follow this sequence when a CI run failed:

1. Check PR status:

```bash
gh pr checks 55 --repo owner/repo
```

2. List recent runs:

```bash
gh run list --repo owner/repo --limit 10
```

3. View the failed run:

```bash
gh run view <run-id> --repo owner/repo
```

4. Fetch failed logs:

```bash
gh run view <run-id> --repo owner/repo --log-failed
```

---

## What to identify

Before proposing a cause, identify:

- the workflow name
- the failed job
- the failed step
- whether the failure is deterministic or looks flaky

Do not stop at the run level if the failing step can be identified.

---

## Triage mindset

Work from facts outward:

- check status
- run summary
- failed step
- failure log signature
- likely cause

Only then suggest a next action.
