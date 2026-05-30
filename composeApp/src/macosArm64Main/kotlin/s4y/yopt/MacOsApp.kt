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
        App()
    }
    restoreWindowFrame()
    app.activateIgnoringOtherApps(true)
}

fun main() {
    createAppWindow()
    NSApplication.sharedApplication().run()
}
