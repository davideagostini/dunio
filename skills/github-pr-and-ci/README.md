# github-pr-and-ci

A skill for using the GitHub CLI to inspect pull requests, workflow runs, checks, and CI failures.

It is designed for operational GitHub work: checking PR status, triaging broken workflows, reading failed logs, and querying GitHub data in a structured way without getting lost in raw output.

---

## What it covers

The skill ships with 6 reference files:

| Reference             | Topics |
| --------------------- | ------ |
| `scope-and-repo.md`   | How to identify the correct repo, PR, run, or URL and when to use `--repo` |
| `pr-and-checks.md`    | Core `gh pr` and `gh run` commands for status inspection |
| `actions-debugging.md` | Step-by-step workflow for investigating failed CI runs |
| `failure-patterns.md` | Common GitHub Actions failure classes and how to distinguish them |
| `gh-api.md`           | `gh api`, `--json`, and `--jq` usage for structured data access |
| `reporting.md`        | How to summarize findings clearly without dumping raw logs |

---

## Typical use cases

- Check the status of a pull request and its CI checks
- Find out why a GitHub Actions run failed
- Inspect failed logs for a PR check
- Query GitHub data with `gh api` or `--json`

---

## How it works

The skill follows a 5-step workflow:

1. **Determine the GitHub scope** — identify repo, PR, run, or URL
2. **Gather the current state** — inspect checks and workflow runs
3. **Investigate failures when needed** — identify failed jobs, steps, and logs
4. **Use API or JSON output when useful** — extract structured data cleanly
5. **Summarize the result** — report current status, failure point, cause, and next action

The agent uses `gh` pragmatically: first the smallest standard subcommand, then `gh api` or JSON filtering only when needed.

---

## Expected structure

```text
github-pr-and-ci/
├── SKILL.md
├── README.md
└── references/
    ├── actions-debugging.md
    ├── failure-patterns.md
    ├── gh-api.md
    ├── pr-and-checks.md
    ├── reporting.md
    └── scope-and-repo.md
```

---

## Example prompts

```text
/github-pr-and-ci check PR 55 in owner/repo
```

```text
/github-pr-and-ci debug why CI failed on this PR
```

```text
/github-pr-and-ci get the latest workflow runs for owner/repo
```

---

## Authoring rules

- Prefer the smallest `gh` command that answers the question
- Use `--repo owner/repo` when not in the target git directory
- Inspect failed logs before diagnosing CI
- Distinguish observed facts from inferred causes
