package s4y.yopt.adapters

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.models.Chat
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.models.ResponseEntry
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.domain.ports.SecureStore
import s4y.yopt.domain.services.AuthService
import s4y.yopt.domain.services.AppPreferencesService
import s4y.yopt.domain.services.ChatService
import s4y.yopt.domain.services.ModelService
import s4y.yopt.domain.services.SettingsService
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end tests for export → import (replace) and export → importAppend (merge)
 * through SettingsService backed by ChatService, ModelService,
 * AuthService, AppPreferencesService.
 */
class ExportImportTest {
    private lateinit var tempDir: java.io.File
    private lateinit var chatRepo: ChatService
    private lateinit var modelRepo: ModelService
    private lateinit var authRepo: AuthService
    private lateinit var prefs: AppPreferencesService
    private lateinit var settingsRepo: SettingsService

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("yopt_export_test").toFile()
        chatRepo = ChatService(KeyValueStore(File(tempDir, "chat.props").absolutePath))
        modelRepo = ModelService(KeyValueStore(File(tempDir, "model.props").absolutePath))
        authRepo = AuthService(SecureStore(File(tempDir, "auth").absolutePath))
        prefs = AppPreferencesService(KeyValueStore(File(tempDir, "prefs.props").absolutePath))
        settingsRepo = SettingsService(modelRepo, chatRepo, authRepo, prefs)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ---- helpers ----

    private fun model(id: String, provider: String = "p") =
        ModelDef(id = id, providerId = provider, officialName = "Model $id")

    private fun chat(id: String, title: String = id, instructions: String = "inst_$id", history: List<ResponseEntry> = emptyList()) =
        Chat(id = id, title = title, instructions = instructions, history = history)

    private fun cred(provider: String, key: String = "key_$provider") =
        AuthCredentials(providerId = provider, apiKey = key)

    private fun entry(text: String) = ResponseEntry(
        timestamp = 0L, prompt = text, response = "resp_$text",
        modelId = "m", modelName = "Model"
    )

    private suspend fun seed(
        models: List<ModelDef> = emptyList(),
        chats: List<Chat> = emptyList(),
        auth: List<AuthCredentials> = emptyList(),
        instructions: String = ""
    ) {
        models.groupBy { it.providerId }.forEach { (pid, ms) ->
            modelRepo.upsertModels(pid, ms)
        }
        chatRepo.importAll(chats)
        authRepo.importAll(auth)
        prefs.setGlobalInstructions(instructions)
    }

    // ===================================================================
    // EXPORT
    // ===================================================================

    @Test
    fun export_emptyState_serializesValidJson() = runTest {
        val exported = settingsRepo.export()
        // encodeDefaults=false: v and globalInstructions with defaults not serialized
        assertTrue(exported.contains("\"models\""))
        assertTrue(exported.contains("\"chats\""))
        assertTrue(exported.contains("\"auth\""))
    }

    @Test
    fun export_roundTripsAllData() = runTest {
        seed(
            models = listOf(model("m1"), model("m2")),
            chats = listOf(chat("c1"), chat("c2")),
            auth = listOf(cred("p1"), cred("p2")),
            instructions = "be helpful"
        )
        val exported = settingsRepo.export()

        // Re-import into clean state and verify identical data
        setUp() // reset
        settingsRepo.import(exported)

        val models = modelRepo.getAllModels().map { it.id }.toSet()
        assertEquals(setOf("m1", "m2"), models)

        val chatIds = chatRepo.observeAll().first().map { it.id }.toSet()
        assertEquals(setOf("c1", "c2"), chatIds)

        val authIds = authRepo.observeCredentials().first().map { it.providerId }.toSet()
        assertEquals(setOf("p1", "p2"), authIds)

        assertEquals("be helpful", prefs.getGlobalInstructions())
    }

    @Test
    fun export_includesGlobalInstructions() = runTest {
        prefs.setGlobalInstructions("you are a pirate")
        val exported = settingsRepo.export()
        assertTrue(exported.contains("you are a pirate"))
    }

