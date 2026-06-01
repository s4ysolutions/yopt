import SwiftUI

struct ResponseActionsBar: View {
    let isExpanded: Bool
    let showMarkdown: Bool
    let onToggleExpand: () -> Void
    let onToggleMarkdown: () -> Void
    let onUseAsPrompt: () -> Void
    let onAppendToPrompt: () -> Void
    let onCopy: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onToggleExpand) {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(isExpanded ? "Collapse" : "Expand")

            Spacer()

            if isExpanded {
                Button(action: onToggleMarkdown) {
                    Image(systemName: showMarkdown ? "chevron.left.forwardslash.chevron.right" : "doc.richtext")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(showMarkdown ? "Switch to Raw" : "Switch to Markdown")
            }

            Button(action: onUseAsPrompt) {
                Image(systemName: "square.and.pencil")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")

            Button(action: onAppendToPrompt) {
                Image(systemName: "text.badge.plus")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")

            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 8)
        .frame(height: DesignTokens.actionBarHeight)
    }
}

struct PromptActionsBar: View {
    let isExpanded: Bool
    let showExpand: Bool
    let onToggleExpand: () -> Void
    let onUseAsPrompt: () -> Void
    let onAppendToPrompt: () -> Void
    let onCopy: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            if showExpand {
                Button(action: onToggleExpand) {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse Prompt" : "Expand Prompt")
            }
            Spacer()
            Button(action: onUseAsPrompt) {
                Image(systemName: "square.and.pencil")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")
            Button(action: onAppendToPrompt) {
                Image(systemName: "text.badge.plus")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")
            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 8)
        .frame(height: DesignTokens.actionBarHeight)
    }
}
