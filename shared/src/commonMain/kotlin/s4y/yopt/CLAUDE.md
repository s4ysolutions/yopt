---
if: file.inDir("shared/src/commonMain/kotlin/s4y/yopt/")
---

## Shared module conventions

Architecture rules live in `.claude/skills/architecture.md` (layering, port rule,
service rule, use-case rule, persistence decision tree, reactive pattern). Read that
first. For the current shape of any class, read the source — do not rely on a list here.

### Reactive pattern (mandatory)
- **Reads** → `observe*(): Flow<T>` (UI subscribes via `collectAsState()`).
- **Writes** → `suspend fun set*(value)` (persists; the flow propagates).
- **Never** add a `get*(): T` for data that already has an `observe*(): Flow<T>` — it creates stale snapshots. A one-shot getter with no corresponding flow (e.g. `export()`) is fine.

### Package layout (`s4y.yopt`)
- `domain/models/` — data classes + enums.
- `domain/ports/` — boundary interfaces (port rule: one per boundary, even single-impl).
- `domain/services/` — domain logic as plain classes (service rule).
- `usecases/` — UI-facing orchestration (use-case rule: never depends on another use case).
- `adapters/` — port implementations.
- `AppModule.kt` — manual DI; created once via `remember { AppModule(platformContext) }` in `App.kt`. No Compose dependency.

### Models
- `ModelDef` carries its own `enabled` flag — toggling goes through the model service's `setModelEnabled()`, not a separate disabled-models store.
- Auth is API-key only (OAuth removed); credentials live in `SecureStore` via `AuthService`.
