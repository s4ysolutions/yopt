import SwiftUI
import UIKit
import ComposeApp

struct MainChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var chatDropdownExpanded = false

    private var selectedModelLabel: String {
        guard let sel = viewModel.models.first(where: { $0.id == viewModel.selectedModel }) else {
            return "Select Model"
        }
        let provName = viewModel.providers.first { $0.id == sel.providerId }?.name
        if let pn = provName { return "\(pn): \(sel.officialName)" }
        return sel.officialName
    }

    var body: some View {
        SplitView {
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
                    onSelectChat: viewModel.selectChat,
                    selectedTags: $viewModel.selectedTags,
                    allTags: viewModel.allTags,
                    tagCounts: viewModel.tagCounts
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
            .padding(.horizontal, DesignTokens.padding12)
            .padding(.top, DesignTokens.padding8)
            .padding(.bottom, DesignTokens.padding8)
        } bottom: {
            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                if history.isEmpty {
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
                                onCopy: { UIPasteboard.general.string = $0 },
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
            .dotGridBackground()
        }
        .ignoresSafeArea(.keyboard)
        .background(Color(uiColor: .systemBackground))
        .sheet(isPresented: $viewModel.showSettings) {
            SettingsView(viewModel: viewModel)
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
}
