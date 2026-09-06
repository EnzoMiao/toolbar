package com.enzo.toolbar.internal

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 右侧动作按钮的容器布局
 * 按模型顺序排列 Toolbar 尾部动作的容器，支持通过 Divider 实现间距。
 */
internal class ToolbarActionsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    init {
        orientation = HORIZONTAL
        showDividers = SHOW_DIVIDER_MIDDLE
    }

    /**
     * 设置动作之间的间距。
     */
    fun setActionSpacing(spacingPx: Int) {
        dividerDrawable = ToolbarSpacingDrawable(spacingPx)
        requestLayout()
    }
}
