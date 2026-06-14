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
    @State private var showRemoveConfirm = false

    private let wordLimit = 50
    private let responsePreviewLength = 200

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.spacing4) {
            // Prompt row
            if !isFirst || entry.prompt != currentPrompt || entry.modelId != currentModelId {
                promptSection
                Divider()
            }

            // Response action buttons
            ResponseActionsBar(
                isExpanded: isExpanded,
                showExpand: !isFirst,
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
        .padding(DesignTokens.padding4)
        .background(
            RoundedRectangle(cornerRadius: DesignTokens.cardCornerRadius)
                .fill(DesignTokens.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.cardCornerRadius)
                .stroke(Color.secondary.opacity(DesignTokens.opacity30), lineWidth: 1)
        )
        .padding(.vertical, DesignTokens.padding4)
        .confirmationDialog("Remove this entry from history?", isPresented: $showRemoveConfirm, titleVisibility: .visible) {
            Button(String(localized: "button.remove"), role: .destructive, action: onRemove)
            Button(String(localized: "button.cancel"), role: .cancel) {}
        }
    }

    private var promptSection: some View {
        // Trim so leading/trailing blank lines don't waste space.
        let trimmedPrompt = entry.prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        // <=3 lines: plain text, no expand button. >3 lines: 1-line ellipsis + expand.
        let collapsible = trimmedPrompt.components(separatedBy: "\n").count > 3
        let showFull = !collapsible || promptExpanded
        // Collapsed: glue all lines into one long string so the single line shows max content.
        let collapsedText = trimmedPrompt.split(separator: "\n").joined(separator: " ")
        return VStack(alignment: .leading, spacing: DesignTokens.spacing2) {
            PromptActionsBar(
                isExpanded: promptExpanded,
                showExpand: collapsible,
                onToggleExpand: { promptExpanded.toggle() },
                onUseAsPrompt: { onUseAsPrompt(entry.prompt) },
                onAppendToPrompt: { onAppendToPrompt(entry.prompt) },
                onCopy: { onCopy(entry.prompt) }
            )
            // Cancel the section's horizontal padding so the expand chevron lines up
            // with the response action bar's chevron.
            .padding(.horizontal, -DesignTokens.padding4)
            Text(showFull ? trimmedPrompt : collapsedText)
                .font(.body)
                .foregroundColor(.secondary)
                .lineLimit(showFull ? nil : 1)
        }
        .padding(DesignTokens.padding4)
        .background(Color.secondary.opacity(DesignTokens.opacity10))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.cornerRadius8))
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
