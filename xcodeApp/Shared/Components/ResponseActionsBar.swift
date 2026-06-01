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
        HStack(spacing: 8) {
            Button(action: onToggleExpand) {
                Image(isExpanded ? "collapse_content" : "expand_content")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(isExpanded ? "Collapse" : "Expand")

            Spacer()

            if isExpanded {
                Button(action: onToggleMarkdown) {
                    Image(showMarkdown ? "raw_on" : "markdown")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(showMarkdown ? "Switch to Raw" : "Switch to Markdown")
            }

            Button(action: onUseAsPrompt) {
                Image("add_box")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")

            Button(action: onAppendToPrompt) {
                Image("shadow_add")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")

            Button(action: onCopy) {
                Image("content_copy")
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
        HStack(spacing: 8) {
            if showExpand {
                Button(action: onToggleExpand) {
                    Image(isExpanded ? "collapse_content" : "expand_content")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse Prompt" : "Expand Prompt")
            }
            Spacer()
            Button(action: onUseAsPrompt) {
                Image("add_box")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")
            Button(action: onAppendToPrompt) {
                Image("shadow_add")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")
            Button(action: onCopy) {
                Image("content_copy")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 8)
        .frame(height: DesignTokens.actionBarHeight)
    }
}
