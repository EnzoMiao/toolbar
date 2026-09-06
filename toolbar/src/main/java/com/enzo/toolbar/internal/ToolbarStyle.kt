package com.enzo.toolbar.internal

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.annotation.StyleRes
import com.enzo.toolbar.R

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 解析并持有 `CommonToolbar` 样式属性解析与持有类
 * 该类作为一个内部数据类，负责从 XML 布局或主题样式中提取 `CommonToolbar` 所需的所有视觉参数。
 * 它采用私有构造函数，通过 [resolve] 静态方法进行实例化，确保了属性解析逻辑的集中管理。
 */
internal class ToolbarStyle private constructor(
    /** 标题文字的 Appearance 样式资源 ID。 */
    val titleTextAppearance: Int,
    /** 副标题文字的 Appearance 样式资源 ID。 */
    val subtitleTextAppearance: Int,
    /** 操作按钮文字的 Appearance 样式资源 ID。 */
    val actionTextAppearance: Int,
    /** 角标（Badge）文字的 Appearance 样式资源 ID。 */
    val badgeTextAppearance: Int,
    /** 图标（返回键、操作键等）的着色列表。 */
    val iconTint: ColorStateList?,
    /** 按钮点击时的水波纹颜色。 */
    val rippleColor: ColorStateList?,
    /** 工具栏底部分割线的颜色。 */
    val dividerColor: Int,
    /** 工具栏底部分割线的高度（单位：像素）。 */
    val dividerHeightPx: Int,
    /** 角标背景的着色列表。 */
    val badgeBackgroundTint: ColorStateList?,
    /** 工具栏内容区域的最小高度（单位：像素）。 */
    val contentMinHeightPx: Int,
    /** 工具栏内容区域的起始内边距（单位：像素）。 */
    val contentPaddingStartPx: Int,
    /** 工具栏内容区域的结束内边距（单位：像素）。 */
    val contentPaddingEndPx: Int,
    /** 操作按钮的最小触摸热区尺寸（单位：像素），确保符合可访问性要求。 */
    val actionMinTouchSizePx: Int,
    /** 操作按钮图标的大小（单位：像素）。 */
    val actionIconSizePx: Int,
    /** 操作按钮文字的特定大小（单位：像素），若 XML 中未配置则为 null，此时应遵循 [actionTextAppearance]。 */
    val actionTextSizePx: Float?,
    /** 多个操作按钮之间的水平间距（单位：像素）。 */
    val actionSpacingPx: Int,
    /** 同一个操作内图标与文字之间的水平间距（单位：像素）。 */
    val actionContentSpacingPx: Int,
    /** 角标相对于图标中心点的水平偏移量（单位：像素）。 */
    val badgeHorizontalOffsetPx: Int,
    /** 角标相对于图标中心点的垂直偏移量（单位：像素）。 */
    val badgeVerticalOffsetPx: Int,
) {
    companion object {
        /**
         * 解析 XML 属性并构造 [ToolbarStyle] 实例。
         *
         * @param context 上下文环境。
         * @param attrs 布局中传入的属性集。
         * @param defStyleAttr 当前主题中引用的默认样式属性（默认为 `R.attr.commonToolbarStyle`）。
         * @param defStyleRes 兜底的样式资源 ID（默认为 `R.style.Widget_Test_CommonToolbar`）。
         * @return 包含完整解析结果的 [ToolbarStyle] 实例。
         */
        fun resolve(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = R.attr.commonToolbarStyle,
            @StyleRes defStyleRes: Int = R.style.Widget_CommonToolbar,
        ): ToolbarStyle {
            // 1. 首先尝试从 attrs 中读取 themeOverlay
            val tempA = context.obtainStyledAttributes(attrs, R.styleable.CommonToolbar, defStyleAttr, defStyleRes)
            val themeOverlay = tempA.getResourceId(R.styleable.CommonToolbar_ct_themeOverlay, 0)
            tempA.recycle()

            // 2. 如果有 Overlay，使用 ContextThemeWrapper 装饰原始 Context
            val styledContext = if (themeOverlay != 0) {
                androidx.appcompat.view.ContextThemeWrapper(context, themeOverlay)
            } else {
                context
            }

            // 3. 获取 TypedArray 以读取最终的自定义属性。
            // 此时如果有 attrs 里的直接属性会优先命中；如果没有则会去 styledContext (即带有 Overlay 的主题) 中找，
            // 最后才会去 defStyleAttr/defStyleRes 找。
            val a = styledContext.obtainStyledAttributes(attrs, R.styleable.CommonToolbar, defStyleAttr, defStyleRes)
            try {
                return ToolbarStyle(
                    titleTextAppearance = a.getResourceId(R.styleable.CommonToolbar_ct_titleTextAppearance, 0),
                    subtitleTextAppearance = a.getResourceId(R.styleable.CommonToolbar_ct_subtitleTextAppearance, 0),
                    actionTextAppearance = a.getResourceId(R.styleable.CommonToolbar_ct_actionTextAppearance, 0),
                    badgeTextAppearance = a.getResourceId(R.styleable.CommonToolbar_ct_badgeTextAppearance, 0),
                    iconTint = a.getColorStateList(R.styleable.CommonToolbar_ct_iconTint),
                    rippleColor = a.getColorStateList(R.styleable.CommonToolbar_ct_rippleColor),
                    dividerColor = a.getColor(R.styleable.CommonToolbar_ct_dividerColor, 0),
                    dividerHeightPx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_dividerHeight, 0),
                    badgeBackgroundTint = a.getColorStateList(R.styleable.CommonToolbar_ct_badgeBackgroundColor),
                    contentMinHeightPx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_contentMinHeight, 0),
                    contentPaddingStartPx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_contentPaddingStart, 0),
                    contentPaddingEndPx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_contentPaddingEnd, 0),
                    actionMinTouchSizePx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_actionMinTouchSize, 0),
                    // 若未显式配置图标大小，则使用默认资源定义的大小
                    actionIconSizePx = a.getDimensionPixelSize(
                        R.styleable.CommonToolbar_ct_actionIconSize,
                        context.resources.getDimensionPixelSize(R.dimen.common_toolbar_icon_size),
                    ),
                    // 仅当显式设置了 textSize 时才读取该值
                    actionTextSizePx = if (a.hasValue(R.styleable.CommonToolbar_ct_actionTextSize)) {
                        a.getDimension(R.styleable.CommonToolbar_ct_actionTextSize, 0f)
                    } else {
                        null
                    },
                    actionSpacingPx = a.getDimensionPixelSize(R.styleable.CommonToolbar_ct_actionSpacing, 0),
                    actionContentSpacingPx = a.getDimensionPixelSize(
                        R.styleable.CommonToolbar_ct_actionContentSpacing,
                        context.resources.getDimensionPixelSize(R.dimen.common_toolbar_action_spacing),
                    ),
                    badgeHorizontalOffsetPx = a.getDimensionPixelOffset(
                        R.styleable.CommonToolbar_ct_badgeHorizontalOffset,
                        0,
                    ),
                    badgeVerticalOffsetPx = a.getDimensionPixelOffset(
                        R.styleable.CommonToolbar_ct_badgeVerticalOffset,
                        0,
                    ),
                )
            } finally {
                // 必须回收 TypedArray 以供后续复用
                a.recycle()
            }
        }
    }
}
