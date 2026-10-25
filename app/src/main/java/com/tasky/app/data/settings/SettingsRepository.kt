package com.tasky.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tasky_settings")

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

@Serializable
data class PomodoroConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
)

@Serializable
data class AppSettings(
    val onboarded: Boolean = false,
    val themeMode: String = ThemeMode.SYSTEM,   // SYSTEM | LIGHT | DARK
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: Int = 1,                 // java.time.DayOfWeek value (1=Mon)
    val defaultListId: String? = null,
    val snoozePresets: List<Int> = listOf(10, 30, 60, 1440),
    val swipeLeftDeletes: Boolean = true,
    val soundsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val habitNagCutoffHour: Int = 21,
    val habitNagCutoffMinute: Int = 0,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "gpt-4o-mini",
    val pomodoro: PomodoroConfig = PomodoroConfig(),
    val listSortModes: Map<String, String> = emptyMap(), // viewKey -> sortKey
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val DEFAULT_LIST = stringPreferencesKey("default_list")
        val SNOOZE_PRESETS = stringSetPreferencesKey("snooze_presets")
        val SWIPE_LEFT_DELETES = booleanPreferencesKey("swipe_left_deletes")
        val SOUNDS = booleanPreferencesKey("sounds")
        val VIBRATION = booleanPreferencesKey("vibration")
        val NAG_CUTOFF_HOUR = intPreferencesKey("nag_hour")
        val NAG_CUTOFF_MINUTE = intPreferencesKey("nag_minute")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val POMODORO_CONFIG = stringPreferencesKey("pomodoro_config")
        val LIST_SORT_MODES = stringPreferencesKey("list_sort_modes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboarded = p[Keys.ONBOARDED] ?: false,
            themeMode = p[Keys.THEME_MODE] ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: true,
            firstDayOfWeek = p[Keys.FIRST_DAY_OF_WEEK] ?: 1,
            defaultListId = p[Keys.DEFAULT_LIST],
            snoozePresets = p[Keys.SNOOZE_PRESETS]?.mapNotNull { it.toIntOrNull() } ?: listOf(10, 30, 60, 1440),
            swipeLeftDeletes = p[Keys.SWIPE_LEFT_DELETES] ?: true,
            soundsEnabled = p[Keys.SOUNDS] ?: true,
            vibrationEnabled = p[Keys.VIBRATION] ?: true,
            habitNagCutoffHour = p[Keys.NAG_CUTOFF_HOUR] ?: 21,
            habitNagCutoffMinute = p[Keys.NAG_CUTOFF_MINUTE] ?: 0,
            appLockEnabled = p[Keys.APP_LOCK] ?: false,
            pinHash = p[Keys.PIN_HASH] ?: "",
            biometricEnabled = p[Keys.BIOMETRIC] ?: false,
            aiBaseUrl = p[Keys.AI_BASE_URL] ?: "",
            aiApiKey = p[Keys.AI_API_KEY] ?: "",
            aiModel = p[Keys.AI_MODEL] ?: "gpt-4o-mini",
            pomodoro = p[Keys.POMODORO_CONFIG]?.let { runCatching { json.decodeFromString<PomodoroConfig>(it) }.getOrNull() }
                ?: PomodoroConfig(),
            listSortModes = p[Keys.LIST_SORT_MODES]?.let { s ->
                runCatching { json.decodeFromString<Map<String, String>>(s) }.getOrDefault(emptyMap())
            } ?: emptyMap(),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    private val _lockState = MutableStateFlow(false)
    val isLockedFlow: Flow<Boolean> get() = _lockState

    fun setLocked(locked: Boolean) {
        _lockState.value = locked
    }

    suspend fun setOnboarded(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = done }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFirstDayOfWeek(day: Int) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day }
    }

    suspend fun setDefaultList(listId: String?) {
        context.dataStore.edit { it[Keys.DEFAULT_LIST] = listId ?: "" }
    }

    suspend fun setSnoozePresets(presets: List<Int>) {
        context.dataStore.edit { it[Keys.SNOOZE_PRESETS] = presets.map { p -> p.toString() }.toSet() }
    }

    suspend fun setSwipeLeftDeletes(deletes: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_LEFT_DELETES] = deletes }
    }

    suspend fun setSounds(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUNDS] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setNagCutoffHourHour(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.NAG_CUTOFF_HOUR] = hour
            it[Keys.NAG_CUTOFF_MINUTE] = minute
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK] = enabled }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = sha256(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean = pinHashMatches(pin)

    private suspend fun pinHashMatches(candidate: String): Boolean {
        val stored = current().pinHash
        return stored.isNotEmpty() && stored == sha256(candidate)
    }

    suspend fun pinMatches(candidate: String): Boolean = pinHashMatches(candidate)

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    suspend fun setAiConfig(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit {
            it[Keys.AI_BASE_URL] = baseUrl.trim()
            it[Keys.AI_API_KEY] = apiKey.trim()
            it[Keys.AI_MODEL] = model.trim()
        }
    }

    suspend fun setPomodoroConfig(config: PomodoroConfig) {
        context.dataStore.edit { it[Keys.POMODORO_CONFIG] = json.encodeToString(config) }
    }

    suspend fun setListSortMode(viewKey: String, sortKey: String) {
        context.dataStore.edit { p ->
            val cur = p[Keys.LIST_SORT_MODES]?.let { s -> runCatching { json.decodeFromString<Map<String, String>>(s) }.getOrDefault(emptyMap()) }
                ?: emptyMap()
            p[Keys.LIST_SORT_MODES] = json.encodeToString(cur + (viewKey to sortKey))
        }
    }

    private fun sha256(text: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
