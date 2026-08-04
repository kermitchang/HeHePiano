# KermitPiano Engineering Rules

These instructions apply to the entire repository.

## Coding Style

- Follow the official Kotlin coding conventions and use four spaces; never use tabs.
- Prefer immutable values and data. Use `val` unless mutation is required and locally contained.
- Keep functions focused, keep public APIs small, and favor composition over inheritance.
- Make visibility explicit when it clarifies intent. Default new implementation details to `internal` or `private`.
- Do not add abstractions, dependencies, or platform branches without a concrete use case.
- Treat compiler warnings as work to resolve; do not suppress them without a written reason.

## Package Rule

- The root package is `com.ruxor.kermitpiano`.
- Package names are lowercase, singular where natural, and describe capability rather than implementation technology.
- Shared application composition belongs under `com.ruxor.kermitpiano.app`.
- A future feature uses `com.ruxor.kermitpiano.feature.<feature>`; reusable infrastructure uses `com.ruxor.kermitpiano.core.<capability>`.
- Platform-specific code keeps the same logical package as its shared counterpart when it implements that counterpart.
- Never use catch-all packages such as `util`, `helpers`, `common`, or `misc`.

## Architecture

- Keep business rules independent from Compose, JVM APIs, MIDI libraries, audio libraries, filesystems, and network clients.
- Dependencies point inward: UI -> application/domain <- infrastructure. Infrastructure implements interfaces owned by the inner layer.
- `composeApp` is the composition root and may depend on feature and core modules; feature and core modules must not depend on `composeApp`.
- Split a module only when there is an actual ownership, dependency, reuse, or build boundary. Do not create speculative empty modules.
- Keep shared behavior in `commonMain`; place JVM or operating-system integration in `jvmMain`.
- Platform checks and vendor APIs must stay behind small interfaces so macOS, Linux, and Raspberry Pi behavior can evolve independently.

## State Management

- Use unidirectional data flow: immutable UI state flows down and typed user actions flow up.
- Each screen or feature has one state owner. Composables render state and emit events; they do not own business workflows.
- Expose observable state as read-only `StateFlow`; keep its mutable counterpart private.
- Model meaningful loading, ready, empty, and failure conditions explicitly rather than with unrelated booleans.
- Keep transient UI-only state local with `remember`; hoist state when another component needs to control it.
- Perform side effects at defined boundaries and inject dispatchers, clocks, filesystems, device access, and engines for testing.

## Naming Rule

- Use `UpperCamelCase` for types and composables, `lowerCamelCase` for functions and properties, and `UPPER_SNAKE_CASE` only for constants.
- Name interfaces and implementations by responsibility; do not prefix interfaces with `I` or suffix every implementation with `Impl`.
- Use nouns for state and models, verbs for actions, and past tense for events that already occurred.
- Name tests as readable behavior statements using backticks.
- One top-level production type per file when the type is substantial; the filename must match that type.

## Folder Rule

- Source-set folders must follow Kotlin Multiplatform conventions: `commonMain`, `commonTest`, `jvmMain`, and `jvmTest`.
- Organize feature code by feature first, then by layer inside that feature when needed.
- Keep tests in the matching package and mirror the production source layout.
- Store shared Compose resources in `commonMain/composeResources` only when a real resource is introduced.
- Generated output and machine-local configuration must never be committed.
- Do not add source folders or modules for roadmap features before implementation begins.

## Commit Rule

- Make each commit atomic, buildable, and limited to one concern.
- Use Conventional Commit subjects: `type(scope): imperative summary`.
- Preferred types are `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, and `chore`.
- Explain the reason and important tradeoffs in the commit body when they are not obvious from the diff.
- Never commit secrets, generated build output, IDE-local files, or commented-out code.
- Do not mix formatting-only changes with behavioral changes.

## Testing Rule

- Add or update tests with every behavior change and every bug fix.
- Test business behavior in `commonTest` whenever it is platform-independent.
- Use `jvmTest` only for JVM integration boundaries and desktop-specific behavior.
- Prefer deterministic fakes over mocks; inject time, randomness, filesystem, device, and engine boundaries.
- Tests must not depend on real MIDI devices, audio hardware, network access, wall-clock timing, or execution order.
- Before committing, run `./gradlew build`. Run focused tests during development and the full build before handoff.
- A skipped or flaky test is a defect and must include a documented owner and follow-up reason.
