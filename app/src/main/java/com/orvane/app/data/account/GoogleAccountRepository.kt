package com.orvane.app.data.account

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.orvane.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G3: Google account sign-in, behind an interface so Google features (G4
 * sync, G5 backup) depend on an abstraction, and the app stays fully usable
 * signed out.
 */
interface AccountRepository {
    /** Current session, or null when signed out. */
    val session: Flow<AccountSession?>

    /** Interactive sign-in; [activity] must be a FragmentActivity/ComponentActivity. */
    suspend fun signIn(activity: Activity): Result<AccountSession>

    /** Clears the session and the credential-manager state. */
    suspend fun signOut()

    /** True when GOOGLE_WEB_CLIENT_ID is configured at build time. */
    val isConfigured: Boolean
}

@Singleton
class GoogleAccountRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val store: AccountSessionStore,
    private val syncScheduler: com.orvane.app.core.google.GoogleSyncScheduler,
) : AccountRepository {
    override val session: Flow<AccountSession?> = store.session

    override val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    override suspend fun signIn(activity: Activity): Result<AccountSession> = runCatching {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            error("Google sign-in is not configured — add GOOGLE_WEB_CLIENT_ID to local.properties (docs/GOOGLE_SETUP.md)")
        }
        if (BuildConfig.DEBUG) {
            // Temporary OrvaneAuth diagnostics: masked client id (last 6 chars)
            // + option flags. Never logged: tokens, full ids, account details.
            android.util.Log.d(
                "OrvaneAuth",
                "signIn: serverClientId suffix=${clientId.takeLast(6)} filterByAuthorizedAccounts=false autoSelect=false",
            )
        }
        val manager = CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build(),
            )
            .build()
        val response = try {
            manager.getCredential(activity, request)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("OrvaneAuth", "getCredential failed: ${e.javaClass.simpleName}: ${e.message}")
            }
            throw e
        }
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val g = GoogleIdTokenCredential.createFrom(credential.data)
            val session = AccountSession(
                id = g.id,
                email = g.id,
                displayName = g.displayName ?: "",
            )
            store.save(session)
            session
        } else {
            error("Unexpected credential type: ${credential.type}")
        }
    }

    override suspend fun signOut() {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        store.clear()
        // G4d: stop the periodic sync worker; it no-ops when signed out, but
        // cancelling keeps the job list clean.
        runCatching { syncScheduler.cancel() }
    }
}
