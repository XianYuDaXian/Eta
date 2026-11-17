package io.github.mangi.eta.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningEffortTest {
    @Test
    fun customRawValueKeepsItsOriginalSpelling() {
        val effort = ReasoningEffort.fromWireValue("  Ultra-High  ", allowCustom = true)

        assertEquals("Ultra-High", effort?.wireValue)
        assertEquals("Ultra-High", effort?.displayName)
        assertTrue(effort!!.isCustom)
        assertTrue(effort.enablesReasoning)
    }

    @Test
    fun knownAliasesStillResolveToBuiltInValues() {
        assertEquals(ReasoningEffort.OFF, ReasoningEffort.fromWireValue("none", allowCustom = true))
        assertEquals(ReasoningEffort.XHIGH, ReasoningEffort.fromWireValue("x-high", allowCustom = true))
        assertFalse(ReasoningEffort.OFF.isCustom)
    }

    @Test
    fun strictParsingKeepsLegacyUnknownFallback() {
        assertNull(ReasoningEffort.fromWireValue("custom-level", allowCustom = false))
    }

    @Test
    fun serializerRoundTripsCustomRawValue() {
        val effort = ReasoningEffort("deep-think")

        assertEquals("\"deep-think\"", Json.encodeToString(ReasoningEffort.serializer(), effort))
        assertEquals(effort, Json.decodeFromString(ReasoningEffort.serializer(), "\"deep-think\""))
    }

    @Test
    fun knownValuesKeepTheirStableOrder() {
        assertEquals(8, ReasoningEffort.knownValues.size)
        assertEquals(
            listOf("off", "default", "minimal", "low", "medium", "high", "xhigh", "max"),
            ReasoningEffort.knownValues.map { it.wireValue },
        )
    }
}