    @Test
    fun export_includesChatHistory() = runTest {
        seed(chats = listOf(chat("c1", history = listOf(entry("q1"), entry("q2")))))
        val exported = settingsRepo.export()
        assertTrue(exported.contains("q1"))
        assertTrue(exported.contains("q2"))
    }

    @Test
    fun export_disabledModel_roundTripsEnabledState() = runTest {
        // Round-trip: disabled model stays disabled after import
        seed(models = listOf(model("m1").copy(enabled = false), model("m2")))
        val exported = settingsRepo.export()
        setUp() // reset
        settingsRepo.import(exported)
        val models = modelRepo.getAllModels()
        val m1 = models.first { it.id == "m1" }
        val m2 = models.first { it.id == "m2" }
        assertEquals(false, m1.enabled)
        assertEquals(true, m2.enabled)
    }

    @Test
    fun export_chatWithLabels_serializesLabels() = runTest {
        seed(chats = listOf(chat("c1").copy(labels = listOf("tag1", "tag2"))))
        val exported = settingsRepo.export()
        assertTrue(exported.contains("\"labels\""))
        assertTrue(exported.contains("\"tag1\""))
        assertTrue(exported.contains("\"tag2\""))
    }

    @Test
    fun export_chatWithDefaultModelId_serializesField() = runTest {
        seed(chats = listOf(chat("c1").copy(defaultModelId = "m1")))
        val exported = settingsRepo.export()
        assertTrue(exported.contains("\"defaultModelId\""))
        assertTrue(exported.contains("\"m1\""))
    }

    @Test
    fun export_nullApiKey_omitted_encodeDefaultsFalse() = runTest {
        // null is default for nullable → omitted by encodeDefaults=false
        seed(auth = listOf(cred("p1", key = "k1"), AuthCredentials("p2", apiKey = null)))
        val exported = settingsRepo.export()
        // apiKey:null omitted; only k1's apiKey appears
        assertTrue(exported.contains("\"k1\""))
        assertTrue(!exported.contains("null"))
    }

    @Test
    fun export_responseEntryDurationMs_serializedWhenNonZero() = runTest {
        val e = ResponseEntry(123L, "q", "r", "m", "Model", durationMs = 999)
        seed(chats = listOf(chat("c1", history = listOf(e))))
        val exported = settingsRepo.export()
        assertTrue(exported.contains("\"durationMs\""))
        assertTrue(exported.contains("999"))
    }

    // ===================================================================
    // IMPORT (replace)
    // ===================================================================

    @Test
    fun import_replacesAllModels() = runTest {
        seed(models = listOf(model("old1"), model("old2")))

        settingsRepo.import("""{"v":1,"models":[{"id":"new1","providerId":"p","officialName":"New 1","enabled":true}],"chats":[],"auth":[],"globalInstructions":""}""")

        val ids = modelRepo.getAllModels().map { it.id }
        assertEquals(listOf("new1"), ids)
    }

    @Test
    fun import_replacesAllChats() = runTest {
        seed(chats = listOf(chat("old1"), chat("old2")))

        settingsRepo.import("""{"v":1,"models":[],"chats":[{"id":"new1","title":"New Chat","instructions":"be nice","labels":[],"history":[]}],"auth":[],"globalInstructions":""}""")

        val ids = chatRepo.observeAll().first().map { it.id }
        assertEquals(listOf("new1"), ids)
    }

    @Test
    fun import_replacesAllAuth() = runTest {
        seed(auth = listOf(cred("old1"), cred("old2")))

        settingsRepo.import("""{"v":1,"models":[],"chats":[],"auth":[{"providerId":"new1","apiKey":"k1"}],"globalInstructions":""}""")

        val ids = authRepo.observeCredentials().first().map { it.providerId }
        assertEquals(listOf("new1"), ids)
    }

    @Test
    fun import_replacesGlobalInstructions() = runTest {
        prefs.setGlobalInstructions("old instructions")

        settingsRepo.import("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"new instructions"}""")

        assertEquals("new instructions", prefs.getGlobalInstructions())
    }

