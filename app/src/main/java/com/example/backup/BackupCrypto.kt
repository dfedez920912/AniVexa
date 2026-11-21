package com.example.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupCrypto {

    const val FILE_EXTENSION = "avb"
    private val MAGIC_HEADER = byteArrayOf(0x41, 0x56, 0x42, 0x31) // "AVB1"
    private const val FORMAT_VERSION: Byte = 0x01
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val MIN_PASSPHRASE_LENGTH = 4

    fun isValidPassphrase(passphrase: String): Boolean {
        return passphrase.trim().length >= MIN_PASSPHRASE_LENGTH
    }

    fun hasAvbMagicHeader(bytes: ByteArray): Boolean {
        if (bytes.size <= MAGIC_HEADER.size + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES) return false
        for (i in MAGIC_HEADER.indices) {
            if (bytes[i] != MAGIC_HEADER[i]) return false
        }
        return bytes[MAGIC_HEADER.size] == FORMAT_VERSION
    }

    fun encrypt(plainBytes: ByteArray, passphrase: String): ByteArray {
        val trimmed = passphrase.trim()
        if (trimmed.length < MIN_PASSPHRASE_LENGTH) {
            throw BackupCryptoException("La contraseña de cifrado debe tener al menos $MIN_PASSPHRASE_LENGTH caracteres.")
        }

        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }

        val keyBytes = deriveAesKeyBytes(trimmed, salt)
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherText = cipher.doFinal(plainBytes)

            ByteBuffer.allocate(MAGIC_HEADER.size + 1 + salt.size + iv.size + cipherText.size)
                .put(MAGIC_HEADER)
                .put(FORMAT_VERSION)
                .put(salt)
                .put(iv)
                .put(cipherText)
                .array()
        } catch (e: GeneralSecurityException) {
            throw BackupCryptoException("No se pudo cifrar el respaldo.", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(encryptedBytes: ByteArray, passphrase: String): ByteArray {
        val trimmed = passphrase.trim()
        if (trimmed.length < MIN_PASSPHRASE_LENGTH) {
            throw BackupCryptoException("Introduce la contraseña del respaldo (mínimo $MIN_PASSPHRASE_LENGTH caracteres).")
        }

        val minSize = MAGIC_HEADER.size + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16
        if (encryptedBytes.size < minSize) {
            throw BackupCryptoException("El archivo seleccionado no tiene un formato .avb válido.")
        }

        val buffer = ByteBuffer.wrap(encryptedBytes)
        val magic = ByteArray(MAGIC_HEADER.size)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC_HEADER)) {
            throw BackupCryptoException("El archivo seleccionado no es una copia cifrada de AniVexa (.avb).")
        }

        val version = buffer.get()
        if (version != FORMAT_VERSION) {
            throw BackupCryptoException("Versión de formato .avb no compatible.")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)
        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val keyBytes = deriveAesKeyBytes(trimmed, salt)
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(cipherText)
        } catch (e: AEADBadTagException) {
            throw BackupCryptoException("Contraseña incorrecta o archivo .avb dañado.", e)
        } catch (e: GeneralSecurityException) {
            throw BackupCryptoException("No se pudo descifrar el archivo .avb.", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveAesKeyBytes(passphrase: String, salt: ByteArray): ByteArray {
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            chars.fill('\u0000')
            spec.clearPassword()
        }
    }
}
