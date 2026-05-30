package s4y.yopt.usecases

import kotlinx.coroutines.flow.Flow
import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ResponseEntry
import s4y.yopt.domain.services.ChatService

class ManageChatsUseCase(private val chats: ChatService) {
    fun observeAll(): Flow<List<Chat>> = chats.observeAll()
    suspend fun findByName(name: String): Chat? = chats.findByName(name)
    suspend fun create(
        title: String,
        instructions: String = "",
        labels: List<String> = emptyList()
    ): Chat = chats.create(title, instructions, labels)
    suspend fun appendHistory(chatId: String, entry: ResponseEntry) =
        chats.appendHistory(chatId, entry)
    suspend fun removeHistoryEntry(chatId: String, index: Int) =
        chats.removeHistoryEntry(chatId, index)
    suspend fun toggleEntryMarkdown(chatId: String, entryTimestamp: Long) =
        chats.toggleEntryMarkdown(chatId, entryTimestamp)
    suspend fun update(chat: Chat) = chats.update(chat)
    suspend fun delete(chatId: String) = chats.delete(chatId)
}
