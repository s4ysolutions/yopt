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
    @Published var splitFraction: Float = 0.4
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
            do {
                for try await chats in bridge.chatsUseCase.observeAll() {
                    let chatList = (chats as? [Chat] ?? []).map(ChatModel.fromKotlin)
                    self.allChats = chatList
                    if self.currentChatId == nil && !chatList.isEmpty {
                        var savedId: String? = nil
                        for try await id in self.bridge.lastChatIdUseCase.observe() {
                            savedId = id as? String
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
            } catch {}
        })

        // Observe enabled models
        observationTasks.append(Task {
            do {
                for try await ms in bridge.modelsUseCase.observeEnabledModels() {
                    self.models = (ms as? [ModelDef] ?? []).map(ModelDefModel.fromKotlin)
                }
            } catch {}
        })

        // Observe providers
        observationTasks.append(Task {
            do {
                for try await ps in bridge.manageProvidersUseCase.observeProviders() {
                    self.providers = (ps as? [ProviderDef] ?? []).map(ProviderModel.fromKotlin)
                }
            } catch {}
        })

        // Observe selected model
        observationTasks.append(Task {
            do {
                for try await modelId in bridge.modelSelectionUseCase.observe() {
                    self.selectedModel = modelId as? String
                    if modelId == nil && !self.models.isEmpty {
                        try? await bridge.modelSelectionUseCase.set(modelId: self.models.first!.id)
                    }
                }
            } catch {}
        })

        // Observe global instructions
        observationTasks.append(Task {
            do {
                for try await instr in bridge.globalInstructionsUseCase.observe() {
                    self.globalInstructions = instr as? String ?? ""
                }
            } catch {}
        })

        // Observe split fraction
        observationTasks.append(Task {
            do {
                for try await f in bridge.splitFractionUseCase.observe() {
                    self.splitFraction = f as? Float ?? 0.4
                }
            } catch {}
        })

        // Observe show markdown
        observationTasks.append(Task {
            do {
                for try await md in bridge.responseDisplayUseCase.observeDefaultShowMarkdown() {
                    self.defaultShowMarkdown = md as? Bool ?? false
                }
            } catch {}
        })

        // Observe last prompt
        observationTasks.append(Task {
            do {
                for try await p in bridge.lastPromptUseCase.observe() {
                    self.lastPrompt = p as? String ?? ""
                    if self.prompt.isEmpty { self.prompt = p as? String ?? "" }
                }
            } catch {}
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
        guard !loading, let chat = currentChat else { return }
        let trimmed = prompt.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, let modelId = selectedModel else { return }
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

    // MARK: - Split

    func saveSplitFraction(_ fraction: Float) {
        Task { try? await bridge.splitFractionUseCase.set(value: fraction) }
    }
}
