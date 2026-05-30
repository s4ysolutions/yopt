---
name: kmp-xcode
description: KMP Compose Multiplatform + Xcode integration — macOS window pattern, Compose Resources bundling, window position save/restore, Gradle vs Xcode tradeoffs
---

# KMP Compose Multiplatform + Xcode Integration

Covers macOS target Xcode build setup. iOS works out of the box (`ComposeUIViewController`); macOS needs specific patterns documented here.

## Source files

| File | Path | Purpose |
|------|------|---------|
| MacOsApp.kt | `composeApp/src/macosArm64Main/kotlin/s4y/yopt/MacOsApp.kt` | `createAppWindow()`, `main()` — Compose window + NSApp lifecycle |
| MainViewController.kt (macOS) | `composeApp/src/macosArm64Main/kotlin/s4y/yopt/MainViewController.kt` | **Dead code** — returns empty NSView (see Root cause 1) |
| macOSApp.swift | `xcodeApp/macApp/macOSApp.swift` | Swift `@main App` — NSApplicationDelegateAdaptor, frame save/restore |
| ContentView.swift (macOS) | `xcodeApp/macApp/ContentView.swift` | **Dead code** — NSViewControllerRepresentable wrapping empty MainViewController |
| Info.plist | `xcodeApp/macApp/Info.plist` | macOS bundle metadata |
| YoPt.entitlements | `xcodeApp/macApp/YoPt.entitlements` | Sandbox=false, network client=true |
| SelectionCapability.kt | `composeApp/src/commonMain/kotlin/s4y/yopt/ui/SelectionCapability.kt` | `expect val supportsTextSelection`, `expect val needsCopyKeyInterceptor` |
| SelectionCapability.macos.kt | `composeApp/src/macosArm64Main/kotlin/s4y/yopt/ui/SelectionCapability.macos.kt` | `actual val needsCopyKeyInterceptor = true` |
| SelectionCapability (other) | `composeApp/src/{desktop,android,ios,wasmJs}Main/.../ui/SelectionCapability.{platform}.kt` | `actual val needsCopyKeyInterceptor = false` |
| build.gradle.kts | `composeApp/build.gradle.kts` | `runMacosNative`, `packageMacosApp`, `createDebugAppBundle` tasks |

## Root cause 1: Empty NSView (no Compose content)

Xcode macOS apps use `NSViewControllerRepresentable` → `MainViewController`. On iOS, `ComposeUIViewController` works. On macOS native, it does not exist — Compose for macOS uses `Window` composable which creates its own `NSWindow`.

**Wrong pattern** (white window — these files still exist in the repo as dead code):

```swift
// ContentView.swift (UNUSED — kept as reference for what NOT to do)
struct ContentView: NSViewControllerRepresentable {
    func makeNSViewController(context: Context) -> NSViewController {
        MainViewControllerKt.MainViewController()  // empty NSView on macOS
    }
    func updateNSViewController(_ nsViewController: NSViewController, context: Context) {}
}
```

```kotlin
// MainViewController.kt (macOS) (UNUSED — kept as reference)
fun MainViewController(): NSViewController = object : NSViewController(nibName = null, bundle = null) {
    override fun loadView() {
        view = NSView()  // empty!
    }
}
```

Both files are dead code — the actual entry point uses the pattern below. They remain in the repo as documentation of what was tried and why it failed.

**Correct pattern** — Compose creates its own window, Swift manages NSApplication lifecycle:

```swift
// macOSApp.swift
import SwiftUI
import AppKit
import ComposeApp

private let defaults = UserDefaults.standard
private let keyX = "window_x"
private let keyY = "window_y"
private let keyW = "window_w"
private let keyH = "window_h"
private var closeObserver: NSObjectProtocol?

final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        MacOsAppKt.createAppWindow()
        waitForWindow()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }

    func applicationWillTerminate(_ notification: Notification) {
        if let observer = closeObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    private func waitForWindow() {
        if let window = NSApp.windows.first {
            restoreFrame(window)
            closeObserver = NotificationCenter.default.addObserver(
                forName: NSWindow.willCloseNotification,
                object: window,
                queue: .main
            ) { _ in
                saveFrame(window)
            }
        } else {
            // Compose Window {} creates NSWindow asynchronously from Swift's perspective.
            // Poll until it appears; 0.05s is enough on all tested hardware.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in
                self?.waitForWindow()
            }
        }
    }
}

private func restoreFrame(_ window: NSWindow) {
    let w = defaults.double(forKey: keyW)
    let h = defaults.double(forKey: keyH)
    if w > 0, h > 0 {
        let x = defaults.double(forKey: keyX)
        let y = defaults.double(forKey: keyY)
        window.setFrame(NSRect(x: x, y: y, width: w, height: h), display: true)
    }
}

private func saveFrame(_ window: NSWindow) {
    let frame = window.frame
    defaults.set(frame.origin.x, forKey: keyX)
    defaults.set(frame.origin.y, forKey: keyY)
    defaults.set(frame.size.width, forKey: keyW)
    defaults.set(frame.size.height, forKey: keyH)
}

@main
struct macOSApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene {
        Settings { EmptyView() }
    }
}
```

