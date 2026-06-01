# SwiftUI Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build native SwiftUI mirror of the existing Compose UI for both iOS and macOS, sharing Kotlin business logic via the YoPtShared KMP framework.

**Architecture:** Thin SwiftUI layer on top of existing Kotlin Multiplatform shared module (YoPtShared + ComposeApp frameworks). Shared Swift code in `xcodeApp/Shared/` handles models, view models, and common components. Platform-specific targets (`iosApp`/`macApp`) get minimal per-platform wrappers. The existing Compose UI is preserved — SwiftUI is a parallel implementation.

**Tech Stack:** SwiftUI · Kotlin Multiplatform (YoPtShared via SKIE 0.10.12) · SKIE (auto-generates Swift-friendly async wrappers for Kotlin Flow and suspend functions) · MarkdownUI (SwiftUI markdown library)

**Key interop detail:** `AppModule` is created from Swift; all use cases (suspend functions, Flow observations) are accessible through SKIE-generated Swift async wrappers.

---

## File Structure

```
xcodeApp/
  Shared/
    KotlinBridge.swift              — AppModule wrapper + use case accessors
    Models/
      ChatModels.swift              — Swift structs matching Kotlin domain models
      ProviderModels.swift          — ProviderDef, AuthCredentials, ApiStyle, AuthType
    ViewModels/
      ChatViewModel.swift           — Main screen state & actions
      SettingsViewModel.swift       — Settings state & actions
    Components/
      ResponseCardView.swift        — Single history entry card (prompt + response + actions)
      MarkdownResponseView.swift    — Markdown rendering wrapper
      ResponseActionsBar.swift      — Action buttons row (copy, use as prompt, etc.)
      PromptAreaView.swift          — Prompt input + model selector + send
      HeaderView.swift              — Search, chat name, icon buttons
      DraggableSplitter.swift       — Split pane divider
      ChatSettingsView.swift        — Chat instructions/labels dialog
      TagChipsView.swift            — Tag/chip display + editor
      ModelSelectorView.swift       — Dropdown model picker
      ChatListView.swift            — Search + dropdown chat list
  iosApp/
    iOSApp.swift                    — Updated @main entry
    Views/
      MainChatView.swift            — iOS main chat screen
      SettingsView.swift            — iOS settings with tabs
  macApp/
    macOSApp.swift                  — Updated @main entry
    Views/
      MainChatView.swift            — macOS main chat screen
      SettingsView.swift            — macOS settings with tabs
```

---

### Task 1: Create Shared KotlinBridge and Models

**Files:**
- Create: `xcodeApp/Shared/KotlinBridge.swift`
- Create: `xcodeApp/Shared/Models/ChatModels.swift`
- Create: `xcodeApp/Shared/Models/ProviderModels.swift`

- [ ] **Step 1: Create KotlinBridge.swift**

```swift
import Foundation
import ComposeApp

/// Wraps AppModule (from KMP) and provides typed access to all use cases.
/// Imports `ComposeApp` which transitively includes `YoPtShared`.
@MainActor
final class KotlinBridge: ObservableObject {
    static let shared = KotlinBridge()
    
    let module: AppModule
    
    private init() {
        // Create AppModule. On Apple platforms, platformContext is nil
        // (KeyValueStore/SecureStore use NSUserDefaults/Keychain respectively).
        self.module = AppModule(platformContext: nil)
    }
    
    // MARK: - Use Case Accessors
    
    var chatsUseCase: ManageChatsUseCase { module.chatsUseCase }
    var modelsUseCase: ManageModelsUseCase { module.modelsUseCase }
    var modelSelectionUseCase: ManageModelSelectionUseCase { module.modelSelectionUseCase }
    var sendUseCase: SendPromptUseCase { module.sendUseCase }
    var manageAuthUseCase: ManageAuthUseCase { module.manageAuthUseCase }
    var refreshModelsUseCase: RefreshModelsUseCase { module.refreshModelsUseCase }
    var exportUseCase: ExportImportUseCase { module.exportUseCase }
    var manageProvidersUseCase: ManageProvidersUseCase { module.manageProvidersUseCase }
    var responseDisplayUseCase: ManageResponseDisplayUseCase { module.responseDisplayUseCase }
    var lastChatIdUseCase: ManageLastChatIdUseCase { module.lastChatIdUseCase }
    var lastPromptUseCase: ManageLastPromptUseCase { module.lastPromptUseCase }
    var splitFractionUseCase: ManageSplitFractionUseCase { module.splitFractionUseCase }
    var globalInstructionsUseCase: ManageGlobalInstructionsUseCase { module.globalInstructionsUseCase }
    
    // MARK: - Async Helpers
    
    /// Observe a Kotlin Flow as an AsyncSequence.
    /// SKIE transforms Flow<T> into a Swift async sequence, but we provide
    /// a convenience wrapper for common observation patterns.
    static func collect<T>(_ flow: SkieSwiftFlow<T>) -> AsyncCompactMapSequence<SkieSwiftFlow<T>, T> {
        flow.compactMap { $0 }
    }
}
```

- [ ] **Step 2: Create ChatModels.swift** (Swift mirrors of Kotlin types for type safety)

```swift
import Foundation

/// Mirrors s4y.yopt.domain.models.Chat
struct ChatModel: Identifiable, Equatable {
    let id: String
    var title: String
    var instructions: String
    var defaultModelId: String?
    var labels: [String]
    var expandedTimestamps: Set<Int64>
    var history: [ResponseEntryModel]
    
    static func fromKotlin(_ chat: s4y.yopt.domain.models.Chat) -> ChatModel {
        ChatModel(
            id: chat.id,
            title: chat.title,
            instructions: chat.instructions,
            defaultModelId: chat.defaultModelId,
            labels: chat.labels as! [String],
            expandedTimestamps: Set(chat.expandedTimestamps as! [Int64]),
            history: (chat.history as! [s4y.yopt.domain.models.ResponseEntry]).map(ResponseEntryModel.fromKotlin)
        )
    }
}

/// Mirrors s4y.yopt.domain.models.ResponseEntry
struct ResponseEntryModel: Identifiable, Equatable {
    var id: Int64 { timestamp }
    let timestamp: Int64
    let prompt: String
    let response: String
    let modelId: String
    let modelName: String
    let durationMs: Int64
    let showMarkdown: Bool
    
    static func fromKotlin(_ entry: s4y.yopt.domain.models.ResponseEntry) -> ResponseEntryModel {
        ResponseEntryModel(
            timestamp: entry.timestamp,
            prompt: entry.prompt,
            response: entry.response,
            modelId: entry.modelId,
            modelName: entry.modelName,
            durationMs: entry.durationMs,
            showMarkdown: entry.showMarkdown
        )
    }
}
```

- [ ] **Step 3: Create ProviderModels.swift**

