---
if: file.inDir("xcodeApp/")
---

# xcodeApp — SwiftUI iOS + macOS

Parallel native app for iOS and macOS. Kotlin domain via `ComposeApp.xcframework` (bridged through `KotlinBridge`).

## Cross-platform safety (`Shared/` compiles for BOTH platforms)

Files in `Shared/` target both iOS and macOS. Platform-specific APIs require `#if` guards:

```swift
#if os(macOS)
    .background(Color(nsColor: .controlBackgroundColor))
#else
    .background(Color(uiColor: .systemBackground))
#endif
```

**Never** use `Color(uiColor:)` / `UIColor` / `UIImage` / `UIFont` in `Shared/` without an `#if os(iOS)` guard.
**Never** use `Color(nsColor:)` / `NSColor` / `NSImage` in `Shared/` without an `#if os(macOS)` guard.

Prefer `DesignTokens` constants — they already handle platform branching internally.

## Types

Swift wrappers live in `Shared/Models/`. Never use raw KMP types in views.

| KMP type | Swift wrapper |
|---|---|
| `Chat` | `ChatModel` (in `ChatModels.swift`) |
| `ResponseEntry` | `ResponseEntryModel` (in `ChatModels.swift`) |
| `ModelDef` | `ModelDefModel` (in `ProviderModels.swift`) |
| `ProviderDef` | `ProviderModel` (in `ProviderModels.swift`) |
| `AuthCredentials` | `AuthCredentialsModel` (in `ProviderModels.swift`) |

## DesignTokens

All visual constants come from `Shared/DesignTokens.swift`. Never hardcode values.

```swift
DesignTokens.topAreaBackground       // Color.accentColor.opacity(0.08)
DesignTokens.topAreaCornerRadius     // 12
DesignTokens.cardCornerRadius        // 12
DesignTokens.cardBackground          // platform-correct card fill (opaque)
DesignTokens.sectionPadding          // 8
DesignTokens.cardVerticalPadding     // 4
DesignTokens.iconSize                // 18
DesignTokens.actionBarHeight         // 28
```

`Image.actionIcon()` — extension on `Image` (not `View`) from `DesignTokens.swift`. Sizes to `iconSize`, tints `.secondary`.

## Background rules

- **Opaque surfaces** (cards, dropdowns, overlays): use `DesignTokens.cardBackground` or `.regularMaterial`. Never use `Color.clear` or `Color(uiColor: .systemBackground)` for these.
- **Dropdown / popover backgrounds**: `.regularMaterial` on iOS (guaranteed opaque), `Color(nsColor: .controlBackgroundColor)` on macOS.
- **GeometryReader for measurement**: `Color.clear` inside `GeometryReader` is fine, but add a SEPARATE `.background(...)` for the actual fill — don't rely on the geometry reader to provide opacity.

```swift
// Correct: measurement + opaque background separated
.background(GeometryReader { geo in
    Color.clear
        .onAppear { height = geo.size.height }
})
#if os(iOS)
.background(Color(uiColor: .systemBackground))
#endif
```

## SwiftUI API versions

- **`onChange`**: iOS deployment target is 16. The zero-param / single-param closure form (`onChange(of:initial:_:)`) requires iOS 17+. Use the `perform:` form for `Shared/` files:
  ```swift
  .onChange(of: value, perform: { _ in ... })   // ✓ iOS 14+ (deprecated warning on macOS 14+ is OK)
  .onChange(of: value) { newValue in ... }      // ✗ iOS 17+ only
  .onChange(of: value) { ... }                  // ✗ iOS 17+ only
  ```

## State and actions

- **`ChatViewModel`** is `@StateObject` in root views (`MainChatView` on iOS, `macOSApp` on macOS). On macOS it is passed to `MacMainChatView` as `@ObservedObject`. Never instantiate it in child views — pass as `@ObservedObject` or via environment.
- **Model selection**: call `viewModel.selectModel(_ id: String)`. Never assign `viewModel.selectedModel` directly (it's driven by the Kotlin flow).
- **All Kotlin mutations**: go through `KotlinBridge` via `ChatViewModel` or `SettingsViewModel`. Views never call `KotlinBridge.shared` directly.

## Dropdown z-ordering

`zIndex()` only affects stacking within the same parent. To ensure a dropdown renders above sibling views further down the hierarchy, apply `zIndex()` on the container view in the parent — not just on the dropdown inside an `.overlay`.

```swift
HeaderView(...)
    .zIndex(1)   // renders above Divider + PromptAreaView siblings
```

## Expand logic (Swift `??` precedence)

```swift
// ✓ correct — parenthesise the ?? operand
(chat?.expandedTimestamps.contains(ts) ?? false) || isFirst || wordCount < 50

// ✗ wrong — Swift ?? binds differently than Kotlin ?:
chat?.expandedTimestamps.contains(ts) ?? false || isFirst || wordCount < 50
```

## Adding a new Swift file

Must register in `xcodeApp/YoPt.xcodeproj/project.pbxproj`:
1. `PBXFileReference` entry
2. Entry in `Shared` group children list
3. Two `PBXBuildFile` entries (one per target)
4. Two `PBXSourcesBuildPhase` entries — iOS target `B2`, macOS target `B4`

IDs follow the `AA000000000000000001XX` sequence (increment last two hex digits from the current max).
