package s4y.yopt.adapters

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.ports.SecureStore
import s4y.yopt.domain.services.AuthService
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeCredentialsTest {
    private lateinit var tempDir: java.io.File
    private lateinit var repo: AuthService

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("yopt_auth_test").toFile()
        repo = AuthService(SecureStore(tempDir.absolutePath))
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun cred(provider: String, key: String = "key_$provider") =
        AuthCredentials(providerId = provider, apiKey = key)

    @Test
    fun mergeCredentials_newProvider_adds() = runTest {
        repo.importAll(listOf(cred("p1")))
        repo.mergeCredentials(listOf(cred("p2")))
        val ids = repo.observeCredentials().first().map { it.providerId }
        assertTrue("p1" in ids)
        assertTrue("p2" in ids)
    }

    @Test
    fun mergeCredentials_existingProvider_skips() = runTest {
        repo.importAll(listOf(cred("p1", key = "original")))
        repo.mergeCredentials(listOf(cred("p1", key = "incoming")))
        val result = repo.observeCredentials().first().first { it.providerId == "p1" }
        assertEquals("original", result.apiKey)
    }

    @Test
    fun mergeCredentials_duplicateIncomingProviders_firstWins() = runTest {
        repo.importAll(emptyList())
        repo.mergeCredentials(listOf(
            cred("p1", key = "first"),
            cred("p1", key = "second")
        ))
        val results = repo.observeCredentials().first().filter { it.providerId == "p1" }
        assertEquals(1, results.size)
        assertEquals("first", results.first().apiKey)
    }

    @Test
    fun mergeCredentials_emptyIncoming_noChange() = runTest {
        repo.importAll(listOf(cred("p1")))
        repo.mergeCredentials(emptyList())
        val ids = repo.observeCredentials().first().map { it.providerId }
        assertEquals(listOf("p1"), ids)
    }

    @Test
    fun mergeCredentials_emptyLocal_addsAll() = runTest {
        repo.importAll(emptyList())
        repo.mergeCredentials(listOf(cred("p1"), cred("p2")))
        val ids = repo.observeCredentials().first().map { it.providerId }.toSet()
        assertEquals(setOf("p1", "p2"), ids)
    }
}
