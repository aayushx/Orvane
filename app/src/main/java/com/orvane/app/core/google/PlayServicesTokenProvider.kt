package com.orvane.app.core.google

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.orvane.app.data.account.AccountSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * G4: access tokens via Google Play services. Interactive requests (with an
 * Activity) use the Identity AuthorizationClient; background requests use the
 * silent GoogleAuthUtil path with the stored account email.
 */
@Singleton
class PlayServicesTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountStore: AccountSessionStore,
) : AccessTokenProvider {

    override suspend fun accessToken(activity: Activity?): String? {
        val session = accountStore.session.first() ?: return null
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
            suspendCancellableCoroutine { cont ->
                Identity.getAuthorizationClient(activity)
                    .authorize(request)
                    .addOnCompleteListener { task ->
                        cont.resume(task.result?.accessToken)
                    }
            }
        }

    private suspend fun silent(accountEmail: String, scopes: List<String>): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                GoogleAuthUtil.getToken(
                    context,
                    accountEmail,
                    "oauth2:${scopes.joinToString(" ")}",
                )
            }.getOrNull()
        }
}
