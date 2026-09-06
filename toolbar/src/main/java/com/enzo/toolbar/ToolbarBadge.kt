package com.enzo.toolbar

import androidx.annotation.ColorInt

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 动作按钮的徽标（红点、数字）定义
 **/
sealed interface ToolbarBadge {
    /** 不显示徽标。 */
    data object None : ToolbarBadge

    /**
     * 只显示圆点的徽标。
     *
     * @property contentDescription 合并到所属动作的可选朗读信息。
     * @property backgroundColor 当前圆点的可选背景色；为空时回退到 Toolbar 样式。
     */
    data class Dot(
        val contentDescription: CharSequence? = null,
        @ColorInt val backgroundColor: Int? = null,
    ) : ToolbarBadge

    /**
     * 显示非负数量的徽标。
     *
     * @property value 实际数量；0 会规范化为 [None]。
     * @property maxDisplayValue 可直接显示的最大值，超过后显示“最大值+”。
     * @property contentDescription 合并到所属动作的可选朗读信息。
     */
    data class Count(
        val value: Int,
        val maxDisplayValue: Int = 99,
        val contentDescription: CharSequence? = null,
    ) : ToolbarBadge
}