```kotlin
// MacOsApp.kt — Kotlin macOS entry point
package s4y.yopt

import androidx.compose.ui.window.Window
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSWindow
import platform.CoreGraphics.CGRectMake
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private class AppDelegate : NSObject(), NSApplicationDelegateProtocol {
    override fun applicationShouldTerminateAfterLastWindowClosed(app: NSApplication) = true

    @OptIn(ExperimentalForeignApi::class)
    override fun applicationWillTerminate(notification: platform.Foundation.NSNotification) {
        saveWindowFrame()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun saveWindowFrame() {
    try {
        val window = NSApplication.sharedApplication().windows.firstOrNull() as? NSWindow ?: return
        val p = NSUserDefaults.standardUserDefaults
        val f = window.frame
        f.useContents {
            p.setDouble(origin.x, "window_x")
            p.setDouble(origin.y, "window_y")
            p.setDouble(size.width, "window_w")
            p.setDouble(size.height, "window_h")
        }
    } catch (_: Exception) {}
}

@OptIn(ExperimentalForeignApi::class)
private fun setupMenu(app: NSApplication) {
    val mainMenu = NSMenu()
    val appMenuItem = NSMenuItem()
    mainMenu.addItem(appMenuItem)
    val appMenu = NSMenu()
    appMenuItem.submenu = appMenu
    appMenu.addItem(
        NSMenuItem(
            title = "Quit YoPt",
            action = NSSelectorFromString("terminate:"),
            keyEquivalent = "q",
        )
    )
    app.mainMenu = mainMenu
}

@OptIn(ExperimentalForeignApi::class)
private fun restoreWindowFrame() {
    val window = NSApplication.sharedApplication().windows.firstOrNull() as? NSWindow ?: return
    val p = NSUserDefaults.standardUserDefaults
    val w = p.doubleForKey("window_w")
    val h = p.doubleForKey("window_h")
    if (w > 0.0 && h > 0.0) {
        val x = p.doubleForKey("window_x")
        val y = p.doubleForKey("window_y")
        window.setFrame(CGRectMake(x, y, w, h), display = true)
    }
}

fun createAppWindow() {
    val app = NSApplication.sharedApplication()
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.delegate = AppDelegate()
    setupMenu(app)
    Window(title = "YoPt") {
        App()  // your root composable
    }
    restoreWindowFrame()  // sync — NSWindow created synchronously, no visual jump
    app.activateIgnoringOtherApps(true)
}

// For Gradle runMacosNative / packageMacosApp
fun main() {
    createAppWindow()
    NSApplication.sharedApplication().run()
}
```

Key points:
- `createAppWindow()` must NOT call `app.run()` — SwiftUI `@main App` manages the Cocoa run loop implicitly.
- `main()` calls `createAppWindow()` + `app.run()` — used by Gradle tasks only.
- SwiftUI body is `Settings { EmptyView() }` — no competing window.
- Frame restore on Kotlin side is synchronous after `Window {}` returns (NSWindow created synchronously on macOS native). Swift side uses async polling because `Window {}` hasn't returned yet when `applicationDidFinishLaunching` fires.
- `@OptIn(ExperimentalForeignApi::class)` required on all functions touching ObjC interop (`NSUserDefaults`, `NSWindow.frame`, `CGRectMake`).
- `@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")` required on `AppDelegate` — K/N ObjC interop changes parameter names from `notification` to `_notification`.

## Root cause 2: Missing Compose Resources

`DefaultMacOsResourceReader` (Compose 1.11.0+) resolves from:
```
NSBundle.mainBundle.resourcePath/compose-resources/<path>
```

