package com.orvane.app.core.google

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * G3/G4 plumbing: email claim extraction from a Google ID token's payload.
 */
class GoogleIdTokenPayloadTest {

    // header.payload.signature — payload = {"email":"me@example.com","sub":"42"}
    private val payload = """{"email":"me@example.com","sub":"42"}"""

    private fun b64url(s: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray())

    @Test
    fun email_extractedFromPayload() {
        val token = "header.${b64url(payload)}.signature"
        assertEquals("me@example.com", GoogleIdTokenPayload.email(token))
    }

    @Test
    fun missingClaim_returnsNull() {
        val token = "header.${b64url("""{"sub":"42"}""")}.signature"
        assertNull(GoogleIdTokenPayload.email(token))
    }

    @Test
    fun malformedToken_returnsNull() {
        assertNull(GoogleIdTokenPayload.email(""))
        assertNull(GoogleIdTokenPayload.email("not-a-jwt"))
        assertNull(GoogleIdTokenPayload.email("a.${b64url("garbage {{")}.b"))
    }
}
