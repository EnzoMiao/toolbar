package com.enzo.toolbar.internal

import com.enzo.toolbar.ToolbarTitleAlignment

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 布局策略相关的内部辅助类
 **/
/**
 * Toolbar 三槽在去除容器 Padding 后的逻辑横向区间数据类。
 * 所有的坐标都是相对于可用内容区起点的逻辑偏移（LTR 下即为左边距）。
 *
 * @property navigationStart 导航槽逻辑起点。
 * @property centerStart 中间槽逻辑起点。
 * @property centerWidth 中间槽分配到的精确宽度。
 * @property actionsStart 动作槽逻辑起点。
 * @property effectiveAlignment 实际生效的对齐方式，可能因空间不足发生降级。
 */
internal data class ToolbarSlotLayout(
    val navigationStart: Int,
    val centerStart: Int,
    val centerWidth: Int,
    val actionsStart: Int,
    val effectiveAlignment: ToolbarTitleAlignment
)

/**
 * Toolbar 横向布局策略算法。
 *
 * 核心逻辑：
 * 1. [ToolbarTitleAlignment.CENTER] (精确居中模式)：
 *    通过计算左右两侧的最大宽度 R = max(navigation, actions)，
 *    在两侧各自预留长度为 R 的对称“安全区”。
 *    中间内容的可用区间即为 [R, W - R]，从而保证中间内容的中心始终与 Toolbar 的几何中心对齐。
 *    注意：如果单侧宽度超过一半，将降级为 [ToolbarTitleAlignment.START]。
 *
 * 2. [ToolbarTitleAlignment.START] (自然靠左/右模式)：
 *    中间内容紧贴导航槽结束位置，并占据剩余所有可用空间，直到动作槽开始。
 */
internal object ToolbarLayoutPolicy {
    /**
     * 根据当前测量值计算三槽的逻辑坐标。
     *
     * @param availableWidth 除去父布局 Padding 后的总宽度 (W)。
     * @param navigationWidth 导航槽（如返回键）已测量的宽度。
     * @param actionsWidth 动作槽（如右侧所有按钮集合）已测量的宽度。
     * @param titleAlignment 对齐策略。
     * @return 包含三槽起点和宽度的布局结果。
     */
    fun calculate(
        availableWidth: Int,
        navigationWidth: Int,
        actionsWidth: Int,
        titleAlignment: ToolbarTitleAlignment,
    ): ToolbarSlotLayout {
        val w = availableWidth.coerceAtLeast(0)
        val nw = navigationWidth.coerceAtLeast(0)
        val aw = actionsWidth.coerceAtLeast(0)
        val halfWidth = w / 2

        val actionsStart = w - aw

        // 如果原本是 CENTER 但空间不足以对称预留，则降级为 START
        val effective = if (titleAlignment == ToolbarTitleAlignment.CENTER && (nw > halfWidth || aw > halfWidth)) {
            ToolbarTitleAlignment.START
        } else {
            titleAlignment
        }

        return when (effective) {
            ToolbarTitleAlignment.CENTER -> {
                val reservation = maxOf(nw, aw)
                ToolbarSlotLayout(
                    navigationStart = 0,
                    centerStart = reservation,
                    centerWidth = (w - reservation * 2).coerceAtLeast(0),
                    actionsStart = actionsStart,
                    effectiveAlignment = ToolbarTitleAlignment.CENTER
                )
            }
            ToolbarTitleAlignment.START -> ToolbarSlotLayout(
                navigationStart = 0,
                centerStart = nw,
                centerWidth = (w - nw - aw).coerceAtLeast(0),
                actionsStart = actionsStart,
                effectiveAlignment = ToolbarTitleAlignment.START
            )
        }
    }
}
