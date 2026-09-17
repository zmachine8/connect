# Tasks and checklists

## Definition of Ready

An issue may enter `Ready` when:

- [ ] its outcome is understandable;
- [ ] it belongs to the current scope and stage;
- [ ] acceptance criteria are testable;
- [ ] dependencies and relevant design are linked;
- [ ] it is small enough for a short-lived branch;
- [ ] an owner and coordinating Lead are known.

## Definition of Done

A task is done when:

- [ ] acceptance criteria are met;
- [ ] the relevant automated and manual checks pass;
- [ ] loading, empty, error and invalid-input states were considered;
- [ ] code and naming follow the architecture;
- [ ] documentation changed where necessary;
- [ ] the pull request was reviewed and merged;
- [ ] its issue was closed with useful evidence;
- [ ] `main` remains green.

## Current Week-3 foundation

- [x] Create Kotlin/Compose Android project.
- [x] Create Login → Users → Chat prototype navigation.
- [x] Add fake local repositories.
- [x] Add example ViewModels and UI state.
- [x] Add initial theme.
- [x] Add one repository unit test.
- [x] Add CI for tests, lint and debug build.
- [x] Add issue and pull-request templates.
- [x] Document architecture and team workflow.
- [ ] Every teammate clones and runs the project.
- [ ] Team confirms the package/application ID `ee.ut.connect`.
- [ ] Team fills Stage 1 Lead assignments.
- [ ] Team creates a GitHub Project board with `Backlog`, `Ready`, `In progress`, `Review`, `Done`.
- [ ] Repository owner enables `main` protection.
- [ ] Design Lead links the first wireframes.

## Next issues to create

### Design and UX

- [ ] Design login/account screen and its validation states.
- [ ] Design users loading, empty and error states.
- [ ] Design chat empty, sending and failed-message states.
- [ ] Define Connect colours, typography and reusable component rules.

### Product and requirements

- [ ] Convert the report requirements into GitHub user stories.
- [ ] Add acceptance criteria to the login, directory and chat stories.
- [ ] Prioritise only the text-message MVP before photo work.

### Development

- [ ] Add Compose previews for all stateless screens.
- [ ] Add empty and error user-list previews.
- [ ] Decide how ViewModels receive repository dependencies before Firebase.
- [ ] Introduce real navigation/form work when covered in weeks 4–6.

### Quality

- [ ] Test blank and whitespace-only chat input.
- [ ] Test user-list success and failure state mapping.
- [ ] Record the supported emulator/device matrix.
- [ ] Prepare three-account Firebase security scenarios for later implementation.

## Issue splitting rule

A task should usually change one screen, one behaviour, one layer or one document. Split it if two teammates could complete independent halves without coordinating the same files.
