package s4y.yopt.adapters

import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.domain.services.ModelService
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeModelsTest {
    private lateinit var tempFile: java.io.File
    private lateinit var repo: ModelService

    @BeforeTest
    fun setUp() {
        tempFile = Files.createTempFile("yopt_model_test", ".properties").toFile()
        repo = ModelService(KeyValueStore(tempFile.absolutePath))
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    private fun model(id: String, provider: String = "p") =
        ModelDef(id = id, providerId = provider, officialName = "Model $id")

    @Test
    fun mergeModels_newId_addsModel() = runTest {
        repo.upsertModels("p", listOf(model("a")))
        repo.mergeModels("p", listOf(model("b")))
        val ids = repo.getAllModels().map { it.id }
        assertTrue("a" in ids)
        assertTrue("b" in ids)
    }

    @Test
    fun mergeModels_existingId_skips() = runTest {
        val original = model("a").copy(officialName = "Original")
        repo.upsertModels("p", listOf(original))
        repo.mergeModels("p", listOf(model("a").copy(officialName = "Incoming")))
        val result = repo.getAllModels().first { it.id == "a" }
        assertEquals("Original", result.officialName)
    }

    @Test
    fun mergeModels_skipsByIdNotProvider() = runTest {
        // model "a" belongs to provider "p1"
        repo.upsertModels("p1", listOf(model("a", provider = "p1")))
        // incoming model "a" with different provider — still skipped because id "a" exists
        repo.mergeModels("p2", listOf(model("a", provider = "p2")))
        val aModels = repo.getAllModels().filter { it.id == "a" }
        assertEquals(1, aModels.size)
        assertEquals("p1", aModels.first().providerId)
    }

    @Test
    fun mergeModels_noExistingModels_addsAll() = runTest {
        repo.mergeModels("p", listOf(model("a"), model("b")))
        val ids = repo.getAllModels().map { it.id }
        assertTrue("a" in ids)
        assertTrue("b" in ids)
    }

    @Test
    fun mergeModels_allExist_noChange() = runTest {
        repo.upsertModels("p", listOf(model("a"), model("b")))
        val before = repo.getAllModels()
        repo.mergeModels("p", listOf(model("a"), model("b")))
        assertEquals(before, repo.getAllModels())
    }
}
