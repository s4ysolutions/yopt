package s4y.yopt.domain.ports

expect class KeyValueStore(context: Any? = null) {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}