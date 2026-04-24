# play-store-release-notes

A skill for generating Google Play Store release notes from engineering artifacts such as git logs, compare ranges, PR summaries, changelogs, and release briefs.

It is designed to produce different note styles for `internal`, `beta`, and `production` tracks without rewriting everything manually each time.

---

## What it covers

The skill ships with 4 reference files:

| Reference              | Topics |
| ---------------------- | ------ |
| `source-material.md`   | How to extract good release input from tags, compare ranges, PRs, commits, and changelogs |
| `classification.md`    | How to separate user-facing changes from internal-only engineering work |
| `release-tracks.md`    | How to adapt content and tone for internal, beta, and production releases |
| `quality-checklist.md` | Final checks for clarity, accuracy, audience fit, and publishability |

---

## Typical use cases

- Generate release notes from a git compare range
- Turn merged PRs into Play Store notes
- Produce different notes for internal QA, beta testers, and production users
- Rewrite technical changelogs into concise public-facing copy

---

## How it works

The skill follows a 4-step workflow:

1. **Gather the release input** — identify the best source material
2. **Classify the changes** — separate user-facing outcomes from internal engineering work
3. **Draft by release track** — adapt content for `internal`, `beta`, and `production`
4. **Validate and tighten** — remove noise, repetition, and unsupported claims

The agent consults the relevant reference file at each step before drafting.

---

## Expected structure

```text
play-store-release-notes/
├── SKILL.md
├── README.md
└── references/
    ├── classification.md
    ├── quality-checklist.md
    ├── release-tracks.md
    └── source-material.md
```

---

## Example prompts

```text
/play-store-release-notes draft notes from git log v2.3.0..v2.4.0
```

```text
/play-store-release-notes summarize these merged PR titles for internal, beta, and production
```

```text
/play-store-release-notes rewrite this changelog into concise production release notes
```

---

## Authoring rules

- Prefer user-visible outcomes over implementation detail
- Internal notes may include operational detail, but production notes should not
- Do not treat every commit as a release note bullet
- Collapse duplicates and rewrite low-signal technical changes into clear user impact where justified