    @Test
    fun import_emptyLists_clearsChatsAuthAndInstructions() = runTest {
        seed(
            models = listOf(model("m1")),
            chats = listOf(chat("c1")),
            auth = listOf(cred("p1")),
            instructions = "something"
        )

        settingsRepo.import("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":""}""")

        // models NOT cleared: groupBy on empty list produces empty map, forEach never runs
        assertEquals(1, modelRepo.getAllModels().size)
        // chats, auth, and instructions ARE replaced with empty
        assertTrue(chatRepo.observeAll().first().isEmpty())
        assertTrue(authRepo.observeCredentials().first().isEmpty())
        assertEquals("", prefs.getGlobalInstructions())
    }

    @Test
    fun import_modelsGroupedByProvider() = runTest {
        val json = """
        {"v":1,
         "models":[
           {"id":"a","providerId":"p1","officialName":"A","enabled":true},
           {"id":"b","providerId":"p1","officialName":"B","enabled":true},
           {"id":"c","providerId":"p2","officialName":"C","enabled":true}
         ],
         "chats":[],"auth":[],"globalInstructions":""}
        """.trimIndent().replace("\n", "")

        settingsRepo.import(json)
        val byProvider = modelRepo.getAllModels().groupBy { it.providerId }
        assertEquals(2, byProvider.size)
        assertEquals(2, byProvider["p1"]!!.size)
        assertEquals(1, byProvider["p2"]!!.size)
    }

    @Test
    fun import_preservesAllChatFields() = runTest {
        settingsRepo.import("""{"v":1,"models":[],"chats":[{"id":"c1","title":"My Chat","instructions":"be brief","defaultModelId":"m1","labels":["tag1","tag2"],"history":[{"timestamp":123,"prompt":"hi","response":"hello","modelId":"m1","modelName":"Model","durationMs":500}]}],"auth":[],"globalInstructions":""}""")

        val chat = chatRepo.observeAll().first().first()
        assertEquals("c1", chat.id)
        assertEquals("My Chat", chat.title)
        assertEquals("be brief", chat.instructions)
        assertEquals("m1", chat.defaultModelId)
        assertEquals(listOf("tag1", "tag2"), chat.labels)
        assertEquals(1, chat.history.size)
        assertEquals(123L, chat.history.first().timestamp)
        assertEquals(500L, chat.history.first().durationMs)
    }

    @Test
    fun import_malformedJson_throwsException() = runTest {
        var threw = false
        try {
            settingsRepo.import("not valid json {{{")
        } catch (_: Exception) {
            threw = true
        }
        assertTrue(threw, "Expected exception for malformed JSON")
    }

    @Test
    fun import_disabledModel_roundTrips() = runTest {
        val json = """{"v":1,"models":[{"id":"a","providerId":"p","officialName":"A","enabled":false}],"chats":[],"auth":[],"globalInstructions":""}"""
        settingsRepo.import(json)
        val m = modelRepo.getAllModels().first()
        assertEquals(false, m.enabled)
    }

    @Test
    fun import_modelDefaultsEnabledToTrue() = runTest {
        // "enabled" field omitted → default true
        val json = """{"v":1,"models":[{"id":"a","providerId":"p","officialName":"A"}],"chats":[],"auth":[],"globalInstructions":""}"""
        settingsRepo.import(json)
        val m = modelRepo.getAllModels().first()
        assertEquals(true, m.enabled)
    }

    @Test
    fun import_nullApiKey_roundTrips() = runTest {
        val json = """{"v":1,"models":[],"chats":[],"auth":[{"providerId":"p1","apiKey":null}],"globalInstructions":""}"""
        settingsRepo.import(json)
        val creds = authRepo.observeCredentials().first()
        assertEquals(null, creds.first().apiKey)
    }

    @Test
    fun import_chatWithLabels_roundTrips() = runTest {
        val json = """{"v":1,"models":[],"chats":[{"id":"c1","title":"T","instructions":"","labels":["a","b"],"history":[]}],"auth":[],"globalInstructions":""}"""
        settingsRepo.import(json)
        val c = chatRepo.observeAll().first().first()
        assertEquals(listOf("a", "b"), c.labels)
    }

    @Test
    fun import_completeOverwrite_oldDataGone() = runTest {
        seed(models = listOf(model("old")), chats = listOf(chat("old")), auth = listOf(cred("old")), instructions = "old")
        val json = """{"v":1,"models":[{"id":"new","providerId":"p","officialName":"New","enabled":true}],"chats":[{"id":"new","title":"New","instructions":"","labels":[],"history":[]}],"auth":[{"providerId":"new","apiKey":"k"}],"globalInstructions":"new"}"""
        settingsRepo.import(json)
        assertEquals(listOf("new"), modelRepo.getAllModels().map { it.id })
        assertEquals(listOf("new"), chatRepo.observeAll().first().map { it.id })
        assertEquals(listOf("new"), authRepo.observeCredentials().first().map { it.providerId })
        assertEquals("new", prefs.getGlobalInstructions())
    }

    @Test
    fun import_globalInstructionsWithNewlines_roundTrips() = runTest {
        val json = """{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"line1\nline2"}"""
        settingsRepo.import(json)
        assertEquals("line1\nline2", prefs.getGlobalInstructions())
    }

    // ===================================================================
    // IMPORT APPEND (merge)
    // ===================================================================

    @Test
    fun importAppend_newModelIds_added() = runTest {
        seed(models = listOf(model("existing")))
        settingsRepo.importAppend("""{"v":1,"models":[{"id":"new","providerId":"p","officialName":"New","enabled":true}],"chats":[],"auth":[],"globalInstructions":""}""")

        val ids = modelRepo.getAllModels().map { it.id }.toSet()
        assertTrue("existing" in ids)
        assertTrue("new" in ids)
    }

    @Test
    fun importAppend_existingModelIds_preserved() = runTest {
        seed(models = listOf(model("a").copy(officialName = "Original")))
        settingsRepo.importAppend("""{"v":1,"models":[{"id":"a","providerId":"p","officialName":"Incoming","enabled":false}],"chats":[],"auth":[],"globalInstructions":""}""")

        val m = modelRepo.getAllModels().first { it.id == "a" }
        assertEquals("Original", m.officialName)
    }

    @Test
    fun importAppend_newChatIds_added() = runTest {
        seed(chats = listOf(chat("existing")))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[{"id":"new","title":"New","instructions":"x","labels":[],"history":[]}],"auth":[],"globalInstructions":""}""")

