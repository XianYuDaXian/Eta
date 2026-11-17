package io.github.mangi.eta.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ReasoningEffortSerializer::class)
data class ReasoningEffort internal constructor(
    val wireValue: String,
    val displayName: String = wireValue,
    internal val rank: Int = CUSTOM_RANK,
) {
    /** 是否为用户新增的原始值，而非内置强度。 */
    val isCustom: Boolean
        get() = wireValue !in knownByWireValue

    val enablesReasoning: Boolean
        get() = wireValue != OFF.wireValue

    companion object {
        internal const val CUSTOM_RANK = 8

        val OFF = ReasoningEffort("off", "Off", 0)
        val DEFAULT = ReasoningEffort("default", "Default", 1)
        val MINIMAL = ReasoningEffort("minimal", "Minimal", 2)
        val LOW = ReasoningEffort("low", "Low", 3)
        val MEDIUM = ReasoningEffort("medium", "Medium", 4)
        val HIGH = ReasoningEffort("high", "High", 5)
        val XHIGH = ReasoningEffort("xhigh", "XHigh", 6)
        val MAX = ReasoningEffort("max", "Max", 7)

        val knownValues: List<ReasoningEffort> = listOf(
            OFF,
            DEFAULT,
            MINIMAL,
            LOW,
            MEDIUM,
            HIGH,
            XHIGH,
            MAX,
        )

        private val knownByWireValue: Map<String, ReasoningEffort> =
            knownValues.associateBy { it.wireValue }

        private val aliases: Map<String, String> = mapOf(
            "none" to OFF.wireValue,
            "x-high" to XHIGH.wireValue,
            "extra_high" to XHIGH.wireValue,
            "extra-high" to XHIGH.wireValue,
        )

        /**
         * 解析持久化字符串。严格模式只接受内置值；宽松模式保留自定义值的原始拼写。
         */
        fun fromWireValue(value: String?, allowCustom: Boolean = true): ReasoningEffort? {
            val raw = value?.trim().orEmpty()
            if (raw.isEmpty()) return null
            val normalized = raw.lowercase()
            val canonical = knownByWireValue[normalized]?.wireValue ?: aliases[normalized]
            if (canonical != null) return knownByWireValue[canonical]
            return if (allowCustom) ReasoningEffort(raw) else null
        }

        fun fromLegacy(thinkingEnabled: Boolean): ReasoningEffort =
            if (thinkingEnabled) DEFAULT else OFF
    }
}

object ReasoningEffortSerializer : KSerializer<ReasoningEffort> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReasoningEffort", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ReasoningEffort) {
        encoder.encodeString(value.wireValue)
    }

    override fun deserialize(decoder: Decoder): ReasoningEffort =
        ReasoningEffort.fromWireValue(decoder.decodeString(), allowCustom = true) ?: ReasoningEffort.DEFAULT
}

@Serializable
data class ModelReasoningCapabilities(
    val supportedEfforts: List<ReasoningEffort> = emptyList(),
    val defaultEffort: ReasoningEffort? = null,
    val defaultEnabled: Boolean? = null,
    val mandatory: Boolean = false,
    val canDisable: Boolean = false,
    val supportsBudget: Boolean = false,
    val maxBudgetTokens: Int? = null,
    val supportsMaxTokens: Boolean? = null,
) {
    val selectableEfforts: List<ReasoningEffort>
        get() = buildList {
            if (canDisable && !mandatory) add(ReasoningEffort.OFF)
            add(ReasoningEffort.DEFAULT)
            supportedEfforts
                .asSequence()
                .filter { it != ReasoningEffort.OFF && it != ReasoningEffort.DEFAULT }
                .distinct()
                .sortedBy(ReasoningEffort::rank)
                .forEach(::add)
        }.let { base ->
            io.github.mangi.eta.config.CustomReasoningEfforts.withSavedPlacement(base)
        }

    fun normalize(requested: ReasoningEffort): ReasoningEffort {
        val selectable = selectableEfforts
        if (requested in selectable) return requested
        if (requested.isCustom) return ReasoningEffort.DEFAULT
        if (requested == ReasoningEffort.OFF || requested == ReasoningEffort.DEFAULT) {
            return ReasoningEffort.DEFAULT
        }
        return selectable
            .filter { !it.isCustom && it != ReasoningEffort.OFF && it.rank <= requested.rank }
            .maxByOrNull(ReasoningEffort::rank)
            ?: ReasoningEffort.DEFAULT
    }
}
