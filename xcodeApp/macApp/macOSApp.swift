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
