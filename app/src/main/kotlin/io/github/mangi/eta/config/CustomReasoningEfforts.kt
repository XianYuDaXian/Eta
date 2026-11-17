package io.github.mangi.eta.config

import io.github.mangi.eta.data.model.ReasoningEffort

/** 自定义推理强度的顺序与持久化辅助。 */
internal object CustomReasoningEfforts {
    const val POSITION_FIRST = "first"
    const val POSITION_LAST = "last"

    /** 清理空白、重复项和与内置值冲突的自定义值，保留原始拼写与输入顺序。 */
    fun normalize(values: Collection<ReasoningEffort>): List<ReasoningEffort> =
        values
            .filter { it.isCustom && it.wireValue.isNotBlank() }
            .distinctBy { it.wireValue }

    /** 按用户选择的位置把自定义值放进内置候选列表。 */
    fun merge(
        base: Collection<ReasoningEffort>,
        custom: Collection<ReasoningEffort>,
        position: String,
    ): List<ReasoningEffort> {
        val cleanedCustom = normalize(custom)
        val cleanedBase = base.filterNot { effort -> cleanedCustom.any { it.wireValue.equals(effort.wireValue, ignoreCase = true) } }
        return if (position == POSITION_FIRST) cleanedCustom + cleanedBase else cleanedBase + cleanedCustom
    }

    /** 读取本地偏好后合并，供模型能力与设置页共用。 */
    fun withSavedPlacement(base: Collection<ReasoningEffort>): List<ReasoningEffort> = merge(
        base = base,
        custom = Prefs.customReasoningEfforts(),
        position = Prefs.customReasoningEffortPosition(),
    )
}
