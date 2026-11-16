package app.spotygram

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Authenticated envelope for MLib CloudSync format 4. */
internal object SyncCrypto {
    private val magic = byteArrayOf(0x4d, 0x4c, 0x49, 0x42, 0x34)
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray {
        require(key.size == 32) { "Invalid MLib sync key" }
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(associatedData)
        val encrypted = cipher.doFinal(plain)
        return magic + nonce + encrypted
    }

    fun decrypt(envelope: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray {
        require(key.size == 32) { "Invalid MLib sync key" }
        require(envelope.size >= magic.size + NONCE_BYTES + TAG_BITS / 8) { "Invalid encrypted sync envelope" }
        require(envelope.copyOfRange(0, magic.size).contentEquals(magic)) { "Unsupported encrypted sync envelope" }
        val nonce = envelope.copyOfRange(magic.size, magic.size + NONCE_BYTES)
        val encrypted = envelope.copyOfRange(magic.size + NONCE_BYTES, envelope.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(encrypted)
    }
}
