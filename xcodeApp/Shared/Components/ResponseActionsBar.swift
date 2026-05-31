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
                Image(systemName: isExpanded ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help(isExpanded ? "Collapse" : "Expand")

            Spacer()

            if isExpanded {
                Button(action: onToggleMarkdown) {
                    Image(systemName: showMarkdown ? "doc.plaintext" : "doc.text.magnifyingglass")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .help(showMarkdown ? "Switch to Raw" : "Switch to Markdown")
            }

            Button(action: onUseAsPrompt) {
                Image(systemName: "arrow.up.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")

            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")

            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 4)
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
                    Image(systemName: isExpanded ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse Prompt" : "Expand Prompt")
            }
            Spacer()
            Button(action: onUseAsPrompt) {
                Image(systemName: "arrow.up.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")
            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")
            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 4)
        .frame(height: DesignTokens.actionBarHeight)
    }
}
