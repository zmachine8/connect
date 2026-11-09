# Connect

Connect is a native Android course prototype for private one-to-one communication. The main flow is **sign in or create an account → People → Chat → send text or an attachment**.

## Current state: Stage 3 / Build and integration

The project timeline and learning across all course weeks and stages are summarized in [the course and project progress report](docs/course-progress-report.md).

Implemented in the current source:

- Firebase email/password registration, sign in, session-based start destination and sign out;
- Firestore profiles, connection requests and username-prefix search across registered users, with existing connections sorted first;
- connection requests, accepting or declining requests, and a user-controlled block list;
- editing your display name and unique username, with a setting to close incoming connection requests;
- group creation with existing connections and persistent group text chat;
- deterministic private conversations and persistent text messages;
- camera/gallery photo selection, preview and captions;
- document sharing up to 200,000 bytes and one approximate location per share action;
- selectable Dark Fantasy, Light and Midnight themes, with the choice saved in Preferences DataStore;
- licensed Cinzel/Lato fonts and adaptive launcher icon;
- compact layouts based on available height, including keyboard insets;
- screen ViewModels, saved chat/profile drafts and attachment cache references;
- Android CI for unit tests, lint and debug builds, plus separate Firestore emulator rules tests.

This is a prototype, not a finished release. Only the latest 30 direct messages are loaded; there is no pagination, push notification, profile-photo upload or end-to-end encryption. The invite action opens Android's share sheet with an install-and-connect message, but the app has no public distribution link yet. See [integration and limitations](CONNECT-INTEGRATION.md).

## People, usernames, and groups

The People screen opens on Connections, followed by Groups, Requests, and Blocked. Find people searches registered usernames by prefix, with existing connections sorted before other results. New accounts choose a username during registration; existing accounts can claim one in Profile. Groups can be created before members are added, and members can be selected from search results. Group chats have persistent text messages.

Profile includes display name, username, a setting to close incoming connection requests, theme selection, and Firebase password-reset email. The Firestore schema adds `/usernames/{lowercaseUsername}`, `/groups/{groupId}`, and `/groups/{groupId}/messages/{messageId}`. Publish the updated `firestore.rules` in Firebase Console before using usernames, request privacy, or group chats. Group membership is selected from the creator's connections in the app; group records cannot be edited or deleted from the client yet.

## Build and run

Use Android Studio, Android SDK API 36 and an emulator/device with Android 8.0 (API 26) or newer. The application ID is `ee.ut.connect`.

The CI uses JDK 17. JDK 21 is the locally tested command-line option; do not assume the system's newest Java version works with Gradle 8.13.

1. Clone the repository and check out the branch to review.
2. Open the repository in Android Studio and set the Gradle JDK to a supported version.
3. Check that `app/google-services.json` corresponds to the intended Firebase development project. This client configuration is present in the repository; do not replace it with service-account credentials.
4. In that Firebase project, enable Email/Password authentication and create a Firestore database.
5. Publish the repository's `firestore.rules` using Firebase Console → Firestore Database → Rules. Committing this file does not deploy it.
6. Sync Gradle and run the `app` configuration.

No Firebase Cloud Storage bucket is used. Photos and documents are capped and stored as Firestore blobs. This avoids requiring a Storage billing upgrade, but usage still depends on Firebase project quotas; it is not a guarantee of unlimited free service.

Local checks on Arch Linux:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Firestore rules tests (Node.js/npm and a compatible Java runtime are required):

```bash
cd rules-tests
npm ci
JAVA_HOME=/usr/lib/jvm/java-21-openjdk npm test
```

These tests use the `demo-connect` emulator project, not production data. They run separately from Android CI.

## Architecture and state

- [Current architecture](docs/architecture.md)
- [State lifetime and recreation checks](docs/state-management.md)
- [Test procedure and evidence](docs/testing.md)

Compose renders observable state. Users and Chat use Firebase repositories. Login and Profile own Firebase operations directly in their ViewModels; repository extraction remains a maintainability improvement. Camera, pickers, location and external file opening remain at the Android/UI boundary. This accurately describes the implementation; it does not claim every Firebase access already follows an interface-based repository layer.

## Team and workflow

Raigo Leesment, Mairon Mihkelsoo, Robin Murumets, Kermo Mätlik and Reimo Zukker.

### Stage 3 responsibilities

| Role | Member |
|---|---|
| Product Co-Leads | Raigo Leesment and Kermo Mätlik |
| Design Lead | Reimo Zukker |
| Development Lead | Mairon Mihkelsoo |
| Quality Lead | Robin Murumets |

Each member has one Stage 3 role, and nobody repeats their Stage 1 or Stage 2 role. After Stage 3, Reimo Zukker will be the Quality Lead for Stage 4. Other Stage 4 assignments have not yet been set. Members contribute across implementation, testing, and peer review. Develop on feature branches, run checks, request peer review, and merge through a pull request.

### Previous stage roles

**Stage 1**

| Role | Member(s) |
|---|---|
| Product Lead | Reimo Zukker |
| Design Lead | Raigo Leesment |
| Quality Lead | Mairon Mihkelsoo |
| Development Co-Leads | Robin Murumets and Kermo Mätlik |

**Stage 2**

| Role | Member(s) |
|---|---|
| Product Co-Leads | Mairon Mihkelsoo and Robin Murumets |
| Design Lead | Kermo Mätlik |
| Quality Lead | Raigo Leesment |
| Development Lead | Reimo Zukker |

## Assets and attribution

Cinzel and Lato font licence files are in `docs/font-licenses/`. The launcher icon was authored as Android vectors with AI assistance. The fantasy backgrounds were added during the design work; record their generation/source and applicable use rights before release. AI-assisted changes must be understood, reviewed and tested by the team.