Resources inside `ComposeApp.framework/composeResources/...` are NOT found — reader only checks main bundle, not embedded frameworks.

### Fix: Copy Compose Resources build phase

Add Run Script build phase to macOS target:

```bash
SRC="$SRCROOT/../composeApp/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"
DST="$TARGET_BUILD_DIR/$CONTENTS_FOLDER_PATH/Resources/compose-resources/composeResources/yopt.composeapp.generated.resources"

if [ -d "$SRC" ]; then
  mkdir -p "$DST"
  cp -R "$SRC"/ "$DST"
  echo "Copied compose resources to $DST"
else
  echo "warning: Compose resources not found at $SRC"
fi
```

The module qualifier for this project is `yopt.composeapp.generated.resources` (defined in `composeApp/build.gradle.kts` as `composeResourcesModulePath`).

`SRC` contains the composeResources directory contents directly (values/, drawable/, etc.). The copy adds the module qualifier subdirectory as required by `DefaultMacOsResourceReader`.

### Resource locations

| Stage | Location |
|-------|----------|
| Source | `composeApp/src/commonMain/composeResources/` |
| Gradle-prepared | `composeApp/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources/` |
| Inside ComposeApp.xcframework | `ComposeApp.xcframework/.../ComposeApp.framework/composeResources/<qualifier>/` |
| **Runtime expected** | `<app>/Contents/Resources/compose-resources/composeResources/<qualifier>/` |

Gradle `runMacosNative` (via `createDebugAppBundle`) and `packageMacosApp` copy resources into the `.app` bundle. Xcode needs the explicit copy phase above.

## Window position/size save and restore

### Save path by platform

| Platform | Close button | Cmd+Q / menu Quit |
|----------|-------------|-------------------|
| Gradle (`applicationWillTerminate`) | No (window already closed) | Yes |
| Xcode (`NSWindow.willCloseNotification`) | Yes | Yes |

On Gradle, `applicationWillTerminate` fires after window closes — `NSApp.windows.first` returns nil, so `saveWindowFrame()` is a no-op. Xcode adds `NSWindow.willCloseNotification` observer which fires BEFORE close.

Both sides must use identical `UserDefaults` keys (`"window_x"`, `"window_y"`, `"window_w"`, `"window_h"`) and both must use `UserDefaults.standard` — NOT `suiteName:` (see pitfalls).

### Known pitfalls

| Pitfall | Symptom | Fix |
|---------|---------|-----|
| `NSUserDefaults(suiteName:)` returns null at runtime | `NullPointerException` in key access | Use `standardUserDefaults` (K/N interop bug: compiler marks non-null but ObjC can return nil) |
| Accessing `NSApplication.windows` from background thread | `EXC_BAD_ACCESS` | Restore frame synchronously in `createAppWindow()` — runs on main thread |
| `LaunchedEffect` + `delay(100)` for frame restore | Visible position jump on launch | Restore sync after `Window {}` returns — NSWindow is created synchronously |
| `setFrameOrigin` only | Window size not restored | Use `setFrame(CGRectMake(x, y, w, h), display: true)` |
| Wrapping editable text fields in key interceptor | Cannot type letter 'c' in prompt/chat name fields | `onPreviewKeyEvent` intercepts ALL `Key.C` keydown events — only wrap read-only response areas |

## Text Selection and Copy on macOS Native

Compose Multiplatform 1.11.0 macOS native (arm64) backend: `isCopyKeyEvent()` in `androidx.compose.foundation.text.selection` throws `NotImplementedError`. Called by `SelectionManager` on any key event reaching a `SelectionContainer` or `BasicTextField(readOnly=true)`.

**What works:**
- Text selection rendering — `SelectionContainer` visually selects text correctly
- `clipboardManager.setText()` — the "push" path to clipboard functions (used by copy buttons)
- `BasicTextField(readOnly=true)` — renders correctly, text is selectable

**What doesn't work (all caused by missing `isCopyKeyEvent`):**
- Cmd+C in `SelectionContainer` → `NotImplementedError` → crash
- Cmd+C in `BasicTextField(readOnly=true)` → `NotImplementedError` → silent failure
- Edit → Copy menu item → same path
- `KeyEvent.isMetaPressed` / `KeyEvent.isCtrlPressed` → same unimplemented native functions; accessing them also throws

