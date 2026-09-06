package com.enzo.toolbar

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 外观配置与数据模型定义
 **/
/**
 * [com.enzo.toolbar.CommonToolbar] 的运行时颜色覆盖快照。
 *
 * 所有字段都允许为空；`null` 表示恢复 XML/style 解析出的默认颜色。该模型适合由滚动进度、
 * 折叠比例或页面状态持续生成，不包含阈值、动画时长等业务规则。
 *
 * @property backgroundColor Toolbar 内容和默认状态栏底层的背景色。
 * @property statusBarBackgroundColor 状态栏 spacer 的独立背景色；为空时使用
 * [com.enzo.toolbar.CommonToolbar.setStatusBarBackground] 配置的 Drawable 或透出 Toolbar 背景。
 * @property titleColor 主标题文字颜色。
 * @property subtitleColor 副标题文字颜色。
 * @property iconTint 导航与尾部动作的全局图标色；[ToolbarAction.tintColor] 优先级更高。
 * @property actionTextColor 导航和尾部文字动作的全局文字色；[ToolbarAction.tintColor] 优先级更高。
 */
data class CommonToolbarAppearance(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val statusBarBackgroundColor: Int? = null,
    @ColorInt val titleColor: Int? = null,
    @ColorInt val subtitleColor: Int? = null,
    @ColorInt val iconTint: Int? = null,
    @ColorInt val actionTextColor: Int? = null,
)

/**
 * CommonToolbar 标准标题模式的不可变渲染快照。
 *
 * 作为一个不可变快照，该模型包含了渲染 Toolbar 所需的所有数据，
 * 适用于 MVI 或 Reducer 架构。UI 层通过调用 render(model) 进行整体更新。
 *
 * @property navigation 逻辑 start 侧（左侧，RTL 下为右侧）的导航动作，通常为返回键或抽屉键。
 * @property title 主标题，显示在中间或左侧。
 * @property subtitle 副标题，显示在主标题下方。
 * @property titleActionId 点击整个标准标题区域触发的稳定标识。如果为 null，则标题区域不可点击。
 * @property titleAlignment 标题的对齐策略，默认为强制居中 [ToolbarTitleAlignment.CENTER]。
 * @property actions 逻辑 end 侧（右侧，RTL 下为左侧）的动作按钮列表，按从中心向外的顺序排列。
 * @property dividerVisible 是否在 Toolbar 底部显示一条水平分割线。
 */
data class CommonToolbarModel(
    val navigation: ToolbarAction? = null,
    val title: CharSequence? = null,
    val subtitle: CharSequence? = null,
    val titleActionId: String? = null,
    val titleAlignment: ToolbarTitleAlignment = ToolbarTitleAlignment.CENTER,
    val actions: List<ToolbarAction> = emptyList(),
    val dividerVisible: Boolean = false,
)

/**
 * Toolbar 中一个可交互动作按钮的展示模型。
 *
 * 一个动作可以表现为图标、文字或两者结合。组件会自动处理间距和触摸目标。
 *
 * @property id 页面内稳定且唯一的动作标识，点击回调将通过此 ID 识别具体动作。
 * @property iconRes 图标资源 ID。
 * @property text 可见文字，通常用于“完成”、“发布”等明确动作。
 * @property textRes 字符串资源 ID，优先级高于 [text]。若非 null，渲染时应优先解析此 ID。
 * @property contentDescription 给屏幕阅读器使用的说明。为了无障碍合规，纯图标动作必须提供此字段。
 * @property tintColor 覆盖默认样式的特定着色。若为 null，则使用主题定义的图标/文字颜色。
 * @property badge 右上角显示的数字或红点，通过 [ToolbarBadge] 定义。
 * @property enabled 按钮是否处于可交互状态（变灰、不可点击）。
 * @property visible 是否参与渲染。若为 false，则不占据布局空间且不参与数量溢出限制。
 */
data class ToolbarAction(
    val id: String,
    @DrawableRes val iconRes: Int? = null,
    val text: CharSequence? = null,
    @StringRes val textRes: Int? = null,
    val contentDescription: CharSequence? = null,
    @ColorInt val tintColor: Int? = null,
    val badge: ToolbarBadge = ToolbarBadge.None,
    val enabled: Boolean = true,
    val visible: Boolean = true,
) {
    /** 快速更新图标 */
    fun icon(@DrawableRes resId: Int?) = copy(iconRes = resId)

    /** 快速更新文字内容 */
    fun text(value: CharSequence?) = copy(text = value, textRes = null)

    /** 快速更新文字资源 ID */
    fun text(@StringRes resId: Int?) = copy(textRes = resId, text = null)

    /** 快速更新角标状态 */
    fun badge(value: ToolbarBadge) = copy(badge = value)

    /** 快速更新启用状态 */
    fun enabled(value: Boolean) = copy(enabled = value)

    /** 快速更新显示状态 */
    fun visible(value: Boolean) = copy(visible = value)

    companion object {
        /** 创建一个纯图标动作 */
        fun icon(
            id: String,
            @DrawableRes iconRes: Int,
            contentDescription: CharSequence,
            @ColorInt tintColor: Int? = null
        ) = ToolbarAction(
            id = id,
            iconRes = iconRes,
            contentDescription = contentDescription,
            tintColor = tintColor
        )

        /** 创建一个纯文字动作 */
        fun text(
            id: String,
            text: CharSequence,
            @ColorInt tintColor: Int? = null
        ) = ToolbarAction(
            id = id,
            text = text,
            tintColor = tintColor
        )

        /** 创建一个带图标和文字的动作 */
        fun iconWithText(
            id: String,
            @DrawableRes iconRes: Int,
            text: CharSequence,
            @ColorInt tintColor: Int? = null
        ) = ToolbarAction(
            id = id,
            iconRes = iconRes,
            text = text,
            contentDescription = text, // 默认使用文字作为无障碍描述
            tintColor = tintColor
        )
    }
}
