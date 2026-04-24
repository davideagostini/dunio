# GitHub API Reference

## When to use `gh api`

Use `gh api` when:

- the normal `gh` subcommands do not expose the needed field
- the user wants a custom filtered view
- the task needs structured response data

Example:

```bash
gh api repos/owner/repo/pulls/55 --jq '.title, .state, .user.login'
```

---

## JSON and JQ

Prefer `--json` plus `--jq` when available.

Example:

```bash
gh issue list --repo owner/repo --json number,title --jq '.[] | "\(.number): \(.title)"'
```

Use JSON mode when:

- summarizing multiple objects
- extracting exact fields
- avoiding ambiguous plain-text parsing

---

## Keep output small

Do not dump large raw JSON payloads unless the user explicitly asks for them.

Extract the fields that answer the question directly.
