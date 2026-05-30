package s4y.yopt.domain.ports

import java.io.File
import java.util.Properties

actual class KeyValueStore actual constructor(context: Any?) {
    private val file = if (context is String) File(context)
                       else File(System.getProperty("user.home"), ".yopt/settings.properties")
    private val props = Properties()

    init {
        file.parentFile?.mkdirs()
        if (file.exists()) file.inputStream().use { props.load(it) }
    }

    actual fun getString(key: String): String? =
        props.getProperty(key)?.takeIf { it.isNotEmpty() }

    actual fun putString(key: String, value: String) {
        props.setProperty(key, value)
        file.outputStream().use { props.store(it, null) }
    }
}