# Source Material Reference

## Best input sources

Prefer these inputs, in this order:

1. Release brief prepared by the team
2. Merged PR list with titles and short descriptions
3. Git compare range between tags or versions
4. Curated changelog entries
5. Raw commit history

The lower the source quality, the more interpretation is required.

---

## What to extract

From the source material, extract:

- New user-facing features
- User-visible improvements
- Bug fixes with observable impact
- Performance, reliability, or security improvements that matter to users
- Explicitly internal-only work that should stay out of public notes

Ignore or down-rank:

- Pure refactors
- Dependency bumps without visible impact
- Renames, formatting, lint cleanup
- Test-only changes
- CI, build, or tooling changes
- Internal analytics or logging changes unless they materially affect reliability

---

## Preferred git inputs

When working from git, prefer:

- Tag-to-tag: `v2.3.0..v2.4.0`
- Release branch compare ranges
- Merge commit subjects
- PR squash commit messages if the team uses them consistently

Examples of useful local commands:

```bash
git log --oneline v2.3.0..v2.4.0
```

```bash
git log --merges --first-parent --oneline v2.3.0..v2.4.0
```

```bash
git shortlog -sne v2.3.0..v2.4.0
```

```bash
git log --pretty=format:'%s' v2.3.0..v2.4.0
```

Use merged history when possible. Raw commit streams often contain too much noise for direct publication.

---

## If the input is messy

If commit history is noisy:

- Collapse related commits into one change
- Infer the user-facing outcome only when it is strongly supported
- Keep uncertain items out of production notes
- Preserve ambiguity only in internal notes

If the source material is incomplete:

- Say what is missing
- Draft a conservative version rather than filling gaps with guesses
