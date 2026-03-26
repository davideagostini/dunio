# Contributing

Thanks for contributing to `Summ Android`.

This project is public because it is useful as:

- a real shared-household finance app
- an Android/Firebase learning project
- a practical reference implementation for a shared-household Android app

## Before you contribute

Please read:

- [README.md](./README.md)

The most important expectations are:

- keep files small
- reuse existing UI patterns
- prefer feature-based `components/`
- avoid introducing new patterns when an existing one already solves the problem
- comment all non-trivial code you add or change

## Setup

1. Create your own Firebase project.
2. Register the Android app with package:

```text
com.davideagostini.summ
```

3. Add `google-services.json` to:

```text
app/google-services.json
```

4. Build locally:

```bash
cd mobile-app
./gradlew assembleDebug
```

If you need a working backend for local manual testing, Firebase setup files live in [`firebase/`](./firebase).

## Pull request guidelines

- keep changes focused
- explain the user-facing effect clearly
- mention assumptions
- mention manual verification steps
- include screenshots for UI changes when helpful

## What not to commit

Do not commit:

- `google-services.json`
- `local.properties`
- `keystore.properties`
- `.jks` / `.keystore` files
- personal Firebase credentials

## Suggested workflow

1. Inspect the existing feature first.
2. Reuse an existing pattern if one already exists.
3. Implement incrementally.
4. Run:

```bash
cd mobile-app
./gradlew assembleDebug
```

5. Open the PR with:

- short change summary
- affected screens/features
- manual verification notes
