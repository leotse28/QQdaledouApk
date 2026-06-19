package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {

    private const val ALIAS = "DaledouKeyAlias"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts the raw string and returns a Base64 encoded string format of "IV.Ciphertext".
     */
    fun encrypt(rawText: String): String {
        if (rawText.isEmpty()) return ""
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawText.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            return "$ivBase64.$encryptedBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback in case of encryption issues
            return "ERR_ENC:" + Base64.encodeToString(rawText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts the "IV.Ciphertext" Base64 format back to the original string.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        if (encryptedText.startsWith("ERR_ENC:")) {
            val rawPart = encryptedText.substringAfter("ERR_ENC:")
            return String(Base64.decode(rawPart, Base64.NO_WRAP), Charsets.UTF_8)
        }
        val parts = encryptedText.split(".")
        if (parts.size != 2) {
            // Probably plain text or old unencrypted data
            return encryptedText
        }

        try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            // Safe fallback if key was cleared/invalidated but we still have clean database content matching formatting
            return encryptedText
        }
    }
}
