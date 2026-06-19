package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.security.CryptoManager

@Entity(tableName = "daledou_accounts")
data class DaledouAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val qq: String,
    val description: String,
    val cookieString: String,      // Will contain Keystore-encrypted IV.Ciphertext base64
    val password: String = "",     // Will contain Keystore-encrypted IV.Ciphertext base64
    val isEncrypted: Boolean = true,
    val isActive: Boolean = true
) {
    /**
     * Helper to obtain the clear-text cookies string transparently.
     */
    fun getDecryptedCookies(): String {
        return if (isEncrypted) {
            CryptoManager.decrypt(cookieString)
        } else {
            cookieString
        }
    }

    /**
     * Helper to obtain the clear-text password transparently.
     */
    fun getDecryptedPassword(): String {
        return if (isEncrypted) {
            CryptoManager.decrypt(password)
        } else {
            password
        }
    }
}
