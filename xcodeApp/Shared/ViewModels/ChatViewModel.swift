import Foundation
import ComposeApp

@MainActor
final class ChatViewModel: ObservableObject {
    private let bridge = KotlinBridge.shared

    // Published State
    @Published var currentChatId: String? = nil
    @Published var chatName: String = ""
    @Published var prompt: String = ""
    @Published var loading: Bool = false
    @Published var error: String? = nil
    @Published var showSettings: Bool = false
    @Published var showChatSettings: Bool = false
    @Published var chatSearchQuery: String = ""
    @Published var allChats: [ChatModel] = []
    @Published var models: [ModelDefModel] = []
    @Published var providers: [ProviderModel] = []
    @Published var selectedModel: String? = nil
    @Published var globalInstructions: String = ""
    @Published var defaultShowMarkdown: Bool = false
    @Published var lastPrompt: String = ""

    // Computed
    var currentChat: ChatModel? {
        allChats.first { $0.id == currentChatId }
    }

    var filteredChats: [ChatModel] {
        let query = chatSearchQuery.lowercased()
        return allChats
            .filter { query.isEmpty || $0.title.lowercased().contains(query) || $0.labels.contains { $0.lowercased().contains(query) } }
            .sorted(by: { a, b in
                let aTime = a.history.last?.timestamp ?? Int64(a.id.dropFirst(5)) ?? 0
                let bTime = b.history.last?.timestamp ?? Int64(b.id.dropFirst(5)) ?? 0
                return aTime > bTime
            })
    }

    private var observationTasks: [Task<Void, Never>] = []

    init() {
        observeFlows()
    }

    deinit {
        observationTasks.forEach { $0.cancel() }
    }

    private func observeFlows() {
        // Observe chats
        observationTasks.append(Task {
            for await chats in bridge.chatsUseCase.observeAll() {
                let chatList = chats.map(ChatModel.fromKotlin)
                self.allChats = chatList
                if chatList.isEmpty {
                    _ = try? await bridge.chatsUseCase.create(title: "New Chat", instructions: "", labels: [])
                } else if self.currentChatId == nil {
                    var savedId: String? = nil
                    for await id in self.bridge.lastChatIdUseCase.observe() {
                        savedId = id
                        break
                    }
                    let saved = chatList.first { $0.id == savedId }
                    self.currentChatId = saved?.id ?? chatList.first?.id
                    self.chatName = saved?.title ?? chatList.first?.title ?? ""
                } else if let cid = self.currentChatId, !chatList.contains(where: { $0.id == cid }) {
                    self.currentChatId = chatList.first?.id
                    self.chatName = chatList.first?.title ?? ""
                }
                if let cid = self.currentChatId, let chat = chatList.first(where: { $0.id == cid }) {
                    self.chatName = chat.title
                }
            }
        })

        // Observe enabled models
        observationTasks.append(Task {
            for await ms in bridge.modelsUseCase.observeEnabledModels() {
                self.models = ms.map(ModelDefModel.fromKotlin)
                if self.selectedModel == nil, let first = ms.first {
                    try? await bridge.modelSelectionUseCase.set(modelId: first.id)
                }
            }
        })

        // Observe providers
        observationTasks.append(Task {
            for await ps in bridge.manageProvidersUseCase.observeProviders() {
                self.providers = ps.map(ProviderModel.fromKotlin)
            }
        })

        // Observe selected model
        observationTasks.append(Task {
            for await modelId in bridge.modelSelectionUseCase.observe() {
                self.selectedModel = modelId
                if modelId == nil && !self.models.isEmpty {
                    try? await bridge.modelSelectionUseCase.set(modelId: self.models.first!.id)
                }
            }
        })

        // Observe global instructions
        observationTasks.append(Task {
            for await instr in bridge.globalInstructionsUseCase.observe() {
                self.globalInstructions = instr
            }
        })

        // Observe show markdown
        observationTasks.append(Task {
            for await md in bridge.responseDisplayUseCase.observeDefaultShowMarkdown() {
                self.defaultShowMarkdown = md as? Bool ?? false
            }
        })

        // Observe last prompt
        observationTasks.append(Task {
            for await p in bridge.lastPromptUseCase.observe() {
                self.lastPrompt = p
                if self.prompt.isEmpty { self.prompt = p }
            }
        })
    }

    // MARK: - Actions

    func selectChat(_ chatId: String) {
        currentChatId = chatId
        Task { try? await bridge.lastChatIdUseCase.set(id: chatId) }
    }

    func createNewChat() {
        Task {
            if let c = try? await bridge.chatsUseCase.create(title: "New Chat", instructions: "", labels: []) {
                currentChatId = c.id
            }
        }
    }

    func deleteCurrentChat() {
        guard let chat = currentChat else { return }
        let remaining = allChats.filter { $0.id != chat.id }
        currentChatId = remaining.first?.id
        Task { try? await bridge.chatsUseCase.delete(chatId: chat.id) }
    }

    func updateChatName(_ title: String) {
        chatName = title
        guard let chat = currentChat, !title.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        Task { try? await bridge.chatsUseCase.update(chat: chat.toKotlinChat()) }
    }

    func send() {
        guard !loading else { return }
        guard let chat = currentChat else { self.error = "No chat available"; return }
        let trimmed = prompt.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        guard let modelId = selectedModel else { self.error = "No model selected — add an API key in Settings"; return }
        loading = true
        error = nil
        Task {
            defer { self.loading = false }
            do {
                let chatToUse = chat.toKotlinChat()
                _ = try await bridge.sendUseCase.invoke(chat: chatToUse, prompt: trimmed, modelId: modelId)
                try? await bridge.lastPromptUseCase.set(value: trimmed)
                self.prompt = ""
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func cancelSend() {
        loading = false
    }

    // MARK: - History Actions

    func removeEntry(at originalIndex: Int, chatId: String) {
        Task { try? await bridge.chatsUseCase.removeHistoryEntry(chatId: chatId, index: Int32(originalIndex)) }
    }

    func toggleEntryExpanded(timestamp: Int64, chatId: String) {
        guard let chat = currentChat else { return }
        let expanded = !chat.expandedTimestamps.contains(timestamp)
        Task { try? await bridge.chatsUseCase.setEntryExpanded(chatId: chatId, entryTimestamp: timestamp, expanded: expanded) }
    }

    func toggleEntryMarkdown(timestamp: Int64, chatId: String) {
        guard let chat = currentChat else { return }
        let entry = chat.history.first { $0.timestamp == timestamp }
        Task {
            try? await bridge.chatsUseCase.toggleEntryMarkdown(chatId: chatId, entryTimestamp: timestamp)
            if let e = entry {
                try? await bridge.responseDisplayUseCase.setDefaultShowMarkdown(show: !e.showMarkdown)
            }
        }
    }

    func selectModel(_ id: String) {
        Task { try? await bridge.modelSelectionUseCase.set(modelId: id) }
    }

    func useAsPrompt(_ text: String) { prompt = text }
    func appendToPrompt(_ text: String) { prompt = prompt + "\n" + text }

}
