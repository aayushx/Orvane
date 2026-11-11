package com.orvane.app.core.google

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.orvane.app.data.account.AccountSession
import com.orvane.app.data.account.AccountSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * G4: access tokens via Google Play services. Interactive requests (with an
 * Activity) use the Identity AuthorizationClient; background requests use the
 * silent GoogleAuthUtil path with the stored account email.
 *
 * Failure taxonomy (never swallow — G4/G5 status lines depend on it):
 * - null return  → genuinely signed out (no stored session);
 * - [ScopeConsentRequiredException] → signed in, scopes not yet consented;
 * - IOException with a distinct message → token mint failed for another reason.
 */
@Singleton
class PlayServicesTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountStore: AccountSessionStore,
) : AccessTokenProvider {

    override suspend fun accessToken(activity: Activity?): String? {
        val session: AccountSession = accountStore.session.first() ?: run {
            Log.d(TAG, "session check: signedIn=false, accountEmail present=false")
            return null
        }
        Log.d(TAG, "session check: signedIn=true, accountEmail present=${session.email.isNotBlank()}")
        val scopes = listOf(AccessTokenProvider.SCOPE_CALENDAR, AccessTokenProvider.SCOPE_DRIVE_APPDATA)
        return if (activity != null) {
            interactive(activity, scopes)
        } else {
            silent(session.email, scopes)
        }
    }

    private suspend fun interactive(activity: Activity, scopes: List<String>): String? =
        withContext(Dispatchers.Main) {
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(scopes.map { Scope(it) })
                .build()
            Log.d(TAG, "token fetch: interactive start")
            suspendCancellableCoroutine { cont ->
                Identity.getAuthorizationClient(activity)
                    .authorize(request)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "token fetch: interactive success")
                            cont.resume(task.result?.accessToken)
                        } else {
                            Log.w(TAG, "token fetch: interactive failed (${task.exception?.javaClass?.simpleName})")
                            cont.resume(null)
                        }
                    }
            }
        }

    private suspend fun silent(accountEmail: String, scopes: List<String>): String =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "token fetch: silent start")
            try {
                val token = GoogleAuthUtil.getToken(
                    context,
                    accountEmail,
                    "oauth2:${scopes.joinToString(" ")}",
                )
                Log.d(TAG, "token fetch: silent success")
                token
            } catch (e: UserRecoverableAuthException) {
                // First-use (or revoked) scope consent — NOT a sign-in failure.
                Log.w(TAG, "token fetch: silent needs consent (${e.javaClass.simpleName})")
                throw ScopeConsentRequiredException(
                    "Authorization required — approve Calendar & Drive access",
                    e,
                )
            } catch (e: GoogleAuthException) {
                Log.w(TAG, "token fetch: silent failed (${e.javaClass.simpleName})")
                throw IOException("Token fetch failed (${e.javaClass.simpleName})", e)
            }
        }

    companion object {
        private const val TAG = "OrvaneAuth"
    }
}
