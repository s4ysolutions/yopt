import Foundation
import ComposeApp

/// Lightweight tagged logger so the send/observe flow is traceable even when the
/// UI shows nothing. Prints to stdout (visible when the binary runs in a terminal).
func dbg(_ tag: String, _ msg: @autoclosure () -> String) {
    print("[\(tag)] \(msg())")
}

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
    @Published var selectedTags: Set<String> = []

    // Computed
    var currentChat: ChatModel? {
        allChats.first { $0.id == currentChatId }
    }

    var tagCounts: [String: Int] {
        var counts: [String: Int] = [:]
        for chat in allChats {
            for label in chat.labels { counts[label, default: 0] += 1 }
        }
        return counts
    }

    var allTags: [String] { tagCounts.keys.sorted() }

    var effectiveTags: Set<String> { selectedTags.intersection(Set(allTags)) }

    var filteredChats: [ChatModel] {
        let query = chatSearchQuery.lowercased()
        let tags = effectiveTags
        return allChats
            .filter { chat in
                tags.isEmpty || tags.allSatisfy { chat.labels.contains($0) }
            }
            .filter { query.isEmpty || $0.title.lowercased().contains(query) || $0.labels.contains { $0.lowercased().contains(query) } }
            .sorted(by: { a, b in
                let aTime = a.history.last?.timestamp ?? Int64(a.id.dropFirst(5)) ?? 0
                let bTime = b.history.last?.timestamp ?? Int64(b.id.dropFirst(5)) ?? 0
                return aTime > bTime
            })
    }

    private var observationTasks: [Task<Void, Never>] = []
    /// The in-flight send, so `cancelSend()` can actually cancel it (fix #2).
    private var sendTask: Task<Void, Never>? = nil
    /// Seed `prompt` from persisted `lastPrompt` only once at startup, never on the
    /// emission caused by our own `set` after a send (fix #1 — prompt-refill race).
    private var didSeedPrompt = false

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
                let curHist = chatList.first { $0.id == self.currentChatId }?.history.count ?? -1
                dbg("observe.chats", "emission count=\(chatList.count) currentHistory=\(curHist)")
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
                dbg("observe.selectedModel", "emission=\(modelId ?? "nil")")
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
                // Fix #1: only seed the input box from persisted lastPrompt ONCE, at
                // startup. After a send we call set(trimmed) ourselves, which makes this
                // flow re-emit; without this guard it would refill the box we just cleared.
                if !self.didSeedPrompt {
                    self.didSeedPrompt = true
                    if self.prompt.isEmpty && !p.isEmpty {
                        self.prompt = p
                        dbg("observe.lastPrompt", "seeded prompt from persisted value len=\(p.count)")
                    } else {
                        dbg("observe.lastPrompt", "seed skipped (prompt empty=\(self.prompt.isEmpty), persisted empty=\(p.isEmpty))")
                    }
                } else {
                    dbg("observe.lastPrompt", "emission len=\(p.count) (no refill — already seeded)")
                }
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
        guard var chat = currentChat, !title.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        chat.title = title
        Task { try? await bridge.chatsUseCase.update(chat: chat.toKotlinChat()) }
    }

    func send() {
        dbg("send", "called. loading=\(loading) promptLen=\(prompt.count) model=\(selectedModel ?? "nil") chat=\(currentChatId ?? "nil")")
        guard !loading else { dbg("send", "BLOCKED: already loading"); return }
        guard let chat = currentChat else { dbg("send", "ABORT: no current chat"); self.error = "No chat available"; return }
        let trimmed = prompt.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { dbg("send", "ABORT: empty prompt"); return }
        guard let modelId = selectedModel else { dbg("send", "ABORT: no model selected"); self.error = "No model selected — add an API key in Settings"; return }
        loading = true
        error = nil
        dbg("send", "state → loading=true error=nil; starting task")
        sendTask = Task {
            defer {
                self.loading = false
                self.sendTask = nil
                dbg("send", "task end → loading=false sendTask=nil")
            }
            do {
                let chatToUse = chat.toKotlinChat()
                dbg("send", "invoke start model=\(modelId) historyCount=\(chat.history.count)")
                let raw = try await bridge.sendUseCase.invoke(chat: chatToUse, prompt: trimmed, modelId: modelId)
                dbg("send", "invoke returned raw type=\(type(of: raw)) value=\(String(describing: raw).prefix(160))")
                if Task.isCancelled { dbg("send", "cancelled after invoke — discarding result"); return }
                _ = try resultOrThrow(result: raw)
                // Keep the prompt in the box on purpose: this app retains the sent text so
                // the newest history entry hides its prompt header (entry.prompt == prompt)
                // and the user can tweak + resend. Do NOT clear it — clearing makes the next
                // send abort on the empty-prompt guard.
                dbg("send", "SUCCESS — persisting lastPrompt, prompt retained (len=\(self.prompt.count))")
                try? await bridge.lastPromptUseCase.set(value: trimmed)
            } catch {
                if Task.isCancelled {
                    dbg("send", "CANCELLED (caught) — not surfacing as error")
                } else {
                    dbg("send", "FAILURE caught: \(error.localizedDescription)")
                    self.error = error.localizedDescription
                    dbg("send", "state → error set, prompt retained (len=\(self.prompt.count))")
                }
            }
        }
    }

    func cancelSend() {
        dbg("cancelSend", "cancelling sendTask=\(sendTask != nil)")
        sendTask?.cancel()   // fix #2: actually cancel the in-flight coroutine, not just the flag
        sendTask = nil
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
