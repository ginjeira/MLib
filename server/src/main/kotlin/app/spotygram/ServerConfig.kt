package app.spotygram

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale

/** Environment-driven configuration for the headless MLib server. */
class ServerConfig {
    val dataDir: File =
        (System.getProperty("mlib.server.data")
            ?: System.getenv("MLIB_SERVER_DATA")
            ?: defaultDataDirectory())
            .let(::File)
            .canonicalFile
    val cacheDir = File(dataDir, "cache")
    val nativeDir: String? =
        System.getProperty("spotygram.native.dir") ?: System.getenv("MLIB_NATIVE_DIR")
    val port: Int = (System.getenv("MLIB_SERVER_PORT") ?: "8080").toIntOrNull() ?: 8080
    val host: String = System.getenv("MLIB_SERVER_HOST") ?: "0.0.0.0"
    /** Service token for Wave 1; replaced by accounts in Wave 2. */
    val streamToken: String? =
        System.getenv("MLIB_STREAM_TOKEN")?.takeIf { it.isNotBlank() }
    val cacheTtlMillis: Long =
        ((System.getenv("MLIB_CACHE_TTL_MINUTES") ?: "10").toLongOrNull() ?: 10L) * 60_000L
    val cacheMaxBytes: Long =
        ((System.getenv("MLIB_CACHE_MAX_MB") ?: "5000").toLongOrNull() ?: 5_000L) * 1_048_576L
    val telegramEnabled: Boolean =
        (System.getenv("MLIB_TELEGRAM_ENABLED") ?: "1") != "0"
    val consoleLogin: Boolean =
        (System.getenv("MLIB_CONSOLE_LOGIN") ?: "1") != "0"
    val phone: String? = System.getenv("MLIB_TELEGRAM_PHONE")?.takeIf { it.isNotBlank() }
    val sourceChat: Long? = System.getenv("MLIB_SOURCE_CHAT")?.toLongOrNull()
    val sourceTitle: String = System.getenv("MLIB_SOURCE_TITLE") ?: "MLib source"
    val code: String? = System.getenv("MLIB_TELEGRAM_CODE")?.takeIf { it.isNotBlank() }
    val password: String? = System.getenv("MLIB_TELEGRAM_PASSWORD")?.takeIf { it.isNotBlank() }
    val locale: Locale =
        when ((System.getenv("MLIB_LANGUAGE") ?: "system").lowercase()) {
            "pt" -> Locale.forLanguageTag("pt-PT")
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }

    private fun defaultDataDirectory(): String = when {
        System.getProperty("os.name").startsWith("Windows") ->
            File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "MLibServer").absolutePath
        System.getProperty("os.name").startsWith("Mac") ->
            File(System.getProperty("user.home"), "Library/Application Support/MLibServer").absolutePath
        else ->
            File(System.getenv("XDG_DATA_HOME") ?: "${System.getProperty("user.home")}/.local/share", "mlib-server").absolutePath
    }

    /** TDLib database key: env override, data file, or a new 32-byte base64 secret. */
    fun databaseKey(): String {
        val provided = System.getenv("MLIB_TDLIB_KEY")?.takeIf { it.isNotBlank() }
        if (provided != null) {
            require(Base64.getDecoder().decode(provided).size == 32) {
                "MLIB_TDLIB_KEY must be a base64-encoded 32-byte key"
            }
            return provided
        }
        val file = File(dataDir, "tdlib.key")
        if (file.isFile) {
            val stored = file.readText().trim()
            require(Base64.getDecoder().decode(stored).size == 32) {
                "tdlib.key must contain a base64-encoded 32-byte key"
            }
            return stored
        }
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.getEncoder().encodeToString(secret)
        file.parentFile.mkdirs()
        file.writeText(encoded)
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
        return encoded
    }
}
