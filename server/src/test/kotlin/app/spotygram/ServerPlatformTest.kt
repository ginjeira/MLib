package app.spotygram

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPlatformTest {
    @Test
    fun profileSchemaIsCreatedOnceAndSurvivesReopen() {
        val directory = File.createTempFile("mlib-server-test", "").let {
            it.delete()
            it.mkdirs()
            it
        }
        try {
            System.setProperty("mlib.server.data", directory.absolutePath)
            val platform = ServerPlatform(ServerConfig())
            val name = "test.db"
            val first =
                platform.openDatabase(
                    name,
                    1,
                    create = { db ->
                        db.execSQL("CREATE TABLE tracks(id TEXT PRIMARY KEY)")
                        db.execSQL("CREATE TABLE sources(id INTEGER PRIMARY KEY)")
                        db.execSQL("CREATE TABLE playlists(id INTEGER PRIMARY KEY)")
                        db.execSQL("CREATE TABLE downloads(track TEXT PRIMARY KEY)")
                        db.execSQL("CREATE TABLE albums(id TEXT PRIMARY KEY)")
                    },
                    upgrade = { _, _, _ -> },
                    opened = {},
                )
            assertEquals(1, count(first, "server_profiles"))
            assertTrue(hasColumn(first, "tracks", "profile_id"))
            assertTrue(hasColumn(first, "sources", "profile_id"))
            assertTrue(hasColumn(first, "playlists", "profile_id"))
            assertTrue(hasColumn(first, "downloads", "profile_id"))
            assertTrue(hasColumn(first, "albums", "profile_id"))
            first.close()

            val second =
                platform.openDatabase(
                    name,
                    1,
                    create = { error("must not recreate") },
                    upgrade = { _, _, _ -> error("must not upgrade") },
                    opened = {},
                )
            assertEquals(1, count(second, "server_profiles"))
            second.close()
        } finally {
            System.clearProperty("mlib.server.data")
        }
    }

    private fun count(db: SqlDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private fun hasColumn(db: SqlDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1) == column) return true
            false
        }
}
