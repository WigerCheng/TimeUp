package io.wiger.timeup.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.wiger.timeup.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object AppSettings {
    private const val DATA_STORE_NAME = "app_settings"
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val LIVE_NOTIFICATION = booleanPreferencesKey("live_notification_enabled")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: DataStore<Preferences>
    private var initialized = false

    private val _themeMode = MutableStateFlow(ThemeMode.MATERIAL_YOU)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _liveNotificationEnabled = MutableStateFlow(false)
    val liveNotificationEnabled: StateFlow<Boolean> = _liveNotificationEnabled.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            dataStore = PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { appContext.preferencesDataStoreFile(DATA_STORE_NAME) },
            )
            runBlocking(Dispatchers.IO) {
                applyPreferences(dataStore.data.first())
            }
            scope.launch {
                dataStore.data.collect { applyPreferences(it) }
            }
            initialized = true
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        if (!::dataStore.isInitialized) return
        scope.launch {
            dataStore.edit { it[THEME_MODE] = mode.name }
        }
    }

    fun setLiveNotificationEnabled(enabled: Boolean) {
        _liveNotificationEnabled.value = enabled
        if (!::dataStore.isInitialized) return
        scope.launch {
            dataStore.edit { it[LIVE_NOTIFICATION] = enabled }
        }
    }

    private fun applyPreferences(preferences: Preferences) {
        _themeMode.value = preferences[THEME_MODE]
            ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.MATERIAL_YOU
        _liveNotificationEnabled.value = preferences[LIVE_NOTIFICATION] ?: false
    }
}
