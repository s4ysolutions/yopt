import SwiftUI

struct ChatListView: View {
    let chats: [ChatModel]
    let onSelect: (String) -> Void
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(chats) { chat in
                    Button(action: { onSelect(chat.id) }) {
                        Text(chat.title)
                            .lineLimit(1)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    Divider()
                }
            }
        }
        .frame(maxHeight: 300)
        .background(.regularMaterial)
        .cornerRadius(8)
        .shadow(radius: 4)
        .onTapGesture {} // prevent dismissal when interacting
    }
}
