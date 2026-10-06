# Current architecture

## Screen responsibilities

| Screen | State owner | Data access |
|---|---|---|
| Login / account creation | LoginViewModel | Firebase Authentication and profile lookup/creation directly in ViewModel |
| People | UsersViewModel | FirebaseUserRepository observing Firestore users |
| Profile | ProfileViewModel | Firestore profile listener/update directly in ViewModel |
| Chat | ChatViewModel | FirestoreChatRepository |
| Selected attachments | AttachmentViewModel | Private cache files and SavedStateHandle references |

Compose screens render state and invoke actions. Navigation is in `ConnectApp`; activity-result launchers, location access and opening received documents/maps are in the Chat UI boundary. `ChatScreen` reads the authenticated UID to align incoming/outgoing bubbles. The code therefore has some Firebase/platform coupling outside repositories; do not describe it as a fully isolated repository-interface architecture.

## State lifetime

Screen state uses Compose `mutableStateOf`, not StateFlow. ViewModels survive configuration changes. SavedStateHandle restores the chat draft, edited profile name/dirty flag and attachment metadata/file references following eligible process recreation. Login fields/password are memory-only. Cache files can be evicted. In-flight writes are not resumed after process death.

Firestore listeners are removed when the owning Users, Chat or Profile ViewModel is cleared. Users also stops observation before sign out. See [state management](../docs/state-management.md).

## Remote data

- `users/{uid}`: displayName and createdAt; emails/passwords are not stored in the directory.
- `chats/{chatId}`: two participantIds and createdAt. The client sorts IDs and joins them with `_`.
- `chats/{chatId}/messages/{messageId}`: senderId, type, createdAt and type-specific payload.
- Message types: text, image (imageBytes and caption), document (name, MIME type, documentBytes and caption), location (latitude/longitude).
- Only the latest 30 messages are queried, ordered by createdAt; there is no older-history pagination.

Photos/documents are capped at 200,000 bytes. Blob storage is a small-prototype compromise and makes conversation reads heavier. Backend access restrictions are in firestore.rules; Android client configuration does not replace those rules.

## Remaining design work

Extract authentication/profile operations behind injectable repositories if required for isolated state tests. Define durable local storage separately from temporary cache and saved-instance state. Record the device matrix and run failure/process-recreation tests. Fake repositories remain as scaffold/test examples; production routes use Firebase.
