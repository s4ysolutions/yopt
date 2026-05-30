package s4y.yopt.adapters

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ResponseEntry
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.domain.services.ChatService
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeChatsTest {
    private lateinit var tempFile: java.io.File
    private lateinit var repo: ChatService

    @BeforeTest
    fun setUp() {
        tempFile = Files.createTempFile("yopt_chat_test", ".properties").toFile()
        repo = ChatService(KeyValueStore(tempFile.absolutePath))
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    private fun entry(prompt: String, response: String = "") = ResponseEntry(
        timestamp = 0L,
        prompt = prompt,
        response = response,
        modelId = "m",
        modelName = "Model"
    )

    private fun chat(id: String, title: String = id, history: List<ResponseEntry> = emptyList()) =
        Chat(id = id, title = title, instructions = "inst_$id", history = history)

    // Seed repo with a known chat, bypassing default "General" chat
    private suspend fun seed(vararg chats: Chat) {
        repo.importAll(chats.toList())
    }

    @Test
    fun mergeChats_newId_appendsChat() = runTest {
        seed(chat("a"))
        repo.mergeChats(listOf(chat("b")))
        val ids = repo.observeAll().first().map { it.id }
        assertTrue("a" in ids)
        assertTrue("b" in ids)
    }

    @Test
    fun mergeChats_existingId_keepsLocalMetadata() = runTest {
        val local = chat("a", title = "local title")
        seed(local)
        repo.mergeChats(listOf(chat("a", title = "incoming title")))
        val result = repo.observeAll().first().first { it.id == "a" }
        assertEquals("local title", result.title)
        assertEquals("inst_a", result.instructions)
    }

    @Test
    fun mergeChats_existingId_unionsHistory() = runTest {
        val localEntry = entry("local prompt")
        seed(chat("a", history = listOf(localEntry)))
        val incomingEntry = entry("incoming prompt")
        repo.mergeChats(listOf(chat("a", history = listOf(incomingEntry))))
        val history = repo.observeAll().first().first { it.id == "a" }.history
        assertEquals(2, history.size)
        assertTrue(localEntry in history)
        assertTrue(incomingEntry in history)
    }

    @Test
    fun mergeChats_existingId_deduplicatesHistory() = runTest {
        val sharedEntry = entry("shared")
        seed(chat("a", history = listOf(sharedEntry)))
        repo.mergeChats(listOf(chat("a", history = listOf(sharedEntry))))
        val history = repo.observeAll().first().first { it.id == "a" }.history
        assertEquals(1, history.size)
    }

    @Test
    fun mergeChats_duplicateIdsInIncoming_firstWins() = runTest {
        seed(/* empty — use default */ )
        val importedChats = listOf(
            chat("x", title = "first"),
            chat("x", title = "second")
        )
        repo.importAll(emptyList()) // clear defaults
        repo.mergeChats(importedChats)
        val results = repo.observeAll().first().filter { it.id == "x" }
        assertEquals(1, results.size)
        assertEquals("first", results.first().title)
    }

    @Test
    fun mergeChats_multipleNewChats_allAppended() = runTest {
        seed(chat("a"))
        repo.mergeChats(listOf(chat("b"), chat("c")))
        val ids = repo.observeAll().first().map { it.id }
        assertEquals(3, ids.size)
        assertTrue("a" in ids && "b" in ids && "c" in ids)
    }
}
