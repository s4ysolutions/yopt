package s4y.yopt.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.ports.KeyValueStore

class ModelService(private val kv: KeyValueStore) {
    private val _models = MutableStateFlow(loadModels())
    private val _customProviders = MutableStateFlow(loadCustomProviders())

    fun getProviders(): List<ProviderDef> = ProviderDef.predefined + _customProviders.value

    fun observeProviders(): Flow<List<ProviderDef>> = _customProviders.map {
        ProviderDef.predefined + it
    }

    fun observeModels(): Flow<List<ModelDef>> = _models

    suspend fun addCustomProvider(def: ProviderDef) {
        _customProviders.value = _customProviders.value + def
        saveCustomProviders()
    }

    suspend fun updateCustomProvider(def: ProviderDef) {
        _customProviders.value = _customProviders.value.map {
            if (it.id == def.id) def else it
        }
        saveCustomProviders()
    }

    suspend fun deleteCustomProvider(id: String) {
        _customProviders.value = _customProviders.value.filter { it.id != id }
        saveCustomProviders()
        _models.value = _models.value.filter { it.providerId != id }
        saveModels()
    }

    suspend fun getAllModels(): List<ModelDef> = _models.value

    suspend fun getEnabledModels(): List<ModelDef> = _models.value.filter { it.enabled }

    suspend fun upsertModels(providerId: String, models: List<ModelDef>) {
        val current = _models.value.toMutableList()
        current.removeAll { it.providerId == providerId }
        current.addAll(models)
        _models.value = current
        saveModels()
    }

    suspend fun mergeModels(providerId: String, models: List<ModelDef>) {
        val existingIds = _models.value.map { it.id }.toSet()
        val toAdd = models.filter { it.id !in existingIds }
        if (toAdd.isNotEmpty()) {
            _models.value = _models.value + toAdd
            saveModels()
        }
    }

    suspend fun setModelEnabled(modelId: String, enabled: Boolean) {
        _models.value = _models.value.map {
            if (it.id == modelId) it.copy(enabled = enabled) else it
        }
        saveModels()
    }

    private fun saveModels() {
        kv.putString("models", Json.encodeToString(_models.value))
    }

    private fun loadModels(): List<ModelDef> {
        return try {
            kv.getString("models")?.let { Json.decodeFromString(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomProviders() {
        kv.putString("custom_providers", Json.encodeToString(_customProviders.value))
    }

    private fun loadCustomProviders(): List<ProviderDef> {
        return try {
            kv.getString("custom_providers")?.let { Json.decodeFromString(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
