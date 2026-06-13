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
        VStack(spacing: DesignTokens.spacing8) {
            if !allLabels.isEmpty {
                labelFilterView
            }
            ScrollView {
                LazyVStack(spacing: DesignTokens.spacing4) {
                    ForEach(filteredChats) { chat in
                        ChatEditRowView(chat: chat, onUpdate: onUpdate, onDelete: onDelete)
                    }
                }
            }
            .frame(maxHeight: .infinity)
        }
        .padding(.top, DesignTokens.padding8)
        .frame(maxHeight: .infinity)
    }

    private var labelFilterView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.spacing6) {
                ForEach(0..<allLabels.count, id: \.self) { i in
                    let label = allLabels[i]
                    let checked = checkedLabels.contains(label)
                    Button(action: {
                        if checked { checkedLabels.remove(label) }
                        else { checkedLabels.insert(label) }
                    }) {
                        Text(label)
                            .font(.caption2)
                            .padding(.horizontal, DesignTokens.padding8)
                            .padding(.vertical, DesignTokens.padding4)
                    }
                    .background(checked ? Color.accentColor.opacity(DesignTokens.opacity15) : Color.secondary.opacity(DesignTokens.opacity10))
                    .foregroundColor(checked ? .accentColor : .primary)
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.cornerRadius6))
                }
            }
            .padding(.horizontal, DesignTokens.padding4)
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
        VStack(alignment: .leading, spacing: DesignTokens.spacing4) {
            if editing {
                TextField("Title", text: $title)
                    .textFieldStyle(.roundedBorder)
                TextEditor(text: $instructions)
                    .font(.caption)
                    .frame(height: DesignTokens.textEditorMinHeight)
                    .overlay(RoundedRectangle(cornerRadius: DesignTokens.cornerRadius4).stroke(Color.secondary.opacity(DesignTokens.opacity30)))
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack {
                    Button(String(localized: "button.save")) {
                        onUpdate(chat, title, instructions, labels)
                        editing = false
                    }
                    Button(String(localized: "button.cancel")) { editing = false }
                }
                .sheet(isPresented: $showAddTag) {
                    AddTagDialog(isPresented: $showAddTag, tags: $labels)
                }
            } else {
                Text(chat.title).font(.body)
                if !chat.labels.isEmpty {
                    HStack(spacing: DesignTokens.spacing4) {
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
                    Button(String(localized: "button.edit")) { editing = true; title = chat.title; instructions = chat.instructions; labels = chat.labels }
                    Button(String(localized: "button.delete"), role: .destructive) { onDelete(chat.id) }
                }
            }
        }
        .padding(DesignTokens.padding8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(DesignTokens.opacity05))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8))
    }
}
