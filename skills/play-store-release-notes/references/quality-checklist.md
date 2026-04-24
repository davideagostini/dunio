# Quality Checklist Reference

## Before finalizing

Check that the release notes:

- Match the requested release track
- Reflect only supported changes
- Focus on user value, not engineering activity
- Avoid duplicate bullets
- Avoid vague filler such as `bug fixes and improvements`
- Avoid raw commit syntax, hashes, or branch names
- Avoid promising future work
- Read naturally when pasted into a store listing or release channel update

---

## Common failures

Weak:

- Bug fixes and performance improvements
- Refactored sync flow
- Updated dependencies

Better:

- Improved sync reliability on unstable connections
- Fixed an issue affecting session recovery after login
- Smoother onboarding and navigation experience

---

## Final pass

Ask:

- Would a real user understand this?
- Is each bullet worth publishing?
- Is there any internal detail leaking into beta or production?
- Did multiple commits get collapsed into one clear outcome?

If the answer to any of these is no, tighten the draft again.
