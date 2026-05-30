package s4y.yopt.domain.ports

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual class SecureStore actual constructor(context: Any?) {
    private val dataDir: File
    private val props = Properties()
    private val masterKey: SecretKey
    private val secureRandom = SecureRandom()

    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    init {
        dataDir = when {
            context is String -> File(context)
            isWindows -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "yopt")
            else -> File(System.getProperty("user.home"), ".yopt")
        }
        dataDir.mkdirs()
        masterKey = loadOrCreateMasterKey()
        loadDataFile()
    }

    actual fun getString(key: String): String? = props.getProperty(key)

    actual fun putString(key: String, value: String) {
        props.setProperty(key, value)
        saveDataFile()
    }

    actual fun remove(key: String) {
        props.remove(key)
        saveDataFile()
    }

    private fun loadOrCreateMasterKey(): SecretKey {
        if (isMac) {
            try {
                val ks = KeyStore.getInstance("KeychainStore", "Apple")
                ks.load(null, null)
                val entry = ks.getEntry("yopt-master", null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey
                val newKey = generateAESKey()
                ks.setEntry("yopt-master", KeyStore.SecretKeyEntry(newKey), null)
                ks.store(null, null)
                return newKey
            } catch (_: Exception) {
                // fall through to PKCS12 fallback
            }
        }

        val ksFile = File(dataDir, "yopt.p12")
        val password = charArrayOf('y', 'o', 'p', 't', '-', 'k', 's', '-', 'p', 'w')
        val ks = KeyStore.getInstance("PKCS12")

        if (ksFile.exists()) {
            ks.load(ksFile.inputStream(), password)
        } else {
            ks.load(null, password)
        }

        val entry = ks.getEntry("yopt-master", KeyStore.PasswordProtection(password)) as? KeyStore.SecretKeyEntry
        if (entry != null) return entry.secretKey

        val newKey = generateAESKey()
        ks.setEntry("yopt-master", KeyStore.SecretKeyEntry(newKey), KeyStore.PasswordProtection(password))
        ksFile.outputStream().use { ks.store(it, password) }

        if (!isWindows) {
            try {
                Files.setPosixFilePermissions(ksFile.toPath(), PosixFilePermissions.fromString("rw-------"))
            } catch (_: Exception) {
                // best effort
            }
        }

        return newKey
    }

    private fun generateAESKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256)
        return kg.generateKey()
    }

    private fun loadDataFile() {
        val dataFile = File(dataDir, "secure.dat")
        if (!dataFile.exists()) return
        try {
            val base64 = dataFile.readText()
            val combined = Base64.getDecoder().decode(base64)
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val spec = GCMParameterSpec(128, iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            val plaintext = cipher.doFinal(ciphertext)
            props.load(ByteArrayInputStream(plaintext))
        } catch (_: Exception) {
            // corrupted or first run — start empty
        }
    }

    private fun saveDataFile() {
        val baos = ByteArrayOutputStream()
        props.store(baos, null)
        val plaintext = baos.toByteArray()

        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec)
        val ciphertext = cipher.doFinal(plaintext)

        val combined = iv + ciphertext
        val dataFile = File(dataDir, "secure.dat")
        dataFile.writeText(Base64.getEncoder().encodeToString(combined))
    }
}