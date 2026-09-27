# Connect: profile, chat, and photo change

Apply this follow-up archive on `feature/chat-profile-photos`, preserving directory structure. It updates the chat screen, model, repository, manifest, Firestore rules, and rules tests. No billing upgrade or Cloud Storage bucket is needed.

Run on the development machine:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest lintDebug assembleDebug
cd rules-tests
npm ci
JAVA_HOME=/usr/lib/jvm/java-21-openjdk npm test
```

In Firebase Console, publish the included `firestore.rules` under Firestore Database → Rules. The repository file does not deploy itself. The updated rules allow photo captions, document messages, and location messages. Publish them before trying to send a captioned photo; existing text and photo messages remain valid.

Tap + to choose Photos, Camera, Location, or Document. Location sends one current approximate position after permission is granted; a document uses the system picker and must be at most 200 KB. Tap a location message to open it in a maps app; tap Open on a document to hand it to a compatible app. Location needs location services enabled. Camera opens the device camera app: use the Android Back gesture/button to cancel. Canceling or starting a new capture clears the earlier photo preview; camera-specific controls and settings belong to the device camera app. The selected image appears above the composer. New messages automatically scroll into view, the keyboard closes after pressing Send, and sent photos retain their aspect ratio. The main Send button sends the image and optional caption as one message. Tap × on the preview to remove the image and send only text. Photo messages contain a compressed JPEG blob and an optional text caption in one Firestore message document. Images are reduced to at most 640 pixels on their longest side and capped at 200 KB. Test the + menu, gallery, camera, current location, small documents, preview removal, captions, and receiving attachments with two accounts. Check that oversized documents are rejected without uploading. This approach is appropriate for a small course prototype; Firestore limits each document to 1 MiB, and fetching recent messages also downloads their images.

Manual text checks: sign in as A and B on separate devices or emulator sessions; both choose each other and see the same conversation. Send a message and observe it arrive without refresh. Restart and confirm history remains. Check the profile screen saves a new name and updates the other account's People list.

Rules tests use a demo emulator project; they do not deploy to or write to your production Firebase project.

Limits: only the latest 30 messages are loaded, no earlier-message pagination, no conversation list, and no photo progress percentage. Failed photo sends stay in the preview for retry. Photos are captured using the system camera activity; the app does not need its own camera permission.
