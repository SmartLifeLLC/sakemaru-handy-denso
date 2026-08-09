package biz.smt_life.android.core.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.masterDataStore: DataStore<Preferences> by preferencesDataStore(name = "master_data")

/**
 * Shared HANDY master metadata.
 */
@Singleton
class MasterDataPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.masterDataStore

    val lastUpdatedAtMillis: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[LAST_UPDATED_AT_MILLIS]
    }

    suspend fun getLastUpdatedAtMillisOnce(): Long? {
        val preferences = dataStore.data.first()
        return preferences[LAST_UPDATED_AT_MILLIS]
    }

    suspend fun setLastUpdatedAtMillis(updatedAtMillis: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_UPDATED_AT_MILLIS] = updatedAtMillis
        }
    }

    companion object {
        private val LAST_UPDATED_AT_MILLIS = longPreferencesKey("master_last_updated_at_millis")
    }
}
