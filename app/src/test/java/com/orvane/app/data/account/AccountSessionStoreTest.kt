package com.orvane.app.data.account

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * G3: the account session persists in DataStore and clears on sign-out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountSessionStoreTest {

    @Test
    fun saveThenRead_roundTripsSession() = runTest {
        val store = AccountSessionStore(ApplicationProvider.getApplicationContext())
        store.save(AccountSession(id = "acc-1", email = "me@example.com", displayName = "Me"))
        val s = store.session.first()
        assertEquals("acc-1", s?.id)
        assertEquals("me@example.com", s?.email)
        assertEquals("Me", s?.displayName)
    }

    @Test
    fun clear_signsOut() = runTest {
        val store = AccountSessionStore(ApplicationProvider.getApplicationContext())
        store.save(AccountSession(id = "acc-1", email = "me@example.com", displayName = "Me"))
        store.clear()
        assertNull(store.session.first())
    }
}
