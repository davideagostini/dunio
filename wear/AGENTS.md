# AGENTS.md

## Project

Net Worth Monitor Wear OS is the **watch companion** for the mobile household finance app.

The watch app must stay lightweight, fast, and focused on a few high-value actions.

It is not a full finance app.

------------------------------------------------------------------------

# Core product principles

The product must be:

- simple
- minimal
- quick to use
- glanceable
- battery friendly

Avoid complexity typical of phone or tablet apps.

The goal is a **small companion surface** for fast actions and at-a-glance access.

------------------------------------------------------------------------

# Technology stack

Frontend: Kotlin, Jetpack Compose for Wear OS

Backend: Firebase Authentication, Cloud Firestore, Wear Data Layer / companion sync

The Wear app must remain compatible with the shared household model used by the mobile app.

------------------------------------------------------------------------

# Available Skills

The project includes reusable skills in `../skills/`.

- For Android project scaffolding, follow `../skills/android-compose-starter/SKILL.md`
- For review and modernization of an existing Compose codebase, follow `../skills/android-compose-review/SKILL.md`
- For GitHub PR, Actions, and CI debugging, follow `../skills/github-pr-and-ci/SKILL.md`
- For Play Store release notes, follow `../skills/play-store-release-notes/SKILL.md`
- For change-focused cleanup and safe simplification, follow `../skills/simplify-code/SKILL.md`

When a task matches a skill domain, consult that skill's `SKILL.md` first and load the relevant
reference files as needed.

------------------------------------------------------------------------

# Wear-first philosophy

The application is **watch-first**.

Primary device: **Wear OS watch**.

Design rules:

- extremely short flows
- one or two primary actions per screen
- avoid text-heavy screens
- avoid long forms
- avoid dense lists
- use compact cards and chips

------------------------------------------------------------------------

# Wear UI rules

Prefer:

- large touch targets
- short labels
- explicit icons
- simple confirmation states
- glanceable summaries

Avoid:

- complex navigation trees
- full-screen data entry
- keyboard-heavy interaction
- dense dashboards
- unnecessary scrolling

Wear-specific rules:

- design for small round screens first, then verify square screens if supported
- keep rotary and swipe-dismiss interactions smooth and predictable
- prefer short-lived screens and lightweight refreshes to preserve battery
- sync with the companion app rather than duplicating complex watch-only state

------------------------------------------------------------------------

# Shared workspace model

The watch app must support the same **household** concept as the mobile app.

The watch should only expose household-scoped data.

Never design it as single-user.

------------------------------------------------------------------------

# Authentication and sync

Authentication and household membership are managed by the companion mobile app and shared Firebase
project.

The watch app should:

- read only household-scoped data
- handle missing or loading states safely
- avoid creating new household state on its own unless explicitly required by the feature

------------------------------------------------------------------------

# Data and state

Wear OS screens should:

- load quickly
- fail gracefully
- show compact loading and error states
- prefer cached or pre-synced data where appropriate

------------------------------------------------------------------------

# Coding guidelines

Follow these principles:

- small files
- descriptive names
- no unnecessary abstractions
- strict Kotlin
- explicit error handling
- keep Wear UI separate from phone UI when behavior diverges

------------------------------------------------------------------------

# What to avoid

Avoid:

- phone-sized layouts on watch
- heavy animated UIs
- long forms
- large charts
- unsafe Firestore assumptions

------------------------------------------------------------------------

# Completion checklist

Before finishing work verify:

- the flow is usable on a small watch screen
- text remains readable
- touch targets are large enough
- household scoping is respected
- loading and error states are handled
- the feature stays fast and simple

If anything cannot be verified, clearly state it.

------------------------------------------------------------------------

# Agent behavior

When working on this module:

1. inspect the project structure
2. create a short plan
3. implement incrementally
4. explain changed files
5. highlight assumptions
6. mention manual verification steps

The goal is clean, maintainable production-quality code.
