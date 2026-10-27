package com.orvane.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {

    private val client = AiClient()

    @Test
    fun `parses strict json suggestion`() {
        val content = """{"title":"Buy groceries","subtasks":["Milk","Eggs","Bread"]}"""
        val s = client.parseSuggestion(content)
        assertEquals("Buy groceries", s!!.title)
        assertEquals(listOf("Milk", "Eggs", "Bread"), s.subtasks)
    }

    @Test
    fun `parses json wrapped in prose or markdown fence`() {
        val content = "Here you go:\n```json\n{\"title\":\"Plan trip\",\"subtasks\":[]}\n```"
        val s = client.parseSuggestion(content)
        assertEquals("Plan trip", s!!.title)
        assertTrue(s.subtasks.isEmpty())
    }

    @Test
    fun `plain text first line becomes title`() {
        val s = client.parseSuggestion("Write report\nCollect data\nMake charts")
        assertEquals("Write report", s!!.title)
        assertEquals(listOf("Collect data", "Make charts"), s.subtasks)
    }

    @Test
    fun `at most five subtasks are kept`() {
        val content = """{"title":"T","subtasks":["1","2","3","4","5","6","7"]}"""
        assertEquals(5, client.parseSuggestion(content)!!.subtasks.size)
    }

    @Test
    fun `blank content yields null`() {
        assertNull(client.parseSuggestion("   "))
    }
}
