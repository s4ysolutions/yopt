import SwiftUI
import AppKit
import ComposeApp

struct MacMainChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var chatDropdownExpanded = false
    @State private var idealTopHeight: CGFloat? = nil

    private func seedIfReady(_ h: CGFloat) {
        guard idealTopHeight == nil, h > 0, viewModel.splitFractionLoaded else { return }
        idealTopHeight = h * CGFloat(max(0.2, min(0.8, viewModel.splitFraction)))
    }

    private var selectedModelLabel: String {
        guard let sel = viewModel.models.first(where: { $0.id == viewModel.selectedModel }) else {
            return "Select Model"
        }
        let provName = viewModel.providers.first { $0.id == sel.providerId }?.name
        if let pn = provName { return "\(pn): \(sel.officialName)" }
        return sel.officialName
    }

    var body: some View {
        Group {
            if viewModel.showSettings {
                SettingsView(viewModel: viewModel)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(nsColor: .windowBackgroundColor))
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            } else {
                mainContent
                    .transition(.move(edge: .leading).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: viewModel.showSettings)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay {
            Button("", action: { viewModel.showSettings = true })
                .keyboardShortcut(",", modifiers: .command)
                .opacity(0)
            Button("", action: viewModel.createNewChat)
                .keyboardShortcut("n", modifiers: .command)
                .opacity(0)
            if viewModel.showSettings {
                Button("", action: { viewModel.showSettings = false })
                    .keyboardShortcut(.escape, modifiers: [])
                    .opacity(0)
            }
        }
        .sheet(isPresented: $viewModel.showChatSettings) {
            if let chat = viewModel.currentChat {
                ChatSettingsView(
                    isPresented: $viewModel.showChatSettings,
                    chat: chat,
                    onSave: { title, instr, labels in
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

    private var mainContent: some View {
        GeometryReader { container in
            let available = container.size.height
        VSplitView {
            // Top: header + prompt
            VStack(spacing: 0) {
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
            .background(RoundedRectangle(cornerRadius: DesignTokens.topAreaCornerRadius).fill(DesignTokens.topAreaBackground))
            .padding(.horizontal, 12)
            .padding(.top, 8)
            .padding(.bottom, 4)
            .frame(minHeight: 120, idealHeight: idealTopHeight ?? (available * CGFloat(viewModel.splitFraction)))
            .onChange(of: available) { _, h in seedIfReady(h) }
            .onChange(of: viewModel.splitFractionLoaded) { _, _ in seedIfReady(available) }
            .background(GeometryReader { topGeo in
                Color.clear
                    .onChange(of: topGeo.size.height) { _, h in
                        // Skip until seeded — avoids echo-saving the restored value.
                        guard available > 0, idealTopHeight != nil else { return }
                        let fraction = Float(max(0.2, min(0.8, h / available)))
                        viewModel.saveSplitFraction(fraction)
                    }
            })

            // Bottom: history
            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                if history.isEmpty {
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
                    LazyVStack(spacing: 0) {
                        ForEach(Array(history.enumerated()), id: \.element.id) { i, entry in
                            let isFirst = i == 0
                            let wordCount = entry.response.split { $0.isWhitespace }.count
                            let respExpanded = (viewModel.currentChat?.expandedTimestamps.contains(entry.timestamp) ?? false)
                                || isFirst
                                || wordCount < 50
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
            .padding(.bottom, 8)
            .dotGridBackground()
            .frame(minHeight: 80)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(nsColor: .windowBackgroundColor))
        }
    }
}