**Dead ends (verified, don't retry):**
- `LocalTextToolbar.showMenu` callback — never called on macOS native; right-click Copy bypasses TextToolbar entirely
- `NSApplication.sendAction(NSSelectorFromString("copy:"), to: null, from: null)` — returns false; Compose NSView not in Cocoa responder chain
- `SelectionContainer` — no public selection getter; selection lives in `internal SelectionManager`

**Fix pattern** (implemented in `MainScreen.kt`, gated by `needsCopyKeyInterceptor`):

Two views behave differently because only `BasicTextField(TextFieldValue)` exposes selection to app code:

```kotlin
// expect val needsCopyKeyInterceptor: Boolean
// macosArm64: true  |  all other targets: false

// Raw view — BasicTextField with TextFieldValue keeps selection readable
var rawFieldValue by remember(entry.response) { mutableStateOf(TextFieldValue(entry.response)) }
var showMarkdownCopyWarning by remember { mutableStateOf(false) }

if (needsCopyKeyInterceptor) {
    Box(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.C) {
                if (entry.showMarkdown) {
                    // SelectionContainer gives no selection access; warn user to right-click.
                    showMarkdownCopyWarning = true
                } else {
                    val sel = rawFieldValue.selection
                    val copied = if (sel.collapsed) entry.response
                        else entry.response.substring(sel.min, sel.max)
                    clipboard.setText(AnnotatedString(copied))
                }
                true
            } else false
        }
    ) {
        // responseBody() — uses rawFieldValue for BasicTextField branch
    }
    if (showMarkdownCopyWarning) {
        AlertDialog(
            onDismissRequest = { showMarkdownCopyWarning = false },
            confirmButton = { TextButton(onClick = { showMarkdownCopyWarning = false }) { Text("OK") } },
            title = { Text("Copy not supported") },
            text = { Text("Cmd+C does not work in Markdown view. Right-click the selection and choose Copy.") },
        )
    }
}
```

Key points:
- Gate interceptor to macOS via `needsCopyKeyInterceptor` — other platforms handle Cmd+C natively with real partial-selection copy
- `BasicTextField` raw view: pass `TextFieldValue` (not `String`); `onValueChange` preserves `entry.response` text while accepting selection changes → `rawFieldValue.selection` stays current
- Markdown view uses `SelectionContainer` — no public selection API → show alert; right-click Copy still works (framework path, bypasses `isCopyKeyEvent`)
- Only wrap read-only response areas — wrapping editable text fields would prevent typing the letter 'c' (the interceptor consumes ALL `Key.C` keydown events)
- The `SelectionCapability.kt` expect/actual files live in `composeApp/src/{platform}Main/kotlin/s4y/yopt/ui/`

**Tracking:** JetBrains Compose Multiplatform — `isCopyKeyEvent` missing native implementation for macOS native target. Once fixed, remove `needsCopyKeyInterceptor` gating and the `onPreviewKeyEvent` workaround.

## Xcode project structure

### Required targets
1. **KMP Build** (aggregate) — Run Script: `./gradlew :composeApp:assembleComposeAppDebugXCFramework`
2. **YoPt macOS** (native app) — depends on KMP Build

### macOS target build phases (in order)
1. Frameworks — ComposeApp.xcframework
2. Sources — macOSApp.swift (ContentView.swift is dead code, not compiled)
3. Resources — Assets.xcassets
4. Copy Compose Resources (Run Script) — see Root cause 2

### plist requirements

Actual keys in `xcodeApp/macApp/Info.plist`:

| Key | Value | Required? |
|-----|-------|-----------|
| `NSHighResolutionCapable` | `true` | Yes — Retina support |
| `LSMinimumSystemVersion` | `$(MACOSX_DEPLOYMENT_TARGET)` | Yes — minimum macOS version |
| `CFBundleIdentifier` | `$(PRODUCT_BUNDLE_IDENTIFIER)` | Yes |
| `NSPrincipalClass` | `NSApplication` | Yes — required when using custom `AppDelegate` |
| `CFBundleIconFile` | (empty) | Optional — app icon |
| `CFBundleDevelopmentRegion` | `$(DEVELOPMENT_LANGUAGE)` | Standard |
| `CFBundleExecutable` | `$(EXECUTABLE_NAME)` | Standard |
| `CFBundleName` | `$(PRODUCT_NAME)` | Standard |
| `CFBundlePackageType` | `$(PRODUCT_BUNDLE_PACKAGE_TYPE)` | Standard |
| `CFBundleShortVersionString` | `$(MARKETING_VERSION)` | Standard |
| `CFBundleVersion` | `$(CURRENT_PROJECT_VERSION)` | Standard |

### Entitlements

`xcodeApp/macApp/YoPt.entitlements`:
- `com.apple.security.app-sandbox` = `false`
- `com.apple.security.network.client` = `true` (required for LLM API calls)

## Gradle vs Xcode for macOS builds

| | Gradle `runMacosNative` | Gradle `packageMacosApp` | Xcode macOS |
|---|---|---|---|
| Build type | Debug | Release | Debug (configurable) |
| Entry point | `MacOsApp.kt` `main()` | `MacOsApp.kt` `main()` | Swift `@main` → `createAppWindow()` |
| Resources | `createDebugAppBundle` copies | `packageMacosApp` copies | Xcode build phase copies |
| Format | `.app` bundle | `.app` bundle | `.app` bundle |
| Debugging | CLI only | CLI only | Xcode debugger, Instruments, lldb |
| Signing/notarization | Manual | Manual | Xcode built-in |
| App Store | Not supported | Not supported | Required |
| CI | Easy (`gradlew`) | Easy (`gradlew`) | Needs `xcodebuild` |
| Binary size | ~38 MB | ~38 MB | ~179 MB |

Xcode binaries are larger because they link both Swift runtime + SwiftUI AND Kotlin/Native runtime. Gradle `.app` bundles (both debug and release) contain only K/N binaries inside the bundle.

All three produce `.app` bundles. `runDebugExecutableMacosArm64` (the raw kexe, no bundle) is NOT used — Compose 1.11.0 resources require an `.app` bundle for `NSBundle.mainBundle.resourcePath` to resolve.

**Recommendation**: Xcode macOS for daily dev and distribution. Gradle `runMacosNative` for quick CLI iteration. `packageMacosApp` = release snapshot for sharing.

## Common error messages

| Error | Cause |
|-------|-------|
| `MissingResourceException: Missing resource with path: composeResources/...` | Resources not copied to `Contents/Resources/compose-resources/` |
| `Unresolved reference 'ComposeUIViewController'` on macOS | macOS has no `ComposeUIViewController` — use `Window` composable |
| White/empty window | `MainViewController` returned empty `NSView` (wrong pattern) |
| `NullPointerException` in `UserDefaults` access | `NSUserDefaults(suiteName:)` returned null at runtime — use `standardUserDefaults` |
| Window jumps on launch | `LaunchedEffect`+`delay` vs sync restore in `createAppWindow()` |
| `kotlin.NotImplementedError: isCopyKeyEvent` | Compose 1.11.0 macOS native — key event handling not implemented. See Text Selection section for gated workaround. |

## Checklist for new macOS Xcode target

1. Kotlin: `createAppWindow()` function (no `app.run()` — SwiftUI runs the Cocoa run loop)
2. Kotlin: sync `restoreWindowFrame()` after `Window {}` (NSWindow exists synchronously)
3. Kotlin: `saveWindowFrame()` in `applicationWillTerminate` (covers Cmd+Q on Gradle builds)
4. Kotlin: `main()` = `createAppWindow()` + `app.run()` (Gradle entry point ONLY — never called from Xcode)
5. Swift: `@NSApplicationDelegateAdaptor` calling `MacOsAppKt.createAppWindow()`
6. Swift: `NSWindow.willCloseNotification` observer for save on close-button (covers what Kotlin's `applicationWillTerminate` can't on Gradle)
7. Swift: `waitForWindow()` polling — Compose `Window {}` not immediately visible to `NSApp.windows`
8. Swift: body = `Settings { EmptyView() }` — no competing SwiftUI window
9. Xcode: KMP Build aggregate target (runs `assembleComposeAppDebugXCFramework`)
10. Xcode: Copy Compose Resources build phase (with concrete qualifier `yopt.composeapp.generated.resources`)
11. Xcode: ComposeApp.xcframework in Frameworks, Libraries, and Embedded Content
12. Both sides use `UserDefaults.standard` (NOT `suiteName:` — K/N interop bug)
13. Info.plist: `NSPrincipalClass` = `NSApplication` (required for custom AppDelegate)
14. Entitlements: sandbox = false, network client = true
