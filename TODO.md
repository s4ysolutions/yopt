# TODO

## Project Status

- ✅ `./gradlew :composeApp:run` — desktop (JVM) run confirmed working (2026-05-25)
- ✅ `./gradlew :composeApp:linkDebugExecutableMacosArm64` — macOS native run confirmed working (2026-05-25); fix: `setActivationPolicy(NSApplicationActivationPolicyRegular)` required for unbundled binary
- ✅ `./gradlew :composeApp:packageMacosApp` — macOS `.app` bundle confirmed working (2026-05-25); includes icon, Info.plist, proper bundle ID; xcodeApp macOS target replaced by this Gradle task

## macOS Text Selection / Copy — Investigation

**Status:** Broken. macOS native target has no text selection in history items. All other platforms work.

**Symptom:** `SelectionContainer` crashes on Cmd+C. `BasicTextField(readOnly=true)` silently fails to copy. Both paths unusable on macOS native.

**Workaround (current):** `SelectionCapability.macos.kt` sets `supportsTextSelection = false`. History items render as plain `Text()` (raw) or `MarkdownResponse()` (markdown) — no selection wrapper, no BasicTextField. Copy-to-clipboard buttons still work via `LocalClipboardManager.setText()` (the "push" path to NSPasteboard functions; the "pull" path — user-initiated copy during text selection — is what crashes).

**Root cause:** Skiko clipboard bridge (Kotlin/Native → `NSPasteboard`) is broken at the Skiko level for clipboard read/getText. Compose 1.11.0. The K/N ObjC interop layer calling `NSPasteboard.general` likely hits a memory management or threading issue inside Skiko's internal clipboard abstraction.

**Commit:** `4309032` — "fix(macos): disable broken text selection, use BasicTextField on other platforms"

### Platform clipboard backends

| Platform | Backend | SelectionContainer | BasicTextField (r/o) | clipboardManager.setText |
|----------|---------|:---:|:---:|:---:|
| Android | Android ClipboardManager | ✅ | ✅ | ✅ |
| Desktop JVM | AWT `java.awt.datatransfer.Clipboard` | ✅ | ✅ | ✅ |
| iOS | UIPasteboard (via K/N interop) | ✅ | ✅ | ✅ |
| macOS native | NSPasteboard (via Skiko/K/N) | ❌ Crash | ❌ Silent fail | ✅ |
| WASM/JS | `navigator.clipboard` | ✅ | ✅ | ✅ |

### Investigation checklist

- [ ] **Reproduce the crash** — enable `supportsTextSelection = true` on macOS, run, select text in history, press Cmd+C. Capture stack trace to confirm crash origin (Skiko clipboard vs Compose selection manager vs K/N ObjC bridge).

- [ ] **Isolate SelectionContainer from BasicTextField** — test them independently. Does SelectionContainer crash on mouse selection alone (without Cmd+C)? Does it crash only on copy? Does `BasicTextField(readOnly=true)` render selection but fail on Cmd+C, or fail to select at all?

- [ ] **Test direct NSPasteboard access** — write a minimal K/N test calling `NSPasteboard.generalPasteboard.clearContents()` + `setString("test", NSPasteboardTypeString)` and read back via `stringForType(NSPasteboardTypeString)`. Does this work outside of Skiko/Compose? If yes, we can build our own clipboard bridge.

- [ ] **Test Cmd+C interception** — use `onPreviewKeyEvent` or native `NSEvent.addLocalMonitorForEventsMatchingMask` to intercept Cmd+C. If SelectionContainer itself doesn't crash on mouse selection (only on Cmd+C), intercept copy and call our own NSPasteboard write.

- [ ] **Check Compose Multiplatform upgrade** — test with Compose 1.12.0+ when available. Check Skiko release notes for macOS clipboard fixes.

- [ ] **Test desktop JVM on macOS** — `./gradlew :composeApp:run` on macOS uses AWT clipboard backend, which works. Confirm it's specifically K/N native target that's broken.

### Solution options

#### Option A: Direct NSPasteboard (expect/actual clipboard)

Bypass Skiko clipboard entirely. Write an expect/actual `Clipboard` interface with `setText(text: String)` and `getText(): String?`. On macOS, implement directly via K/N ObjC interop to NSPasteboard. Replace all `LocalClipboardManager.setText()` calls with this interface. Re-enable `SelectionContainer` only if crash is from Cmd+C (not from selection rendering).

- **Risk:** Skiko may still crash if it internally hooks into the same NSPasteboard. Unknown whether crash is from K/N ObjC interop issues or from Skiko-level conflicts.
- **Effort:** Low (1 file expect + 6 actuals). ~50 lines per platform.
- **Pro:** Keeps everything in Compose/Kotlin. Copy buttons become reliable.
- **Con:** Only fixes copy button path (already works). Doesn't fix text selection unless crash is isolated to copy action.

#### Option B: Cmd+C interception + custom copy

Enable `SelectionContainer` on macOS. Intercept Cmd+C key events before they reach Skiko clipboard. Copy selected text to NSPasteboard directly. If crash is only on the copy action (not on selection rendering), this fixes everything.

- **Risk:** If crash is inside `SelectionContainer`'s selection rendering/setup (not just Cmd+C), this won't work. Crash could be in NSView focus handling or NSTextInputClient protocol.
- **Effort:** Medium. Need NSEvent monitor + NSPasteboard interop. ~100 lines macOS-specific.
- **Pro:** Full text selection if it works.
- **Con:** May not work if crash is deeper than clipboard. Fragile — future Compose updates could break or fix it.

