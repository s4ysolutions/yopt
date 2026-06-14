package s4y.yopt.usecases

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import s4y.yopt.domain.models.ApiStyle
import s4y.yopt.domain.models.AuthType
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.ports.HttpGateway
import s4y.yopt.domain.ports.KeyValueStore
import s4y.yopt.domain.services.LLMService
import s4y.yopt.domain.services.ModelService
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshModelsUseCaseTest {
    private lateinit var tempFile: java.io.File
    private lateinit var modelService: ModelService
    private lateinit var fakeHttp: FakeHttpGateway
    private lateinit var llmService: LLMService
    private lateinit var useCase: RefreshModelsUseCase

    private val provider = ProviderDef(
        id = "custom-1",
        name = "TestProvider",
        apiStyle = ApiStyle.OPENAI,
        authType = AuthType.ApiKey("TestProvider API Key"),
        baseUrl = "https://example.com/v1",
        predefined = false
    )

    @BeforeTest
    fun setUp() {
        tempFile = Files.createTempFile("yopt_refresh_test", ".properties").toFile()
        modelService = ModelService(KeyValueStore(tempFile.absolutePath))
        fakeHttp = FakeHttpGateway()
        llmService = LLMService(modelService, fakeHttp)
        useCase = RefreshModelsUseCase(modelService, llmService)
    }

    @AfterTest
    fun tearDown() { tempFile.delete() }

    @Test
    fun `refresh success returns Result with fetched models`() = runTest {
        fakeHttp.getResponse = """{"data":[{"id":"model-a"},{"id":"model-b"}]}"""

        val result = useCase.refresh(provider, "key")

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertTrue(list.any { it.id == "custom-1:model-a" }, "Expected model-a in result")
        assertTrue(list.any { it.id == "custom-1:model-b" }, "Expected model-b in result")
    }

    @Test
    fun `refresh success stores models in ModelService`() = runTest {
        fakeHttp.getResponse = """{"data":[{"id":"model-a"}]}"""

        useCase.refresh(provider, "key")

        val stored = modelService.getAllModels().filter { it.providerId == "custom-1" }
        assertEquals(1, stored.size)
        assertEquals("custom-1:model-a", stored[0].id)
        assertEquals("model-a", stored[0].officialName)
    }

    @Test
    fun `refresh failure returns Result failure when HTTP throws`() = runTest {
        fakeHttp.getError = Exception("connection refused")

        val result = useCase.refresh(provider, "key")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        val msg = ex!!.message ?: ex.cause?.message ?: ""
        assertTrue(msg.contains("connection refused"), "Expected 'connection refused' in error message, got: $msg")
    }

    @Test
    fun `refresh failure does not store models on HTTP error`() = runTest {
        fakeHttp.getError = Exception("connection refused")

        useCase.refresh(provider, "key")

        val stored = modelService.getAllModels().filter { it.providerId == "custom-1" }
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `refresh rethrows CancellationException instead of wrapping it`() = runTest {
        fakeHttp.getError = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            useCase.refresh(provider, "key")
        }
    }
}

private class FakeHttpGateway : HttpGateway {
    var getResponse: String = ""
    var getError: Throwable? = null

    override suspend fun get(url: String, headers: Map<String, String>): String {
        getError?.let { throw it }
        return getResponse
    }

    override suspend fun post(url: String, headers: Map<String, String>, body: String): String {
        error("post not used in refresh tests")
    }
}
