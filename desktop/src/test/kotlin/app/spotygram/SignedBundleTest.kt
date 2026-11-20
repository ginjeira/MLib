package app.spotygram

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedBundleTest {
    @Test
    fun zipTraversalIsRejectedBeforeSignatureVerification() {
        val root = Files.createTempDirectory("mlib-signed-bundle").toFile()
        try {
            val archive = File(root, "bundle.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("../escape.txt"))
                zip.write("must not escape".toByteArray())
                zip.closeEntry()
            }
            val destination = File(root, "destination")

            var failed = false
            try {
                SignedBundle.extract(archive, destination, "1.0.0", "linux-x64")
            } catch (_: Throwable) {
                failed = true
            }

            assertTrue(failed)
            assertFalse(File(root, "escape.txt").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingUpdateKeyFailsClosed() {
        val root = Files.createTempDirectory("mlib-signed-bundle-key").toFile()
        try {
            val archive = File(root, "bundle.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("signature.bin"))
                zip.write(ByteArray(64))
                zip.closeEntry()
            }

            var failed = false
            try {
                SignedBundle.extract(archive, File(root, "destination"), "1.0.0", "linux-x64")
            } catch (_: Throwable) {
                failed = true
            }

            assertTrue(failed)
        } finally {
            root.deleteRecursively()
        }
    }
}
