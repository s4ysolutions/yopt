import SwiftUI

struct SettingsView: View {
    @ObservedObject var viewModel: ChatViewModel
    @StateObject private var settingsVM = SettingsViewModel()

    var body: some View {
        VStack(spacing: 0) {
#if os(iOS)
            HStack {
                Button(action: { viewModel.showSettings = false }) {
                    Image(systemName: "chevron.left")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(String(localized: "help.back"))

                Text(String(localized: "settings.title"))
                    .font(.title2)
                    .padding(.leading, 8)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)

            Divider()
                .padding(.vertical, 8)

            Picker("", selection: $settingsVM.selectedTab) {
                Text(String(localized: "tab.providers")).tag(0)
                Text(String(localized: "tab.chats")).tag(1)
                Text(String(localized: "tab.global")).tag(2)
                Text(String(localized: "tab.synchronization")).tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 12)
#else
            HStack {
                Text(String(localized: "settings.title"))
                    .font(.title2)
                Spacer()
                Button(action: { viewModel.showSettings = false }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
                .help(String(localized: "help.close"))
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)

            Divider()
                .padding(.vertical, 8)

            Picker("", selection: $settingsVM.selectedTab) {
                Text(String(localized: "tab.providers")).tag(0)
                Text(String(localized: "tab.chats")).tag(1)
                Text(String(localized: "tab.global")).tag(2)
                Text(String(localized: "tab.synchronization")).tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 12)
#endif

            Group {
                switch settingsVM.selectedTab {
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
