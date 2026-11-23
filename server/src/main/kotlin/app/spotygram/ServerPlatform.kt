package app.spotygram

import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow

/** Headless JVM platform: no UI, no offline downloads, streaming happens in HttpApi. */
class ServerPlatform(private val config: ServerConfig = ServerConfig()) : Platform {
    override val filesDir: File = config.dataDir
    override val cacheDir: File = config.cacheDir
    override val fullOptics = false
    override val deviceModel get() = "MLib Server"
    override val systemVersion get() = System.getProperty("os.version").orEmpty()
    override val releaseAbi: String =
        when {
            System.getProperty("os.name").startsWith("Windows") -> "windows-x64"
            System.getProperty("os.name").startsWith("Mac") -> "macos-arm64"
            else -> "linux-x64"
        }
    override val testDc = false
    override val wifi = true
    override val tdlib: TdClient = ServerTdClient()
    override val locale: Locale
        get() =
            when (preferences("settings").getString("language", "system")) {
                "pt" -> Locale.forLanguageTag("pt-PT")
                "en" -> Locale.ENGLISH
                else -> config.locale
            }

    private val preferenceFiles = ConcurrentHashMap<String, ServerPreferences>()
    private val text = ServerText()

    init {
        listOf(filesDir, cacheDir).forEach { directory ->
            check(directory.isDirectory || directory.mkdirs()) {
                "MLib server storage could not be created: ${directory.absolutePath}"
            }
        }
    }

    override fun preferences(name: String): Preferences =
        preferenceFiles.computeIfAbsent(name) {
            ServerPreferences(File(filesDir, "preferences/$name.json"))
        }

    override fun string(id: Int, vararg arguments: Any?) = text.string(id, locale, *arguments)
    override fun plural(id: Int, quantity: Int, vararg arguments: Any?) =
        text.plural(id, quantity, locale, *arguments)

    override fun openDatabase(
        name: String,
        version: Int,
        create: (SqlDatabase) -> Unit,
        upgrade: (SqlDatabase, Int, Int) -> Unit,
        opened: (SqlDatabase) -> Unit,
    ): SqlDatabase {
        val db = ServerDatabase(File(filesDir, name))
        try {
            val old = db.rawQuery("PRAGMA user_version", null).use { it.moveToFirst(); it.getInt(0) }
            check(old <= version) { "This library needs a newer MLib version" }
            if (old < version) {
                db.transaction {
                    if (old == 0) create(this) else upgrade(this, old, version)
                    execSQL("PRAGMA user_version=$version")
                }
            }
            ensureProfileSchema(db)
            opened(db)
            return db
        } catch (error: Throwable) {
            db.close()
            throw error
        }
    }

    /** Wave 1 groundwork: profile columns exist; enforcement arrives with onboarding (Wave 3). */
    private fun ensureProfileSchema(db: SqlDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS server_profiles(id INTEGER PRIMARY KEY, name TEXT NOT NULL, created_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT OR IGNORE INTO server_profiles(id,name,created_at) VALUES(1,'Owner',?)",
            arrayOf(System.currentTimeMillis()),
        )
        for (table in listOf("tracks", "sources", "playlists", "downloads", "albums")) {
            if (hasColumn(db, table, "profile_id")) continue
            db.execSQL("ALTER TABLE $table ADD COLUMN profile_id INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE INDEX IF NOT EXISTS ${table}_profile ON $table(profile_id)")
        }
    }

    private fun hasColumn(db: SqlDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1) == column) return true
            false
        }

    override fun databaseKey(): String = config.databaseKey()

    private val syncKeyFile get() = File(filesDir, "sync.key")

    override fun syncEncryptionKey(): ByteArray? =
        syncKeyFile.takeIf { it.isFile }?.readText()?.trim()?.let {
            java.util.Base64.getDecoder().decode(it).takeIf { key -> key.size == 32 }
        }

    override fun storeSyncEncryptionKey(key: ByteArray) {
        require(key.size == 32) { "Invalid MLib sync encryption key" }
        syncKeyFile.writeText(java.util.Base64.getEncoder().encodeToString(key))
        syncKeyFile.setReadable(false, false)
        syncKeyFile.setReadable(true, true)
        syncKeyFile.setWritable(false, false)
        syncKeyFile.setWritable(true, true)
    }

    override fun clearSyncEncryptionKey() {
        syncKeyFile.delete()
    }

    override fun observeNetwork(onType: (String) -> Unit): AutoCloseable {
        // TDLib owns reconnection on the server; there is no OS observer.
        onType("networkTypeOther")
        return AutoCloseable {}
    }

    override fun metadata(path: String, name: String) = AudioMetadata(0)

    override fun openUrl(url: String) {
        System.err.println("MLib server ignores openUrl: $url")
    }

    override fun shareFile(file: File, mime: String) {}

    override fun personalMessageLink(user: Long, message: Long, account: Long): String =
        "https://web.telegram.org/k/#$user?message=$message"

    override fun languageSettings(app: SpotygramApp) {}

    override fun startDownloads(app: SpotygramApp) {}

    override fun stopDownloads(app: SpotygramApp) {}

    override fun startAudioTransfer(app: SpotygramApp) {}

    override fun installer(app: SpotygramApp): UpdateInstallation = ServerUpdateInstaller

    override fun assetName(version: String, abi: String) =
        "mlib-server-$version-$abi.tar.gz"

    override fun logPlaybackError(code: String) {
        System.err.println("MLib server playback error: $code")
    }
}

/** The server never installs updates; updates are a deployment concern. */
object ServerUpdateInstaller : UpdateInstallation {
    override val state = MutableStateFlow(InstallState())
    override fun start(asset: UpdateAsset?) {}
    override fun cancel() {}
}
