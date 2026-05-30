package s4y.yopt.domain.ports

interface HttpGateway {
    suspend fun post(url: String, headers: Map<String, String>, body: String): String
    suspend fun get(url: String, headers: Map<String, String>): String
}
