package app.spotygram

import java.io.File
import java.nio.file.Files
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class LibraryMigrationTest {
    @Test
    fun upgradingVersionSevenPreservesLibraryAndSyncJournal() = runBlocking {
        val root = Files.createTempDirectory("mlib-migration").toFile()
        val databaseFile = File(root, "library.db")
        val writer = "00000000-0000-0000-0000-000000000001"

        DesktopDatabase(databaseFile).use { db ->
            db.execSQL("CREATE TABLE tracks(id TEXT PRIMARY KEY,chat INTEGER,message INTEGER,file INTEGER,title TEXT,artist TEXT,duration INTEGER,size INTEGER,source TEXT,date INTEGER,art TEXT DEFAULT '',path TEXT DEFAULT '',liked INTEGER DEFAULT 0,available INTEGER DEFAULT 1,document INTEGER DEFAULT 0,file_key TEXT DEFAULT '',remote_id TEXT DEFAULT '',saved INTEGER DEFAULT 0,cloud INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE sources(id INTEGER PRIMARY KEY,title TEXT,cursor INTEGER DEFAULT 0,complete INTEGER DEFAULT 0,document_cursor INTEGER DEFAULT 0,documents_complete INTEGER DEFAULT 0,newest INTEGER DEFAULT 0,document_newest INTEGER DEFAULT 0)")
            db.execSQL("CREATE TABLE playlists(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,sync_id TEXT,account INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE playlist_tracks(playlist INTEGER,track TEXT,position INTEGER,PRIMARY KEY(playlist,track))")
            db.execSQL("CREATE TABLE downloads(track TEXT PRIMARY KEY,position INTEGER)")
            db.execSQL("CREATE TABLE temporary_audio(file_key TEXT PRIMARY KEY,remote_id TEXT NOT NULL,path TEXT NOT NULL)")
            db.execSQL("CREATE TABLE playback_progress(track TEXT PRIMARY KEY,position INTEGER NOT NULL,duration INTEGER NOT NULL,file_key TEXT NOT NULL,size INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE sync_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
            db.execSQL("CREATE TABLE sync_values(account INTEGER NOT NULL,key TEXT NOT NULL,clock INTEGER NOT NULL,writer TEXT NOT NULL,value TEXT NOT NULL,pending INTEGER NOT NULL DEFAULT 1,PRIMARY KEY(account,key))")
            db.execSQL("INSERT INTO sync_meta(key,value) VALUES('device',?),('clock','4'),('account','123')", arrayOf(writer))
            db.execSQL("INSERT INTO sources(id,title) VALUES(123,'GinjMusic')")
            db.execSQL("INSERT INTO tracks(id,chat,message,file,title,artist,duration,size,source,date,liked,available,document,file_key,remote_id,saved,cloud) VALUES('123:456',123,456,0,'Test Track','Test Artist',180,4000000,'GinjMusic',1700000000,1,1,0,'','',1,1)")
            db.execSQL("INSERT INTO sync_values(account,key,clock,writer,value,pending) VALUES(?,?,?,?,?,0)", arrayOf<Any?>(123L, "favorite/123:456", 4L, writer, """{"v":true}"""))
            db.execSQL("PRAGMA user_version=7")
        }

        val library = Library(MigrationPlatform(root))
        try {
            library.reload()

            assertEquals(123L, library.sync.account)
            assertEquals("Test Track", library.state.value.tracks.single().title)
            assertEquals("GinjMusic", library.state.value.sources.single().title)
            assertEquals("favorite/123:456", library.sync.records().single().key)
        } finally {
            library.close()
        }

        try {
            val reopened = Library(MigrationPlatform(root))
            try {
                reopened.reload()
                assertEquals("Test Track", reopened.state.value.tracks.single().title)
                assertEquals(123L, reopened.sync.account)
                assertTrue(reopened.sync.records().isNotEmpty())
            } finally {
                reopened.close()
            }
        } finally {
            root.deleteRecursively()
        }
    }
}

private class MigrationPlatform(override val filesDir: File) : Platform {
    override val cacheDir = File(filesDir, "cache").apply { mkdirs() }
    override val fullOptics = true
    override val deviceModel = "migration-test"
    override val systemVersion = "test"
    override val locale = Locale.forLanguageTag("pt-PT")
    override val releaseAbi = "x86_64"
    override val wifi = true
    override val tdlib = object : TdClient {
        override fun createClientId() = 1
        override fun send(client: Int, request: String) = Unit
        override fun receive(timeout: Double): String? = null
        override fun execute(request: String): String? = null
    }
    private val preferences = mutableMapOf<String, MemoryPreferences>()

    override fun preferences(name: String): Preferences =
        preferences.getOrPut(name) { MemoryPreferences() }

    override fun string(id: Int, vararg arguments: Any?) = id.toString()
    override fun plural(id: Int, quantity: Int, vararg arguments: Any?) = id.toString()

    override fun openDatabase(
        name: String,
        version: Int,
        create: (SqlDatabase) -> Unit,
        upgrade: (SqlDatabase, Int, Int) -> Unit,
        opened: (SqlDatabase) -> Unit,
    ): SqlDatabase {
        val db = DesktopDatabase(File(filesDir, name))
        try {
            val old = db.rawQuery("PRAGMA user_version", null).use { it.moveToFirst(); it.getInt(0) }
            db.transaction {
                if (old == 0) create(this) else if (old < version) upgrade(this, old, version)
                execSQL("PRAGMA user_version=$version")
            }
            opened(db)
            return db
        } catch (error: Throwable) {
            db.close()
            throw error
        }
    }

    override fun databaseKey() = "unused"
    override fun observeNetwork(onType: (String) -> Unit) = AutoCloseable {}
    override fun metadata(path: String, name: String) = AudioMetadata(0)
    override fun openUrl(url: String) = Unit
    override fun personalMessageLink(user: Long, message: Long, account: Long) = ""
    override fun languageSettings(app: SpotygramApp) = Unit
    override fun startDownloads(app: SpotygramApp) = Unit
    override fun stopDownloads(app: SpotygramApp) = Unit
    override fun installer(app: SpotygramApp): UpdateInstallation = error("Not used in migration tests")
    override fun assetName(version: String, abi: String) = ""
    override fun logPlaybackError(code: String) = Unit
}

private class MemoryPreferences : Preferences {
    private val values = mutableMapOf<String, Any?>()
    override fun contains(key: String) = values.containsKey(key)
    override fun getString(key: String, default: String?) = values[key] as? String ?: default
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun getLong(key: String, default: Long) = values[key] as? Long ?: default
    override fun getFloat(key: String, default: Float) = values[key] as? Float ?: default

    override fun edit(): Preferences.Editor = object : Preferences.Editor {
        override fun putString(key: String, value: String?) = apply { values[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        override fun putInt(key: String, value: Int) = apply { values[key] = value }
        override fun putLong(key: String, value: Long) = apply { values[key] = value }
        override fun putFloat(key: String, value: Float) = apply { values[key] = value }
        override fun remove(key: String) = apply { values.remove(key) }
        override fun clear() = apply { values.clear() }
        override fun commit() = true
        override fun apply() = Unit
    }
}
