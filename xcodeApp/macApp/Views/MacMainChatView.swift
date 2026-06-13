import SwiftUI
import AppKit
import ComposeApp

/// Root chat view for macOS — the primary content of the app window.
///
/// **Role in the view hierarchy:**
/// ```
/// macOSApp → MacMainChatView
/// ```
/// macOSApp holds the `ChatViewModel` as a `@StateObject` and passes it in.
/// Unlike the iOS version (`MainChatView`), macOS uses a `SplitView` to divide the
/// window into a top pane (header + prompt area) and a scrollable bottom pane (history).
///
/// **Layout strategy:**
/// - When `viewModel.showSettings == true`, renders `SettingsView` full-window
///   with a slide-right transition.
/// - Otherwise renders `mainContent`: a `SplitView` whose top pane contains
///   `HeaderView` (chat search/dropdown, rename, action buttons) stacked above
///   `PromptAreaView` (text input, send, model picker).
///   The bottom pane is a scrollable list of `ResponseCardView` entries.
///
/// **Key dependencies:**
/// - `ChatViewModel` — @ObservedObject, owned by macOSApp.
///   See xcodeApp/CLAUDE.md: "Never instantiate it in child views".
/// - `SplitView` — persisted fraction (`mainSplitFraction`) via @AppStorage, no flicker.
/// - `DesignTokens` — all visual constants (paddings, radii, colors, icon sizes).
/// - `KotlinBridge` — indirect through ChatViewModel; views never call KotlinBridge directly.
///
/// **Keyboard shortcuts:**
/// - `Cmd+,` (Settings) and `Cmd+N` (New Chat) are handled at the app level
///   via `CommandGroup` in `macOSApp.swift`.
/// - `Escape` (close settings) is a local hidden button overlay — it's
///   sheet-dismissal logic and doesn't belong in the menu bar.
///
struct MacMainChatView: View {
    // ───────────────────────────────────────────────────────────
    // MARK: - State
    // ───────────────────────────────────────────────────────────

    /// Single ViewModel instance for the entire macOS app lifecycle.
    /// Owned by macOSApp, passed via ObservedObject.
    @ObservedObject var viewModel: ChatViewModel

    // ───────────────────────────────────────────────────────────
    // MARK: - Body
    // ───────────────────────────────────────────────────────────

    var body: some View {
        Group {
            if viewModel.showSettings {
                // Settings screen — replaces chat content entirely.
                // Uses `.trailing` slide to indicate "pushed from the side".
                SettingsView(viewModel: viewModel)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(nsColor: .windowBackgroundColor))
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            } else {
                // Main chat UI — header + prompt + history in a split pane.
                // Uses `.leading` slide so the transition feels directional.
                mainContent
                    .transition(.move(edge: .leading).combined(with: .opacity))
            }
        }
        // 250ms easeInOut matches native macOS pane-slide timing.
        .animation(.easeInOut(duration: 0.25), value: viewModel.showSettings)
        .frame(maxWidth: .infinity, maxHeight: .infinity)

