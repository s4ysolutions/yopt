package s4y.yopt.domain.ports

import platform.Foundation.NSUserDefaults

actual class KeyValueStore actual constructor(context: Any?) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? = defaults.stringForKey(key)
    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}