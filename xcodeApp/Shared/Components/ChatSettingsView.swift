import SwiftUI

struct ChatSettingsView: View {
    @Binding var isPresented: Bool
    let chat: ChatModel
    let onSave: (String, String, [String]) -> Void

    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false
    @FocusState private var focusedField: FocusField?

    enum FocusField {
        case instructions
        case tags
    }

    private func save() {
        onSave(chat.title, instructions, labels)
        isPresented = false
    }

    var body: some View {
        VStack(spacing: 16) {
#if os(macOS)
            HStack {
                Text("Chat Settings")
                    .font(.title2)
                    .fontWeight(.semibold)
                Spacer()
                Button(action: { isPresented = false }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
                .help("Close (Esc)")
            }
            .padding(.bottom, 8)
            Divider()
#else
            Text("Chat Settings")
                .font(.headline)
#endif

            VStack(alignment: .leading, spacing: 6) {
                Text("Instructions")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .textCase(.uppercase)
                    .foregroundColor(.secondary)
                TextEditor(text: $instructions)
                    .font(.body)
                    .scrollContentBackground(.hidden)
                    .frame(height: 100)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                    )
                    .focused($focusedField, equals: .instructions)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("Tags")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .textCase(.uppercase)
                    .foregroundColor(.secondary)
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .focused($focusedField, equals: .tags)
            }

            Spacer()
                .frame(height: 8)

            HStack(spacing: 12) {
                Button("Cancel") { isPresented = false }
                    .keyboardShortcut(.cancelAction)
                Spacer()
                Button("Save") { save() }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut("s", modifiers: [.command])
            }
        }
        .padding(16)
#if os(macOS)
        .frame(minWidth: 420, idealWidth: 460)
        .fixedSize(horizontal: false, vertical: true)
        .presentationDragIndicator(.hidden)
#else
        .presentationDetents([.medium])
#endif
        .sheet(isPresented: $showAddTag) {
            AddTagDialog(isPresented: $showAddTag, tags: $labels)
        }
        .onAppear {
            instructions = chat.instructions
            labels = chat.labels
            #if os(macOS)
            focusedField = .instructions
            #endif
        }
    }
}