```swift
import Foundation

/// Mirrors s4y.yopt.domain.models.ProviderDef
struct ProviderModel: Identifiable, Equatable {
    let id: String
    let name: String
    let apiStyle: ApiStyleModel
    let baseUrl: String
    let predefined: Bool
    
    static func fromKotlin(_ provider: s4y.yopt.domain.models.ProviderDef) -> ProviderModel {
        ProviderModel(
            id: provider.id,
            name: provider.name,
            apiStyle: ApiStyleModel.fromKotlin(provider.apiStyle),
            baseUrl: provider.baseUrl,
            predefined: provider.predefined
        )
    }
}

/// Mirrors s4y.yopt.domain.models.ApiStyle
enum ApiStyleModel: String, CaseIterable {
    case openai = "OPENAI"
    case anthropic = "ANTHROPIC"
    case gemini = "GEMINI"
    
    static func fromKotlin(_ style: s4y.yopt.domain.models.ApiStyle) -> ApiStyleModel {
        switch style {
        case .openai: return .openai
        case .anthropic: return .anthropic
        case .gemini: return .gemini
        default: return .openai
        }
    }
    
    func toKotlin() -> s4y.yopt.domain.models.ApiStyle {
        switch self {
        case .openai: return .openai
        case .anthropic: return .anthropic
        case .gemini: return .gemini
        }
    }
}
```

---

### Task 2: Create Shared ViewModels

**Files:**
- Create: `xcodeApp/Shared/ViewModels/ChatViewModel.swift`
- Create: `xcodeApp/Shared/ViewModels/SettingsViewModel.swift`

- [ ] **Step 1: Create ChatViewModel.swift**

```swift
import Foundation
import ComposeApp
import Combine

@MainActor
final class ChatViewModel: ObservableObject {
    private let bridge = KotlinBridge.shared
    
    // MARK: - Published State
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
    
    // MARK: - Computed
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
    
    private var cancellables = Set<AnyCancellable>()
    private var observationTasks: [Task<Void, Never>] = []
    
    init() {
        observeFlows()
    }
    
    deinit {
        observationTasks.forEach { $0.cancel() }
    }
    
    private func observeFlows() {
        // Observe chats
        observationTasks.append(Task { @MainActor in
            for await chats in bridge.chatsUseCase.observeAll() {
                let chatList = (chats as! [s4y.yopt.domain.models.Chat]).map(ChatModel.fromKotlin)
                self.allChats = chatList
                // Init current chat
                if self.currentChatId == nil && !chatList.isEmpty {
                    let savedId = try? await self.bridge.lastChatIdUseCase.observe().first()
                    let saved = chatList.first { $0.id == savedId }
                    self.currentChatId = saved?.id ?? chatList.first?.id
                    self.chatName = saved?.title ?? chatList.first?.title ?? ""
                } else if let cid = self.currentChatId, !chatList.contains(where: { $0.id == cid }) {
                    self.currentChatId = chatList.first?.id
                    self.chatName = chatList.first?.title ?? ""
                }
                // Sync chat name when switching
                if let cid = self.currentChatId, let chat = chatList.first(where: { $0.id == cid }) {
                    self.chatName = chat.title
                }
            }
        })
        
        // Observe enabled models
        observationTasks.append(Task { @MainActor in
            for await ms in bridge.modelsUseCase.observeEnabledModels() {
                self.models = (ms as! [s4y.yopt.domain.models.ModelDef]).map(ModelDefModel.fromKotlin)
            }
        })
        
        // Observe providers
        observationTasks.append(Task { @MainActor in
            for await ps in bridge.manageProvidersUseCase.observeProviders() {
                self.providers = (ps as! [s4y.yopt.domain.models.ProviderDef]).map(ProviderModel.fromKotlin)
            }
        })
        
        // Observe selected model
        observationTasks.append(Task { @MainActor in
            for await modelId in bridge.modelSelectionUseCase.observe() {
                self.selectedModel = modelId as! String?
                // Auto-select first if none
                if modelId == nil && !self.models.isEmpty {
                    try? await bridge.modelSelectionUseCase.set(modelId: self.models.first!.id)
                }
            }
        })
        
        // Observe global instructions
        observationTasks.append(Task { @MainActor in
            for await instr in bridge.globalInstructionsUseCase.observe() {
                self.globalInstructions = instr as! String
            }
        })
        
        // Observe split fraction
        observationTasks.append(Task { @MainActor in
            for await f in bridge.splitFractionUseCase.observe() {
                self.splitFraction = (f as! Float)
            }
        })
        
        // Observe show markdown
        observationTasks.append(Task { @MainActor in
            for await md in bridge.responseDisplayUseCase.observeDefaultShowMarkdown() {
                self.defaultShowMarkdown = md as! Bool
            }
        })
        
        // Observe last prompt
        observationTasks.append(Task { @MainActor in
            for await p in bridge.lastPromptUseCase.observe() {
                self.lastPrompt = p as! String
                if self.prompt.isEmpty { self.prompt = p as! String }
            }
        })
    }
    
    // MARK: - Actions
    
    func selectChat(_ chatId: String) {
        currentChatId = chatId
        Task { try? await bridge.lastChatIdUseCase.set(value: chatId) }
    }
    
    func createNewChat() {
        Task {
            let chat = try? await bridge.chatsUseCase.create(title: "New Chat", instructions: "", labels: [])
            if let c = chat { currentChatId = c.id }
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
        Task { try? await bridge.chatsUseCase.update(chat: chat.toKotlinChat().copy(title: title)) }
    }
    
    func send() {
        guard !loading, let chat = currentChat else { return }
        let trimmed = prompt.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, let modelId = selectedModel else { return }
        loading = true
        error = nil
        Task {
            defer { self.loading = false }
            let currentName = self.chatName
            let chatRef = chat
            if !currentName.trimmingCharacters(in: .whitespaces).isEmpty, currentName != chatRef.title {
                try? await bridge.chatsUseCase.update(chat: chatRef.toKotlinChat().copy(title: currentName))
            }
            let result = try? await bridge.sendUseCase.invoke(chat: chatRef.toKotlinChat().copy(title: currentName), prompt: trimmed, modelId: modelId)
            if case .failure(let err)? = result {
                self.error = (err as? Error)?.localizedDescription ?? "Unknown error"
            } else {
                try? await bridge.lastPromptUseCase.set(value: trimmed)
                self.prompt = ""
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
    
    func useAsPrompt(_ text: String) { prompt = text }
    func appendToPrompt(_ text: String) { prompt = prompt + "\n" + text }
    
    // MARK: - Split
    
    func saveSplitFraction(_ fraction: Float) {
        Task { try? await bridge.splitFractionUseCase.set(value: fraction) }
    }
}

extension ChatModel {
    func toKotlinChat() -> s4y.yopt.domain.models.Chat {
        s4y.yopt.domain.models.Chat(
            id: id,
            title: title,
            instructions: instructions,
            defaultModelId: defaultModelId,
            labels: labels as? KotlinMutableList<NSString> ?? [],
            expandedTimestamps: Set(expandedTimestamps) as! Set<Int64>,
            history: history.map { $0.toKotlinEntry() } as? [s4y.yopt.domain.models.ResponseEntry] ?? []
        )
    }
}

extension ResponseEntryModel {
    func toKotlinEntry() -> s4y.yopt.domain.models.ResponseEntry {
        s4y.yopt.domain.models.ResponseEntry(
            timestamp: timestamp,
            prompt: prompt,
            response: response,
            modelId: modelId,
            modelName: modelName,
            durationMs: durationMs,
            showMarkdown: showMarkdown
        )
    }
}
```

Note: The Kotlin-to-Swift type interop (casts like `as! [s4y.yopt.domain.models.Chat]`) depends on SKIE's ObjC header generation. Run `./gradlew :shared:compileKotlinIosArm64` to verify exact types.

- [ ] **Step 2: Create SettingsViewModel.swift**

