package s4y.yopt.domain.ports

actual class SecureStore actual constructor(context: Any?) {
    actual fun getString(key: String): String? = window.localStorage.getItem(key)
    actual fun putString(key: String, value: String) = window.localStorage.setItem(key, value)
    actual fun remove(key: String) = window.localStorage.removeItem(key)
}