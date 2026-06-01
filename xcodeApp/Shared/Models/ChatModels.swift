import Foundation
import ComposeApp

/// Mirrors Chat (from KMP YoPtShared)
struct ChatModel: Identifiable, Equatable {
    let id: String
    var title: String
    var instructions: String
    var defaultModelId: String?
    var labels: [String]
    var expandedTimestamps: Set<Int64>
    var history: [ResponseEntryModel]

    static func fromKotlin(_ chat: Chat) -> ChatModel {
        ChatModel(
            id: chat.id,
            title: chat.title,
            instructions: chat.instructions,
            defaultModelId: chat.defaultModelId,
            labels: chat.labels,
            expandedTimestamps: Set(chat.expandedTimestamps.map { $0.int64Value }),
            history: chat.history.map(ResponseEntryModel.fromKotlin)
        )
    }

    func toKotlinChat() -> Chat {
        Chat(
            id: id,
            title: title,
            instructions: instructions,
            defaultModelId: defaultModelId,
            labels: labels,
            expandedTimestamps: Set(expandedTimestamps.map { KotlinLong(longLong: $0) }),
            history: history.map { $0.toKotlinEntry() }
        )
    }
}

/// Mirrors ResponseEntry (from KMP YoPtShared)
struct ResponseEntryModel: Identifiable, Equatable {
    var id: Int64 { timestamp }
    let timestamp: Int64
    let prompt: String
    let response: String
    let modelId: String
    let modelName: String
    let durationMs: Int64
    let showMarkdown: Bool

    static func fromKotlin(_ entry: ResponseEntry) -> ResponseEntryModel {
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

    func toKotlinEntry() -> ResponseEntry {
        ResponseEntry(
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
