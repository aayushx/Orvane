package com.orvane.app.core.google

/**
 * Extracts the account email from a Google ID token (JWT). Pure and
 * unit-testable; base64url decoding only — signature is NOT verified here
 * (the token was minted by Credential Manager on-device).
 */
object GoogleIdTokenPayload {

    /** The `email` claim of the JWT payload, or null when missing/unparseable. */
    fun email(idToken: String): String? = claim(idToken, "email")

    private fun claim(idToken: String, key: String): String? {
        // JWT = header.payload.signature; payload may contain no padding.
        val payload = idToken.split(".").getOrNull(1) ?: return null
        val json = try {
            kotlinx.serialization.json.Json.parseToJsonElement(
                base64UrlDecode(payload),
            ).let { it as? kotlinx.serialization.json.JsonObject }
        } catch (_: Exception) {
            null
        } ?: return null
        return (json[key] as? kotlinx.serialization.json.JsonPrimitive)?.content
    }

    private fun base64UrlDecode(s: String): String {
        val padded = s.padEnd((s.length + 3) / 4 * 4, '=')
        val bytes = java.util.Base64.getUrlDecoder().decode(padded)
        return String(bytes, Charsets.UTF_8)
    }
}
