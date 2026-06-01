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
    @FocusState private var searchFocused: Bool

    var body: some View {
        VStack(spacing: 8) {
            if columnWidth < 630 {
                // Narrow layout
                VStack(spacing: 8) {
                    HStack {
                        chatSearchField
                            .zIndex(1)
                        actionButtons
                    }
                    .zIndex(1)
                    chatNameField
                }
            } else {
                // Wide layout
                HStack(spacing: 8) {
                    chatSearchField
                        .frame(width: columnWidth * 0.3)
                        .zIndex(1)
                    chatNameField
                    actionButtons
                }
            }
        }
        .overlay {
            Button("") { searchFocused = true }
                .keyboardShortcut("f", modifiers: .command)
                .opacity(0)
                .frame(width: 0, height: 0)
        }
        .background(GeometryReader { geo in
            Color.clear
                .onAppear { columnWidth = geo.size.width }
#if os(macOS)
                .onChange(of: geo.size.width) { _, newWidth in columnWidth = newWidth }
#else
                .onChange(of: geo.size.width) { newWidth in columnWidth = newWidth }
#endif
        })
    }

    private var chatListView: some View {
        ChatListView(
            chats: filteredChats,
            onSelect: { id in
                onSelectChat(id)
                chatSearchQuery = ""
                chatDropdownExpanded = false
            },
            onDismiss: { chatDropdownExpanded = false }
        )
    }

    private var chatSearchField: some View {
        HStack(spacing: 4) {
            TextField("Search...", text: $chatSearchQuery)
                .textFieldStyle(.plain)
                .focused($searchFocused)
#if os(macOS)
                .onChange(of: chatSearchQuery) { chatDropdownExpanded = true }
#else
                .onChange(of: chatSearchQuery) { _ in chatDropdownExpanded = true }
#endif
            Button {
                chatDropdownExpanded.toggle()
            } label: {
                Image(systemName: "chevron.down")
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
        .background(
            GeometryReader { geo in
                Color.clear
                    .onAppear { searchFieldHeight = geo.size.height }
#if os(macOS)
                    .onChange(of: geo.size.height) { _, h in searchFieldHeight = h }
#else
                    .onChange(of: geo.size.height) { h in searchFieldHeight = h }
#endif
            }
        )
#if os(iOS)
        .background(Color(uiColor: .systemBackground))
#endif
        // Overlay (not popover): macOS popover steals key focus on open,
        // breaking incremental typing in the search field.
        .overlay(alignment: .topLeading) {
            if chatDropdownExpanded {
                chatListView
                    .fixedSize(horizontal: false, vertical: true)
                    .offset(y: searchFieldHeight + 4)
                    .zIndex(10)
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
                Image(systemName: "plus")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("New Chat")

            if allChatsCount > 1 {
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help("Delete Chat")
            }

            Button(action: onChatSettings) {
                Image(systemName: "slider.horizontal.3")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Chat Settings")

            Button(action: onSettings) {
                Image(systemName: "gear")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Settings")
        }
    }
}
