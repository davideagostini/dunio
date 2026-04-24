# Parallel Review Reference

## When to stay local

Review locally when:

- The diff is tiny
- The scope is a single very small file
- Spawning sub-agents would cost more coordination than it saves

---

## When to parallelize

Use four sub-agents in parallel when:

- The diff spans multiple files
- The touched area crosses modules or layers
- Reuse lookup across the codebase matters
- The user asked for a substantial review

---

## Recommended review split

Give every sub-agent the same scope, but assign a distinct role:

1. `reuse`
2. `quality`
3. `efficiency`
4. `clarity-and-standards`

Ask each sub-agent to return:

- file
- line or nearest symbol
- problem
- recommended fix
- confidence

Keep outputs concise.

---

## Agent type guidance

Use available agent types pragmatically:

- `explorer` for broad codebase lookup, especially reuse opportunities
- `default` for review passes that require stronger reasoning

Do not invent unavailable roles. Adapt the task to the actual Codex agent types available in the environment.
