package com.example.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.MessageDigest
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
    private val MAGIC_HEADER_V2 = byteArrayOf(0x41, 0x56, 0x42, 0x32) // "AVB2" (passwordless app-scoped envelope)
    private val MAGIC_HEADER_V1 = byteArrayOf(0x41, 0x56, 0x42, 0x31) // "AVB1" (legacy)
    private const val FORMAT_VERSION_V2: Byte = 0x02
    private const val FORMAT_VERSION_V1: Byte = 0x01
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000

    private val ENVELOPE_DOMAIN = byteArrayOf(
        0x41, 0x6E, 0x69, 0x56, 0x65, 0x78, 0x61, 0x3A,
        0x41, 0x56, 0x42, 0x2D, 0x32, 0x30, 0x32, 0x36
    )

    private val MIX_PATTERN = byteArrayOf(
        0x5A, 0x3C, 0x71, 0x19, 0x6E, 0x2D, 0x44, 0x53,
        0x1B, 0x6F, 0x3A, 0x7E, 0x22, 0x49, 0x65, 0x31
    )

    fun hasAvbMagicHeader(bytes: ByteArray): Boolean {
        val headerSize = MAGIC_HEADER_V2.size + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES
        if (bytes.size <= headerSize) return false
        val isV2 = startsWith(bytes, MAGIC_HEADER_V2) && bytes[MAGIC_HEADER_V2.size] == FORMAT_VERSION_V2
        val isV1 = startsWith(bytes, MAGIC_HEADER_V1) && bytes[MAGIC_HEADER_V1.size] == FORMAT_VERSION_V1
        return isV2 || isV1
    }

    fun encrypt(plainBytes: ByteArray): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }

        val keyBytes = deriveEnvelopeKeyBytes(salt)
        return try {
            val aesSpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, aesSpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherText = cipher.doFinal(plainBytes)

            ByteBuffer.allocate(MAGIC_HEADER_V2.size + 1 + salt.size + iv.size + cipherText.size)
                .put(MAGIC_HEADER_V2)
                .put(FORMAT_VERSION_V2)
                .put(salt)
                .put(iv)
                .put(cipherText)
                .array()
        } catch (e: GeneralSecurityException) {
            throw BackupCryptoException("No se pudo cifrar el respaldo (.avb).", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(encryptedBytes: ByteArray): ByteArray {
        val minSize = MAGIC_HEADER_V2.size + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16
        if (encryptedBytes.size < minSize) {
            throw BackupCryptoException("El archivo seleccionado no tiene un formato .avb válido.")
        }

        val buffer = ByteBuffer.wrap(encryptedBytes)
        val magic = ByteArray(MAGIC_HEADER_V2.size)
        buffer.get(magic)
        val version = buffer.get()

        if (magic.contentEquals(MAGIC_HEADER_V1) && version == FORMAT_VERSION_V1) {
            throw BackupCryptoException(
                "Este archivo .avb fue generado con una versión protegida con contraseña que ya no es compatible."
            )
        }
        if (!magic.contentEquals(MAGIC_HEADER_V2) || version != FORMAT_VERSION_V2) {
            throw BackupCryptoException("El archivo seleccionado no es una copia cifrada válida de AniVexa (.avb).")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)
        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val keyBytes = deriveEnvelopeKeyBytes(salt)
        return try {
            val aesSpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesSpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(cipherText)
        } catch (e: AEADBadTagException) {
            throw BackupCryptoException("El archivo .avb está dañado o fue alterado.", e)
        } catch (e: GeneralSecurityException) {
            throw BackupCryptoException("No se pudo descifrar el archivo .avb.", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveEnvelopeKeyBytes(salt: ByteArray): ByteArray {
        val seedChars = buildEnvelopeInputChars()
        val spec = PBEKeySpec(seedChars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            seedChars.fill('\u0000')
            spec.clearPassword()
        }
    }

    private fun buildEnvelopeInputChars(): CharArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = ByteArray(ENVELOPE_DOMAIN.size + MIX_PATTERN.size)
        for (i in ENVELOPE_DOMAIN.indices) {
            combined[i] = (ENVELOPE_DOMAIN[i].toInt() xor MIX_PATTERN[i % MIX_PATTERN.size].toInt()).toByte()
        }
        for (i in MIX_PATTERN.indices) {
            combined[ENVELOPE_DOMAIN.size + i] = (MIX_PATTERN[i].toInt() xor (0x37 + i)).toByte()
        }
        val hashBytes = digest.digest(combined)
        combined.fill(0)
        val hexBuilder = StringBuilder(hashBytes.size * 2)
        for (b in hashBytes) {
            hexBuilder.append(String.format("%02x", b.toInt() and 0xFF))
        }
        hashBytes.fill(0)
        val chars = CharArray(hexBuilder.length)
        hexBuilder.getChars(0, hexBuilder.length, chars, 0)
        hexBuilder.clear()
        return chars
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        for (i in prefix.indices) {
            if (bytes[i] != prefix[i]) return false
        }
        return true
    }

    private fun StringBuilder.clear() {
        for (i in indices) {
            setCharAt(i, '\u0000')
        }
        setLength(0)
    }
}
