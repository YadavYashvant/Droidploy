package com.example.droidploy.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "droidploy_prefs",
        Context.MODE_PRIVATE
    )

    fun saveCloudflareCredentials(apiToken: String, accountId: String, zoneId: String) {
        prefs.edit().apply {
            putString("cf_api_token", apiToken)
            putString("cf_account_id", accountId)
            putString("cf_zone_id", zoneId)
            apply()
        }
    }

    fun getApiToken(): String {
        return prefs.getString("cf_api_token", "") ?: ""
    }

    fun getAccountId(): String {
        return prefs.getString("cf_account_id", "") ?: ""
    }

    fun getZoneId(): String {
        return prefs.getString("cf_zone_id", "") ?: ""
    }

    fun saveDomain(domain: String) {
        prefs.edit().putString("domain", domain).apply()
    }

    fun getDomain(): String {
        return prefs.getString("domain", "") ?: ""
    }

    fun saveLastCommand(command: String) {
        prefs.edit().putString("last_command", command).apply()
    }

    fun getLastCommand(): String {
        return prefs.getString("last_command", "node index.js") ?: "node index.js"
    }

    fun isConfigured(): Boolean {
        return getApiToken().isNotEmpty() &&
               getAccountId().isNotEmpty() &&
               getZoneId().isNotEmpty()
    }
}

