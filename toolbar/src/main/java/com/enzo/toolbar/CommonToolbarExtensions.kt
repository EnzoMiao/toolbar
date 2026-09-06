package com.enzo.toolbar

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : CommonToolbar 相关的扩展方法
 **/
/**
 * 使用统一默认返回行为渲染 CommonToolbar。
 *
 * 这是对 [com.enzo.toolbar.CommonToolbar.render] 的兼容性扩展，不修改任何已有模型或渲染 API。
 * 默认导航动作使用通用返回图标，点击后交给宿主的 [AppCompatActivity.onBackPressedDispatcher]；
 * 特殊页面可通过 `navigation = null` 完全隐藏导航位。
 *
 * @param toolbar 要渲染的 Toolbar 实例。
 * @param title 主标题；为空时遵循 [com.enzo.toolbar.CommonToolbar.render] 的 XML 默认值继承规则。
 * @param subtitle 副标题；为空时遵循 XML 默认值继承规则。
 * @param navigation 导航动作；省略时使用通用返回动作，传 `null` 时不显示导航按钮。
 * @param actions 逻辑 end 侧动作列表，最多允许三个可见动作。
 * @param titleActionId 标题区域点击时派发的稳定动作 ID；为空时标题不可点击。
 * @param titleAlignment 标题对齐策略。
 * @param dividerVisible 是否显示底部分割线。
 * @param inheritXmlDefaults 是否继承 XML 中声明的标题、副标题和导航默认值。
 * @param onNavigationClick 自定义导航点击行为；为空时调用宿主的返回分发器。
 * @param onActionClick 除导航动作之外的其他动作点击回调。
 * @return 无返回值；Toolbar 会立即提交本次模型和回调。
 * @throws IllegalArgumentException 如果模型不满足 CommonToolbar 的校验规则。
 * @throws IllegalStateException 如果 Toolbar 渲染不在主线程执行。
 */
fun AppCompatActivity.setupCommonToolbar(
    toolbar: CommonToolbar,
    title: CharSequence? = null,
    subtitle: CharSequence? = null,
    navigation: ToolbarAction? = ToolbarAction.icon(
        id = CommonToolbar.ACTION_BACK,
        iconRes = R.drawable.ic_common_toolbar_back,
        contentDescription = getString(R.string.toolbar_back),
    ),
    actions: List<ToolbarAction> = emptyList(),
    titleActionId: String? = null,
    titleAlignment: ToolbarTitleAlignment = ToolbarTitleAlignment.CENTER,
    dividerVisible: Boolean = false,
    inheritXmlDefaults: Boolean = true,
    onNavigationClick: (() -> Unit)? = null,
    onActionClick: (actionId: String) -> Unit = {},
) {
    renderCommonToolbar(
        host = this,
        toolbar = toolbar,
        title = title,
        subtitle = subtitle,
        navigation = navigation,
        actions = actions,
        titleActionId = titleActionId,
        titleAlignment = titleAlignment,
        dividerVisible = dividerVisible,
        inheritXmlDefaults = inheritXmlDefaults,
        onNavigationClick = onNavigationClick,
        onActionClick = onActionClick,
    )
}

/**
 * 在 Fragment 中使用统一默认返回行为渲染 CommonToolbar。
 *
 * 该扩展必须在 Fragment 已附加到 Activity 且 Toolbar 已创建后调用，通常放在
 * `onViewCreated()` 或绑定 UI 状态的渲染函数中。它与 Activity 版本使用完全相同的参数和行为，
 * 返回事件由 Fragment 所属 Activity 的 [ComponentActivity.onBackPressedDispatcher] 处理。
 *
 * @param toolbar 要渲染的 Toolbar 实例。
 * @param title 主标题；为空时遵循 [CommonToolbar.render] 的 XML 默认值继承规则。
 * @param subtitle 副标题；为空时遵循 XML 默认值继承规则。
 * @param navigation 导航动作；省略时使用通用返回动作，传 `null` 时不显示导航按钮。
 * @param actions 逻辑 end 侧动作列表，最多允许三个可见动作。
 * @param titleActionId 标题区域点击时派发的稳定动作 ID；为空时标题不可点击。
 * @param titleAlignment 标题对齐策略。
 * @param dividerVisible 是否显示底部分割线。
 * @param inheritXmlDefaults 是否继承 XML 中声明的标题、副标题和导航默认值。
 * @param onNavigationClick 自定义导航点击行为；为空时调用宿主 Activity 的返回分发器。
 * @param onActionClick 除导航动作之外的其他动作点击回调。
 * @return 无返回值；Toolbar 会立即提交本次模型和回调。
 * @throws IllegalStateException 如果 Fragment 尚未附加 Activity，或 Toolbar 渲染不在主线程执行。
 * @throws IllegalArgumentException 如果模型不满足 CommonToolbar 的校验规则。
 */
fun Fragment.setupCommonToolbar(
    toolbar: CommonToolbar,
    title: CharSequence? = null,
    subtitle: CharSequence? = null,
    navigation: ToolbarAction? = ToolbarAction.icon(
        id = CommonToolbar.ACTION_BACK,
        iconRes = R.drawable.ic_common_toolbar_back,
        contentDescription = getString(R.string.toolbar_back),
    ),
    actions: List<ToolbarAction> = emptyList(),
    titleActionId: String? = null,
    titleAlignment: ToolbarTitleAlignment = ToolbarTitleAlignment.CENTER,
    dividerVisible: Boolean = false,
    inheritXmlDefaults: Boolean = true,
    onNavigationClick: (() -> Unit)? = null,
    onActionClick: (actionId: String) -> Unit = {},
) {
    renderCommonToolbar(
        host = requireActivity(),
        toolbar = toolbar,
        title = title,
        subtitle = subtitle,
        navigation = navigation,
        actions = actions,
        titleActionId = titleActionId,
        titleAlignment = titleAlignment,
        dividerVisible = dividerVisible,
        inheritXmlDefaults = inheritXmlDefaults,
        onNavigationClick = onNavigationClick,
        onActionClick = onActionClick,
    )
}

private fun renderCommonToolbar(
    host: ComponentActivity,
    toolbar: CommonToolbar,
    title: CharSequence?,
    subtitle: CharSequence?,
    navigation: ToolbarAction?,
    actions: List<ToolbarAction>,
    titleActionId: String?,
    titleAlignment: ToolbarTitleAlignment,
    dividerVisible: Boolean,
    inheritXmlDefaults: Boolean,
    onNavigationClick: (() -> Unit)?,
    onActionClick: (actionId: String) -> Unit,
) {
    // 先固定本次渲染使用的导航快照，避免回调执行时被外部可变引用改变语义。
    val navigationSnapshot = navigation
    toolbar.render(
        model = CommonToolbarModel(
            navigation = navigationSnapshot,
            title = title,
            subtitle = subtitle,
            titleActionId = titleActionId,
            titleAlignment = titleAlignment,
            actions = actions,
            dividerVisible = dividerVisible,
        ),
        inheritXmlDefaults = inheritXmlDefaults,
    ) { actionId ->
        if (navigationSnapshot?.id == actionId) {
            onNavigationClick?.invoke()
                ?: host.onBackPressedDispatcher.onBackPressed()
        } else {
            onActionClick(actionId)
        }
    }

    // render() 的默认合并规则会把 null 导航回退到 XML 导航默认值；这里使用已有 setter
    // 明确清除它，保证特殊页面传 navigation = null 时确实没有返回按钮，同时保留 XML 标题等其他默认值。
    if (navigationSnapshot == null && inheritXmlDefaults) {
        toolbar.setNavigationAction(null)
    }
}
