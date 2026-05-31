package s4y.yopt.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ResponseEntry
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.usecases.currentTimeMillis

class ChatService(private val kv: KeyValueStore) {
    private val _chats = MutableStateFlow(load())

    fun observeAll(): Flow<List<Chat>> = _chats

    suspend fun findByName(name: String): Chat? =
        _chats.value.find { it.title == name }

    suspend fun create(title: String, instructions: String, labels: List<String>): Chat {
        val c = Chat("chat_${currentTimeMillis()}", title, instructions, labels = labels)
        _chats.value = _chats.value + c
        save()
        return c
    }

    suspend fun appendHistory(chatId: String, entry: ResponseEntry) {
        _chats.value = _chats.value.map {
            if (it.id == chatId) it.copy(history = it.history + entry) else it
        }
        save()
    }

    suspend fun removeHistoryEntry(chatId: String, index: Int) {
        _chats.value = _chats.value.map {
            if (it.id == chatId) {
                it.copy(history = it.history.filterIndexed { i, _ -> i != index })
            } else it
        }
        save()
    }

    suspend fun toggleEntryMarkdown(chatId: String, entryTimestamp: Long) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    history = chat.history.map { entry ->
                        if (entry.timestamp == entryTimestamp) entry.copy(showMarkdown = !entry.showMarkdown) else entry
                    }
                )
            } else chat
        }
        save()
    }

    suspend fun setEntryExpanded(chatId: String, entryTimestamp: Long, expanded: Boolean) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) chat.copy(
                expandedTimestamps = if (expanded)
                    chat.expandedTimestamps + entryTimestamp
                else
                    chat.expandedTimestamps - entryTimestamp
            ) else chat
        }
        save()
    }

    suspend fun importAll(chats: List<Chat>) {
        _chats.value = chats
        save()
    }

    suspend fun mergeChats(chats: List<Chat>) {
        val current = _chats.value.toMutableList()
        val localById = current.associateBy { it.id }.toMutableMap()
        for (incoming in chats) {
            val existing = localById[incoming.id]
            if (existing == null) {
                current.add(incoming)
                localById[incoming.id] = incoming
            } else {
                val merged = existing.copy(
                    history = existing.history + incoming.history.filter { it !in existing.history }
                )
                val idx = current.indexOfFirst { it.id == incoming.id }
                current[idx] = merged
                localById[incoming.id] = merged
            }
        }
        _chats.value = current
        save()
    }

    suspend fun update(chat: Chat) {
        _chats.value = _chats.value.map { if (it.id == chat.id) chat else it }
        save()
    }

    suspend fun delete(chatId: String) {
        _chats.value = _chats.value.filter { it.id != chatId }
        if (_chats.value.isEmpty()) {
            _chats.value = listOf(Chat("default", "General", "You are a helpful assistant."))
        }
        save()
    }

    private fun load(): List<Chat> {
        return try {
            kv.getString("chats")?.let { Json.decodeFromString(it) }
        } catch (_: Exception) { null }
            ?: listOf(Chat("default", "General", "You are a helpful assistant."))
    }

    private fun save() {
        kv.putString("chats", Json.encodeToString(_chats.value))
    }
}
