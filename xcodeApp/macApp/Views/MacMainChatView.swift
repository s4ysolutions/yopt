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

    /// Controls the chat-search dropdown overlay.
    /// Local state only — the dropdown is ephemeral UI, not persisted.
    @State private var chatDropdownExpanded = false

    // ───────────────────────────────────────────────────────────
    // MARK: - Derived State
    // ───────────────────────────────────────────────────────────

    /// Human-readable label for the currently selected model.
    ///
    /// Format: `"ProviderName: ModelName"` when a provider is identifiable,
    /// falling back to just `"ModelName"`, or `"Select Model"` when nothing
    /// is selected. This is a computed property rather than stored state because
    /// it derives purely from `viewModel.models` + `viewModel.selectedModel`,
    /// both of which update reactively from Kotlin flows.
    private var selectedModelLabel: String {
        guard let sel = viewModel.models.first(where: { $0.id == viewModel.selectedModel }) else {
            return "Select Model"
        }
        let provName = viewModel.providers.first { $0.id == sel.providerId }?.name
        if let pn = provName { return "\(pn): \(sel.officialName)" }
        return sel.officialName
    }

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

    /// The primary two-pane layout for the chat interface.
    ///
    /// Uses `SplitView` (a custom draggable divider, not NSSplitView) so we
    /// own the persistence and animation. The split fraction is stored at
    /// `@AppStorage("mainSplitFraction")` — synchronous on launch, no first-frame
    /// flicker. See `SplitView.swift` for the drag-absolute-Y mechanism.
    ///
    /// **Top pane (collapsible)** — `VStack` of:
    /// 1. `HeaderView` (chat search, rename, action buttons)
    /// 2. `Divider`
    /// 3. `PromptAreaView` (input, send/cancel, model picker, error)
    ///
    ///   The entire top area gets a rounded rectangle background with a subtle
    ///   accent tint (`DesignTokens.topAreaBackground`), visually grouping the
    ///   controls as a cohesive input region.
    ///
    /// **Bottom pane (scrollable)** — chat history in reverse chronological
    ///   order (newest first), each rendered as a `ResponseCardView`.
    ///   Empty state shows a placeholder icon + hint.
    ///   Background uses `.dotGridBackground()` — a subtle dot pattern from
    ///   DesignTokens — for visual texture.
    ///
    private var mainContent: some View {
        SplitView {
            // ═══════════════════════════════════════════════════
            // TOP PANE: Header + Prompt Area
            // ═══════════════════════════════════════════════════
            VStack(spacing: 0) {
                // ── Header ────────────────────────────────────
                // Chat search/dropdown, inline rename, action buttons
                // (new chat, delete, chat settings, global settings).
                // `.zIndex(1)` is critical — the dropdown overlay inside
                // HeaderView must render above the PromptAreaView sibling
                // that follows in the VStack.
                HeaderView(
                    chatSearchQuery: $viewModel.chatSearchQuery,
                    chatDropdownExpanded: $chatDropdownExpanded,
                    chatName: $viewModel.chatName,
                    filteredChats: viewModel.filteredChats,
                    allChatsCount: viewModel.allChats.count,
                    onCreateNew: viewModel.createNewChat,
                    onDelete: viewModel.deleteCurrentChat,
                    onChatSettings: { viewModel.showChatSettings = true },
                    onSettings: { viewModel.showSettings = true },
                    onSelectChat: viewModel.selectChat
                )
                .padding(.horizontal, DesignTokens.sectionPadding)
                .padding(.top, DesignTokens.sectionPadding)
                .zIndex(1)

                Divider()
                    .padding(.vertical, DesignTokens.sectionPadding)

                // ── Prompt Input Area ─────────────────────────
                // Text editor, send/cancel button, model picker,
                // error state display. Drives the `EditorHeightKey`
                // preference that SplitView uses to compute the
                // true content minimum (chrome + editor min height).
                PromptAreaView(
                    prompt: $viewModel.prompt,
                    loading: $viewModel.loading,
                    selectedModelName: selectedModelLabel,
                    selectedModelId: viewModel.selectedModel,
                    models: viewModel.models,
                    modelsEmpty: viewModel.models.isEmpty,
                    error: viewModel.error,
                    onSend: viewModel.send,
                    onCancel: viewModel.cancelSend,
                    onSelectModel: viewModel.selectModel,
                    onOpenSettings: { viewModel.showSettings = true }
                )
                .padding(.horizontal, DesignTokens.sectionPadding)
                .padding(.bottom, DesignTokens.sectionPadding)
            }
            // Visual grouping: rounded rect with subtle accent tint.
            .background(RoundedRectangle(cornerRadius: DesignTokens.topAreaCornerRadius).fill(DesignTokens.topAreaBackground))
            // Outer padding matches the window's natural chrome inset.
            // These are outside the background so the visual container
            // appears "floating" inside the window, not edge-to-edge.
            .padding(.horizontal, 12)
            .padding(.top, 8)
            .padding(.bottom, 4)
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
                        Text("Send a prompt to get started")
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
                            .padding(.horizontal, 12)
                            .padding(.top, i == 0 ? 0 : DesignTokens.cardVerticalPadding)
                            .padding(.bottom, i == history.count - 1 ? 0 : DesignTokens.cardVerticalPadding)
                        }
                    }
                }
            }
            // Bottom padding gives clearance from the SplitView divider handle.
            .padding(.bottom, 8)
            // Decorative dot-grid background for visual depth.
            .dotGridBackground()
        }
        .background(Color(nsColor: .windowBackgroundColor))
    }
}
