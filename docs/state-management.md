# State management

Login and Profile own their observable Compose state in screen-scoped ViewModels. Their composables render that state and call ViewModel methods for user actions. Firebase work is no longer owned by these composables. Users and Chat retain their existing ViewModels and mutableStateOf state; StateFlow is not required for this design.

## State lifetime

- Login fields, mode, progress and authentication result survive rotation in LoginViewModel. Passwords are not written to saved state. Login fields reset after process death.
- ProfileViewModel owns the profile listener and removes it when cleared. An edited name and its dirty flag use SavedStateHandle. Incoming snapshots do not overwrite an unsaved edit. Saving is disabled while the initial profile is loading.
- ChatViewModel saves its text draft in SavedStateHandle, including clearing it after successful sends. Messages reload from Firestore; sending/uploading flags are not restored after process death.
- AttachmentViewModel retains selections across rotation. Photo/document bytes use private cache files, with paths and document metadata in SavedStateHandle. The pending camera path is also saved so a restored camera result can find its file. Files are removed when cancelled, replaced, sent successfully, or the attachment ViewModel is cleared. Android can evict cache files; restoration is best effort.
- Small transient state, such as the attachment menu, remains local to Compose.

An in-flight write cannot be resumed by SavedStateHandle after process death. Check the conversation before retrying a send whose outcome is uncertain.

## Manual verification

1. Rotate on Sign in and Create account: fields and mode remain. Rotate during authentication: progress remains and successful authentication opens People.
2. Edit a profile name and rotate: the unsaved edit remains. Verify a database snapshot does not replace it. Save and check the People list reflects the change.
3. Type a chat draft and rotate: the draft remains. Send: it clears only on success. A failure leaves it available to retry.
4. Select a photo/document and rotate: the preview/name remains. Send or cancel: the selection clears. Rotate while the camera app is open, then finish taking the picture.
5. With a draft/attachment selected, background the app and use `adb shell am kill ee.ut.connect`, then restore from Recents. Do not use force-stop as a saved-state restoration test.
6. Leave a chat/profile and sign out; verify no obsolete listener changes the visible screen.

## Automated checks

Run `./gradlew testDebugUnitTest lintDebug assembleDebug` and the existing Firestore rules tests. The implementation environment could not download Gradle from services.gradle.org, so compilation, lint, and Android runtime behavior still require local verification.
