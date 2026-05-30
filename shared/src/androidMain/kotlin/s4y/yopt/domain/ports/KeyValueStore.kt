package s4y.yopt.domain.ports

actual class KeyValueStore actual constructor(context: Any?) {
    private val prefs = (context as Context).getSharedPreferences(
        "yopt_settings", Context.MODE_PRIVATE
    )

    actual fun getString(key: String): String? = prefs.getString(key, null)
    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}