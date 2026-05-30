package s4y.yopt.adapters

import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.domain.ports.SecureStore
import s4y.yopt.domain.services.AppPreferencesService
import s4y.yopt.domain.services.AuthService
import s4y.yopt.domain.services.ChatService
import s4y.yopt.domain.services.ModelService
import s4y.yopt.domain.services.SettingsService
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MergeGlobalInstructionsTest {
    private lateinit var tmpDir: java.io.File

    @BeforeTest
    fun setUp() {
        tmpDir = Files.createTempDirectory("yopt_gi_test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    private suspend fun makeRepo(initial: String = ""): Pair<SettingsService, AppPreferencesService> {
        val kvFile = java.io.File(tmpDir, "prefs.properties")
        val prefs = AppPreferencesService(KeyValueStore(kvFile.absolutePath))
        if (initial.isNotEmpty()) prefs.setGlobalInstructions(initial)
        val repo = SettingsService(
            models = ModelService(KeyValueStore(kvFile.absolutePath)),
            chats = ChatService(KeyValueStore(kvFile.absolutePath)),
            authService = AuthService(SecureStore(tmpDir.absolutePath)),
            prefs = prefs
        )
        return repo to prefs
    }

    private fun exportJson(globalInstructions: String) =
        """{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"$globalInstructions"}"""

    @Test
    fun mergeGlobalInstructions_emptyIncoming_keepsExisting() = runTest {
        val (repo, get) = makeRepo("existing")
        repo.importAppend(exportJson(""))
        assertEquals("existing", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_blankIncoming_keepsExisting() = runTest {
        val (repo, get) = makeRepo("existing")
        repo.importAppend(exportJson("   "))
        assertEquals("existing", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_identicalIncoming_noChange() = runTest {
        val (repo, get) = makeRepo("hello")
        repo.importAppend(exportJson("hello"))
        assertEquals("hello", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_emptyExisting_setsIncoming() = runTest {
        val (repo, get) = makeRepo("")
        repo.importAppend(exportJson("new"))
        assertEquals("new", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_incomingSubstringOfExisting_noChange() = runTest {
        val (repo, get) = makeRepo("hello world")
        repo.importAppend(exportJson("hello"))
        assertEquals("hello world", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_existingSubstringOfIncoming_noChange() = runTest {
        val (repo, get) = makeRepo("hello")
        repo.importAppend(exportJson("hello world"))
        assertEquals("hello", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_disjoint_appendsWithNewline() = runTest {
        val (repo, get) = makeRepo("first")
        repo.importAppend(exportJson("second"))
        assertEquals("first\nsecond", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_trimmedIncoming_noDoubleNewline() = runTest {
        val (repo, get) = makeRepo("first")
        repo.importAppend(exportJson("  second  "))
        assertEquals("first\nsecond", get.getGlobalInstructions())
    }

    @Test
    fun mergeGlobalInstructions_repeatedAppend_noGrowth() = runTest {
        val (repo, get) = makeRepo("first")
        repo.importAppend(exportJson("second"))
        val after1 = get.getGlobalInstructions()
        repo.importAppend(exportJson("second"))
        assertEquals(after1, get.getGlobalInstructions())
    }
}
