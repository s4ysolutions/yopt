import SwiftUI
import ComposeApp

struct ContentView: NSViewControllerRepresentable {
    func makeNSViewController(context: Context) -> NSViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateNSViewController(_ nsViewController: NSViewController, context: Context) {}
}
