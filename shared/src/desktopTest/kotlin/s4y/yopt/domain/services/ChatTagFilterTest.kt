package s4y.yopt.domain.services

import s4y.yopt.domain.models.Chat
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatTagFilterTest {

    private fun chat(id: String, title: String, labels: List<String>) =
        Chat(id = id, title = title, labels = labels)

    private val chats = listOf(
        chat("1", "KMP build error", listOf("kotlin", "gradle")),
        chat("2", "Coroutine question", listOf("kotlin")),
        chat("3", "Trip plan", listOf("travel")),
        chat("4", "Untagged note", emptyList()),
    )

    @Test
    fun `no query and no tags returns all`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = emptySet())
        assertEquals(listOf("1", "2", "3", "4"), result.map { it.id })
    }

    @Test
    fun `single tag keeps only chats carrying it`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("kotlin"))
        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun `multiple tags require all to be present`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("kotlin", "gradle"))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `query matches title case-insensitively`() {
        val result = ChatTagFilter.filter(chats, query = "trip", selectedTags = emptySet())
        assertEquals(listOf("3"), result.map { it.id })
    }

    @Test
    fun `query matches a label`() {
        val result = ChatTagFilter.filter(chats, query = "gradle", selectedTags = emptySet())
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `query and tags combine with AND`() {
        val result = ChatTagFilter.filter(chats, query = "coroutine", selectedTags = setOf("kotlin"))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `tag absent from every chat yields empty`() {
        val result = ChatTagFilter.filter(chats, query = "", selectedTags = setOf("nonexistent"))
        assertEquals(emptyList(), result.map { it.id })
    }
}
