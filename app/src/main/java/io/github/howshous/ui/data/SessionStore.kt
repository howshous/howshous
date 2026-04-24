package io.github.howshous.ui.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "howshous_prefs")

object SessionKeys {
    val ROLE = stringPreferencesKey("role")
    val UID = stringPreferencesKey("uid")
    val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
    val SESSION_ID = stringPreferencesKey("session_id")
    val SESSION_LAST_ACTIVE = longPreferencesKey("session_last_active")
    val LAST_NOTIF_TS = longPreferencesKey("last_notif_ts")
    val PHONE_NOTIFS_ENABLED = booleanPreferencesKey("phone_notifs_enabled")
}

suspend fun saveRole(context: Context, role: String) {
    context.dataStore.edit { it[SessionKeys.ROLE] = role }
}

suspend fun saveUid(context: Context, uid: String) {
    context.dataStore.edit { it[SessionKeys.UID] = uid }
}

suspend fun setWelcomeShown(context: Context) {
    context.dataStore.edit { it[SessionKeys.WELCOME_SHOWN] = true }
}

fun readRoleFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[SessionKeys.ROLE] ?: "" }

fun readUidFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[SessionKeys.UID] ?: "" }

fun readWelcomeShownFlow(context: Context): Flow<Boolean> =
    context.dataStore.data.map { it[SessionKeys.WELCOME_SHOWN] ?: false }

fun readSessionIdFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[SessionKeys.SESSION_ID] ?: "" }

suspend fun ensureSessionId(context: Context): String {
    val (currentId, lastActive) = context.dataStore.data.map { prefs ->
        Pair(
            prefs[SessionKeys.SESSION_ID] ?: "",
            prefs[SessionKeys.SESSION_LAST_ACTIVE],
        )
    }.first()

    val now = System.currentTimeMillis()
    val timeoutMs = 30L * 60L * 1000L // 30 minutes inactivity timeout
    val isExpired = lastActive != null && (now - lastActive!!) > timeoutMs

    val finalId = if (currentId.isBlank() || isExpired) {
        java.util.UUID.randomUUID().toString()
    } else {
        currentId
    }

    context.dataStore.edit { prefs ->
        prefs[SessionKeys.SESSION_ID] = finalId
        prefs[SessionKeys.SESSION_LAST_ACTIVE] = now
    }

    return finalId
}

suspend fun clearSession(context: Context) {
    context.dataStore.edit { it.clear() }
}

suspend fun saveLastNotifiedAtMs(context: Context, value: Long) {
    context.dataStore.edit { it[SessionKeys.LAST_NOTIF_TS] = value }
}

suspend fun readLastNotifiedAtMs(context: Context): Long {
    return context.dataStore.data.map { it[SessionKeys.LAST_NOTIF_TS] ?: 0L }.first()
}

fun readPhoneNotifsEnabledFlow(context: Context): Flow<Boolean> =
    context.dataStore.data.map { it[SessionKeys.PHONE_NOTIFS_ENABLED] ?: true }

suspend fun setPhoneNotifsEnabled(context: Context, enabled: Boolean) {
    context.dataStore.edit { it[SessionKeys.PHONE_NOTIFS_ENABLED] = enabled }
}