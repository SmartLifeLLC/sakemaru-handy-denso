package biz.smt_life.android.core.ui

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Secure storage for authentication token and picker information.
 * Uses EncryptedSharedPreferences per CLAUDE.md security requirements.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "handy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuth(token: String, pickerId: Int, pickerCode: String, pickerName: String, defaultWarehouseId: Int) {
        sharedPreferences.edit {
            putString(KEY_TOKEN, token)
            putInt(KEY_PICKER_ID, pickerId)
            putString(KEY_PICKER_CODE, pickerCode)
            putString(KEY_PICKER_NAME, pickerName)
            putInt(KEY_WAREHOUSE_ID, defaultWarehouseId)
        }
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun getPickerId(): Int {
        return sharedPreferences.getInt(KEY_PICKER_ID, -1)
    }

    fun getPickerCode(): String? {
        return sharedPreferences.getString(KEY_PICKER_CODE, null)
    }

    fun getPickerName(): String? {
        return sharedPreferences.getString(KEY_PICKER_NAME, null)
    }

    fun getDefaultWarehouseId(): Int {
        return sharedPreferences.getInt(KEY_WAREHOUSE_ID, -1)
    }

    fun clearAuth() {
        sharedPreferences.edit {
            remove(KEY_TOKEN)
            remove(KEY_PICKER_ID)
            remove(KEY_PICKER_CODE)
            remove(KEY_PICKER_NAME)
            remove(KEY_WAREHOUSE_ID)
        }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_PICKER_ID = "picker_id"
        private const val KEY_PICKER_CODE = "picker_code"
        private const val KEY_PICKER_NAME = "picker_name"
        private const val KEY_WAREHOUSE_ID = "default_warehouse_id"
    }
}
