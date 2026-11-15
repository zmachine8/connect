# Unread dots and optional push notifications

## Free-plan behavior

The app can show in-app unread dots without Cloud Functions. It listens to the latest message in each connection and group, then compares that message's timestamp with the signed-in user's last-read marker in Firestore. Opening a conversation updates the marker and clears the dot. Dot detection runs while Connect is running and is refreshed when the app opens; it does not generate an Android notification while the app is closed.

Deploy the Firestore rules from the repository root so the app can store and read its last-read markers:

```bash
npx --yes firebase-tools deploy --only firestore:rules --project connect-f1a30
```

This Firestore rules deployment works on the Spark free plan. The first time this feature is used, an incoming conversation may show a dot until opened because Connect has no previous last-read marker for it.

## Optional Android push notifications

The `functions/` directory contains optional Cloud Functions that increment unread counts and send FCM notifications for direct and group messages. The Android client registers a Firebase Installation ID (FID), and the function targets those FIDs. Firebase requires the project to use the **Blaze** plan with a linked billing account to deploy these functions. If remaining on Spark, skip the following deployment; the in-app dots continue to work.

The app stores its FID under the signed-in user's `fcmInstallations` subcollection. Deploy the updated Firestore rules before installing this version so that this write is allowed; Firestore rules deployment remains available on Spark:

```bash
npx --yes firebase-tools deploy --only firestore:rules --project connect-f1a30
```

To enable push later, use Node.js 22 and deploy from the repository root:

```bash
cd functions
npm install
cd ..
npx --yes firebase-tools deploy --only functions --project connect-f1a30
```

Blaze is pay-as-you-go. Firebase provides no-cost usage allowances for Functions, but related build and artifact storage can incur charges. Budget alerts do not cap spending, so review the Firebase pricing details before linking billing. Do not add service-account credentials to the Android app or repository.

Push notifications include the sender and a short preview. Photo, file and location messages use a generic preview when there is no text. Tapping a push opens Connect, but does not currently navigate directly into the conversation. Android notification permission and device settings also affect delivery.
