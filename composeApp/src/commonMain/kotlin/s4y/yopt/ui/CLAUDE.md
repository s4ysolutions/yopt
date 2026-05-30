---
if: file.inDir("composeApp/src/")
---

## UI patterns

- **No ViewModel** — state is `remember { mutableStateOf(...) }` directly in composables.
- **Persisted state is reactive** — read via `collectAsState()`, write via `scope.launch { useCase.set(value) }`.
- **Never** use `LaunchedEffect` to load initial values for data that has an `observe*(): Flow<T>` — the flow IS the initial load.
- **Adding state**: local UI state → `remember { mutableStateOf(...) }`. Persisted state → add `observe/set` to a repository, expose via use case, consume via `collectAsState()`.
- **Material 3** components only (`androidx.compose.material3.*`).
- **Material Symbols Rounded** for icons — all icons defined in `AppIcons.kt` as named `ImageVector` constants. Never inline `Icons.Default.Xxx` or `Icons.Rounded.Xxx`; use `AppIcons.TheName` and add new constants to `AppIcons` when needed.
- **Tooltips** on icon-only buttons — wrap in `TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above), tooltip = { PlainTooltip { Text("...") } }, state = rememberTooltipState())`. Add `@OptIn(ExperimentalMaterial3Api::class)` to the enclosing `@Composable` function. Import `TooltipAnchorPosition` and `TooltipDefaults` from `androidx.compose.material3`.
- **Thread new use cases** through `MainScreen`'s constructor and `App()` in `App.kt`.
- **Safe area (required on every screen)** — every full-screen `Column` root must carry `Modifier.windowInsetsPadding(WindowInsets.systemBars)` before `.padding(12.dp)`. No-op on desktop; keeps content clear of status/nav bars on Android/iOS.
- Spacing pattern: `Spacer(Modifier.height(n.dp))` between sections.
- **Responsive layout** — `BoxWithConstraints` (threshold 630.dp) toggles between two header modes. **Wide**: single `Row` — search `Box(width = maxWidth * 0.3f)` | chat name `weight(1f)` | icon buttons. **Narrow**: two rows — top `Row` has search `Box(weight(1f))` then icon buttons; second row is chat name `fillMaxWidth`. Capture `maxWidth` as a local `val` before entering any nested lambda to avoid implicit-receiver errors.
- **Tag chips** — tags are displayed as horizontally-scrollable rows of small `OutlinedButton` chips (28.dp height). Each chip shows the tag name with a "×" suffix; clicking removes it. An additional `OutlinedButton("+ Add tag")` opens an `AlertDialog` for entering a new tag (empty and duplicate tags rejected).
- **History items** use `itemsIndexed`. Per-item rules:
  - **Prompt area**: hidden for the first (most-recent) item when `entry.prompt == prompt` AND `entry.modelId == selectedModel` (both match current input and model selection); shown otherwise. Layout uses `BoxWithConstraints` (630.dp): narrow/expanded → buttons-row above text; wide+collapsed → text left, buttons right. Replace/Append/Copy always visible; Expand only when text overflows.
  - **Response area**: all 5 buttons (Collapse/Expand, Raw/MD, UseAsPrompt, Append, Copy) always shown unconditionally above the response. Auto-expand: first item always expanded; others expanded if word count < `RESPONSE_AUTO_EXPAND_WORD_LIMIT` (= 50), collapsed otherwise. When expanded and `respContentHeightPx > windowHeightPx`, the same 5 buttons repeat at the bottom of the card.
  - **Bottom bar**: plain `Row` with info only — RemoveFromHistory, timestamp, model name (`weight(1f, fill=false)`, ellipsis), duration. No action buttons in the bottom bar.
  - `MarkdownResponse` is an `expect`/`actual` — multiplatform-markdown-renderer on Android/Desktop/iOS, plain `Text` on JS/macOS.
