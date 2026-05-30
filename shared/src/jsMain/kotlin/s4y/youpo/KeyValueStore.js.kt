package s4y.yopt.infra

actual class KeyValueStore actual constructor(context: Any?) {
    actual fun getString(key: String): String? =
        js("localStorage.getItem(key)") as? String

    actual fun putString(key: String, value: String) {
        js("localStorage.setItem(key, value)")
    }
}
