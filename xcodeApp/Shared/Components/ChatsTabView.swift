import SwiftUI

struct ChatsTabView: View {
    let chats: [ChatModel]
    let onUpdate: (ChatModel, String, String, [String]) -> Void
    let onDelete: (String) -> Void

    @State private var checkedLabels: Set<String> = []

    private var allLabels: [String] {
        Array(Set(chats.flatMap { $0.labels })).sorted()
    }

    private var filteredChats: [ChatModel] {
        if checkedLabels.isEmpty { return chats }
        return chats.filter { chat in
            checkedLabels.allSatisfy { chat.labels.contains($0) }
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            if !allLabels.isEmpty {
                labelFilterView
            }
            ScrollView {
                LazyVStack(spacing: 4) {
                    ForEach(filteredChats) { chat in
                        ChatEditRowView(chat: chat, onUpdate: onUpdate, onDelete: onDelete)
                    }
                }
            }
            .frame(maxHeight: .infinity)
        }
        .padding(.top, 8)
        .frame(maxHeight: .infinity)
    }

    private var labelFilterView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(0..<allLabels.count, id: \.self) { i in
                    let label = allLabels[i]
                    let checked = checkedLabels.contains(label)
                    Button(action: {
                        if checked { checkedLabels.remove(label) }
                        else { checkedLabels.insert(label) }
                    }) {
                        Text(label)
                            .font(.caption2)
                    }
                    .buttonStyle(.bordered)
                    .tint(checked ? .accentColor : nil)
                    .controlSize(.small)
                }
            }
        }
    }
}

struct ChatEditRowView: View {
    let chat: ChatModel
    let onUpdate: (ChatModel, String, String, [String]) -> Void
    let onDelete: (String) -> Void

    @State private var editing = false
    @State private var title: String = ""
    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if editing {
                TextField("Title", text: $title)
                    .textFieldStyle(.roundedBorder)
                TextEditor(text: $instructions)
                    .font(.caption)
                    .frame(height: 60)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.secondary.opacity(0.3)))
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                HStack {
                    Button("Save") {
                        onUpdate(chat, title, instructions, labels)
                        editing = false
                    }
                    Button("Cancel") { editing = false }
                }
                .overlay {
                    AddTagDialog(isPresented: $showAddTag, tags: $labels)
                }
            } else {
                Text(chat.title).font(.body)
                if !chat.labels.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(chat.labels, id: \.self) { label in
                            Text(label)
                                .font(.caption2)
                                .foregroundColor(.accentColor)
                        }
                    }
                }
                if !chat.instructions.isEmpty {
                    Text(chat.instructions.prefix(100))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                HStack {
                    Button("Edit") { editing = true; title = chat.title; instructions = chat.instructions; labels = chat.labels }
                    Button("Delete", role: .destructive) { onDelete(chat.id) }
                }
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.05))
        .cornerRadius(8)
    }
}
