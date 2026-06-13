package s4y.yopt.domain.services

import s4y.yopt.domain.models.Chat

object ChatTagFilter {
    fun filter(chats: List<Chat>, query: String, selectedTags: Set<String>): List<Chat> =
        chats.filter { chat ->
            val tagGate = selectedTags.isEmpty() ||
                selectedTags.all { tag -> chat.labels.contains(tag) }
            val textGate = query.isBlank() ||
                chat.title.contains(query, ignoreCase = true) ||
                chat.labels.any { label -> label.contains(query, ignoreCase = true) }
            tagGate && textGate
        }
}
