import SwiftUI

struct HeaderView: View {
    @Binding var chatSearchQuery: String
    @Binding var chatDropdownExpanded: Bool
    @Binding var chatName: String
    let filteredChats: [ChatModel]
    let allChatsCount: Int
    let onCreateNew: () -> Void
    let onDelete: () -> Void
    let onChatSettings: () -> Void
    let onSettings: () -> Void
    let onSelectChat: (String) -> Void

    @State private var columnWidth: CGFloat = 0
    @State private var searchFieldHeight: CGFloat = 44

    var body: some View {
        VStack(spacing: 8) {
            if columnWidth < 630 {
                // Narrow layout
                VStack(spacing: 8) {
                    HStack {
                        chatSearchField
                        actionButtons
                    }
                    chatNameField
                }
            } else {
                // Wide layout
                HStack(spacing: 8) {
                    chatSearchField
                        .frame(width: columnWidth * 0.3)
                    chatNameField
                    actionButtons
                }
            }
        }
        .background(GeometryReader { geo in
            Color.clear
                .onAppear { columnWidth = geo.size.width }
                .onChange(of: geo.size.width) { columnWidth = $0 }
        })
    }

    private var chatSearchField: some View {
        HStack(spacing: 4) {
            TextField("Search...", text: $chatSearchQuery)
                .textFieldStyle(.plain)
                .onChange(of: chatSearchQuery) { _ in chatDropdownExpanded = true }
            Button {
                chatDropdownExpanded.toggle()
            } label: {
                Image("keyboard_arrow_down")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Chat List")
        }
        .padding(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
        )
        .background(GeometryReader { geo in
            Color.clear
                .onAppear { searchFieldHeight = geo.size.height }
                .onChange(of: geo.size.height) { searchFieldHeight = $0 }
        })
        .overlay(alignment: .top) {
            if chatDropdownExpanded {
                ChatListView(
                    chats: filteredChats,
                    onSelect: { id in
                        onSelectChat(id)
                        chatSearchQuery = ""
                        chatDropdownExpanded = false
                    },
                    onDismiss: { chatDropdownExpanded = false }
                )
                .offset(y: searchFieldHeight)
                .zIndex(100)
            }
        }
    }

    private var chatNameField: some View {
        TextField("Chat Name", text: $chatName)
            .textFieldStyle(.plain)
            .padding(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
            )
    }

    private var actionButtons: some View {
        HStack(spacing: 8) {
            Button(action: onCreateNew) {
                Image("add")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("New Chat")

            if allChatsCount > 1 {
                Button(action: onDelete) {
                    Image("delete_forever")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help("Delete Chat")
            }

            Button(action: onChatSettings) {
                Image("tune")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Chat Settings")

            Button(action: onSettings) {
                Image("settings")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Settings")
        }
    }
}