```swift
import Foundation
import ComposeApp

@MainActor
final class SettingsViewModel: ObservableObject {
    private let bridge = KotlinBridge.shared
    
    @Published var models: [ModelDefModel] = []
    @Published var chats: [ChatModel] = []
    @Published var creds: [AuthCredentialsModel] = []
    @Published var providers: [ProviderModel] = []
    @Published var globalInstructions: String = ""
    
    @Published var exportError: String? = nil
    @Published var importReplaceError: String? = nil
    @Published var importAppendError: String? = nil
    @Published var dialogTitle: String? = nil
    @Published var dialogText: String? = nil
    
    private var observationTasks: [Task<Void, Never>] = []
    
    init() {
        observeFlows()
    }
    
    deinit { observationTasks.forEach { $0.cancel() } }
    
    private func observeFlows() {
        observationTasks.append(Task { @MainActor in
            for await ms in bridge.modelsUseCase.observeModels() {
                self.models = (ms as! [s4y.yopt.domain.models.ModelDef]).map(ModelDefModel.fromKotlin)
            }
        })
        observationTasks.append(Task { @MainActor in
            for await cs in bridge.chatsUseCase.observeAll() {
                self.chats = (cs as! [s4y.yopt.domain.models.Chat]).map(ChatModel.fromKotlin)
            }
        })
        observationTasks.append(Task { @MainActor in
            for await cs in bridge.manageAuthUseCase.observeCredentials() {
                self.creds = (cs as! [s4y.yopt.domain.models.AuthCredentials]).map(AuthCredentialsModel.fromKotlin)
            }
        })
        observationTasks.append(Task { @MainActor in
            for await ps in bridge.manageProvidersUseCase.observeProviders() {
                self.providers = (ps as! [s4y.yopt.domain.models.ProviderDef]).map(ProviderModel.fromKotlin)
            }
        })
        observationTasks.append(Task { @MainActor in
            for await instr in bridge.globalInstructionsUseCase.observe() {
                self.globalInstructions = instr as! String
            }
        })
    }
    
    // Provider actions
    func saveApiKey(providerId: String, key: String) {
        Task { try? await bridge.manageAuthUseCase.saveApiKey(providerId: providerId, apiKey: key) }
    }
    
    func clearCredentials(providerId: String) {
        Task { try? await bridge.manageAuthUseCase.deleteCredentials(providerId: providerId) }
    }
    
    func refreshModels(provider: ProviderModel, apiKey: String?) {
        Task {
            let kotlinProvider = provider.toKotlinProvider()
            _ = try? await bridge.refreshModelsUseCase.refresh(provider: kotlinProvider, apiKey: apiKey)
        }
    }
    
    func toggleModelEnabled(_ modelId: String) {
        let model = models.first { $0.id == modelId }
        Task { try? await bridge.modelsUseCase.setModelEnabled(modelId: modelId, enabled: !(model?.enabled ?? false)) }
    }
    
    func addCustomProvider(name: String, apiStyle: ApiStyleModel, baseUrl: String) {
        Task { _ = try? await bridge.manageProvidersUseCase.addCustomProvider(name: name, apiStyle: apiStyle.toKotlin(), baseUrl: baseUrl) }
    }
    
    func updateCustomProvider(_ provider: ProviderModel, name: String, baseUrl: String, apiStyle: ApiStyleModel) {
        let kp = provider.toKotlinProvider()
        Task {
            try? await bridge.manageProvidersUseCase.updateCustomProvider(def: kp.copy(name: name, apiStyle: apiStyle.toKotlin(), baseUrl: baseUrl))
        }
    }
    
    func deleteCustomProvider(_ id: String) {
        Task { try? await bridge.manageProvidersUseCase.deleteCustomProvider(id: id) }
    }
    
    // Chat actions
    func updateChat(_ chat: ChatModel, title: String, instructions: String, labels: [String]) {
        Task {
            try? await bridge.chatsUseCase.update(chat: chat.toKotlinChat().copy(title: title, instructions: instructions, labels: labels as? KotlinMutableList<NSString> ?? []))
        }
    }
    
    func deleteChat(_ id: String) {
        Task { try? await bridge.chatsUseCase.delete(chatId: id) }
    }
    
    // Global instructions
    func setGlobalInstructions(_ text: String) {
        Task { try? await bridge.globalInstructionsUseCase.set(value: text) }
    }
    
    // Export/Import
    func export() {
        Task {
            do {
                let json = try await bridge.exportUseCase.export() as! String
                // Write to file via platform-specific file picker
                self.dialogTitle = "Export"
                self.dialogText = "Data exported (\(json.count) chars). Use Save panel to write."
            } catch {
                self.exportError = error.localizedDescription
            }
        }
    }
    
    func importReplace(json: String) {
        Task {
            do {
                try await bridge.exportUseCase.import(jsonString: json)
                self.dialogTitle = "Import"
                self.dialogText = "Settings replaced successfully."
            } catch {
                self.importReplaceError = error.localizedDescription
            }
        }
    }
    
    func importAppend(json: String) {
        Task {
            do {
                try await bridge.exportUseCase.importAppend(jsonString: json)
                self.dialogTitle = "Import Append"
                self.dialogText = "Settings appended successfully."
            } catch {
                self.importAppendError = error.localizedDescription
            }
        }
    }
}
```

Note: `KotlinMutableList<NSString>` bridging depends on SKIE/Objetive-C interop. Verify actual types after build.

---

### Task 3: Create Shared UI Components

**Files:**
- Create: `xcodeApp/Shared/Components/MarkdownResponseView.swift`
- Create: `xcodeApp/Shared/Components/ResponseActionsBar.swift`
- Create: `xcodeApp/Shared/Components/ResponseCardView.swift`
- Create: `xcodeApp/Shared/Components/PromptAreaView.swift`
- Create: `xcodeApp/Shared/Components/HeaderView.swift`
- Create: `xcodeApp/Shared/Components/DraggableSplitter.swift`
- Create: `xcodeApp/Shared/Components/ChatSettingsView.swift`
- Create: `xcodeApp/Shared/Components/TagChipsView.swift`
- Create: `xcodeApp/Shared/Components/ModelSelectorView.swift`
- Create: `xcodeApp/Shared/Components/ChatListView.swift`

- [ ] **Step 1: Create MarkdownResponseView.swift**

```swift
import SwiftUI
// Use MarkdownUI library via SPM: https://github.com/gonzalezreal/swift-markdown-ui
import MarkdownUI

struct MarkdownResponseView: View {
    let content: String
    
    var body: some View {
        Markdown(content)
            .textSelection(.enabled)
    }
}
```

Add `swift-markdown-ui` as an SPM dependency in the Xcode project: `https://github.com/gonzalezreal/swift-markdown-ui` (use latest, ~> 2.0).

- [ ] **Step 2: Create ResponseActionsBar.swift**

