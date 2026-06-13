import ComposeApp
import SwiftUI

struct ChatTopPaneView: View {
    @ObservedObject var viewModel: ChatViewModel

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
                onRename: viewModel.updateChatName,
                selectedTags: $viewModel.selectedTags,
                allTags: viewModel.allTags,
                tagCounts: viewModel.tagCounts
            )
            .padding(.horizontal, DesignTokens.sectionPadding)
            .padding(.top, DesignTokens.sectionPadding)
            .zIndex(1)
            .background(GeometryReader { proxy in
                Color.clear.preference(key: ChatTopPanelMinHeight.self, value: proxy.size.height)
            })

            Divider()
                .padding(.vertical, DesignTokens.sectionPadding)
                .background(GeometryReader { proxy in
                    Color.clear.preference(key: ChatTopPanelMinHeight.self, value: proxy.size.height)
                })

            PromptAreaView(
                prompt: $viewModel.prompt,
                loading: $viewModel.loading,
                selectedModelName: selectedModelLabel,
                selectedModelId: viewModel.selectedModel,
                models: viewModel.models,
                providers: viewModel.providers,
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
        .background(GeometryReader { proxy in
            // bottom padding
            Color.clear.preference(key: ChatTopPanelMinHeight.self,value: DesignTokens.padding8 * 2)
        })
    }
}
