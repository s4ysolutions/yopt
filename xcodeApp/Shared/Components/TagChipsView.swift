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

    var body: some View {
        Group {
            if isPresented {
                VStack(spacing: 12) {
                    Text("Add Tag")
                        .font(.headline)
                    TextField("Tag", text: $newTagText)
                        .textFieldStyle(.roundedBorder)
                    HStack {
                        Button("Cancel") {
                            newTagText = ""
                            isPresented = false
                        }
                        Button("Add") {
                            let trimmed = newTagText.trimmingCharacters(in: .whitespaces)
                            if !trimmed.isEmpty && !tags.contains(trimmed) {
                                tags.append(trimmed)
                            }
                            newTagText = ""
                            isPresented = false
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding()
                .frame(width: 300)
                .background(.regularMaterial)
                .cornerRadius(12)
                .shadow(radius: 8)
            }
        }
    }
}