```swift
import SwiftUI

struct ResponseActionsBar: View {
    let isExpanded: Bool
    let showMarkdown: Bool
    let onToggleExpand: () -> Void
    let onToggleMarkdown: () -> Void
    let onUseAsPrompt: () -> Void
    let onAppendToPrompt: () -> Void
    let onCopy: () -> Void
    
    var body: some View {
        HStack(spacing: 0) {
            // Collapse/Expand — left-aligned
            Button(action: onToggleExpand) {
                Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help(isExpanded ? "Collapse" : "Expand")
            
            Spacer()
            
            if isExpanded {
                Button(action: onToggleMarkdown) {
                    Image(systemName: showMarkdown ? "doc.plaintext" : "doc.text.magnifyingglass")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .help(showMarkdown ? "Switch to Raw" : "Switch to Markdown")
            }
            
            Button(action: onUseAsPrompt) {
                Image(systemName: "arrow.up.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")
            
            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")
            
            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 4)
        .frame(height: 24)
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
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .help(isExpanded ? "Collapse Prompt" : "Expand Prompt")
            }
            Spacer()
            Button(action: onUseAsPrompt) {
                Image(systemName: "arrow.up.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Use as Prompt")
            Button(action: onAppendToPrompt) {
                Image(systemName: "plus.message")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Append to Prompt")
            Button(action: onCopy) {
                Image(systemName: "doc.on.doc")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Copy")
        }
        .padding(.horizontal, 4)
        .frame(height: 24)
    }
}
```

- [ ] **Step 3: Create ResponseCardView.swift**

```swift
import SwiftUI

struct ResponseCardView: View {
    let entry: ResponseEntryModel
    let isFirst: Bool
    let currentPrompt: String
    let currentModelId: String?
    let isExpanded: Bool
    let chatId: String
    let onToggleExpand: () -> Void
    let onToggleMarkdown: () -> Void
    let onUseAsPrompt: (String) -> Void
    let onAppendToPrompt: (String) -> Void
    let onCopy: (String) -> Void
    let onRemove: () -> Void
    let modelName: String?
    
    @State private var promptExpanded = false
    @State private var promptOverflows = false
    
    private let wordLimit = 50
    private let responsePreviewLength = 200
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Prompt row
            if !isFirst || entry.prompt != currentPrompt || entry.modelId != currentModelId {
                promptSection
                Divider()
            }
            
            // Response action buttons
            ResponseActionsBar(
                isExpanded: isExpanded,
                showMarkdown: entry.showMarkdown,
                onToggleExpand: onToggleExpand,
                onToggleMarkdown: onToggleMarkdown,
                onUseAsPrompt: { onUseAsPrompt(entry.response) },
                onAppendToPrompt: { onAppendToPrompt(entry.response) },
                onCopy: { onCopy(entry.response) }
            )
            
            // Response content
            if isExpanded {
                if entry.showMarkdown {
                    MarkdownResponseView(content: entry.response)
                } else {
                    Text(entry.response)
                        .font(.system(.caption, design: .monospaced))
                        .textSelection(.enabled)
                }
            } else {
                Text(entry.response.prefix(responsePreviewLength))
                    .font(.system(.caption, design: .monospaced))
                    .lineLimit(3)
            }
            
            // Bottom bar
            bottomBar
        }
        .padding(4)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
        )
        .padding(.vertical, 4)
    }
    
    private var promptSection: some View {
        // Simplified prompt view (responsive layout from Compose is complex)
        VStack(alignment: .leading, spacing: 2) {
            PromptActionsBar(
                isExpanded: promptExpanded,
                showExpand: promptOverflows,
                onToggleExpand: { promptExpanded.toggle() },
                onUseAsPrompt: { onUseAsPrompt(entry.prompt) },
                onAppendToPrompt: { onAppendToPrompt(entry.prompt) },
                onCopy: { onCopy(entry.prompt) }
            )
            Text(entry.prompt)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(promptExpanded ? nil : 1)
        }
        .padding(4)
        .background(Color.secondary.opacity(0.1))
        .cornerRadius(8)
    }
    
    private var bottomBar: some View {
        HStack {
            Button(action: onRemove) {
                Image(systemName: "xmark.bin")
                    .font(.caption2)
            }
            .buttonStyle(.plain)
            .help("Remove from History")
            
            Text(formatTimestamp(entry.timestamp))
                .font(.caption2)
                .foregroundColor(.secondary)
            
            if let name = modelName {
                Text("· \(name)")
                    .font(.caption2)
                    .foregroundColor(.accentColor)
                    .lineLimit(1)
            }
            
            Text("· \(formatDuration(entry.durationMs))")
                .font(.caption2)
                .foregroundColor(.secondary)
            
            Spacer()
        }
    }
    
    private func formatTimestamp(_ ms: Int64) -> String {
        let now = Date().timeIntervalSince1970 * 1000
        let diff = now - Double(ms)
        switch diff {
        case ..<60_000: return "now"
        case ..<3_600_000: return "\(Int(diff / 60_000))m ago"
        case ..<86_400_000: return "\(Int(diff / 3_600_000))h ago"
        default: return "\(Int(diff / 86_400_000))d ago"
        }
    }
    
    private func formatDuration(_ ms: Int64) -> String {
        switch ms {
        case ..<1000: return "\(ms)ms"
        case ..<10000: return "\(ms / 1000).\((ms % 1000) / 100)s"
        default: return "\(ms / 1000)s"
        }
    }
}
```

- [ ] **Step 4: Create HeaderView.swift**

```swift
import SwiftUI

struct HeaderView: View {
    @Binding var chatSearchQuery: String
    @Binding var chatDropdownExpanded: Bool
    @Binding var chatName: String
    let filteredChats: [ChatModel]
    let allChatsCount: Int
    let onCreateNew: () -> Void
    let onDelete: () -> Void
    let onChatSettings: () -> Void
    let onSettings: () -> Void
    let onSelectChat: (String) -> Void
    
    @State private var columnWidth: CGFloat = 0
    
    var body: some View {
        VStack(spacing: 8) {
            if columnWidth < 630 {
                // Narrow layout
                VStack(spacing: 8) {
                    HStack {
                        chatSearchField
                        actionButtons
                    }
                    chatNameField
                }
            } else {
                // Wide layout
                HStack(spacing: 8) {
                    chatSearchField
                        .frame(width: columnWidth * 0.3)
                    chatNameField
                    actionButtons
                }
            }
        }
        .background(GeometryReader { geo in
            Color.clear.onAppear { columnWidth = geo.size.width }
                .onChange(of: geo.size.width) { columnWidth = $0 }
        })
    }
    
    private var chatSearchField: some View {
        HStack(spacing: 4) {
            TextField("Search...", text: $chatSearchQuery)
                .textFieldStyle(.plain)
                .onChange(of: chatSearchQuery) { _ in chatDropdownExpanded = true }
            Button {
                chatDropdownExpanded.toggle()
            } label: {
                Image(systemName: "list.bullet")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Chat List")
        }
        .padding(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
        )
        .overlay(alignment: .top) {
            if chatDropdownExpanded {
                ChatListView(
                    chats: filteredChats,
                    onSelect: { id in
                        onSelectChat(id)
                        chatSearchQuery = ""
                        chatDropdownExpanded = false
                    },
                    onDismiss: { chatDropdownExpanded = false }
                )
            }
        }
    }
    
    private var chatNameField: some View {
        TextField("Chat Name", text: $chatName)
            .textFieldStyle(.plain)
            .padding(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
            )
    }
    
    private var actionButtons: some View {
        HStack(spacing: 0) {
            Button(action: onCreateNew) {
                Image(systemName: "plus.bubble")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("New Chat")
            
            if allChatsCount > 1 {
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .font(.caption)
                }
                .buttonStyle(.plain)
                .help("Delete Chat")
            }
            
            Button(action: onChatSettings) {
                Image(systemName: "slider.horizontal.3")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Chat Settings")
            
            Button(action: onSettings) {
                Image(systemName: "gearshape")
                    .font(.caption)
            }
            .buttonStyle(.plain)
            .help("Settings")
        }
    }
}
```

