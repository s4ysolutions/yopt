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
                        .font(.body)
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

            Picker("Tab", selection: $selectedTab) {
                Text("Providers").tag(0)
                Text("Chats").tag(1)
                Text("Global").tag(2)
                Text("Export").tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 12)

            TabView(selection: $selectedTab) {
                ProvidersTabView(settingsVM: settingsVM)
                    .tag(0)
                ChatsTabView(chats: settingsVM.chats, onUpdate: settingsVM.updateChat, onDelete: settingsVM.deleteChat)
                    .tag(1)
                GlobalTabView(settingsVM: settingsVM)
                    .tag(2)
                ExportTabView(settingsVM: settingsVM)
                    .tag(3)
            }
            .tabViewStyle(.automatic)
            .padding(.horizontal, 12)
        }
        .frame(minWidth: 500, minHeight: 400)
    }
}
