package com.enzo.toolbar.internal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.enzo.toolbar.ToolbarTitleAlignment

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 中间内容区域的自定义布局，处理精确居中逻辑
 * 这是一个轻量级的自定义 ViewGroup，专门负责 CommonToolbar 的三个核心槽位 View 的测量与摆放。
 * 它不处理具体背景、样式或 Insets，仅作为 [ToolbarLayoutPolicy] 算法在 Android View 系统中的载体。
 *
 * 约束：
 *   View，顺序为：[0] 导航, [1] 中间内容, [2] 动作列表。
 */
internal open class ToolbarContentLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    /** 标题对齐模式，改变后会触发重新布局。 */
    var titleAlignment: ToolbarTitleAlignment = ToolbarTitleAlignment.CENTER
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** 实际生效的对齐模式（可能因空间不足降级）。仅在 measure 之后有效。 */
    var effectiveAlignment: ToolbarTitleAlignment = ToolbarTitleAlignment.CENTER
        private set(value) {
            if (field != value) {
                field = value
                onAlignmentChanged?.invoke(value)
            }
        }

    /** 对齐方式发生实际变化（如降级）时的回调。 */
    var onAlignmentChanged: ((ToolbarTitleAlignment) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 三槽布局依赖固定的 navigation、center、actions 顺序；结构异常时立即失败，
        // 避免后续测量把错误的子 View 当成标题或动作槽而产生难以定位的偏移。
        require(childCount == EXPECTED_CHILD_COUNT) {
            "ToolbarContentLayout 必须包含 navigation、center、actions 三个直接子 View"
        }

        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)

        // 2. 测量侧边槽位
        val sideWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth / 2, MeasureSpec.AT_MOST)
        val childHeightSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom,
            LayoutParams.WRAP_CONTENT,
        )

        val navigation = getChildAt(NAVIGATION_INDEX)
        val center = getChildAt(CENTER_INDEX)
        val actions = getChildAt(ACTIONS_INDEX)

        navigation.measure(sideWidthSpec, childHeightSpec)
        actions.measure(sideWidthSpec, childHeightSpec)

        // 3. 应用布局策略
        val slots = ToolbarLayoutPolicy.calculate(
            availableWidth = availableWidth,
            navigationWidth = navigation.measuredWidth,
            actionsWidth = actions.measuredWidth,
            titleAlignment = titleAlignment,
        )
        effectiveAlignment = slots.effectiveAlignment

        // 4. 精确测量中间槽位
        center.measure(
            MeasureSpec.makeMeasureSpec(slots.centerWidth, MeasureSpec.EXACTLY),
            childHeightSpec,
        )

        // 5. 确定总高度：取三者中的最大值，并考虑最小高度限制和 Padding
        val desiredHeight = maxOf(
            minimumHeight,
            navigation.measuredHeight + paddingTop + paddingBottom,
            center.measuredHeight + paddingTop + paddingBottom,
            actions.measuredHeight + paddingTop + paddingBottom,
        )
        setMeasuredDimension(
            resolveSize(measuredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val navigation = getChildAt(NAVIGATION_INDEX)
        val center = getChildAt(CENTER_INDEX)
        val actions = getChildAt(ACTIONS_INDEX)

        val availableWidth = width - paddingLeft - paddingRight

        // 再次根据测量结果获取精确的槽位起点
        val slots = ToolbarLayoutPolicy.calculate(
            availableWidth = availableWidth,
            navigationWidth = navigation.measuredWidth,
            actionsWidth = actions.measuredWidth,
            titleAlignment = titleAlignment,
        )

        // 摆放所有子 View，内部会自动处理 RTL 转换
        layoutAtLogicalStart(navigation, slots.navigationStart)
        layoutAtLogicalStart(center, slots.centerStart)
        layoutAtLogicalStart(actions, slots.actionsStart)
    }

    /**
     * 在指定的逻辑起点摆放 View。
     * 逻辑起点是指从布局“开始方向”（LTR 为左，RTL 为右）的偏移量。
     * 内部会自动处理垂直居中。
     */
    private fun layoutAtLogicalStart(child: View, logicalStart: Int) {
        val childTop = paddingTop + (height - paddingTop - paddingBottom - child.measuredHeight) / 2
        val childLeft = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            // RTL 模式：从右边界反向计算物理左坐标
            width - paddingRight - logicalStart - child.measuredWidth
        } else {
            // LTR 模式：物理左坐标即为逻辑起点加上容器 Padding
            paddingLeft + logicalStart
        }
        child.layout(
            childLeft,
            childTop,
            childLeft + child.measuredWidth,
            childTop + child.measuredHeight,
        )
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(params: LayoutParams): LayoutParams {
        return LayoutParams(params)
    }

    override fun checkLayoutParams(params: LayoutParams): Boolean = true

    private companion object {
        const val EXPECTED_CHILD_COUNT = 3
        const val NAVIGATION_INDEX = 0
        const val CENTER_INDEX = 1
        const val ACTIONS_INDEX = 2
    }
}