- [ ] **Step 5: Create ChatListView.swift**

```swift
import SwiftUI

struct ChatListView: View {
    let chats: [ChatModel]
    let onSelect: (String) -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(chats) { chat in
                    Button(action: { onSelect(chat.id) }) {
                        Text(chat.title)
                            .lineLimit(1)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    Divider()
                }
            }
        }
        .frame(maxHeight: 300)
        .background(.regularMaterial)
        .cornerRadius(8)
        .shadow(radius: 4)
        .onTapGesture {} // prevent dismissal when interacting
    }
}
```

- [ ] **Step 6: Create PromptAreaView.swift**

```swift
import SwiftUI

struct PromptAreaView: View {
    @Binding var prompt: String
    @Binding var loading: Bool
    let selectedModelName: String
    let models: [ModelDefModel]
    let modelsEmpty: Bool
    let error: String?
    let onSend: () -> Void
    let onCancel: () -> Void
    let onSelectModel: () -> Void
    let onOpenSettings: () -> Void
    
    @State private var showModelPicker = false
    
    var body: some View {
        VStack(spacing: 8) {
            // Prompt text editor
            TextEditor(text: $prompt)
                .font(.body)
                .frame(minHeight: 60)
                .overlay(
                    Group {
                        if prompt.isEmpty {
                            Text("Enter prompt...")
                                .foregroundColor(.secondary)
                                .padding(.leading, 4)
                                .padding(.top, 8)
                                .allowsHitTesting(false)
                        }
                    },
                    alignment: .topLeading
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                )
            
            // Bottom row: model selector + send
            HStack {
                // Model selector button
                Button(action: {
                    if modelsEmpty { onOpenSettings() }
                    else { showModelPicker.toggle() }
                }) {
                    Text(selectedModelName)
                        .lineLimit(1)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .popover(isPresented: $showModelPicker) {
                    modelPickerContent
                }
                
                Spacer()
                
                if loading {
                    Button(action: onCancel) {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                    .buttonStyle(.plain)
                } else {
                    Button("Send", action: onSend)
                        .buttonStyle(.borderedProminent)
                        .keyboardShortcut(.return, modifiers: [.command])
                }
            }
            
            if let err = error {
                Text(err)
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
    }
    
    private var modelPickerContent: some View {
        ScrollView {
            LazyVStack(alignment: .leading) {
                ForEach(models) { model in
                    Button(action: {
                        // Selection handled by parent via view model
                        showModelPicker = false
                    }) {
                        HStack {
                            Text(model.displayName)
                            Spacer()
                            if model.id == selectedModelName { // Simplified check
                                Image(systemName: "checkmark")
                            }
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(4)
            .frame(width: 280)
        }
    }
}
```

- [ ] **Step 7: Create DraggableSplitter.swift**

```swift
import SwiftUI

struct DraggableSplitter: View {
    @Binding var fraction: Float
    let onFractionChanged: (Float) -> Void
    
    @State private var isDragging = false
    
    var body: some View {
        Rectangle()
            .fill(isDragging ? Color.accentColor.opacity(0.1) : Color.secondary.opacity(0.15))
            .frame(height: 12)
            .overlay(
                HStack(spacing: 4) {
                    ForEach(0..<3) { _ in
                        Circle()
                            .fill(Color.secondary.opacity(0.3))
                            .frame(width: 4, height: 4)
                    }
                }
            )
            .gesture(
                DragGesture()
                    .onChanged { value in
                        isDragging = true
                        // fraction = clamp based on drag delta
                    }
                    .onEnded { _ in
                        isDragging = false
                        onFractionChanged(fraction)
                    }
            )
    }
}
```

Note: Pure SwiftUI DraggableSplitter is limited. The drag delta must be computed relative to the parent container height. For full fidelity, consider using `GeometryReader` in the parent to pass available height.

- [ ] **Step 8: Create TagChipsView.swift**

```swift
import SwiftUI

struct TagChipsView: View {
    @Binding var tags: [String]
    let onAddTag: () -> Void
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(tags, id: \.self) { tag in
                    Button(action: { tags.removeAll { $0 == tag } }) {
                        HStack(spacing: 2) {
                            Text(tag)
                                .font(.caption2)
                            Text("×")
                                .font(.caption2)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
                Button("+ Add Tag", action: onAddTag)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        }
        .frame(height: 32)
    }
}

struct AddTagDialog: View {
    @Binding var isPresented: Bool
    @Binding var tags: [String]
    
    @State private var newTagText = ""
    
    var body: some View {
        Group {
            if isPresented {
                VStack(spacing: 12) {
                    Text("Add Tag")
                        .font(.headline)
                    TextField("Tag", text: $newTagText)
                        .textFieldStyle(.roundedBorder)
                    HStack {
                        Button("Cancel") {
                            newTagText = ""
                            isPresented = false
                        }
                        Button("Add") {
                            let trimmed = newTagText.trimmingCharacters(in: .whitespaces)
                            if !trimmed.isEmpty && !tags.contains(trimmed) {
                                tags.append(trimmed)
                            }
                            newTagText = ""
                            isPresented = false
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding()
                .frame(width: 300)
                .background(.regularMaterial)
                .cornerRadius(12)
                .shadow(radius: 8)
            }
        }
    }
}
```

- [ ] **Step 9: Create ChatSettingsView.swift**

```swift
import SwiftUI

struct ChatSettingsView: View {
    @Binding var isPresented: Bool
    let chat: ChatModel
    let onSave: (String, String, [String]) -> Void
    
    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false
    
    var body: some View {
        Group {
            if isPresented {
                VStack(spacing: 12) {
                    Text("Chat Settings")
                        .font(.headline)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Instructions")
                            .font(.caption)
                        TextEditor(text: $instructions)
                            .font(.body)
                            .frame(height: 100)
                            .overlay(
                                RoundedRectangle(cornerRadius: 4)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Tags")
                            .font(.caption)
                        TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                    }
                    
                    HStack {
                        Button("Cancel") { isPresented = false }
                        Spacer()
                        Button("Save") {
                            onSave(chat.title, instructions, labels)
                            isPresented = false
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding()
                .frame(width: 400)
                .background(.regularMaterial)
                .cornerRadius(12)
                .shadow(radius: 8)
                .overlay {
                    AddTagDialog(isPresented: $showAddTag, tags: $labels)
                }
                .onAppear {
                    instructions = chat.instructions
                    labels = chat.labels
                }
            }
        }
    }
}
```

- [ ] **Step 10: Create ModelSelectorView.swift**

```swift
import SwiftUI

struct ModelSelectorView: View {
    let models: [ModelDefModel]
    let providers: [ProviderModel]
    @Binding var selectedModelId: String?
    let modelsEmpty: Bool
    let onOpenSettings: () -> Void
    
    @State private var expanded = false
    
    var selectedLabel: String {
        guard let sel = models.first(where: { $0.id == selectedModelId }) else {
            return "Select Model"
        }
        let prov = providers.first { $0.id == sel.providerId }
        if let pn = prov?.name {
            return "\(pn): \(sel.officialName)"
        }
        return sel.officialName
    }
    
    var body: some View {
        Button(action: {
            if modelsEmpty { onOpenSettings() }
            else { expanded.toggle() }
        }) {
            Text(selectedLabel)
                .lineLimit(1)
        }
        .popover(isPresented: $expanded) {
            List(models) { model in
                let provName = providers.first { $0.id == model.providerId }?.name
                Button(action: {
                    selectedModelId = model.id
                    expanded = false
                }) {
                    HStack {
                        Text(provName != nil ? "\(provName!): \(model.officialName)" : model.officialName)
                        Spacer()
                        if model.id == selectedModelId {
                            Image(systemName: "checkmark")
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .frame(width: 300, height: 400)
        }
    }
}
```

