# Current architecture

## Screen responsibilities

| Screen | State owner | Data access |
|---|---|---|
| Login / account creation | LoginViewModel | Firebase Authentication and profile lookup/creation directly in ViewModel |
| People | UsersViewModel | PeopleRepository observes contact requests, connections and blocks; username search queries a global username prefix index |
| Groups / group chat | UsersViewModel and GroupRepository | Groups are observed by membership; group chat reads and writes persistent text messages |
| Profile | ProfileViewModel | Firestore profile listener/update directly in ViewModel; username reservation is updated transactionally |
| Chat | ChatViewModel | FirestoreChatRepository |
| Selected attachments | AttachmentViewModel | Private cache files and SavedStateHandle references |

Compose screens render state and invoke actions. Navigation is in `ConnectApp`; activity-result launchers, location access and opening received documents/maps are in the Chat UI boundary. `ChatScreen` reads the authenticated UID to align incoming/outgoing bubbles. The code therefore has some Firebase/platform coupling outside repositories; do not describe it as a fully isolated repository-interface architecture.

## State lifetime

Screen state uses Compose `mutableStateOf`, not StateFlow. ViewModels survive configuration changes. SavedStateHandle restores the chat draft, edited profile name/dirty flag and attachment metadata/file references following eligible process recreation. Login fields/password are memory-only. Cache files can be evicted. In-flight writes are not resumed after process death.

Firestore listeners are removed when the owning Users, Chat or Profile ViewModel is cleared. Users also stops observation before sign out. See [state management](../docs/state-management.md).

## Remote data

- `users/{uid}`: displayName, optional unique username, request privacy setting and createdAt; emails/passwords are not stored in the directory.
- `usernames/{lowercaseUsername}`: unique username reservation to user ID. Search queries the public username index by prefix; profile emails remain in Firebase Authentication.
- `groups/{groupId}`: name, memberIds, creator and creation time. A group starts with its creator; only that creator can add accounts from username search. The first implementation supports up to 10 members.
- `groups/{groupId}/messages/{messageId}`: sender ID/name, text and creation time, readable and writable only by group members.
- `contactRequests/{requestId}`: sender and recipient IDs, sender display name, pending/accepted/declined status and creation time.
- `connections/{sortedUidA_sortedUidB}`: the two accepted participants and the request that established the connection.
- `blocks/{blockerUid_blockedUid}`: directional block records. Firestore rules deny conversation reads and message writes if either participant blocks the other.
- `chats/{chatId}`: two participantIds and createdAt. The client sorts IDs and joins them with `_`.
- `chats/{chatId}/messages/{messageId}`: senderId, type, createdAt and type-specific payload.
- Message types: text, image (imageBytes and caption), document (name, MIME type, documentBytes and caption), location (latitude/longitude).
- Only the latest 30 messages are queried, ordered by createdAt; there is no older-history pagination.
- New chats require an accepted connection. Existing chat documents are treated as connections for compatibility with earlier project data.
- The theme choice is stored locally in Preferences DataStore and does not require a Firestore profile change.

Photos/documents are capped at 200,000 bytes. Blob storage is a small-prototype compromise and makes conversation reads heavier. Backend access restrictions are in firestore.rules; Android client configuration does not replace those rules.

## Remaining design work

Extract authentication/profile operations behind injectable repositories if required for isolated state tests. Define durable local storage separately from temporary cache and saved-instance state. Record the device matrix and run failure/process-recreation tests. Group editing, member removal, leaving groups, push notifications, and profile photo upload are not in this implementation. Fake repositories remain as scaffold/test examples; production routes use Firebase.
