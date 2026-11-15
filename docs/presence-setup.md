# Online presence setup

Connect displays an **Online** or **Offline** label beside a person's name in the People lists, incoming requests and username search results. Until the app receives a presence value, it displays **Unavailable** so it does not guess that someone is offline.

## Firebase setup

1. The Realtime Database instance is `https://connect-f1a30-default-rtdb.europe-west1.firebasedatabase.app`. Its URL is set under `project_info.firebase_url` in `app/google-services.json`.
2. From the repository root, deploy `database.rules.json` with the Firebase CLI:

   ```bash
   firebase deploy --only database
   ```

The rules let signed-in users read status and allow each account to write only its own status. Presence values contain a state and server timestamp. Status is visible to authenticated Connect users; the UI only shows it for people returned by the app's existing People and search queries.

## How it works

The app marks its signed-in account online while the app is in the foreground. It marks the account offline when the app goes to the background or signs out. Firebase Realtime Database's disconnect operation also marks an app instance offline after a connection loss or crash. Each app instance writes to its own connection entry, so an account remains online if another device is still active.

If you replace `app/google-services.json`, make sure the `firebase_url` and deployed database rules belong to the same Firebase project as the existing Firestore configuration.

## Manual checks

1. Install the same build on two devices and sign in with different test accounts.
2. With both apps open, confirm each account is shown as **Online** on the other device.
3. Put one app in the background and confirm that account changes to **Offline**; bring it back to the foreground and confirm it becomes **Online** again.
4. Turn off Wi-Fi on one device and confirm its status changes to **Offline** after the database detects disconnection; restore Wi-Fi and confirm it returns to **Online**.
5. Sign out on one device and check that the status is **Offline**.
6. Sign in to the same account on two devices. Background one device and verify the account remains **Online** while the other stays in the foreground.