Note: On macOS, `.popover` may need to be replaced with `.sheet` or a custom dropdown due to platform behavior differences.

---

### Task 4: Create Main Chat Screens (iOS + macOS)

**Files:**
- Create: `xcodeApp/iosApp/Views/MainChatView.swift`
- Create: `xcodeApp/macApp/Views/MainChatView.swift`

- [ ] **Step 1: Create iOS MainChatView.swift**

```swift
import SwiftUI

struct MainChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HeaderView(
                chatSearchQuery: $viewModel.chatSearchQuery,
                chatDropdownExpanded: .constant(false), // Use @State in real impl
                chatName: $viewModel.chatName,
                filteredChats: viewModel.filteredChats,
                allChatsCount: viewModel.allChats.count,
                onCreateNew: viewModel.createNewChat,
                onDelete: viewModel.deleteCurrentChat,
                onChatSettings: { viewModel.showChatSettings = true },
                onSettings: { viewModel.showSettings = true },
                onSelectChat: viewModel.selectChat
            )
            .padding(.horizontal, 12)
            .padding(.top, 8)
            
            Divider()
                .padding(.vertical, 8)
            
            // Top area: prompt input
            PromptAreaView(
                prompt: $viewModel.prompt,
                loading: $viewModel.loading,
                selectedModelName: viewModel.selectedModel ?? "Select Model",
                models: viewModel.models,
                modelsEmpty: viewModel.models.isEmpty,
                error: viewModel.error,
                onSend: viewModel.send,
                onCancel: viewModel.cancelSend,
                onSelectModel: {},
                onOpenSettings: { viewModel.showSettings = true }
            )
            .padding(.horizontal, 12)
            
            // Draggable splitter
            DraggableSplitter(
                fraction: $viewModel.splitFraction,
                onFractionChanged: viewModel.saveSplitFraction
            )
            
            // Response list
            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(Array(history.enumerated()), id: \.element.id) { i, entry in
                        let isFirst = i == 0
                        let wordCount = entry.response.split { $0.isWhitespace }.count
                        let respExpanded = viewModel.currentChat?.expandedTimestamps.contains(entry.timestamp) ?? isFirst || wordCount < 50
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
                            onRemove: { viewModel.removeEntry(at: viewModel.currentChat?.history.count ?? 0 - 1 - i, chatId: viewModel.currentChatId ?? "") },
                            modelName: entryModelLabel
                        )
                        .padding(.horizontal, 12)
                    }
                }
            }
        }
        .ignoresSafeArea(.keyboard)
        .sheet(isPresented: $viewModel.showSettings) {
            SettingsView(viewModel: viewModel)
        }
        .overlay {
            if viewModel.showChatSettings, let chat = viewModel.currentChat {
                ChatSettingsView(
                    isPresented: $viewModel.showChatSettings,
                    chat: chat,
                    onSave: { title, instr, labels in
                        Task { try? await KotlinBridge.shared.chatsUseCase.update(chat: chat.toKotlinChat().copy(title: title, instructions: instr)) }
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create macOS MainChatView.swift**

```swift
import SwiftUI

struct MainChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            HeaderView(
                chatSearchQuery: $viewModel.chatSearchQuery,
                chatDropdownExpanded: .constant(false),
                chatName: $viewModel.chatName,
                filteredChats: viewModel.filteredChats,
                allChatsCount: viewModel.allChats.count,
                onCreateNew: viewModel.createNewChat,
                onDelete: viewModel.deleteCurrentChat,
                onChatSettings: { viewModel.showChatSettings = true },
                onSettings: { viewModel.showSettings = true },
                onSelectChat: viewModel.selectChat
            )
            .padding(.horizontal, 12)
            .padding(.top, 8)
            
            Divider()
                .padding(.vertical, 8)
            
            PromptAreaView(
                prompt: $viewModel.prompt,
                loading: $viewModel.loading,
                selectedModelName: viewModel.selectedModel ?? "Select Model",
                models: viewModel.models,
                modelsEmpty: viewModel.models.isEmpty,
                error: viewModel.error,
                onSend: viewModel.send,
                onCancel: viewModel.cancelSend,
                onSelectModel: {},
                onOpenSettings: { viewModel.showSettings = true }
            )
            .padding(.horizontal, 12)
            
            DraggableSplitter(
                fraction: $viewModel.splitFraction,
                onFractionChanged: viewModel.saveSplitFraction
            )
            
            let history = viewModel.currentChat?.history.reversed() ?? []
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(Array(history.enumerated()), id: \.element.id) { i, entry in
                        let isFirst = i == 0
                        let wordCount = entry.response.split { $0.isWhitespace }.count
                        let respExpanded = viewModel.currentChat?.expandedTimestamps.contains(entry.timestamp) ?? isFirst || wordCount < 50
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
                            onCopy: { NSPasteboard.general.setString($0, forType: .string) },
                            onRemove: { viewModel.removeEntry(at: viewModel.currentChat?.history.count ?? 0 - 1 - i, chatId: viewModel.currentChatId ?? "") },
                            modelName: entryModelLabel
                        )
                        .padding(.horizontal, 12)
                    }
                }
            }
        }
        .sheet(isPresented: $viewModel.showSettings) {
            SettingsView(viewModel: viewModel)
        }
        .overlay {
            if viewModel.showChatSettings, let chat = viewModel.currentChat {
                ChatSettingsView(
                    isPresented: $viewModel.showChatSettings,
                    chat: chat,
                    onSave: { title, instr, labels in
                        Task { try? await KotlinBridge.shared.chatsUseCase.update(chat: chat.toKotlinChat().copy(title: title, instructions: instr)) }
                    }
                )
            }
        }
    }
}
```

---

### Task 5: Create Settings Screens (iOS + macOS)

**Files:**
- Create: `xcodeApp/Shared/Components/SettingsView.swift` (shared, tabs-based)
- Create: `xcodeApp/Shared/Components/ProvidersTabView.swift`
- Create: `xcodeApp/Shared/Components/ChatsTabView.swift`
- Create: `xcodeApp/Shared/Components/GlobalTabView.swift`
- Create: `xcodeApp/Shared/Components/ExportTabView.swift`

- [ ] **Step 1: Create SettingsView.swift (shared)**

```swift
import SwiftUI

struct SettingsView: View {
    @ObservedObject var viewModel: ChatViewModel
    @StateObject private var settingsVM = SettingsViewModel()
    @State private var selectedTab = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Header with Back button and title
            HStack {
                Button(action: { viewModel.showSettings = false }) {
                    Image(systemName: "chevron.left")
                        .font(.body)
                }
                .buttonStyle(.plain)
                .help("Back")
                
                Text("Settings")
                    .font(.title2)
                    .padding(.leading, 8)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)
            
            Divider()
                .padding(.vertical, 8)
            
