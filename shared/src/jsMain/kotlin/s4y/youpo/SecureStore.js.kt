package s4y.yopt.infra

// localStorage is the only key-value storage available in browsers.
// There is no encrypted storage primitive on the web platform.
actual class SecureStore actual constructor(context: Any?) {
    actual fun getString(key: String): String? =
        js("localStorage.getItem(key)") as? String

    actual fun putString(key: String, value: String) {
        js("localStorage.setItem(key, value)")
    }

    actual fun remove(key: String) {
        js("localStorage.removeItem(key)")
    }
}