        // ── Chat Settings Sheet ───────────────────────────────
        // A modal sheet for editing the current chat's title, instructions,
        // and labels. Triggered by the "slider" button in HeaderView.
        // Note: `Chat` and `KotlinLong` conversions bridge the KMP ⇄ Swift boundary.
        .sheet(isPresented: $viewModel.showChatSettings) {
            if let chat = viewModel.currentChat {
                ChatSettingsView(
                    isPresented: $viewModel.showChatSettings,
                    chat: chat,
                    onSave: { title, instr, labels in
                        // Map Swift models back to KMP types for the bridge call.
                        let updated = Chat(
                            id: chat.id, title: title, instructions: instr,
                            defaultModelId: chat.defaultModelId, labels: labels,
                            expandedTimestamps: Set(chat.expandedTimestamps.map { KotlinLong(longLong: $0) }),
                            history: chat.history.map { $0.toKotlinEntry() }
                        )
                        Task { try? await KotlinBridge.shared.chatsUseCase.update(chat: updated) }
                    }
                )
            }
        }
    }

    // ───────────────────────────────────────────────────────────
    // MARK: - Main Content (SplitView layout)
    // ───────────────────────────────────────────────────────────

    private var mainContent: some View {
        SplitView {
            ChatTopPaneView(viewModel: viewModel)
        } bottom: {
            // ═══════════════════════════════════════════════════
            // BOTTOM PANE: Scrollable Chat History
            // ═══════════════════════════════════════════════════
            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                if history.isEmpty {
                    // ── Empty State ───────────────────────────
                    // Shown when the current chat has no entries yet.
                    // Centered icon + "Send a prompt to get started" hint.
                    VStack(spacing: 12) {
                        Image(systemName: "text.bubble")
                            .font(.system(size: 36))
                            .foregroundColor(.secondary.opacity(0.35))
                        Text(String(localized: "prompt.emptyChatMessage"))
                            .font(.body)
                            .foregroundColor(.secondary.opacity(0.5))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
                } else {
                    // ── Response Cards ────────────────────────
                    // Each entry in the chat's history (reversed = newest first)
                    // rendered as a ResponseCardView with expand/collapse control,
                    // markdown toggle, copy/paste actions, and model attribution.
                    LazyVStack(spacing: 0) {
                        ForEach(Array(history.enumerated()), id: \.element.id) { i, entry in
                            let isFirst = i == 0

                            // Expand logic per xcodeApp/CLAUDE.md:
                            // `(chat?.expandedTimestamps.contains(ts) ?? false) || isFirst || wordCount < 50`
                            // Swift `??` precedence requires parentheses on the lhs operand.
                            let wordCount = entry.response.split { $0.isWhitespace }.count
                            let respExpanded = (viewModel.currentChat?.expandedTimestamps.contains(entry.timestamp) ?? false)
                                || isFirst
                                || wordCount < 50

                            // Resolve the model label for this specific entry.
                            // Iterates through viewModel.models to find the model
                            // used when this response was generated, then formats
                            // it as "ProviderName: ModelName" (same pattern as
                            // selectedModelLabel above).
                            let entryModel = viewModel.models.first { $0.id == entry.modelId }
                            let entryProviderName = viewModel.providers.first { $0.id == entryModel?.providerId }?.name
                            let entryModelLabel = entryProviderName != nil ? "\(entryProviderName!): \(entry.modelName)" : entry.modelName

                            ResponseCardView(
                                entry: entry,
                                isFirst: isFirst,
                                currentPrompt: viewModel.prompt,
                                currentModelId: viewModel.selectedModel,
                                isExpanded: respExpanded,
                                chatId: viewModel.currentChatId ?? "",
                                onToggleExpand: { viewModel.toggleEntryExpanded(timestamp: entry.timestamp, chatId: viewModel.currentChatId ?? "") },
                                onToggleMarkdown: { viewModel.toggleEntryMarkdown(timestamp: entry.timestamp, chatId: viewModel.currentChatId ?? "") },
                                onUseAsPrompt: viewModel.useAsPrompt,
                                onAppendToPrompt: viewModel.appendToPrompt,
                                onCopy: { NSPasteboard.general.clearContents(); NSPasteboard.general.setString($0, forType: .string) },
                                onRemove: { viewModel.removeEntry(at: (viewModel.currentChat?.history.count ?? 0) - 1 - i, chatId: viewModel.currentChatId ?? "") },
                                modelName: entryModelLabel
                            )
                            .padding(.horizontal, DesignTokens.padding12)
                            .padding(.top, i == 0 ? 0 : DesignTokens.cardVerticalPadding)
                            .padding(.bottom, i == history.count - 1 ? 0 : DesignTokens.cardVerticalPadding)
                        }
                    }
                }
            }
            // Bottom padding gives clearance from the SplitView divider handle.
            .padding(.bottom, DesignTokens.padding8)
            // Decorative dot-grid background for visual depth.
            .dotGridBackground()
        }
        .background(Color(nsColor: .windowBackgroundColor))
    }
}