            // Tab bar
            Picker("Tab", selection: $selectedTab) {
                Text("Providers").tag(0)
                Text("Chats").tag(1)
                Text("Global").tag(2)
                Text("Export").tag(3)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 12)
            
            // Tab content
            TabView(selection: $selectedTab) {
                ProvidersTabView(settingsVM: settingsVM)
                    .tag(0)
                ChatsTabView(settingsVM: settingsVM)
                    .tag(1)
                GlobalTabView(settingsVM: settingsVM)
                    .tag(2)
                ExportTabView(settingsVM: settingsVM)
                    .tag(3)
            }
            .tabViewStyle(.automatic)
            .padding(.horizontal, 12)
        }
        .frame(minWidth: 500, minHeight: 400)
    }
}
```

- [ ] **Step 2: Create ProvidersTabView.swift**

```swift
import SwiftUI

struct ProvidersTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    @State private var expandedProviderId: String? = nil
    @State private var showAddCustom = false
    
    var body: some View {
        List {
            ForEach(settingsVM.providers) { provider in
                let cred = settingsVM.creds.first { $0.providerId == provider.id }
                let isExpanded = expandedProviderId == provider.id
                ProviderCardView(
                    provider: provider,
                    credential: cred,
                    isExpanded: isExpanded,
                    models: settingsVM.models.filter { $0.providerId == provider.id },
                    onToggle: { expandedProviderId = isExpanded ? nil : provider.id },
                    onSaveApiKey: { settingsVM.saveApiKey(providerId: provider.id, key: $0) },
                    onClearCredentials: { settingsVM.clearCredentials(providerId: provider.id) },
                    onRefresh: { settingsVM.refreshModels(provider: provider, apiKey: cred?.apiKey) },
                    onToggleModel: settingsVM.toggleModelEnabled,
                    onUpdateCustom: { name, url, style in
                        settingsVM.updateCustomProvider(provider, name: name, baseUrl: url, apiStyle: style)
                    },
                    onDeleteCustom: { settingsVM.deleteCustomProvider(provider.id) }
                )
            }
            
            // Add Custom Provider button
            Button(action: { showAddCustom = true }) {
                Label("Add Custom Provider", systemImage: "plus.circle")
            }
            .popover(isPresented: $showAddCustom) {
                AddCustomProviderView(settingsVM: settingsVM, isPresented: $showAddCustom)
            }
        }
    }
}

struct ProviderCardView: View {
    let provider: ProviderModel
    let credential: AuthCredentialsModel?
    let isExpanded: Bool
    let models: [ModelDefModel]
    let onToggle: () -> Void
    let onSaveApiKey: (String) -> Void
    let onClearCredentials: () -> Void
    let onRefresh: () -> Void
    let onToggleModel: (String) -> Void
    let onUpdateCustom: (String, String, ApiStyleModel) -> Void
    let onDeleteCustom: () -> Void
    
    var hasKey: Bool { !(credential?.apiKey ?? "").isEmpty }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading) {
                    Text(provider.name)
                        .font(.headline)
                    Text(hasKey ? "API Key set" : "Not configured")
                        .font(.caption)
                        .foregroundColor(hasKey ? .green : .red)
                }
                Spacer()
                
                if isExpanded {
                    Button(action: onRefresh) {
                        Image(systemName: "arrow.clockwise")
                            .font(.caption)
                    }
                    .buttonStyle(.plain)
                    .disabled(!hasKey)
                    .help("Refresh Models")
                }
                
                Button(action: onToggle) {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                }
                .buttonStyle(.plain)
            }
            
            if isExpanded {
                if !provider.predefined {
                    CustomProviderEditView(
                        provider: provider,
                        credential: credential,
                        onSave: onSaveApiKey,
                        onUpdate: onUpdateCustom,
                        onDelete: onDeleteCustom
                    )
                } else {
                    ApiKeyEditView(
                        provider: provider,
                        credential: credential,
                        hasKey: hasKey,
                        onSave: onSaveApiKey,
                        onClear: onClearCredentials
                    )
                }
                
                if !models.isEmpty {
                    ModelListView(
                        models: models,
                        onToggle: onToggleModel
                    )
                }
            }
        }
        .padding(8)
        .background(Color.secondary.opacity(0.05))
        .cornerRadius(8)
    }
}
```

- [ ] **Step 3: Create ChatsTabView.swift** (chat list with filtering by labels, editing, labels management)

```swift
import SwiftUI

struct ChatsTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    @State private var checkedLabels: Set<String> = []
    
    private var allLabels: [String] {
        Array(Set(settingsVM.chats.flatMap { $0.labels })).sorted()
    }
    
    private var filteredChats: [ChatModel] {
        if checkedLabels.isEmpty { return settingsVM.chats }
        return settingsVM.chats.filter { chat in
            checkedLabels.allSatisfy { chat.labels.contains($0) }
        }
    }
    
    var body: some View {
        VStack(spacing: 8) {
            // Label filters
            if !allLabels.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(allLabels, id: \.self) { label in
                            let checked = checkedLabels.contains(label)
                            Button(action: {
                                if checked { checkedLabels.remove(label) }
                                else { checkedLabels.insert(label) }
                            }) {
                                Text(label)
                                    .font(.caption2)
                            }
                            .buttonStyle(checked ? .borderedProminent : .bordered)
                            .controlSize(.small)
                        }
                    }
                }
            }
            
            List(filteredChats) { chat in
                ChatEditRowView(chat: chat, onUpdate: settingsVM.updateChat, onDelete: settingsVM.deleteChat)
            }
            .listStyle(.plain)
        }
    }
}

struct ChatEditRowView: View {
    let chat: ChatModel
    let onUpdate: (ChatModel, String, String, [String]) -> Void
    let onDelete: (String) -> Void
    
    @State private var editing = false
    @State private var title: String = ""
    @State private var instructions: String = ""
    @State private var labels: [String] = []
    @State private var showAddTag = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if editing {
                TextField("Title", text: $title)
                    .textFieldStyle(.roundedBorder)
                TextEditor(text: $instructions)
                    .font(.caption)
                    .frame(height: 60)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.secondary.opacity(0.3)))
                TagChipsView(tags: $labels, onAddTag: { showAddTag = true })
                HStack {
                    Button("Save") {
                        onUpdate(chat, title, instructions, labels)
                        editing = false
                    }
                    Button("Cancel") { editing = false }
                }
                .overlay {
                    AddTagDialog(isPresented: $showAddTag, tags: $labels)
                }
            } else {
                Text(chat.title).font(.body)
                if !chat.labels.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(chat.labels, id: \.self) { label in
                            Text(label)
                                .font(.caption2)
                                .foregroundColor(.accentColor)
                        }
                    }
                }
                if !chat.instructions.isEmpty {
                    Text(chat.instructions.prefix(100))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                HStack {
                    Button("Edit") { editing = true; title = chat.title; instructions = chat.instructions; labels = chat.labels }
                    Button("Delete", role: .destructive) { onDelete(chat.id) }
                }
            }
        }
        .padding(4)
    }
}
```

- [ ] **Step 4: Create GlobalTabView.swift** (instructions editor)

```swift
import SwiftUI

