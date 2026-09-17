# Architecture

## Dependency direction

```text
Compose screen → ViewModel → Repository interface → data source
```

- **UI:** renders state and reports user actions. It does not load or save data.
- **ViewModel:** owns screen state and coordinates actions.
- **Repository:** provides the operation the application needs without exposing storage details.
- **Data source:** fake data now; Firestore, Firebase Authentication and Storage later.

Nothing skips a layer. A repository must not know about a screen, and a screen must not know whether data comes from a fake, Room or Firebase implementation.

## Current implementation

- `FakeUserRepository` supplies local users.
- `FakeChatRepository` supplies one local message.
- `UsersViewModel` models loading, success and error states.
- `ChatViewModel` owns message draft and prototype messages.
- Navigation connects Login, Users and Chat.

## Future insertion points

| Future work | Location |
|---|---|
| Firebase Authentication | `data/remote/auth` behind an authentication repository |
| Firestore users/chats | `data/remote/firestore` behind repository interfaces |
| Firebase Storage photos | remote data source behind a photo/message repository |
| Camera capture | UI/system boundary invoked by a ViewModel action |
| Room/offline cache, if chosen | `data/local` behind the same repositories |

The fake repositories remain useful for previews and fast tests after Firebase is introduced.
