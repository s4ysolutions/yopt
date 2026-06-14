package s4y.yopt.domain.services

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import s4y.yopt.domain.models.ApiStyle
import s4y.yopt.domain.models.ModelDef
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.domain.ports.HttpGateway

class LLMService(
    private val models: ModelService,
    private val http: HttpGateway
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun base(prov: ProviderDef): String = prov.baseUrl.trimEnd('/')

    private fun modelId(providerId: String, apiId: String) = "$providerId:$apiId"

    private fun apiModelId(id: String) = id.substringAfter(":")

    suspend fun send(
        prompt: String, model: ModelDef, systemInstructions: String?, apiKey: String?
    ): String {
        if (apiKey.isNullOrBlank()) throw Exception("API key required")
        val prov = models.getProviders().find { it.id == model.providerId }
            ?: throw Exception("Unknown provider: ${model.providerId}")
        return try {
            when (prov.apiStyle) {
                ApiStyle.OPENAI -> openAI(prompt, systemInstructions, apiModelId(model.id), prov, apiKey)
                ApiStyle.ANTHROPIC -> anthropic(prompt, systemInstructions, apiModelId(model.id), prov, apiKey)
                ApiStyle.GEMINI -> gemini(prompt, systemInstructions, apiModelId(model.id), prov, apiKey)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("${prov.name}: ${e.message ?: "unknown"}", e)
        }
    }

    suspend fun fetchModels(prov: ProviderDef, apiKey: String?): List<ModelDef> {
        if (prov.id == "huggingface") return huggingfaceModels(prov)
        return try {
            when (prov.apiStyle) {
                ApiStyle.OPENAI -> fetchOpenAIModels(prov, apiKey)
                ApiStyle.GEMINI -> fetchGeminiModels(prov, apiKey)
                ApiStyle.ANTHROPIC -> anthropicModels(prov)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("${prov.name}: ${e.message ?: "unknown"}", e)
        }
    }

    private fun anthropicModels(prov: ProviderDef): List<ModelDef> = listOf(
        "claude-opus-4-7", "claude-opus-4-5", "claude-opus-4-1",
        "claude-sonnet-4-6", "claude-sonnet-4-5",
        "claude-haiku-4-5", "claude-3.5-haiku",
    ).map { ModelDef(modelId(prov.id, it), prov.id, it) }

    private fun huggingfaceModels(prov: ProviderDef): List<ModelDef> = listOf(
        "meta-llama/Llama-4-Maverick-17B-128E-Instruct",
        "meta-llama/Llama-4-Scout-17B-16E-Instruct",
        "meta-llama/Llama-3.3-70B-Instruct",
        "mistralai/Mistral-Small-3.1-24B-Instruct-2503",
        "mistralai/Mistral-Large-Instruct-2411",
        "deepseek-ai/DeepSeek-R1",
        "deepseek-ai/DeepSeek-V3-0324",
        "Qwen/Qwen3-235B-A22B",
        "Qwen/Qwen3-30B-A3B",
        "Qwen/Qwen3-Coder-30B-A3B-Instruct",
        "google/gemma-3-27b-it",
        "microsoft/Phi-4-mini-instruct",
    ).map { ModelDef(modelId(prov.id, it), prov.id, it) }

    private suspend fun fetchOpenAIModels(prov: ProviderDef, apiKey: String?): List<ModelDef> {
        val url = "${base(prov)}/models"
        val headers = mutableMapOf<String, String>()
        if (!apiKey.isNullOrBlank()) headers["Authorization"] = "Bearer $apiKey"
        val raw = try {
            http.get(url, headers)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("GET $url failed: ${e.message ?: "unknown"}", e)
        }
        val r = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            throw Exception("GET $url returned non-JSON: ${raw.take(300)}", e)
        }
        r["error"]?.jsonObject?.let {
            val msg = it["message"]?.jsonPrimitive?.content ?: "API error"
            val code = it["code"]?.jsonPrimitive?.content
            val meta = it["metadata"]?.jsonObject?.get("raw")?.jsonPrimitive?.content
            throw Exception(buildString {
                append(msg)
                if (code != null) append(" (code: $code)")
                if (meta != null) append(" — $meta")
            })
        }
        return r["data"]?.jsonArray?.map {
            it.jsonObject["id"]?.jsonPrimitive?.content ?: throw Exception("Unexpected model entry in response")
        }?.map { ModelDef(modelId(prov.id, it), prov.id, it) } ?: throw Exception("GET $url: missing 'data' array. Response: ${raw.take(300)}")
    }

    private suspend fun fetchGeminiModels(prov: ProviderDef, apiKey: String?): List<ModelDef> {
        val key = apiKey?.trim()?.ifBlank { null } ?: throw Exception("API key required to fetch Gemini models")
        val url = "${base(prov)}/models?key=$key"
        val raw = try {
            http.get(url, emptyMap())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("GET $url failed: ${e.message ?: "unknown"}", e)
        }
        val r = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            throw Exception("GET $url returned non-JSON: ${raw.take(300)}", e)
        }
        r["error"]?.jsonObject?.let { err ->
            val msg = err["message"]?.jsonPrimitive?.content ?: "Gemini API error"
            val code = err["code"]?.jsonPrimitive?.content
            val status = err["status"]?.jsonPrimitive?.content
            throw Exception(buildString {
                append(msg)
                if (code != null) append(" (code: $code)")
                if (status != null) append(" — $status")
                append(" (check that your key has the Generative Language API enabled)")
            })
        }
        return r["models"]?.jsonArray?.map {
            val fullName = it.jsonObject["name"]?.jsonPrimitive?.content
                ?: throw Exception("Unexpected model entry in Gemini response")
            val id = fullName.removePrefix("models/")
            val displayName = it.jsonObject["displayName"]?.jsonPrimitive?.content ?: id
            ModelDef(modelId(prov.id, id), prov.id, displayName)
        } ?: throw Exception("GET $url: missing 'models' array. Response: ${raw.take(300)}")
    }

    private suspend fun openAI(
        p: String, s: String?, m: String, prov: ProviderDef, key: String
    ): String {
        val msgs = buildJsonArray {
            if (!s.isNullOrBlank()) add(buildJsonObject { put("role", "system"); put("content", s) })
            add(buildJsonObject { put("role", "user"); put("content", p) })
        }
        val body = buildJsonObject { put("model", m); put("messages", msgs) }
        val r = json.parseToJsonElement(
            http.post("${base(prov)}/chat/completions", mapOf("Authorization" to "Bearer $key"), body.toString())
        ).jsonObject
        r["error"]?.jsonObject?.let {
            val msg = it["message"]?.jsonPrimitive?.content ?: "OpenAI API error"
            val code = it["code"]?.jsonPrimitive?.content
            val meta = it["metadata"]?.jsonObject?.get("raw")?.jsonPrimitive?.content
            throw Exception(buildString {
                append(msg)
                if (code != null) append(" (code: $code)")
                if (meta != null) append(" — $meta")
            })
        }
        val content = r["choices"]?.jsonArray?.get(0)?.jsonObject
            ?.get("message")?.jsonObject?.get("content")
        return when (content) {
            is JsonPrimitive -> content.content
            is JsonArray -> content.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
            else -> throw Exception("Unexpected OpenAI response: ${r.toString().take(500)}")
        }
    }

    private suspend fun anthropic(
        p: String, s: String?, m: String, prov: ProviderDef, key: String
    ): String {
        val body = buildJsonObject {
            put("model", m)
            put("max_tokens", 4096)
            if (!s.isNullOrBlank()) put("system", s)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "user"); put("content", p) })
            }
        }
        val headers = mapOf(
            "x-api-key" to key,
            "anthropic-version" to "2023-06-01",
        )
        val r = json.parseToJsonElement(
            http.post("${base(prov)}/messages", headers, body.toString())
        ).jsonObject
        r["error"]?.jsonObject?.let { err ->
            val msg = err["message"]?.jsonPrimitive?.content ?: "Anthropic API error"
            val type = err["type"]?.jsonPrimitive?.content
            throw Exception(buildString {
                if (type != null) append("[$type] ")
                append(msg)
            })
        }
        val blocks = r["content"]?.jsonArray
            ?: throw Exception("Unexpected Anthropic response: ${r.toString().take(500)}")
        return blocks.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
    }

    private suspend fun gemini(
        p: String, s: String?, m: String, prov: ProviderDef, key: String
    ): String {
        val body = buildJsonObject {
            if (!s.isNullOrBlank()) putJsonObject("system_instruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", s) }) }
            }
            putJsonArray("contents") {
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { add(buildJsonObject { put("text", p) }) }
                })
            }
        }
        val r = json.parseToJsonElement(
            http.post("${base(prov)}/models/${m}:generateContent?key=${key}", emptyMap(), body.toString())
        ).jsonObject
        r["error"]?.jsonObject?.let { err ->
            val msg = err["message"]?.jsonPrimitive?.content ?: "Gemini API error"
            val code = err["code"]?.jsonPrimitive?.content
            val status = err["status"]?.jsonPrimitive?.content
            throw Exception(buildString {
                append(msg)
                if (code != null) append(" (code: $code)")
                if (status != null) append(" — $status")
            })
        }
        val parts = r["candidates"]?.jsonArray?.get(0)?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?: throw Exception("Unexpected Gemini response: ${r.toString().take(500)}")
        return parts.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
    }
}
