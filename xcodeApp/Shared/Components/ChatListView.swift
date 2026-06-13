import SwiftUI

struct ChatListView: View {
    let chats: [ChatModel]
    let onSelect: (String) -> Void
    let onDismiss: () -> Void

    @State private var hoveredId: String? = nil

    private static let rowHeight: CGFloat = 30
    private static let maxVisible = 10

    var body: some View {
        let count = chats.count
        let idealH = min(CGFloat(count) * Self.rowHeight + 8, CGFloat(Self.maxVisible) * Self.rowHeight + 8)
        ZStack {
            // Opaque base layer — guarantees the dropdown is not see-through,
            // regardless of how the ScrollView paints its own backing.
            RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8)
                .fill(DesignTokens.cardBackground)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(chats) { chat in
                        Button(action: { onSelect(chat.id) }) {
                            Text(chat.title)
                                .lineLimit(1)
                                .padding(.horizontal, DesignTokens.padding12)
                                .padding(.vertical, DesignTokens.padding6)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(hoveredId == chat.id ? Color.accentColor.opacity(DesignTokens.opacity12) : Color.clear)
                                .cornerRadius(DesignTokens.cornerRadius4)
                        }
                        .buttonStyle(.plain)
                        .frame(maxWidth: .infinity)
                        .onHover { hoveredId = $0 ? chat.id : nil }
                    }
                }
                .padding(DesignTokens.padding4)
            }
            .scrollContentBackground(.hidden)
        }
        .frame(idealWidth: DesignTokens.chatListMinWidth, maxWidth: DesignTokens.chatListMaxWidth, idealHeight: idealH, maxHeight: idealH)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8))
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8)
                .stroke(Color.secondary.opacity(DesignTokens.opacity20), lineWidth: 1)
        )
        .shadow(radius: 6, y: 3)
    }
}
