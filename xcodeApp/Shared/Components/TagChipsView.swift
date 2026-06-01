import SwiftUI

struct TagChipsView: View {
    @Binding var tags: [String]
    let onAddTag: () -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(tags, id: \.self) { tag in
                    Button(action: { tags.removeAll { $0 == tag } }) {
                        HStack(spacing: 2) {
                            Text(tag)
                                .font(.caption2)
                            Text("\u{00D7}")
                                .font(.caption2)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
                Button("+ Add Tag", action: onAddTag)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        }
        .frame(height: 32)
    }
}

struct AddTagDialog: View {
    @Binding var isPresented: Bool
    @Binding var tags: [String]

    @State private var newTagText = ""

    private func commit() {
        let trimmed = newTagText.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty && !tags.contains(trimmed) {
            tags.append(trimmed)
        }
        newTagText = ""
        isPresented = false
    }

    var body: some View {
        VStack(spacing: 12) {
            Text("Add Tag")
                .font(.headline)
            TextField("Tag", text: $newTagText)
                .textFieldStyle(.roundedBorder)
                .onSubmit { commit() }
            HStack {
                Button("Cancel") {
                    newTagText = ""
                    isPresented = false
                }
                Button("Add", action: commit)
                    .buttonStyle(.borderedProminent)
                    .disabled(newTagText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
        .padding()
        .frame(width: 260)
    }
}
