package com.orvane.app.data.account

/**
 * G3: the signed-in Google account, persisted in DataStore. Only display
 * fields are stored — no tokens (ID tokens expire in ~1h; G4 exchanges
 * tokens through the repository at sync time).
 */
data class AccountSession(
    val id: String,          // Google account id ("sub")
    val email: String,
    val displayName: String,
)
