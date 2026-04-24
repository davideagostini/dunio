# GitHub Actions Failure Patterns Reference

## Common classes of failure

### Code or test failure

Signals:

- assertion failures
- compiler or linter errors
- reproducible stack traces

Typical next action:

- fix code, tests, or config in the repo

### Permissions or auth failure

Signals:

- `403`
- token scope errors
- permissions denied
- missing GitHub App or secret access

Typical next action:

- inspect workflow permissions, token scopes, or secret availability

### Missing secrets or environment configuration

Signals:

- missing environment variables
- secret not found
- authentication setup step fails before tests even start

Typical next action:

- verify secrets, environments, and repo or org settings

### Infrastructure or flaky runner failure

Signals:

- network timeouts
- transient package registry failures
- runner startup issues
- job succeeds on rerun without code changes

Typical next action:

- rerun, isolate flakes, or improve retry strategy

### Workflow logic or script failure

Signals:

- shell script exits unexpectedly
- path assumptions are wrong
- matrix or conditional logic behaves incorrectly

Typical next action:

- inspect workflow YAML and the failing script step

---

## Reporting rule

Always distinguish:

- observed failure signature
- likely failure class
- inferred root cause

Do not present a guess as a confirmed cause.
