# Connect

Connect is a native Android course prototype for private one-to-one communication. The main flow is **sign in or create an account → People → Chat → send text or an attachment**.

## Current state: Stage 2 / Design

Implemented in the current source:

- Firebase email/password registration, sign in, session-based start destination and sign out;
- Firestore profiles and a live People directory excluding the current user;
- editing your own display name;
- deterministic private conversations and persistent text messages;
- camera/gallery photo selection, preview and captions;
- document sharing up to 200,000 bytes and one approximate location per share action;
- dark fantasy Compose theme, licensed Cinzel/Lato fonts and adaptive launcher icon;
- compact layouts based on available height, including keyboard insets;
- screen ViewModels, saved chat/profile drafts and attachment cache references;
- Android CI for unit tests, lint and debug builds, plus separate Firestore emulator rules tests.

This is a prototype, not a finished release. Only the latest 30 messages are loaded; there is no pagination, push notification, search, groups, profile-photo upload or end-to-end encryption. See [integration and limitations](CONNECT-INTEGRATION.md).

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

- [Current architecture](WORK/ARCHITECTURE.md)
- [State lifetime and recreation checks](docs/state-management.md)
- [Test procedure and evidence](WORK/TESTS-AND-CHECKS.md)
- [Course stages and outstanding work](WORK/COURSE-ROADMAP.md)

Compose renders observable state. Users and Chat use Firebase repositories. Login and Profile own Firebase operations directly in their ViewModels; repository extraction remains a maintainability improvement. Camera, pickers, location and external file opening remain at the Android/UI boundary. This accurately describes the implementation; it does not claim every Firebase access already follows an interface-based repository layer.

## Team and workflow

Raigo Leesment, Mairon Mihkelsoo, Robin Murumets, Kermo Mätlik and Reimo Zukker.

Develop on feature branches, run checks, request peer review and merge through a pull request. [Team handbook](WORK/README.md) · [Role rotation](WORK/ROLE-ROTATION.md).

## Assets and attribution

Cinzel and Lato font licence files are in `docs/font-licenses/`. The launcher icon was authored as Android vectors with AI assistance. The fantasy backgrounds were added during the design work; record their generation/source and applicable use rights before release. AI-assisted changes must be understood, reviewed and tested by the team.
