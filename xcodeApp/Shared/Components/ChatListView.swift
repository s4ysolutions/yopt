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
            RoundedRectangle(cornerRadius: 8)
                .fill(DesignTokens.cardBackground)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(chats) { chat in
                        Button(action: { onSelect(chat.id) }) {
                            Text(chat.title)
                                .lineLimit(1)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(hoveredId == chat.id ? Color.accentColor.opacity(0.12) : Color.clear)
                                .cornerRadius(4)
                        }
                        .buttonStyle(.plain)
                        .frame(maxWidth: .infinity)
                        .onHover { hoveredId = $0 ? chat.id : nil }
                    }
                }
                .padding(4)
            }
            .scrollContentBackground(.hidden)
        }
        .frame(idealWidth: 260, maxWidth: 360, idealHeight: idealH, maxHeight: idealH)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.secondary.opacity(0.2), lineWidth: 1)
        )
        .shadow(radius: 6, y: 3)
    }
}
