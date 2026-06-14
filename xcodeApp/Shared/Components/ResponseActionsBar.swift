import SwiftUI

struct ResponseActionsBar: View {
    let isExpanded: Bool
    let showExpand: Bool
    let showMarkdown: Bool
    let onToggleExpand: () -> Void
    let onToggleMarkdown: () -> Void
    let onUseAsPrompt: () -> Void
    let onAppendToPrompt: () -> Void
    let onCopy: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            if showExpand {
                Button(action: onToggleExpand) {
                    Image(systemName: isExpanded ? "chevron.up.circle" : "chevron.down.circle")
                        .actionIcon()
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse" : "Expand")
            } else {
                // Placeholder to keep actions right-aligned
                Color.clear
                    .frame(width: DesignTokens.actionBarHeight, height: DesignTokens.actionBarHeight)
            }

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
                Image(systemName: "checkmark.rectangle")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.useAsPrompt"))

            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.rectangle")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.appendToPrompt"))

            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.copy"))
        }
        .padding(.horizontal, DesignTokens.padding8)
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
                    Image(systemName: isExpanded ? "chevron.up.circle" : "chevron.down.circle")
                        .actionIcon()
                        .opacity(DesignTokens.opacity70)
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse Prompt" : "Expand Prompt")
            }
            Spacer()
            Button(action: onUseAsPrompt) {
                Image(systemName: "checkmark.rectangle")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.useAsPrompt"))
            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.rectangle")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.appendToPrompt"))
            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .actionIcon()
            }
            .buttonStyle(.plain)
            .help(String(localized: "help.copy"))
        }
        .padding(.horizontal, DesignTokens.padding8)
        .frame(height: DesignTokens.actionBarHeight)
    }
}
