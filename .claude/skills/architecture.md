---
name: architecture
description: YoPt architecture — layering rules, ports vs services, use-case rule, persistence decision tree, reactive pattern, DI wiring
---

# YoPt Architecture

This skill is the source of truth for **architectural rules**. It is NOT a catalogue
of classes — for the current shape of any model, service, port, or use case, read the
actual source under `shared/src/commonMain/kotlin/s4y/yopt/`. If a class signature here
and the code disagree, the code wins; if a *rule* here and the code disagree, the code
is the bug.

## Layering

Dependencies point inward only:

```
UI  →  usecases  →  domain.services  →  domain.ports        →  adapters (impl)
                 ↘  domain.models  ↙        (interfaces)         (implement ports)
```

```
shared/src/commonMain/kotlin/s4y/yopt/
  domain/
    models/    — data classes + enums (no behaviour, no I/O)
    ports/     — interfaces for every boundary crossing (see port rule)
    services/  — domain logic, plain CLASSES (see service rule)
  usecases/    — thin orchestration the UI consumes
  adapters/    — port implementations (Ktor, secure storage, …)
  AppModule.kt — manual DI wiring
```

## Core rules

### Port rule
**Every dependency that crosses the domain↔infrastructure boundary gets a port
interface in `domain/ports/` — even when there is exactly one implementation.** This
keeps the domain testable and infra-swappable. `HttpGateway` (single impl,
`KtorHttpAdapter`) is the canonical example: one implementation, still a port.

Platform-specific stores are ports expressed as `expect class` (`KeyValueStore`,
`SecureStore`), with an `actual` per platform.

### Service rule
**Domain services in `domain/services/` are plain classes — never wrapped in an
interface.** They are not boundary crossings; they ARE the domain logic. They depend
only on ports and on other services, and are consumed directly by use cases. This does
not contradict the port rule: ports are for boundaries, services are the inside.

### Use-case rule
**A use case must not depend on another use case.** Depend on domain services (or ports)
instead. If two use cases need shared behaviour, that behaviour belongs in a service.

## Persistence decision tree

1. **Simple scalar** (String, Float, Boolean) → `AppPreferencesService` over `KeyValueStore`. No new port, no new adapter.
2. **Structured non-sensitive** (JSON list) → a domain service (e.g. `ChatService`, `ModelService`) over `KeyValueStore`. No new port, no new adapter.
3. **Credentials / secrets** → `AuthService` (domain service) over `SecureStore` (expect/actual port). Never `KeyValueStore`.
4. **HTTP calls** → the `HttpGateway` port via `KtorHttpAdapter`.

`KeyValueStore` and `SecureStore` are platform-specific and therefore ports; the
services layered on top of them are plain classes.

## Reactive pattern

Services expose `Flow` for reads and `suspend fun` for writes. The flow IS the initial
load — never use `LaunchedEffect` to fetch initial values. UI subscribes via
`collectAsState()`.

Never pair an `observe*(): Flow<T>` with a `get*(): T` for the same data — the flow is
the single source of truth and a getter creates stale snapshots. A one-shot getter with
no corresponding flow (e.g. `export()`) is fine.

```kotlin
val selectedModel by modelSelectionUseCase.observe().collectAsState(null)
scope.launch { modelSelectionUseCase.set(m.id) }
```

**Stale-value fix.** When UI local state captures a flow value, key `remember` on that
value so it resets when the persisted value arrives (first frame shows the default,
second frame shows the real value):

```kotlin
val persisted by useCase.observe().collectAsState(default)
var local by remember(persisted) { mutableStateOf(persisted) }
```

## DI wiring (`AppModule.kt`)

Manual DI, no framework. `AppModule` constructs ports → services → use cases in
dependency order and exposes use cases to the UI. It lives in `shared/` and has no
Compose dependency; create it once with `remember { AppModule(platformContext) }` in
`App.kt`. Wire new dependencies here; read the file for the current graph rather than
relying on a copy.

## Adding a feature — checklist

1. Need a new model? → `domain/models/` (data class only).
2. Does it cross a boundary (platform, network, secrets)? → define a **port** in `domain/ports/`, implement it in `adapters/`.
3. Domain logic / persistence over an existing port? → a **service** class in `domain/services/` (consult the persistence decision tree).
4. Expose to UI → a **use case** in `usecases/` (depends on services/ports, never other use cases).
5. Wire it in `AppModule.kt`.
6. UI consumes the use case via the reactive pattern.
