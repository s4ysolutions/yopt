package s4y.yopt.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import s4y.yopt.domain.ports.KeyValueStore

class AppPreferencesService(
    private val kv: KeyValueStore
) {
    companion object {
        const val DEFAULT_SPLIT_FRACTION = 0.4f
    }

    // LastChatId
    private val _lastChatId = MutableStateFlow(kv.getString("lastChatId"))

    fun observeLastChatId(): Flow<String?> = _lastChatId.asStateFlow()

    suspend fun setLastChatId(id: String?) {
        _lastChatId.value = id
        kv.putString("lastChatId", id ?: "")
    }

    // GlobalInstructions
    private val _globalInstructions = MutableStateFlow(kv.getString("globalInstructions") ?: "")

    fun observeGlobalInstructions(): Flow<String> = _globalInstructions.asStateFlow()
    fun getGlobalInstructions(): String = _globalInstructions.value

    suspend fun setGlobalInstructions(value: String) {
        _globalInstructions.value = value
        kv.putString("globalInstructions", value)
    }

    // LastPrompt
    private val _lastPrompt = MutableStateFlow(kv.getString("lastPrompt") ?: "")

    fun observeLastPrompt(): Flow<String> = _lastPrompt.asStateFlow()

    suspend fun setLastPrompt(value: String) {
        _lastPrompt.value = value
        kv.putString("lastPrompt", value)
    }

    // ModelSelection
    private val _selectedModel = MutableStateFlow(kv.getString("selectedModel"))

    fun observeSelectedModel(): Flow<String?> = _selectedModel.asStateFlow()

    suspend fun setSelectedModel(modelId: String) {
        _selectedModel.value = modelId
        kv.putString("selectedModel", modelId)
    }

    // ResponseDisplay
    private val _showMarkdown = MutableStateFlow(
        kv.getString("responseShowMarkdown")?.toBooleanStrictOrNull() ?: false
    )

    fun observeDefaultShowMarkdown(): Flow<Boolean> = _showMarkdown.asStateFlow()
    fun getDefaultShowMarkdown(): Boolean = _showMarkdown.value

    suspend fun setDefaultShowMarkdown(show: Boolean) {
        _showMarkdown.value = show
        kv.putString("responseShowMarkdown", show.toString())
    }

    // SplitFraction
    private val _splitFraction = MutableStateFlow(
        kv.getString("splitFraction")?.toFloatOrNull() ?: DEFAULT_SPLIT_FRACTION
    )

    fun observeSplitFraction(): Flow<Float> = _splitFraction.asStateFlow()

    suspend fun setSplitFraction(value: Float) {
        _splitFraction.value = value
        kv.putString("splitFraction", value.toString())
    }
}
