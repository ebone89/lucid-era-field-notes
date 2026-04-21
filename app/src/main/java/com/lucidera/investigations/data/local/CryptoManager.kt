package com.lucidera.investigations.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles KeyStore operations for database encryption.
 * Fixes NPE by using Keystore to wrap/unwrap a persistent passphrase.
 */
object CryptoManager {
    private const val KEY_ALIAS = "db_encryption_key"
    private const val KEYSTORE_TYPE = "AndroidKeyStore"
    private const val PREFS_NAME = "crypto_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_db_passphrase"
    private const val IV_KEY = "crypto_iv"

    fun getPassphrase(context: android.content.Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val encryptedPassphraseBase64 = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivBase64 = prefs.getString(IV_KEY, null)

        return if (encryptedPassphraseBase64 != null && ivBase64 != null) {
            // Decrypt existing passphrase
            val encryptedPassphrase = Base64.decode(encryptedPassphraseBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            decryptPassphrase(encryptedPassphrase, iv)
        } else {
            // Generate, encrypt, and store new passphrase
            val newPassphrase = generateRandomPassphrase()
            val (encrypted, iv) = encryptPassphrase(newPassphrase)
            prefs.edit()
                .putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encrypted, Base64.DEFAULT))
                .putString(IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
            newPassphrase
        }
    }

    private fun generateRandomPassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateKey()
    }

    private fun encryptPassphrase(passphrase: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encrypted = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
        return Pair(encrypted, cipher.iv)
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_TYPE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun nukeEverything(context: android.content.Context) {
        // 1. Delete the key from Keystore
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        keyStore.deleteEntry(KEY_ALIAS)

        // 2. Delete the database file
        context.deleteDatabase("fieldbook.db")

        // 3. Optional: Clear SharedPreferences or other app data if needed
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // 4. Force exit to ensure all memory caches are cleared and DB is closed
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
