# Send flow (xcodeApp) — prompt → response or error

Traces what happens from tapping **Send** to either a rendered response or a
visible error, across the SwiftUI ↔ Kotlin boundary. File references are
`path:line`.

## Components

| Layer | File | Role |
|---|---|---|
| Button | `Shared/Components/PromptAreaView.swift` | Send button (`onSend`), error `Text`, reports min-height |
| Pane | `macApp/Views/ChatTopPaneView.swift` / `iosApp/Views/MainChatView.swift` | Wires `PromptAreaView` to `ChatViewModel` |
| State | `Shared/ViewModels/ChatViewModel.swift` | `send()`, `@Published` state, flow observers |
| Bridge | `interop/ResultInterop.kt` (`:shared` appleMain) | Unboxes Kotlin `Result`, rethrows failure |
| Use case | `usecases/SendPromptUseCase.kt` (`:shared`) | Calls LLM, appends history, returns `Result<ResponseEntry>` |
| Split | `Shared/Components/SplitView.swift` | Grows top pane when content (incl. error) needs height |

## Expected flow

```
PromptAreaView Send button (onSend)
        │
        ▼
ChatViewModel.send()                                 [ChatViewModel.swift:156]
  guard !loading                                     :157   ← blocks re-entry while in flight
  guard currentChat                                  :158
  trimmed = prompt.trimmed; guard non-empty          :159-160
  guard selectedModel                                :161
  loading = true                                     :162   → spinner replaces Send button
  error = nil                                        :163   → clears previous error, pane can shrink
  Task:
    raw = await sendUseCase.invoke(chat, trimmed, modelId)   :168
        │  (Kotlin: never throws — returns Result<ResponseEntry> boxed as Any?)
        ▼
    SendPromptUseCase.invoke()                       [SendPromptUseCase.kt:20]
      resolve model / credentials / system prompt
      llm.send(...)                                  :31
      append entry to history (mutates ChatService)  :45   → chats flow emits
      Result.success(entry)  OR  Result.failure(e)   :51 / :53
        │
        ▼
    resultOrThrow(result: raw)                       [ChatViewModel.swift:169]
        │
        ▼
    ResultInterop.resultOrThrow()                    [ResultInterop.kt]
      (raw as Result<Any?>).getOrThrow()
        success → returns unwrapped value
        failure → throws Throwable  ──(@Throws → SKIE)──► Swift error
        │
   ┌────┴─────────────────────────┐
   ▼ SUCCESS                       ▼ FAILURE (throws → catch)
   set lastPrompt = trimmed            error = e.localizedDescription
   prompt RETAINED (not cleared)       prompt retained, lastPrompt untouched
   defer loading = false               defer loading = false
        │                                   │
        ▼                                   ▼
   chats flow already emitted →        PromptAreaView shows red Text     :79-85
   allChats updates :54-75 →           → reports taller ChatTopPanelMinHeight :88
   currentChat recomputes →            → SplitView grows top pane        :39-51
   newest entry renders in history
```

## Success branch — what the UI should do

1. `invoke` appended the entry; the `observeAll()` task (`:53-76`) receives the new
   chat, updates `allChats`, `currentChat` recomputes, history list renders the new card.
2. `prompt` is **retained** (not cleared) so the newest entry hides its prompt header
   and the user can edit + resend. Clearing it would abort the next send on the
   empty-prompt guard.
3. `loading` back to false → Send button returns.

## Error branch — what the UI should do

1. `resultOrThrow` throws → `catch` sets `error` (`:172-173`).
2. `prompt` is **kept** (user can retry/edit) — failure short-circuits before `:174`.
3. `PromptAreaView` renders the red error `Text` (`:79-85`) and reports its height via
   `ChatTopPanelMinHeight` (`:88`); `SplitView` grows the top pane (`:39-51`).

---

## Review findings (suspected regressions / defects)

### 1. Prompt must be RETAINED after send (was broken, now fixed)
The app intentionally keeps the sent text in the input box after a successful send:
the newest history entry hides its prompt header while `entry.prompt == prompt &&
entry.modelId == selectedModel`, and the user iterates by editing + resending.

The original code achieved retention by accident: `prompt = ""` followed by the
`lastPrompt` observer refilling it (`if prompt.isEmpty { prompt = p }`) after the
`set(trimmed)` re-emission. An earlier attempt to "fix the refill race" with a
seed-once guard removed the refill but left `prompt = ""`, so the box ended up empty
and the next send aborted on the empty-prompt guard (`:160`).

**Current fix:** never clear the prompt on success (removed `prompt = ""`). Persisted
`lastPrompt` is seeded into the box only once at startup (`didSeedPrompt`), so a later
`set` emission cannot overwrite the user's text. Retention now comes from simply not
clearing, not from a clear+refill round-trip.

### 2. `cancelSend()` does not cancel the task — MEDIUM
`ChatViewModel.swift:182-183` only flips `loading = false`. The send `Task` is not stored
or cancelled, so the network call keeps running; its `defer`/`catch` later mutate `loading`,
`error`, `prompt`, and history out from under a subsequent send. Store the `Task` and
`cancel()` it.

### 3. Split fraction never shrinks back after error clears — LOW
`SplitView.swift:39-51` only raises `fraction` when min-height grows (`minGrew`). When the
error clears and the pane needs less height, `fraction` stays enlarged. Cosmetic.

### 4. Debug logging still present — cleanup
`ChatViewModel.swift` `print("[send] …")` and `ResultInterop.kt` `println("[ResultInterop] …")`.
Remove or gate before merge.
