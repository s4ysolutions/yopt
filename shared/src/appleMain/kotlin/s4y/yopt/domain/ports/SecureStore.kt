package s4y.yopt.domain.ports

import platform.Foundation.NSUserDefaults

// Credentials stored in NSUserDefaults under "yopt_secure." prefix.
//
// On iOS, NSUserDefaults is encrypted at rest via Data Protection (enabled by default).
// On macOS, the home directory is protected by FileVault when enabled (default on modern Macs,
// since macOS 10.13+). When FileVault is off, credentials are stored in plaintext — same
// risk profile as many desktop apps that use config files.
//
// Keychain (SecItemAdd/SecItemCopyMatching) would provide hardware-backed encryption and
// per-item access control. Blocked by Kotlin/Native CF-interop: CFDictionaryCreate expects
// CValuesRef<CPointerVarOf<CPointer<out CPointed>>> for key/value arrays, but Security
// framework constants (kSecClass, etc.) are typed as CPointer<__CFString>, producing type
// mismatches with cValuesOf. An ObjC bridging header could work around this by constructing
// NSDictionary on the ObjC side and returning it as a CFDictionaryRef. Tracked in README.md
// under "Known Future Work".
actual class SecureStore actual constructor(context: Any?) {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "yopt_secure."

    private fun key(name: String) = prefix + name

    actual fun getString(key: String): String? = defaults.stringForKey(key(key))

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key(key))
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key(key))
    }
}