package io.github.mangi.eta.config

import io.github.mangi.eta.data.model.ReasoningEffort
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomReasoningEffortsTest {
    @Test
    fun normalizeKeepsOrderAndDropsInvalidValues() {
        val values = listOf(
            ReasoningEffort("deep-think"),
            ReasoningEffort.HIGH,
            ReasoningEffort("deep-think"),
            ReasoningEffort("  "),
        )

        assertEquals(
            listOf(ReasoningEffort("deep-think")),
            CustomReasoningEfforts.normalize(values),
        )
    }

    @Test
    fun mergeCanPlaceCustomValuesFirst() {
        val merged = CustomReasoningEfforts.merge(
            base = listOf(ReasoningEffort.OFF, ReasoningEffort.HIGH),
            custom = listOf(ReasoningEffort("Ultra")),
            position = CustomReasoningEfforts.POSITION_FIRST,
        )

        assertEquals(
            listOf(ReasoningEffort("Ultra"), ReasoningEffort.OFF, ReasoningEffort.HIGH),
            merged,
        )
    }

    @Test
    fun mergeDefaultsToPlacingCustomValuesLast() {
        val merged = CustomReasoningEfforts.merge(
            base = listOf(ReasoningEffort.OFF, ReasoningEffort.HIGH),
            custom = listOf(ReasoningEffort("Ultra")),
            position = "unknown",
        )

        assertEquals(
            listOf(ReasoningEffort.OFF, ReasoningEffort.HIGH, ReasoningEffort("Ultra")),
            merged,
        )
    }

    @Test
    fun orderExistingDoesNotAddUnselectedCustomValues() {
        val ordered = CustomReasoningEfforts.orderExisting(
            base = listOf(ReasoningEffort.OFF, ReasoningEffort("deep-think")),
            position = CustomReasoningEfforts.POSITION_LAST,
        )

        assertEquals(
            listOf(ReasoningEffort.OFF, ReasoningEffort("deep-think")),
            ordered,
        )
    }
}
