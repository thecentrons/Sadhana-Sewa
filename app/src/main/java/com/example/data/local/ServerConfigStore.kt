package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.R
import com.google.firebase.FirebaseApp

data class ServerConfig(
    val defaultDatabaseId: String,
    val customDatabaseId: String,
    val updateLink: String,
    val firebaseProjectId: String
) {
    val activeDatabaseId: String
        get() = customDatabaseId.trim().ifBlank { defaultDatabaseId }

    val isUsingCustomDatabase: Boolean
        get() = customDatabaseId.trim().isNotBlank() && customDatabaseId.trim() != defaultDatabaseId
}

/**
 * Lightweight local store for runtime-configurable cloud database ID and APK update link.
 * Allows switching Firestore Database ID or APK release download URL directly inside the app
 * without rebuilding or reinstalling.
 */
object ServerConfigStore {
    private const val PREFS_NAME = "sadhana_server_config"
    private const val KEY_CUSTOM_DB_ID = "custom_firestore_db_id"
    private const val KEY_UPDATE_URL = "app_update_url"
    const val DEFAULT_UPDATE_URL = "https://github.com/thecentrons/Sadhana-Sewa/releases/latest"

    fun getConfig(context: Context): ServerConfig {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultDb = appCtx.getString(R.string.firestore_database_id)
        val customDb = prefs.getString(KEY_CUSTOM_DB_ID, "").orEmpty().trim()
        val savedUrl = prefs.getString(KEY_UPDATE_URL, DEFAULT_UPDATE_URL).orEmpty().trim()
            .ifBlank { DEFAULT_UPDATE_URL }
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId.orEmpty() }
            .getOrDefault("")
        return ServerConfig(
            defaultDatabaseId = defaultDb,
            customDatabaseId = customDb,
            updateLink = savedUrl,
            firebaseProjectId = projectId
        )
    }

    fun getActiveDatabaseId(context: Context): String = getConfig(context).activeDatabaseId

    fun saveConfig(context: Context, customDatabaseId: String, updateLink: String): ServerConfig {
        val appCtx = context.applicationContext
        val cleanDb = customDatabaseId.trim().take(128)
        val cleanUrl = updateLink.trim().take(300).ifBlank { DEFAULT_UPDATE_URL }
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOM_DB_ID, cleanDb)
            .putString(KEY_UPDATE_URL, cleanUrl)
            .apply()
        return getConfig(appCtx)
    }

    fun resetToDefault(context: Context): ServerConfig {
        val appCtx = context.applicationContext
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_CUSTOM_DB_ID)
            .putString(KEY_UPDATE_URL, DEFAULT_UPDATE_URL)
            .apply()
        return getConfig(appCtx)
    }

    fun openUpdateLink(context: Context, rawUrl: String): Boolean {
        val normalized = rawUrl.trim()
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