#### Option C: NSTextView as response widget (expect/actual composable)

Replace the response area composable with a native NSTextView on macOS. Use K/N ObjC interop to create and manage the NSView. Similar to how `UIKitView` works on iOS (but no official macOS equivalent in Compose).

- **Risk:** No official API for embedding NSViews in Compose Multiplatform macOS windows. Would need to reverse-engineer the NSWindow/NSView hierarchy.
- **Effort:** High. Deep K/N ObjC interop. May not work at all.
- **Pro:** Native macOS text selection, copy, services menu, lookup, etc.
- **Con:** Huge maintenance burden. Fragile across Compose versions. Loses markdown rendering (NSTextView doesn't render markdown natively).

#### Option D: Move macOS target from Compose to SwiftUI (AppKit)

Rewrite the macOS app in SwiftUI + AppKit. Keep shared Kotlin code for domain/usecases/infra (expose via KMP framework). UI becomes SwiftUI-native.

**Markdown in SwiftUI:**
- `swift-markdown-ui` (gonzalezreal, 2.5k stars) — rich Markdown rendering, but text selection limited to within single paragraph (issue #264, open since Oct 2023, no fix). Cross-paragraph selection broken.
- `MarkdownView` (LiYanan2004) — WebView-based, full text selection works. Less native feel.
- Native `Text(AttributedString)` — iOS 15+/macOS 13+ has `AttributedString(from:options:)` with markdown support. Full native text selection via `.textSelection(.enabled)`. Limited markdown: no tables, no fenced code blocks with syntax highlighting.
- Custom `NSTextView` in SwiftUI via `NSViewRepresentable` — full native text, full selection, but markdown must be rendered to NSAttributedString manually.

**Tradeoffs:**
| Aspect | Compose (current) | SwiftUI |
|--------|:---:|:---:|
| Text selection | ❌ Broken | ✅ Native, works |
| Markdown rendering | ✅ multiplatform-markdown-renderer | ⚠️ Fragmented, each lib has limits |
| Shared business logic | ✅ KMP shared module | ✅ KMP shared module (same) |
| UI code sharing | ✅ commonMain for Android/iOS/Desktop | ❌ macOS-only UI rewrite |
| Bundle size | ~38 MB (Gradle kexe) | ~179 MB (Swift+SwiftUI+K/N) |
| Debugging | CLI only | Xcode debugger, Instruments, lldb |
| Maintenance | 1 UI codebase | 2 UI codebases (Compose + SwiftUI) |
| App Store | Not supported | Required for distribution |

- **Risk:** High. Dual UI codebases. SwiftUI markdown libraries also have selection issues.
- **Effort:** Very high. Complete macOS UI rewrite.
- **Pro:** Native text selection. Xcode tooling. App Store distribution path.
- **Con:** Loses UI code sharing. 2x UI maintenance. ~4x larger binary. SwiftUI markdown selection also non-trivial.

#### Option E: Wait for Compose/Skiko fix

File a YouTrack issue with a reproducer. Wait for JetBrains to fix the Skiko macOS clipboard bridge.

- **Risk:** Unknown timeline. Could be months or years.
- **Effort:** Minimal (file issue, maintain workaround).
- **Pro:** No code changes needed. Fix propagates to all Compose macOS users.
- **Con:** No improvement until fixed. Users stuck with button-only copy.

### Recommended next steps

1. **Reproduce + isolate the crash** (Investigation checklist items 1-2) — determine if crash is in Cmd+C copy action only, or in selection rendering itself. This determines which solutions are viable.

2. **If crash is Cmd+C only:** Option B (intercept Cmd+C, custom copy via NSPasteboard) is the best fix — low effort, keeps full text selection.

3. **If crash is in selection rendering:** Option A (custom clipboard) for copy-button reliability + wait for JetBrains fix (Option E). Or pursue Option D if native selection is critical for product.

4. **Before considering SwiftUI:** build a prototype SwiftUI view with `NSTextView` + markdown rendering to validate that markdown + text selection actually works end-to-end. The swift-markdown-ui cross-paragraph selection bug suggests this path may not be straightforward either.

## Known Problems

- [ ] **Remove `AuthService.getCredentials` getter** — violates "never mix observable and getter" rule. Replace usage in `SendPromptUseCase` with `observeCredentials().first().find { it.providerId == pid }`. (`shared/.../services/AuthService.kt`, `SendPromptUseCase.kt:21`)

- [ ] **Remove `ModelRepository.getAllModels` getter** — same violation. Replace `getEnabledModels()` in `ManageModelsUseCase` with `observeModels().first().filter { it.enabled }`. (`shared/.../port/ModelRepository.kt:9`)

- [ ] **Move provider API-key URLs to `ProviderDef`** — add `apiKeysUrl: String?` field, populate in `PredefinedProviders`, remove `when (provider.id)` switch from `SettingsDialog.kt:496-504`. Also remove dead `"huggingface"` case.

- [ ] **Update stale `infra/CLAUDE.md`** — remove `OAuthBrowser`, `OAuthClient`, `OAuthRedirectBus`, `PKCE`, `Sha256` from "Infra classes" section (all deleted).

- [ ] **Batch model enable/disable writes** — add `setModelsEnabled(ids: List<String>, enabled: Boolean)` to `ModelRepository` + `ManageModelsUseCase`. Fix All-on/All-off buttons in `SettingsDialog.kt:322-334` to use it.
