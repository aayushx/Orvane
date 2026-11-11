package com.orvane.app.core.google

import android.app.Activity
import java.io.IOException

/**
 * G4/G5: access tokens for Google APIs (Calendar, Drive AppData). Credential
 * Manager's ID token proves identity but carries no API scopes, so a separate
 * authorization step is needed.
 */
interface AccessTokenProvider {

    /**
     * Returns an access token for the app's Google scopes, or null when the
     * user is signed out or consent can't be obtained in this context.
     *
     * @param activity when non-null, an interactive consent flow may be shown;
     *   when null (background/WorkManager), only silent paths are used.
     */
    suspend fun accessToken(activity: Activity?): String?

    companion object {
        const val SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar"
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    }
}

/**
 * Raised when the account session exists but the stored account has never
 * granted the Calendar/Drive scopes (first-use consent pending, or revoked).
 * Carries the recoverable flow so callers can distinguish it from a true
 * "not signed in" state — never report this as "Not signed in".
 */
class ScopeConsentRequiredException(
    message: String,
    cause: Throwable,
) : IOException(message, cause)
