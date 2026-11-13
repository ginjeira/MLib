package app.spotygram

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCryptoTest {
    private val key = ByteArray(32) { it.toByte() }
    private val plain = "MLib private sync payload".toByteArray()
    private val aad = "mlib-sync/4|123456|snapshot|42".toByteArray()

    @Test
    fun encryptsAndDecryptsWithAuthenticatedMetadata() {
        val encrypted = SyncCrypto.encrypt(plain, key, aad)
        assertNotEquals(String(plain), String(encrypted))
        assertArrayEquals(plain, SyncCrypto.decrypt(encrypted, key, aad))
    }

    @Test
    fun eachEncryptionUsesAFreshNonce() {
        val first = SyncCrypto.encrypt(plain, key, aad)
        val second = SyncCrypto.encrypt(plain, key, aad)
        assertTrue(!first.contentEquals(second))
    }

    @Test
    fun rejectsWrongKey() {
        val encrypted = SyncCrypto.encrypt(plain, key, aad)
        assertTrue(runCatching { SyncCrypto.decrypt(encrypted, ByteArray(32) { 7 }, aad) }.isFailure)
    }

    @Test
    fun rejectsModifiedCiphertext() {
        val encrypted = SyncCrypto.encrypt(plain, key, aad)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertTrue(runCatching { SyncCrypto.decrypt(encrypted, key, aad) }.isFailure)
    }

    @Test
    fun rejectsAssociatedDataMismatch() {
        val encrypted = SyncCrypto.encrypt(plain, key, aad)
        assertTrue(runCatching { SyncCrypto.decrypt(encrypted, key, "different-account".toByteArray()) }.isFailure)
    }

    @Test
    fun rejectsInvalidEnvelopeAndKeySize() {
        assertTrue(runCatching { SyncCrypto.decrypt(byteArrayOf(1, 2, 3), key, aad) }.isFailure)
        assertTrue(runCatching { SyncCrypto.encrypt(plain, ByteArray(16), aad) }.isFailure)
    }
}