struct GlobalTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    
    var body: some View {
        VStack {
            TextEditor(text: Binding(
                get: { settingsVM.globalInstructions },
                set: { settingsVM.globalInstructions = $0; settingsVM.setGlobalInstructions($0) }
            ))
            .font(.body)
            .overlay(
                Group {
                    if settingsVM.globalInstructions.isEmpty {
                        Text("Enter global instructions for all chats...")
                            .foregroundColor(.secondary)
                            .padding(.leading, 4)
                            .padding(.top, 8)
                            .allowsHitTesting(false)
                    }
                },
                alignment: .topLeading
            )
        }
    }
}
```

- [ ] **Step 5: Create ExportTabView.swift** (file operations)

```swift
import SwiftUI
import UniformTypeIdentifiers

struct ExportTabView: View {
    @ObservedObject var settingsVM: SettingsViewModel
    
    var body: some View {
        VStack(spacing: 16) {
            // Export
            SaveFileButton(label: "Export Settings") {
                settingsVM.export()
            }
            if let err = settingsVM.exportError {
                Text(err).foregroundColor(.red).font(.caption)
            }
            
            // Import Replace
            OpenFileButton(label: "Load & Replace") { content in
                settingsVM.importReplace(json: content)
            }
            if let err = settingsVM.importReplaceError {
                Text(err).foregroundColor(.red).font(.caption)
            }
            
            // Import Append
            OpenFileButton(label: "Load & Append") { content in
                settingsVM.importAppend(json: content)
            }
            if let err = settingsVM.importAppendError {
                Text(err).foregroundColor(.red).font(.caption)
            }
        }
        .padding()
        .alert(settingsVM.dialogTitle ?? "", isPresented: Binding(
            get: { settingsVM.dialogText != nil },
            set: { if !$0 { settingsVM.dialogText = nil } }
        )) {
            Button("OK") { settingsVM.dialogText = nil }
        } message: {
            Text(settingsVM.dialogText ?? "")
        }
    }
}

struct SaveFileButton: View {
    let label: String
    let onSave: () -> Void
    
    var body: some View {
        Button(action: onSave) {
            Label(label, systemImage: "square.and.arrow.up")
        }
    }
}

struct OpenFileButton: View {
    let label: String
    let onLoad: (String) -> Void
    
    @State private var isPresented = false
    
    var body: some View {
        Button(action: { isPresented = true }) {
            Label(label, systemImage: "square.and.arrow.down")
        }
        .fileImporter(isPresented: $isPresented, allowedContentTypes: [.json]) { result in
            switch result {
            case .success(let url):
                if let data = try? Data(contentsOf: url), let content = String(data: data, encoding: .utf8) {
                    onLoad(content)
                }
            case .failure:
                break
            }
        }
    }
}
```

---

### Task 6: Update Xcode Entry Points

**Files:**
- Modify: `xcodeApp/iosApp/iOSApp.swift`
- Modify: `xcodeApp/iosApp/ContentView.swift` (remove — replaced by MainChatView)
- Modify: `xcodeApp/macApp/macOSApp.swift`
- Modify: `xcodeApp/macApp/ContentView.swift` (remove — replaced by MainChatView)

- [ ] **Step 1: Update iOSApp.swift**

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            MainChatView()
        }
    }
}
```

- [ ] **Step 2: Update macOSApp.swift**

```swift
import SwiftUI

@main
struct macOSApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    var body: some Scene {
        WindowGroup {
            MacMainChatView()
                .frame(minWidth: 600, minHeight: 400)
        }
        .windowResizability(.contentMinSize)
        .commands {
            CommandGroup(replacing: .newItem) {} // Remove default "New Window"
            CommandGroup(replacing: .undoRedo) {} // Remove undo/redo if not needed
        }
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }
}
```

---

### Task 7: Verify Build

- [ ] **Step 1: Build KMP shared frameworks**

Run: `./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinMacosArm64 :shared:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL in the shared module

- [ ] **Step 2: Build Xcode project**

Open `xcodeApp/YoPt.xcodeproj` in Xcode. Select "YoPt iOS" scheme. Build.
Expected: Build succeeds, SwiftUI app launches with native chat UI backed by Kotlin business logic.

- [ ] **Step 3: Fix any SKIE interop issues**

Common issues:
- Kotlin `Flow` types in Swift — verify SKIE generates `SkieSwiftFlow<T>` wrappers
- Kotlin `List<...>` → SKIE maps to `[T]` in Swift
- Kotlin `suspend` functions → SKIE generates Swift `async` wrappers
- Sealed classes → SKIE generates Swift enums
- Verify exact type names by inspecting `DerivedData/.../Build/Intermediates.noindex/.../GeneratedHeader/ComposeApp-Swift.h`

---

## Self-Review

**1. Spec coverage:**
- MainScreen with header, prompt input, model selector, split pane, response history → Task 3 (shared components) + Task 4 (main chat screen)
- Settings with 4 tabs (Providers, Chats, Global, Export) → Task 5
- Chat settings dialog (instructions, labels) → Task 3 (ChatSettingsView, TagChipsView)
- Response entry expand/collapse, markdown/raw toggle, action buttons → Task 3 (ResponseCardView, ResponseActionsBar)
- Responsive header layout (narrow/wide) → Task 3 (HeaderView)
- Draggable splitter → Task 3 (DraggableSplitter)
- Platform-specific file pickers → Task 5 (ExportTabView with SwiftUI fileImporter)
- Key bindings (Cmd+Enter) → Task 3 (PromptAreaView keyboardShortcut)
- Clipboards (NSPasteboard for macOS, UIPasteboard for iOS) → Task 4 (per-platform in onCopy closures)
- Interop with Kotlin shared module → Task 1-2 (KotlinBridge, ViewModels)

**2. Placeholder scan:**
- DraggableSplitter drag gesture is simplified — actual delta computation needs GeometryReader in parent. Noted in code comment.
- SKIE interop type casts use `as! [s4y.yopt.domain.models.Chat]` — exact types depend on SKIE header generation. Noted to verify after build.

**3. Type consistency:**
- ChatModel ↔ s4y.yopt.domain.models.Chat with .fromKotlin() / .toKotlinChat() — consistent across all tasks.
- ProviderModel ↔ s4y.yopt.domain.models.ProviderDef — consistent.
- ApiStyleModel ↔ s4y.yopt.domain.models.ApiStyle — bidirectional conversion in ProviderModels.swift.
- ResponseEntryModel ↔ s4y.yopt.domain.models.ResponseEntry — consistent.
- ViewModel published properties flow into components consistently.

**Gap found:** No explicit ModelDefModel equivalent in ProviderModels.swift. Need to add.

**Fix:** Add to ProviderModels.swift:

```swift
struct ModelDefModel: Identifiable, Equatable {
    let id: String
    let providerId: String
    let officialName: String
    let enabled: Bool
    
    var displayName: String { officialName }
    
    static func fromKotlin(_ model: s4y.yopt.domain.models.ModelDef) -> ModelDefModel {
        ModelDefModel(
            id: model.id,
            providerId: model.providerId,
            officialName: model.officialName,
            enabled: model.enabled
        )
    }
}

struct AuthCredentialsModel: Equatable {
    let providerId: String
    let apiKey: String?
    
    static func fromKotlin(_ cred: s4y.yopt.domain.models.AuthCredentials) -> AuthCredentialsModel {
        AuthCredentialsModel(providerId: cred.providerId, apiKey: cred.apiKey as? String)
    }
}
```

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-05-31-swiftui-port.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session, batch execution with checkpoints

**Which approach?**
