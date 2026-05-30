package s4y.yopt.adapters

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import s4y.yopt.domain.ports.HttpGateway

class KtorHttpAdapter : HttpGateway {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    override suspend fun post(url: String, headers: Map<String, String>, body: String): String {
        try {
            return client.post(url) {
                headers.forEach { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
        } catch (e: Exception) {
            val extra = try {
                val b = (e as? io.ktor.client.plugins.ResponseException)?.response?.bodyAsText()?.take(300)
                if (!b.isNullOrBlank()) " — $b" else ""
            } catch (_: Exception) { "" }
            throw Exception("${e.message ?: "unknown"}$extra", e)
        }
    }

    override suspend fun get(url: String, headers: Map<String, String>): String {
        try {
            return client.get(url) {
                headers.forEach { (k, v) -> header(k, v) }
            }.bodyAsText()
        } catch (e: Exception) {
            val extra = try {
                val b = (e as? io.ktor.client.plugins.ResponseException)?.response?.bodyAsText()?.take(300)
                if (!b.isNullOrBlank()) " — $b" else ""
            } catch (_: Exception) { "" }
            throw Exception("${e.message ?: "unknown"}$extra", e)
        }
    }
}
