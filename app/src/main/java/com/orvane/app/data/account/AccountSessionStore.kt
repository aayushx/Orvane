package com.orvane.app.data.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accountStore: DataStore<Preferences> by preferencesDataStore(name = "orvane_account")

/**
 * G3: persists the signed-in account in its own DataStore file so the
 * settings store stays untouched.
 */
@Singleton
class AccountSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ID = stringPreferencesKey("account_id")
        val EMAIL = stringPreferencesKey("account_email")
        val NAME = stringPreferencesKey("account_name")
    }

    val session: Flow<AccountSession?> = context.accountStore.data
        .map { p ->
            val id = p[Keys.ID]
            if (id.isNullOrBlank()) {
                null
            } else {
                AccountSession(
                    id = id,
                    email = p[Keys.EMAIL] ?: "",
                    displayName = p[Keys.NAME] ?: "",
                )
            }
        }

    suspend fun save(session: AccountSession) {
        context.accountStore.edit { p ->
            p[Keys.ID] = session.id
            p[Keys.EMAIL] = session.email
            p[Keys.NAME] = session.displayName
        }
    }

    suspend fun clear() {
        context.accountStore.edit { it.clear() }
    }
}
