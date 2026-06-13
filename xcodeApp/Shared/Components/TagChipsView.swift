import SwiftUI

struct TagChipsView: View {
    @Binding var tags: [String]
    let onAddTag: () -> Void

    var body: some View {
        FlowLayout(spacing: 4) {
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
            Button(action: onAddTag) {
                HStack(spacing: 2) {
                    Text("+")
                        .font(.caption2)
                    Text(String(localized: "chatSettings.addTag"))
                        .font(.caption2)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }
}

struct FlowLayout: Layout {
    let spacing: CGFloat

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        var size = CGSize.zero
        var lineWidth: CGFloat = 0
        let width = proposal.width ?? .infinity

        for subview in subviews {
            let subviewSize = subview.sizeThatFits(.unspecified)

            if lineWidth + subviewSize.width + spacing > width, lineWidth > 0 {
                size.height += subviewSize.height + spacing
                lineWidth = 0
            }

            lineWidth += subviewSize.width + spacing
            size.width = max(size.width, lineWidth)
        }

        if lineWidth > 0 {
            size.height += subviews.last?.sizeThatFits(.unspecified).height ?? 0
        }

        return size
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        var x = bounds.minX
        var y = bounds.minY
        let width = bounds.width

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if x + size.width + spacing > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += size.height + spacing
            }

            subview.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += size.width + spacing
        }
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
            Text(String(localized: "chatSettings.addTag"))
                .font(.headline)
            TextField("Tag", text: $newTagText)
                .textFieldStyle(.roundedBorder)
                .onSubmit { commit() }
            HStack {
                Button(String(localized: "button.cancel")) {
                    newTagText = ""
                    isPresented = false
                }
                Button(String(localized: "button.save"), action: commit)
                    .buttonStyle(.borderedProminent)
                    .disabled(newTagText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
        .padding()
#if os(macOS)
        .frame(width: 260)
        .fixedSize(horizontal: false, vertical: true)
#else
        .presentationDetents([.height(180)])
#endif
    }
}
