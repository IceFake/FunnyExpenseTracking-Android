# AGENTS.md - Development Guide for FunnyExpenseTracking Android

Essential information for AI agents working on this Android codebase.

## Editor and AI Tool Rules
- No Cursor rules (`.cursor/rules/` or `.cursorrules`).
- No GitHub Copilot instructions (`.github/copilot-instructions.md`).
- Kotlin version: 2.0.21 (gradle/libs.versions.toml).
- Kotlin code style: official (gradle.properties).
- JDK version: 11 (compileOptions in build.gradle.kts).

## Build Commands
```bash
# Build
./gradlew build
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug

# Testing
./gradlew test                          # Run all unit tests
./gradlew test --tests "*ClassName"     # Run a single test class
./gradlew test --tests "*ClassName.methodName"  # Run a single test method
./gradlew connectedAndroidTest          # Run instrumented tests

# Linting
./gradlew lint                          # Run lint on all variants
./gradlew lintDebug                     # Run lint on debug variant
./gradlew lintRelease                   # Run lint on release variant
```

## Linting and Code Quality
- No custom lint plugins (ktlint/detekt).
- Uses Android Lint with default settings.
- Lint checks can be run per variant as above.

## Code Style Guidelines

### Package Structure
- Clean Architecture: `data`, `domain`, `ui`.
- `data/`: local (Room), remote (Retrofit), repository implementations.
- `domain/`: models, repository interfaces, use cases.
- `ui/`: ViewModels, Fragments, Contracts (MVI), common UI components.

### Naming Conventions
- **Classes**: PascalCase (`TransactionRepository`)
- **Functions/Variables**: camelCase (`getAllTransactions`)
- **Constants**: UPPER_SNAKE_CASE (top‑level or companion object)
- **Files**: Match class name; one class per file (except sealed hierarchies).
- **Suffixes**: `ViewModel`, `Fragment`, `Contract`, `Repository` (interface), `RepositoryImpl` (implementation), `UseCases` (container class).

### Imports
- Group by source: standard library, AndroidX, third‑party, project internal.
- Use alias for conflicts: `import ... as EntityType`
- Keep imports sorted; Android Studio auto‑format is acceptable.

### Kotlin Features
- **Data Classes**: Models, DTOs, entities, UI state.
- **Sealed Classes/Interfaces**: UI events (`sealed class TransactionUiEvent`).
- **Extension Functions**: Layer conversions (`.toDomainModel()`, `.toEntity()`).
- **Coroutines**: `suspend` functions, expose `Flow`.
- **Delegated Properties**: `by viewModels()`, `by lazy`.
- **Default Arguments**: Sensible defaults in data class constructors.

### Comments
- Primary language: Chinese for documentation.
- Use KDoc (`/** ... */`) for public members.
- Inline comments for complex logic (optional).

### Formatting
- 4 spaces per indent.
- Line length: aim for 100‑120 characters.
- Braces: Kotlin standard (opening brace on same line).
- Use trailing commas in multi‑line parameter lists (recommended but not enforced).
- Use blank lines to separate logical blocks.

### File Organization
- One class per file.
- Group sealed class subtypes together.
- Keep extension functions close to the class they extend.

## Architecture Overview

**MVVM + Clean Architecture**:

1. **UI Layer**: Fragments + ViewModels + MVI (State/Event).
   - ViewModels extend `BaseViewModel<S: UiState, E: UiEvent>`.
   - UI state: immutable `data class`.
   - UI events: `sealed class`.
   - Use `updateState` and `sendEvent` methods.

2. **Domain Layer**: Use cases, repository interfaces, models.
   - Use cases are functions grouped in `*UseCases` classes.
   - Models are plain Kotlin data classes.

3. **Data Layer**: Repository implementations, local (Room), remote (Retrofit).
   - Repositories are `@Singleton` scoped with Hilt.
   - Offline‑first strategy; sync when network available.
   - Sync status tracked via `SyncStatus` enum.

## Conventions and Patterns

### MVI (Model‑View‑Intent)
- Each feature has a `*Contract.kt` defining `UiState` and `UiEvent`.
- ViewModel: `StateFlow` for state, `SharedFlow` for events.
- Fragments: observe `uiState`, send events via `onEvent`.

### Repository Pattern
- Interface in `domain/repository/`.
- Implementation in `data/repository/` with `@Singleton`.
- Handle local/remote merging and sync logic.
- Use extension functions `toDomainModel()`, `toEntity()`, `toDto()` for layer mapping.

### Background Tasks & Network
- Use `WorkManager` for periodic tasks (asset snapshot, stock price sync).
- Retrofit with `OkHttp` logging interceptor for network calls.
- DTOs in `data/remote/dto/`, API services in `data/remote/api/`.

## Dependency Injection
- **Hilt** for DI.
- Modules in `di/` package (`AppModule`, `DatabaseModule`, `NetworkModule`, `RepositoryModule`).
- Use `@Inject` constructor injection.
- ViewModels: `@HiltViewModel`.
- Activities/Fragments: `@AndroidEntryPoint`.

## Error Handling
- Use `Resource` sealed class (`Success`, `Error`, `Loading`) for network results.
- ViewModel state includes `errorMessage: String?` and `loadingState: LoadingState`.
- Show user‑friendly messages via `ShowMessage` UI events.

## Testing Guidelines
- Unit tests: `src/test/java/` (JUnit 4).
- Instrumented tests: `src/androidTest/java/`.
- Naming: `*Test` (unit), `*InstrumentedTest` (Android).
- Use `runTest` for coroutine testing.
- Mock repositories with fake implementations.

## CI/CD
- GitHub Actions workflow `.github/workflows/release.yml` builds and releases APK on tag push.
- Uses `./gradlew assembleRelease`.
- No automated testing in CI yet.

## Notes for AI Agents
- Follow existing patterns in the same package.
- Prefer extension functions over utility classes for conversions.
- Use Kotlin coroutines (`suspend`, `Flow`) for async code.
- Avoid `!!` operator; use safe calls or proper null handling.
- Keep UI state immutable; update via `copy` or reducer functions.
- Add Chinese documentation comments for new public members.
- When adding a feature, create corresponding `Contract`, `ViewModel`, `Fragment`, and repository methods.
- Add new dependencies to appropriate Hilt module.
- Run lint and tests before submitting changes.

---

*Last updated: 2026‑02‑27*  
*Based on analysis of the codebase.*