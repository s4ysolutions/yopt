package s4y.yopt.domain.ports

actual class KeyValueStore actual constructor(context: Any?) {
    actual fun getString(key: String): String? = window.localStorage.getItem(key)
    actual fun putString(key: String, value: String) = window.localStorage.setItem(key, value)
}