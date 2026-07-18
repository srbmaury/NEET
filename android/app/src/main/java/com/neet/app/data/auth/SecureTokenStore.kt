package com.neet.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "neet_auth_token_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_IV_LENGTH_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * `androidx.security:security-crypto` (EncryptedSharedPreferences) is deprecated as of mid-2025
 * in favor of using Android Keystore directly — this wraps an AES/GCM key generated in the
 * Keystore (hardware-backed on most devices) around plain DataStore Preferences storage, which
 * only ever holds the resulting ciphertext.
 */
class SecureTokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("encrypted_token")
    private val emailKey = stringPreferencesKey("account_email")

    private val secretKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    suspend fun saveToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + ciphertext
        context.authDataStore.edit { it[tokenKey] = Base64.encodeToString(combined, Base64.NO_WRAP) }
    }

    fun tokenFlow(): Flow<String?> = context.authDataStore.data.map { prefs ->
        val encoded = prefs[tokenKey] ?: return@map null
        runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    /** Just an identifier for display ("Signed in as x@y.com") — not a credential, so no
     * encryption needed unlike [saveToken]. */
    suspend fun saveEmail(email: String) {
        context.authDataStore.edit { it[emailKey] = email }
    }

    fun emailFlow(): Flow<String?> = context.authDataStore.data.map { it[emailKey] }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
