package s4y.yopt

import platform.AppKit.NSViewController
import platform.AppKit.NSView

fun MainViewController(): NSViewController = object : NSViewController(nibName = null, bundle = null) {
    override fun loadView() {
        view = NSView()
    }
}
