package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SettingsState(
    val onboardingCompleted: Boolean = false,
    val isDarkMode: Boolean = false,
    val darkModeSetByUser: Boolean = false,
    val expirationAlerts: Boolean = true,
    val lowStockAlerts: Boolean = true,
    val vegetarianMode: Boolean = false,
    val smartRecipeIdeas: Boolean = true,
    val metricMeasurements: Boolean = true,
    val householdSharing: Boolean = false
)

private val Context.settingsDataStore by preferencesDataStore(name = "shelflife_settings")

class AppSettingsStore(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DARK_MODE_SET_BY_USER = booleanPreferencesKey("dark_mode_set_by_user")
        val EXPIRATION_ALERTS = booleanPreferencesKey("expiration_alerts")
        val LOW_STOCK_ALERTS = booleanPreferencesKey("low_stock_alerts")
        val VEGETARIAN_MODE = booleanPreferencesKey("vegetarian_mode")
        val SMART_RECIPE_IDEAS = booleanPreferencesKey("smart_recipe_ideas")
        val METRIC_MEASUREMENTS = booleanPreferencesKey("metric_measurements")
        val HOUSEHOLD_SHARING = booleanPreferencesKey("household_sharing")
    }

    val settings: Flow<SettingsState> = context.settingsDataStore.data.map { preferences ->
        SettingsState(
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
            isDarkMode = preferences[Keys.DARK_MODE] ?: false,
            darkModeSetByUser = preferences[Keys.DARK_MODE_SET_BY_USER] ?: false,
            expirationAlerts = preferences[Keys.EXPIRATION_ALERTS] ?: true,
            lowStockAlerts = preferences[Keys.LOW_STOCK_ALERTS] ?: true,
            vegetarianMode = preferences[Keys.VEGETARIAN_MODE] ?: false,
            smartRecipeIdeas = preferences[Keys.SMART_RECIPE_IDEAS] ?: true,
            metricMeasurements = preferences[Keys.METRIC_MEASUREMENTS] ?: true,
            householdSharing = preferences[Keys.HOUSEHOLD_SHARING] ?: false
        )
    }

    suspend fun setOnboardingCompleted(value: Boolean) = setBoolean(Keys.ONBOARDING_COMPLETED, value)
    suspend fun setDarkMode(value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DARK_MODE] = value
            preferences[Keys.DARK_MODE_SET_BY_USER] = true
        }
    }
    suspend fun setExpirationAlerts(value: Boolean) = setBoolean(Keys.EXPIRATION_ALERTS, value)
    suspend fun setLowStockAlerts(value: Boolean) = setBoolean(Keys.LOW_STOCK_ALERTS, value)
    suspend fun setVegetarianMode(value: Boolean) = setBoolean(Keys.VEGETARIAN_MODE, value)
    suspend fun setSmartRecipeIdeas(value: Boolean) = setBoolean(Keys.SMART_RECIPE_IDEAS, value)
    suspend fun setMetricMeasurements(value: Boolean) = setBoolean(Keys.METRIC_MEASUREMENTS, value)
    suspend fun setHouseholdSharing(value: Boolean) = setBoolean(Keys.HOUSEHOLD_SHARING, value)

    private suspend fun setBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
