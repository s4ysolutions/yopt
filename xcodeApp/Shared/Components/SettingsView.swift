import SwiftUI

struct SettingsView: View {
    @ObservedObject var viewModel: ChatViewModel
    @StateObject private var settingsVM = SettingsViewModel()
    @State private var selectedTab = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: { viewModel.showSettings = false }) {
                    Image(systemName: "chevron.left")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help("Back")

                Text("Settings")
                    .font(.title2)
                    .padding(.leading, 8)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)

            Divider()
                .padding(.vertical, 8)

            Picker("", selection: $selectedTab) {
                Text("Providers").tag(0)
                Text("Chats").tag(1)
                Text("Global").tag(2)
                Text("Synchronization").tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 12)

            Group {
                switch selectedTab {
                case 0:
                    ProvidersTabView(settingsVM: settingsVM)
                case 1:
                    ChatsTabView(chats: settingsVM.chats, onUpdate: settingsVM.updateChat, onDelete: settingsVM.deleteChat)
                        .padding(.horizontal, 12)
                case 2:
                    GlobalTabView(settingsVM: settingsVM)
                        .padding(.horizontal, 12)
                case 3:
                    ExportTabView(settingsVM: settingsVM)
                        .padding(.horizontal, 12)
                default:
                    EmptyView()
                }
            }
        }
        // ── Hidden keyboard shortcut ─────────────────────────
        // Escape closes the settings view (local key equivalent,
        // not a menu-bar command — kept as hidden button overlay).
        // `.opacity(0)` preserves keyboard focus (unlike `.hidden()`).
        .overlay {
            Button("", action: { viewModel.showSettings = false })
                .keyboardShortcut(.escape, modifiers: [])
                .opacity(0)
        }
    }
}