        val ids = chatRepo.observeAll().first().map { it.id }.toSet()
        assertTrue("existing" in ids)
        assertTrue("new" in ids)
    }

    @Test
    fun importAppend_existingChat_metadataPreserved() = runTest {
        seed(chats = listOf(chat("c1", title = "Local Title")))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[{"id":"c1","title":"Incoming Title","instructions":"incoming","labels":[],"history":[]}],"auth":[],"globalInstructions":""}""")

        val c = chatRepo.observeAll().first().first { it.id == "c1" }
        assertEquals("Local Title", c.title)
    }

    @Test
    fun importAppend_existingChat_historyUnioned() = runTest {
        val local = entry("local")
        seed(chats = listOf(chat("c1", history = listOf(local))))
        val incoming = entry("incoming")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[{"id":"c1","title":"c1","instructions":"inst_c1","labels":[],"history":[{"timestamp":0,"prompt":"incoming","response":"resp_incoming","modelId":"m","modelName":"Model","durationMs":0}]}],"auth":[],"globalInstructions":""}""")

        val history = chatRepo.observeAll().first().first { it.id == "c1" }.history
        assertEquals(2, history.size)
        assertTrue(history.any { it.prompt == "local" })
        assertTrue(history.any { it.prompt == "incoming" })
    }

    @Test
    fun importAppend_newAuthProviders_added() = runTest {
        seed(auth = listOf(cred("existing")))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[{"providerId":"new","apiKey":"k"}],"globalInstructions":""}""")

        val ids = authRepo.observeCredentials().first().map { it.providerId }.toSet()
        assertTrue("existing" in ids)
        assertTrue("new" in ids)
    }

    @Test
    fun importAppend_existingAuth_preserved() = runTest {
        seed(auth = listOf(cred("p1", key = "original_key")))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[{"providerId":"p1","apiKey":"incoming_key"}],"globalInstructions":""}""")

        val c = authRepo.observeCredentials().first().first { it.providerId == "p1" }
        assertEquals("original_key", c.apiKey)
    }

    @Test
    fun importAppend_disjointInstructions_appends() = runTest {
        prefs.setGlobalInstructions("first")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"second"}""")
        assertEquals("first\nsecond", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_emptyInstructions_noChange() = runTest {
        prefs.setGlobalInstructions("existing")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"  "}""")
        assertEquals("existing", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_duplicateInstructions_noChange() = runTest {
        prefs.setGlobalInstructions("hello")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"hello"}""")
        assertEquals("hello", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_allEmpty_noCrash() = runTest {
        seed() // empty state
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":""}""")

        assertTrue(modelRepo.getAllModels().isEmpty())
        assertTrue(chatRepo.observeAll().first().isEmpty())
        assertTrue(authRepo.observeCredentials().first().isEmpty())
        assertEquals("", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_multipleCalls_idempotent() = runTest {
        seed(models = listOf(model("a")))

        val json = """{"v":1,"models":[{"id":"b","providerId":"p","officialName":"B","enabled":true}],"chats":[],"auth":[],"globalInstructions":""}"""
        settingsRepo.importAppend(json)
        val countAfter1 = modelRepo.getAllModels().size

        settingsRepo.importAppend(json)
        val countAfter2 = modelRepo.getAllModels().size

        assertEquals(countAfter1, countAfter2)
    }

    // ===================================================================
    // FULL ROUND-TRIP: export then importAppend (simulate device sync)
    // ===================================================================

    @Test
    fun fullRoundTrip_exportThenAppendToFreshInstance() = runTest {
        seed(
            models = listOf(model("m1"), model("m2")),
            chats = listOf(chat("c1", history = listOf(entry("q1"))), chat("c2")),
            auth = listOf(cred("p1")),
            instructions = "be helpful"
        )
        val exported = settingsRepo.export()

        // Fresh instance starts with default chat "General"
        setUp()
        settingsRepo.importAppend(exported)

        assertEquals(setOf("m1", "m2"), modelRepo.getAllModels().map { it.id }.toSet())
        // default chat from fresh instance persists; c1, c2 appended
        assertEquals(setOf("default", "c1", "c2"), chatRepo.observeAll().first().map { it.id }.toSet())
        assertEquals(setOf("p1"), authRepo.observeCredentials().first().map { it.providerId }.toSet())
        assertEquals("be helpful", prefs.getGlobalInstructions())

        val history = chatRepo.observeAll().first().first { it.id == "c1" }.history
        assertEquals(1, history.size)
    }

    @Test
    fun importAppend_modelsFromNewProvider_added() = runTest {
        seed(models = listOf(model("m1", provider = "p1")))
        settingsRepo.importAppend("""{"v":1,"models":[{"id":"m2","providerId":"p2","officialName":"M2","enabled":true}],"chats":[],"auth":[],"globalInstructions":""}""")
        val byProvider = modelRepo.getAllModels().groupBy { it.providerId }
        assertEquals(2, byProvider.size)
        assertTrue("m2" in byProvider["p2"]!!.map { it.id })
    }

    @Test
    fun importAppend_existingChat_labelsPreserved() = runTest {
        seed(chats = listOf(chat("c1").copy(labels = listOf("local_tag"))))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[{"id":"c1","title":"c1","instructions":"inst_c1","labels":["incoming_tag"],"history":[]}],"auth":[],"globalInstructions":""}""")
        val c = chatRepo.observeAll().first().first { it.id == "c1" }
        assertEquals(listOf("local_tag"), c.labels)
    }

    @Test
    fun importAppend_existingChat_defaultModelIdPreserved() = runTest {
        seed(chats = listOf(chat("c1").copy(defaultModelId = "local_model")))
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[{"id":"c1","title":"c1","instructions":"inst_c1","defaultModelId":"incoming_model","labels":[],"history":[]}],"auth":[],"globalInstructions":""}""")
        val c = chatRepo.observeAll().first().first { it.id == "c1" }
        assertEquals("local_model", c.defaultModelId)
    }

    @Test
    fun importAppend_existingEmptyInstructions_setsIncoming() = runTest {
        prefs.setGlobalInstructions("")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"incoming"}""")
        assertEquals("incoming", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_existingSubstringOfIncoming_noChange() = runTest {
        prefs.setGlobalInstructions("hello")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"hello world"}""")
        // "hello" is substring of "hello world" → skip
        assertEquals("hello", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_incomingSubstringOfExisting_noChange() = runTest {
        prefs.setGlobalInstructions("hello world")
        settingsRepo.importAppend("""{"v":1,"models":[],"chats":[],"auth":[],"globalInstructions":"hello"}""")
        assertEquals("hello world", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_complexMixed_allTypesMerged() = runTest {
        seed(
            models = listOf(model("shared_model"), model("local_only", provider = "p2")),
            chats = listOf(chat("shared_chat", title = "Local Title"), chat("local_only_chat")),
            auth = listOf(cred("shared_provider", key = "local_key"), cred("local_only_provider")),
            instructions = "local instructions"
        )
        val json = """
        {"v":1,
         "models":[
           {"id":"shared_model","providerId":"p","officialName":"Incoming Name","enabled":true},
           {"id":"incoming_only","providerId":"p","officialName":"Incoming","enabled":true}
         ],
         "chats":[
           {"id":"shared_chat","title":"Incoming Title","instructions":"x","labels":[],"history":[{"timestamp":99,"prompt":"hi","response":"hey","modelId":"m","modelName":"Model"}]},
           {"id":"incoming_only_chat","title":"Incoming","instructions":"y","labels":[],"history":[]}
         ],
         "auth":[
           {"providerId":"shared_provider","apiKey":"incoming_key"},
           {"providerId":"incoming_provider","apiKey":"k"}
         ],
         "globalInstructions":"incoming instructions"}
        """.trimIndent().replace("\n", "")

        settingsRepo.importAppend(json)

        // Models: local_only + shared_model (preserved) + incoming_only (new)
        val modelIds = modelRepo.getAllModels().map { it.id }.toSet()
        assertEquals(setOf("shared_model", "local_only", "incoming_only"), modelIds)
        assertEquals("Model shared_model", modelRepo.getAllModels().first { it.id == "shared_model" }.officialName)

        // Chats: local_only_chat + shared_chat (preserved metadata, unioned history) + incoming_only_chat (new)
        val chatIds = chatRepo.observeAll().first().map { it.id }.toSet()
        assertEquals(setOf("shared_chat", "local_only_chat", "incoming_only_chat"), chatIds)
        assertEquals("Local Title", chatRepo.observeAll().first().first { it.id == "shared_chat" }.title)
        assertEquals(1, chatRepo.observeAll().first().first { it.id == "shared_chat" }.history.size)

        // Auth: local_only_provider + shared_provider (preserved) + incoming_provider (new)
        val authIds = authRepo.observeCredentials().first().map { it.providerId }.toSet()
        assertEquals(setOf("shared_provider", "local_only_provider", "incoming_provider"), authIds)
        assertEquals("local_key", authRepo.observeCredentials().first().first { it.providerId == "shared_provider" }.apiKey)

        // Instructions: appended
        assertEquals("local instructions\nincoming instructions", prefs.getGlobalInstructions())
    }

    @Test
    fun importAppend_malformedJson_throwsException() = runTest {
        var threw = false
        try {
            settingsRepo.importAppend("not json {{{")
        } catch (_: Exception) {
            threw = true
        }
        assertTrue(threw, "Expected exception for malformed JSON")
    }
}
