# PR and Checks Reference

## Core commands

Check CI status on a PR:

```bash
gh pr checks 55 --repo owner/repo
```

List recent workflow runs:

```bash
gh run list --repo owner/repo --limit 10
```

View a run summary:

```bash
gh run view <run-id> --repo owner/repo
```

View failed logs only:

```bash
gh run view <run-id> --repo owner/repo --log-failed
```

---

## Good command order

For PR status:

1. `gh pr checks`
2. `gh run list`
3. `gh run view`

For a workflow run already known:

1. `gh run view`
2. `gh run view --log-failed`

---

## Structured output

Many `gh` commands support `--json`. Use it when:

- the output is too verbose
- the user wants a filtered subset
- you need to summarize multiple runs or checks

Example:

```bash
gh issue list --repo owner/repo --json number,title --jq '.[] | "\(.number): \(.title)"'
```
