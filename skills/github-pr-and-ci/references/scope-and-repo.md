# GitHub Scope and Repo Reference

## Identify the target first

Determine whether the user is asking about:

- a repository
- a pull request
- a workflow run
- a branch
- a GitHub URL

If the user provides a PR URL or run URL, prefer using that directly.

---

## When to use `--repo`

Use `--repo owner/repo` whenever:

- you are not inside the target repository
- the current directory is unrelated
- you want to avoid ambiguity across multiple clones

Examples:

```bash
gh pr checks 55 --repo owner/repo
```

```bash
gh run list --repo owner/repo --limit 10
```

---

## If scope is unclear

If you do not know the repo, PR number, or run being referenced, stop and ask for the missing target instead of guessing.
