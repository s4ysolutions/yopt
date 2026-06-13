import SwiftUI

struct TagFilterSheet: View {
    let allTags: [String]
    let tagCounts: [String: Int]
    @Binding var selectedTags: Set<String>
    let onClear: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing12) {
            Text(String(localized: "tagFilter.title"))
                .font(.headline)

            if allTags.isEmpty {
                Text(String(localized: "tagFilter.empty"))
                    .foregroundColor(.secondary)
            } else {
                ScrollView {
                    FlowLayout(spacing: DesignTokens.spacing4) {
                        ForEach(allTags, id: \.self) { tag in
                            let isOn = selectedTags.contains(tag)
                            Button {
                                if isOn { selectedTags.remove(tag) } else { selectedTags.insert(tag) }
                            } label: {
                                Text("\(tag) (\(tagCounts[tag] ?? 0))")
                                    .font(.caption2)
                                    .padding(.horizontal, DesignTokens.padding8)
                                    .padding(.vertical, DesignTokens.padding4)
                            }
                            .buttonStyle(.bordered)
                            .controlSize(.small)
                            .tint(isOn ? .accentColor : .secondary)
                        }
                    }
                }
                .frame(maxHeight: DesignTokens.modelPickerMaxHeight)
            }

            HStack {
                Button(String(localized: "tagFilter.clear"), action: onClear)
                Spacer()
                Button(String(localized: "tagFilter.done"), action: onDone)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
#if os(macOS)
        .frame(width: DesignTokens.chatSettingsMinWidth)
        .fixedSize(horizontal: false, vertical: true)
#else
        .presentationDetents([.medium])
#endif
    }
}
