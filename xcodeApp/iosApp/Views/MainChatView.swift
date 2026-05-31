import SwiftUI
import UIKit
import ComposeApp

struct MainChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var chatDropdownExpanded = false
    @State private var totalHeight: CGFloat = 600

    private var selectedModelLabel: String {
        guard let sel = viewModel.models.first(where: { $0.id == viewModel.selectedModel }) else {
            return "Select Model"
        }
        let provName = viewModel.providers.first { $0.id == sel.providerId }?.name
        if let pn = provName { return "\(pn): \(sel.officialName)" }
        return sel.officialName
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header + prompt area with tinted rounded background
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
            .background(DesignTokens.topAreaBackground)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.topAreaCornerRadius))
            .padding(.horizontal, 12)
            .padding(.top, 8)

            DraggableSplitter(
                fraction: $viewModel.splitFraction,
                totalHeight: totalHeight,
                onFractionChanged: viewModel.saveSplitFraction
            )

            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                LazyVStack(spacing: DesignTokens.cardVerticalPadding * 2) {
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
                        .padding(.horizontal, 12)
                    }
                }
            }
            .dotGridBackground()
        }
        .ignoresSafeArea(.keyboard)
        .background(GeometryReader { geo in
            Color.clear
                .onAppear { totalHeight = geo.size.height }
                .onChange(of: geo.size.height) { totalHeight = $0 }
        })
        .sheet(isPresented: $viewModel.showSettings) {
            SettingsView(viewModel: viewModel)
        }
        .overlay {
            if viewModel.showChatSettings, let chat = viewModel.currentChat {
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
