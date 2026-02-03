package biz.smt_life.android.core.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hostDataStore: DataStore<Preferences> by preferencesDataStore(name = "host_settings")

/**
 * DataStore for Host configuration.
 * Stores the base URL for API calls per CLAUDE.md requirements.
 */
@Singleton
class HostPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.hostDataStore

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[BASE_URL_KEY] ?: DEFAULT_BASE_URL
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = url
        }
    }

    suspend fun getBaseUrlOnce(): String {
        val preferences = dataStore.data.first()
        return preferences[BASE_URL_KEY] ?: DEFAULT_BASE_URL
    }

    companion object {
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
    }
}
