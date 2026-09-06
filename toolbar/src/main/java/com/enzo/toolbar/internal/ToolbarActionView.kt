package com.enzo.toolbar.internal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.enzo.toolbar.R
import com.enzo.toolbar.ToolbarAction
import com.enzo.toolbar.ToolbarBadge
import com.enzo.toolbar.databinding.ViewCommonToolbarActionBinding
import kotlin.math.ceil

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 单个动作按钮的 View 实现
 * 职责：
 * 1. 组合图标、文字和徽标 (Badge)。
 * 2. 处理点击、禁用状态和波纹效果。
 * 3. 自动适配点击热区（满足 48dp 最小触摸目标要求）。
 * 4. 为纯图标动作提供 Tooltip 和无障碍朗读增强。
 */
internal class ToolbarActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCommonToolbarActionBinding.inflate(LayoutInflater.from(context), this, true)
    private var appliedActionTextAppearance: Int? = null
    private var defaultActionTextColors: ColorStateList? = null
    private var defaultActionTextSizePx: Float? = null
    private var badgePlacement: BadgePlacement = BadgePlacement.HIDDEN
    private var badgeHorizontalOffsetPx: Int = 0
    private var badgeVerticalOffsetPx: Int = 0
    private val contentBasePaddingEnd: Int = binding.commonToolbarActionContent.paddingEnd

    init {
        // Badge 会部分越过内容右上角；外层不关闭裁剪时，悬浮在 binding root 外的区域会被切掉。
        clipChildren = false
        clipToPadding = false
    }

    /**
     * 将业务模型绑定到 View。
     *
     * @param action 动作快照模型。
     * @param style 统一的 Toolbar 样式属性。
     * @param onClick 点击事件回调，参数为 action.id。
     */
    fun bind(
        action: ToolbarAction,
        style: ToolbarStyle,
        onClick: (String) -> Unit,
        iconTintOverride: ColorStateList? = null,
        actionTextColorOverride: Int? = null,
    ) {
        // 强制设置最小触摸尺寸，防止因图标过小导致点击困难
        minimumWidth = style.actionMinTouchSizePx
        minimumHeight = style.actionMinTouchSizePx

        // 处理交互状态
        isEnabled = action.enabled
        isClickable = action.enabled
        isFocusable = true
        alpha = if (action.enabled) 1f else DISABLED_ALPHA

        bindContent(action, style)
        ensureActionTextAppearance(style)
        updateColors(action, style, iconTintOverride, actionTextColorOverride)

        // 3. 徽标 (Badge) 绑定
        bindBadge(action, style)

        // 4. 无障碍与交互反馈
        contentDescription = buildAccessibilityDescription(action)
        // 为纯图标动作添加长按 Tooltip 提示
        TooltipCompat.setTooltipText(
            this,
            action.contentDescription?.takeIf { resolveActionText(action).isNullOrEmpty() },
        )
        // 设置水波纹点击背景
        background = RippleDrawable(style.rippleColor ?: ColorStateList.valueOf(0), null, null)
        setOnClickListener(if (action.enabled) OnClickListener { onClick(action.id) } else null)
    }

    private fun bindContent(action: ToolbarAction, style: ToolbarStyle) {
        val resolvedText = resolveActionText(action)
        binding.commonToolbarActionIcon.apply {
            visibility = if (action.iconRes == null) View.GONE else View.VISIBLE
            if (action.iconRes == null) {
                setImageDrawable(null)
            } else {
                setImageResource(action.iconRes)
            }
            updateLayoutParams<LinearLayout.LayoutParams> {
                width = style.actionIconSizePx
                height = style.actionIconSizePx
            }
        }
        binding.commonToolbarActionText.apply {
            visibility = if (resolvedText.isNullOrEmpty()) View.GONE else View.VISIBLE
            text = resolvedText ?: ""
            updateLayoutParams<LinearLayout.LayoutParams> {
                marginStart = if (action.iconRes != null && !resolvedText.isNullOrEmpty()) {
                    style.actionContentSpacingPx
                } else {
                    0
                }
            }
        }
    }

    /**
     * 只更新高频滚动场景需要的图标/文字颜色，不重建点击、Badge 或布局状态。
     *
     * @param action 当前动作模型，用于应用单项 tint 优先级。
     * @param style XML/style 解析出的默认颜色来源。
     * @param iconTintOverride 运行时全局图标色；为空时恢复 style。
     * @param actionTextColorOverride 运行时全局文字动作色；为空时恢复 TextAppearance。
     */
    fun updateColors(
        action: ToolbarAction,
        style: ToolbarStyle,
        iconTintOverride: ColorStateList?,
        actionTextColorOverride: Int?,
    ) {
        val iconTint = action.tintColor?.let(ColorStateList::valueOf) ?: iconTintOverride ?: style.iconTint
        ImageViewCompat.setImageTintList(binding.commonToolbarActionIcon, iconTint)
        val textColors = (action.tintColor ?: actionTextColorOverride)
            ?.let(ColorStateList::valueOf)
            ?: defaultActionTextColors
        textColors?.let(binding.commonToolbarActionText::setTextColor)
    }

    private fun ensureActionTextAppearance(style: ToolbarStyle) {
        val content = binding.commonToolbarActionText
        if (appliedActionTextAppearance != style.actionTextAppearance) {
            TextViewCompat.setTextAppearance(content, style.actionTextAppearance)
            defaultActionTextColors = content.textColors
            defaultActionTextSizePx = content.textSize
            appliedActionTextAppearance = style.actionTextAppearance
        }
        val targetTextSizePx = style.actionTextSizePx ?: defaultActionTextSizePx
        if (targetTextSizePx != null && content.textSize != targetTextSizePx) {
            // 直接尺寸只覆盖 TextAppearance 的字号，其余字重、字体和颜色仍由 TextAppearance 管理。
            content.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSizePx)
        }
    }

    /**
     * 重写测量逻辑，确保文字在有限空间内能正确截断。
     * 特别是当右侧动作较多时，需要限制单项的最大宽度，避免挤占导航或标题区域。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val content = binding.commonToolbarActionContent
        val text = binding.commonToolbarActionText
        text.maxWidth = Int.MAX_VALUE
        content.updatePaddingRelative(end = contentBasePaddingEnd)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val badge = binding.commonToolbarActionBadge
        val reservedEndWidth = when (badgePlacement) {
            BadgePlacement.TEXT_CORNER_OUTSIDE -> {
                ceil(badge.measuredWidth * BADGE_OUTSIDE_VISIBLE_FRACTION).toInt() +
                    badgeHorizontalOffsetPx.coerceAtLeast(0)
            }
            else -> 0
        }
        content.updatePaddingRelative(end = contentBasePaddingEnd + reservedEndWidth)

        if (text.visibility == View.VISIBLE && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val iconWidth = occupiedWidth(binding.commonToolbarActionIcon)
            val textMargins = (text.layoutParams as ViewGroup.MarginLayoutParams).let {
                it.marginStart + it.marginEnd
            }
            // 文字只使用扣除图标、内部间距、Badge 预留和外层 padding 后的剩余宽度。
            text.maxWidth = (
                MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd -
                    content.paddingStart - content.paddingEnd - iconWidth - textMargins
                ).coerceAtLeast(0)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        positionBadge()
    }

    private fun positionBadge() {
        val badge = binding.commonToolbarActionBadge
        if (badgePlacement == BadgePlacement.HIDDEN || badge.visibility != View.VISIBLE) {
            badge.translationX = 0f
            badge.translationY = 0f
            return
        }

        val root = binding.commonToolbarActionRoot
        val anchorRect = when (badgePlacement) {
            BadgePlacement.ICON_CORNER_OUTSIDE -> descendantRect(root, binding.commonToolbarActionIcon)
            BadgePlacement.TEXT_CORNER_OUTSIDE -> descendantRect(root, binding.commonToolbarActionText)
            BadgePlacement.HIDDEN -> return
        }
        val direction = if (isRtl()) -1f else 1f
        val centerX = logicalEnd(anchorRect) + direction * (
            badge.measuredWidth * BADGE_OUTSIDE_CENTER_OFFSET_FRACTION + badgeHorizontalOffsetPx
            )
        val unboundedLeft = centerX - badge.measuredWidth / 2f
        val desiredLeft = if (badgePlacement == BadgePlacement.ICON_CORNER_OUTSIDE) {
            // 纯图标动作的点击槽始终保持 48dp；宽 Count 只在槽内向内展开，不得反向撑大布局。
            constrainBadgeLeftToAction(unboundedLeft, root, badge.measuredWidth)
        } else {
            unboundedLeft
        }
        val desiredTop = anchorRect.top - badge.measuredHeight * BADGE_OUTSIDE_TOP_FRACTION +
            badgeVerticalOffsetPx
        badge.translationX = desiredLeft - badge.left
        badge.translationY = desiredTop - badge.top
    }

    private fun constrainBadgeLeftToAction(desiredLeft: Float, root: View, badgeWidth: Int): Float {
        val minimumLeft = -root.left.toFloat()
        val maximumLeft = (width - root.left - badgeWidth).toFloat()
        return if (minimumLeft <= maximumLeft) {
            desiredLeft.coerceIn(minimumLeft, maximumLeft)
        } else {
            minimumLeft
        }
    }

    private fun descendantRect(parent: ViewGroup, child: View): Rect = Rect().also { rect ->
        child.getDrawingRect(rect)
        parent.offsetDescendantRectToMyCoords(child, rect)
    }

    private fun logicalEnd(rect: Rect): Float = if (isRtl()) rect.left.toFloat() else rect.right.toFloat()

    private fun isRtl(): Boolean = layoutDirection == View.LAYOUT_DIRECTION_RTL

    private fun occupiedWidth(view: View): Int {
        if (view.visibility != View.VISIBLE) return 0
        val margins = view.layoutParams as ViewGroup.MarginLayoutParams
        return view.measuredWidth + margins.marginStart + margins.marginEnd
    }

    /**
     * 绑定徽标 (Badge)。
     * Dot 与 Count 始终共享同一种锚点策略，差异只限于自身尺寸和显示内容。
     */
    private fun bindBadge(action: ToolbarAction, style: ToolbarStyle) {
        val resolvedText = resolveActionText(action)
        badgeHorizontalOffsetPx = style.badgeHorizontalOffsetPx
        badgeVerticalOffsetPx = style.badgeVerticalOffsetPx
        badgePlacement = when {
            action.badge == ToolbarBadge.None -> BadgePlacement.HIDDEN
            action.iconRes != null && resolvedText.isNullOrEmpty() -> BadgePlacement.ICON_CORNER_OUTSIDE
            else -> BadgePlacement.TEXT_CORNER_OUTSIDE
        }
        binding.commonToolbarActionBadge.apply {
            backgroundTintList = (action.badge as? ToolbarBadge.Dot)
                ?.backgroundColor
                ?.let(ColorStateList::valueOf)
                ?: style.badgeBackgroundTint
            TextViewCompat.setTextAppearance(this, style.badgeTextAppearance)

            when (val badge = action.badge) {
                ToolbarBadge.None -> visibility = View.GONE
                is ToolbarBadge.Dot -> {
                    // 红点模式：固定小尺寸且不显示文字
                    visibility = View.VISIBLE
                    text = null
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        width = resources.getDimensionPixelSize(R.dimen.common_toolbar_badge_dot_size)
                        height = resources.getDimensionPixelSize(R.dimen.common_toolbar_badge_dot_size)
                        leftMargin = 0
                        rightMargin = 0
                    }
                    minWidth = 0
                    setPadding(0, 0, 0, 0)
                }
                is ToolbarBadge.Count -> {
                    // 数字模式：自适应宽度并显示数字（通常带 99+ 逻辑）
                    visibility = View.VISIBLE
                    text = badge.displayText()
                    val horizontal = resources.getDimensionPixelSize(
                        R.dimen.common_toolbar_badge_padding_horizontal,
                    )
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                        height = resources.getDimensionPixelSize(R.dimen.common_toolbar_badge_min_size)
                        // Badge 是覆盖层，尺寸由自身 padding 决定；不能再把水平 padding
                        // 写进左右 margin，否则 FrameLayout 会把徽标额外计入 ActionView
                        // 的测量宽度，导致“图标 + Count”从 48dp 被撑大。
                        leftMargin = 0
                        rightMargin = 0
                    }
                    minWidth = resources.getDimensionPixelSize(R.dimen.common_toolbar_badge_min_size)
                    setPadding(horizontal, 0, horizontal, 0)
                }
            }
        }
        requestLayout()
    }

    /** 构建无障碍朗读描述，将按钮含义与徽标信息结合。 */
    private fun buildAccessibilityDescription(action: ToolbarAction): CharSequence? {
        val base = action.contentDescription ?: resolveActionText(action)
        val badgeDescription = when (val badge = action.badge) {
            ToolbarBadge.None -> null
            is ToolbarBadge.Dot -> badge.contentDescription
            is ToolbarBadge.Count -> badge.contentDescription
        }
        return listOfNotNull(base, badgeDescription)
            .filter { it.isNotBlank() }
            .joinToString(separator = "，")
            .ifBlank { null }
    }

    /**
     * 解析动作最终显示文字。资源文字必须在 View 层解析，因为模型校验器不持有 Context。
     */
    private fun resolveActionText(action: ToolbarAction): CharSequence? {
        return action.textRes?.let(context::getText) ?: action.text
    }

    private enum class BadgePlacement {
        HIDDEN,
        ICON_CORNER_OUTSIDE,
        TEXT_CORNER_OUTSIDE,
    }

    private companion object {
        const val DISABLED_ALPHA = 0.38f
        const val BADGE_OUTSIDE_CENTER_OFFSET_FRACTION = 0.25f
        const val BADGE_OUTSIDE_TOP_FRACTION = 0.25f
        const val BADGE_OUTSIDE_VISIBLE_FRACTION = 0.75f
    }
}
