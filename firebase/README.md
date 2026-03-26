# Firebase Setup Files

This folder contains the minimum Firebase configuration files needed to run the Android app with your own Firebase project.

These are the public repo Firebase files for the Android app. They do not include any local testing allowlist or private development bypass.

Contents:

- `firebase.json`
- `firestore.rules`
- `firestore.indexes.json`

Deploy from this folder:

```bash
cd mobile-app/firebase
firebase use --add
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
```

You can also skip `firebase use --add` and deploy with `--project <your-project-id>`.

Important invite convention:

- invite documents must use the recipient email lowercased as the document ID
- example: `households/{householdId}/invites/alice@example.com`

The Android join flow depends on that document ID convention when accepting an invite.

These files are setup infrastructure for the Android app. They are included to make the app runnable, but the repository remains app-first in scope.
