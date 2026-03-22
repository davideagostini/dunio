# Summ Android

`Summ` is an Android personal finance app for shared household money tracking.

This public setup is focused on:

- Android app
- Firebase Authentication
- Cloud Firestore


## What the app does

- Google sign-in with Firebase Auth
- household-based shared data
- dashboard / overview
- assets with monthly snapshots
- entries / transactions
- categories
- recurring transactions
- month close
- household members and invites
- quick entry flow and Quick Settings tile

All finance data is scoped to a `household`.

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Firebase Authentication
- Cloud Firestore
- Navigation Compose

## Requirements

- Android Studio
- JDK 17
- Android SDK installed
- Firebase project

## Firebase setup

### 1. Create a Firebase project

- Open [Firebase Console](https://console.firebase.google.com/)
- Create a new project

### 2. Register an Android app

Use this package name:

```text
com.davideagostini.summ
```

### 3. Enable Authentication

- Go to `Authentication`
- Enable `Google`
- Set project support email
- Complete project branding if Firebase asks for it

### 4. Create Firestore

- Go to `Firestore Database`
- Create a database
- Start in the mode you prefer
- Add your own security rules before using real data

### 5. Add SHA fingerprints

From the Android project root:

```bash
cd mobile-app
./gradlew signingReport
```

Add the debug `SHA-1` and `SHA-256` to the Android app entry in Firebase.

### 6. Download `google-services.json`

- Download the file from Firebase
- Place it here:

```text
mobile-app/app/google-services.json
```

This file is intentionally ignored by Git and is not included in the public repo.

## Firestore structure

The app expects a household-scoped Firestore model like this:

```text
users/{uid}
households/{householdId}
households/{householdId}/members/{userId}
households/{householdId}/categories/{categoryId}
households/{householdId}/transactions/{transactionId}
households/{householdId}/assets/{assetId}
households/{householdId}/assets/{assetId}/history/{entryId}
households/{householdId}/recurringTransactions/{recurringTransactionId}
households/{householdId}/monthCloses/{period}
households/{householdId}/invites/{inviteId}
```

## Run locally

```bash
cd mobile-app
./gradlew assembleDebug
./gradlew installDebug
```

## First-run checklist

1. Sign in with Google.
2. Create a household or join one.
3. Add categories if needed.
4. Add entries and assets.
5. Verify Firestore writes are happening in your Firebase project.

## Important Git safety notes

Do not commit:

- `google-services.json`
- `local.properties`
- Firebase admin/service account JSON files
- signing keys / keystores

If `google-services.json` was already added to Git locally, remove it from the index before publishing:

```bash
git rm --cached mobile-app/app/google-services.json
```

## Current status

The Android app is actively used as:

- a personal/shared finance app
- a playground for studying Android features, libraries, and product ideas

Some areas are still evolving and should be treated as work in progress.

## Roadmap ideas

- widgets
- Wear OS support
- AI-powered finance features
- import / export data
- notifications and reminders
- better backup and migration flows

## License

See [LICENSE](./LICENSE).
