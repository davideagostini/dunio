# AGENTS.md

## Project

Net Worth Monitor mobile is a **mobile-first multi-user personal finance application**.

The app allows multiple users, for example a couple, to manage shared finances in a single
workspace.

Example: Two different users sign in with Google and both access the same shared dashboard.

------------------------------------------------------------------------

# Core product principles

The product must be:

- simple
- minimal
- clean
- fast
- easy to understand
- optimized for daily use

Avoid complexity typical of accounting software.

The goal is a **lightweight financial dashboard**, not a full accounting suite.

------------------------------------------------------------------------

# Technology stack

Frontend: Kotlin, Jetpack Compose, Android

Backend: Firebase Authentication, Cloud Firestore

Optional later: Firebase Cloud Functions, Firebase Storage

The mobile app must remain compatible with the existing Firebase project and shared household model.

------------------------------------------------------------------------

# Available Skills

The project includes reusable skills in `skills/`.

- For Android project scaffolding, follow `skills/android-compose-starter/SKILL.md`
- For review and modernization of an existing Compose codebase, follow `skills/android-compose-review/SKILL.md`
- For GitHub PR, Actions, and CI debugging, follow `skills/github-pr-and-ci/SKILL.md`
- For Play Store release notes, follow `skills/play-store-release-notes/SKILL.md`
- For change-focused cleanup and safe simplification, follow `skills/simplify-code/SKILL.md`

When a task matches a skill domain, consult that skill's `SKILL.md` first and load the relevant
reference files as needed.

------------------------------------------------------------------------

# Mobile-first philosophy

The application is **mobile-first**.

Primary device: **smartphone**.

Desktop is not relevant for this module.

UI rules:

- large touch targets
- simple navigation
- minimal forms
- limited fields
- avoid dense tables
- prefer card layouts
- quick data entry

------------------------------------------------------------------------

# Visual design

The UI must be:

clean
minimal
calm
simple

Design rules:

Prefer:

- whitespace
- clean typography
- card-based layouts
- subtle shadows
- neutral colors
- minimal icons

Avoid:

- visual clutter
- complex dashboards
- unnecessary animations
- heavy UI frameworks

Numbers should be the visual focus.

------------------------------------------------------------------------

# Navigation

Mobile navigation should use **bottom navigation**.

Primary screens:

Dashboard
Assets
Transactions
Settings

Users should reach main actions in very few taps.

------------------------------------------------------------------------

# Shared workspace model

The application must support **shared financial workspaces**.

The shared entity is called **household**.

Example flow:

User A signs in with Google
User B signs in with Google

Both belong to the same household.

Both see the same:

- assets
- liabilities
- transactions
- charts
- dashboard

Roles:

owner
member

All financial data belongs to a household.

Never design the system as single-user.

------------------------------------------------------------------------

# Authentication

Authentication uses:

Firebase Authentication
Google Provider

Requirements:

- Google login
- persistent sessions
- protected routes or screens
- users must belong to a household to access finance data

First login flow:

1. user signs in
2. user creates a household or joins one
3. user enters the dashboard

------------------------------------------------------------------------

# Firestore data architecture

Firestore should be structured for **household isolation and query efficiency**.

Recommended structure:

households/{householdId}

households/{householdId}/members/{userId}

households/{householdId}/assets/{assetId}

households/{householdId}/transactions/{transactionId}

households/{householdId}/categories/{categoryId}

households/{householdId}/invites/{inviteId}

This ensures all finance queries are scoped to a household.

------------------------------------------------------------------------

# Collections

## users

User metadata.

Fields:

uid
email
name
photoURL
createdAt

------------------------------------------------------------------------

## households

Shared workspace.

Fields:

name
ownerId
createdAt

------------------------------------------------------------------------

## members

Path:

households/{householdId}/members/{userId}

Fields:

userId
role
email
name
photoURL
joinedAt

------------------------------------------------------------------------

## assets

Path:

households/{householdId}/assets/{assetId}

Fields:

name
type (asset | liability)
category
value
currency
createdAt
updatedAt

------------------------------------------------------------------------

## transactions

Path:

households/{householdId}/transactions/{transactionId}

Fields:

date
description
amount
type (income | expense)
category
createdAt
updatedAt

------------------------------------------------------------------------

## categories

Path:

households/{householdId}/categories/{categoryId}

Fields:

name
type (income | expense)

------------------------------------------------------------------------

## invites

Path:

households/{householdId}/invites/{inviteId}

Fields:

email
role
status
createdAt

------------------------------------------------------------------------

# Security rules

Security is critical.

Rules must ensure:

- user must be authenticated
- user must belong to the household
- users cannot access other households

All reads and writes must verify membership.

Never rely on frontend checks.

------------------------------------------------------------------------

# Screens

Primary screens:

- Dashboard
- Assets
- Transactions / Entries
- Settings

Shared settings flows include:

- household members
- categories
- recurring items
- currency
- export
- theme
- language

------------------------------------------------------------------------

# Forms

Use validation.

Forms required:

- create household
- join household
- invite user
- create asset
- edit asset
- create transaction
- edit transaction

Forms should be minimal and fast.

------------------------------------------------------------------------

# Coding guidelines

Follow these principles:

- small files
- descriptive names
- avoid unnecessary abstractions
- strict Kotlin
- isolate Firebase logic
- remove dead code
- explicit error handling

------------------------------------------------------------------------

# What to avoid

Avoid:

- single-user architecture
- hardcoded household IDs
- large monolithic components
- unsafe Firestore queries
- heavy UI frameworks
- dense layouts

------------------------------------------------------------------------

# Completion checklist

Before finishing work verify:

- authentication works
- Google login works
- household membership exists
- finance data scoped by householdId
- protected screens exist
- CRUD flows exist
- forms validate input
- delete confirmation exists
- dashboard calculations are correct
- security rules exist

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
