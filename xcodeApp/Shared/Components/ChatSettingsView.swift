import SwiftUI

struct ChatSettingsView: View {
    @Binding var isPresented: Bool
    let chat: ChatModel
    let onSave: (String, String, [String]) -> Void

    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false

    var body: some View {
        VStack(spacing: 12) {
            Text("Chat Settings")
                .font(.headline)

            VStack(alignment: .leading, spacing: 4) {
                Text("Instructions")
                    .font(.caption)
                TextEditor(text: $instructions)
                    .font(.body)
                    .scrollContentBackground(.hidden)
                    .frame(height: 100)
                    .overlay(
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                    )
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Tags")
                    .font(.caption)
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
            }

            HStack {
                Button("Cancel") { isPresented = false }
                Spacer()
                Button("Save") {
                    onSave(chat.title, instructions, labels)
                    isPresented = false
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .frame(minWidth: 400, idealWidth: 440)
        .sheet(isPresented: $showAddTag) {
            AddTagDialog(isPresented: $showAddTag, tags: $labels)
        }
        .onAppear {
            instructions = chat.instructions
            labels = chat.labels
        }
    }
}
