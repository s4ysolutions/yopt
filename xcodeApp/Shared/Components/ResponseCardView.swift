import SwiftUI

struct ResponseCardView: View {
    let entry: ResponseEntryModel
    let isFirst: Bool
    let currentPrompt: String
    let currentModelId: String?
    let isExpanded: Bool
    let chatId: String
    let onToggleExpand: () -> Void
    let onToggleMarkdown: () -> Void
    let onUseAsPrompt: (String) -> Void
    let onAppendToPrompt: (String) -> Void
    let onCopy: (String) -> Void
    let onRemove: () -> Void
    let modelName: String?

    @State private var promptExpanded = false
    @State private var promptOverflows = false
    @State private var showRemoveConfirm = false

    private let wordLimit = 50
    private let responsePreviewLength = 200

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Prompt row
            if !isFirst || entry.prompt != currentPrompt || entry.modelId != currentModelId {
                promptSection
                Divider()
            }

            // Response action buttons
            ResponseActionsBar(
                isExpanded: isExpanded,
                showMarkdown: entry.showMarkdown,
                onToggleExpand: onToggleExpand,
                onToggleMarkdown: onToggleMarkdown,
                onUseAsPrompt: { onUseAsPrompt(entry.response) },
                onAppendToPrompt: { onAppendToPrompt(entry.response) },
                onCopy: { onCopy(entry.response) }
            )

            // Response content
            if isExpanded {
                if entry.showMarkdown {
                    MarkdownResponseView(content: entry.response)
                } else {
                    Text(entry.response)
                        .font(.system(.caption, design: .monospaced))
                        .textSelection(.enabled)
                }
            } else {
                Text(entry.response.prefix(responsePreviewLength))
                    .font(.system(.caption, design: .monospaced))
                    .lineLimit(3)
            }

            // Bottom bar
            bottomBar
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: DesignTokens.cardCornerRadius)
                .fill(DesignTokens.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.cardCornerRadius)
                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
        )
        .padding(.vertical, 4)
        .confirmationDialog("Remove this entry from history?", isPresented: $showRemoveConfirm, titleVisibility: .visible) {
            Button(String(localized: "button.remove"), role: .destructive, action: onRemove)
            Button(String(localized: "button.cancel"), role: .cancel) {}
        }
    }

    private var promptSection: some View {
        VStack(alignment: .leading, spacing: 2) {
            PromptActionsBar(
                isExpanded: promptExpanded,
                showExpand: promptOverflows,
                onToggleExpand: { promptExpanded.toggle() },
                onUseAsPrompt: { onUseAsPrompt(entry.prompt) },
                onAppendToPrompt: { onAppendToPrompt(entry.prompt) },
                onCopy: { onCopy(entry.prompt) }
            )
            Text(entry.prompt)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(promptExpanded ? nil : 1)
        }
        .padding(4)
        .background(Color.secondary.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var bottomBar: some View {
        HStack(spacing: 8) {
            Button(action: { showRemoveConfirm = true }) {
                Image(systemName: "trash")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.removeFromHistory"))

            Text(formatTimestamp(entry.timestamp))
                .font(.caption2)
                .foregroundColor(.secondary)

            if let name = modelName {
                Text(" \u{00B7} \(name)")
                    .font(.caption2)
                    .foregroundColor(.accentColor)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                Spacer()
            }

            Text(" \u{00B7} \(formatDuration(entry.durationMs))")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }

    private func formatTimestamp(_ ms: Int64) -> String {
        let now = Date().timeIntervalSince1970 * 1000
        let diff = now - Double(ms)
        switch diff {
        case ..<60_000: return "now"
        case ..<3_600_000: return "\(Int(diff / 60_000))m ago"
        case ..<86_400_000: return "\(Int(diff / 3_600_000))h ago"
        default: return "\(Int(diff / 86_400_000))d ago"
        }
    }

    private func formatDuration(_ ms: Int64) -> String {
        switch ms {
        case ..<1000: return "\(ms)ms"
        case ..<10000: return "\(ms / 1000).\((ms % 1000) / 100)s"
        default: return "\(ms / 1000)s"
        }
    }
}
