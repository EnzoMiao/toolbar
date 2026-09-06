package com.enzo.toolbar.internal

import com.enzo.toolbar.CommonToolbarModel
import com.enzo.toolbar.ToolbarAction
import com.enzo.toolbar.ToolbarBadge

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : CommonToolbarModel 的合法性校验器
 **/
/**
 * 对 CommonToolbar 模型执行统一校验与规范化的门禁类。
 *
 * 职责：
 * 1. 业务约束：限制右侧动作数量 (MAX 3)。
 * 2. 数据安全：校验 ID 唯一性、空 ID 检查、纯图标动作必须有描述等。
 * 3. 无障碍：确保纯图标按钮有替代文本。
 * 4. 规范化：自动处理 Count 为 0 的 Badge 转为 None 等。
 */
internal object ToolbarModelValidator {
    /**
     * 校验并规范化渲染模型。
     *
     * @param model 页面提交的原始模型。
     * @return 经过规范化处理、可安全渲染的模型快照。
     * @throws IllegalArgumentException 违反上述职责规则时抛出。
     */
    fun validate(model: CommonToolbarModel): CommonToolbarModel {
        // 1. 规范化所有 Badge
        val normalizedNavigation = model.navigation?.normalizeBadge()
        val normalizedActions = model.actions.map { it.normalizeBadge() }
        val visibleActions = normalizedActions.filter { it.visible }

        // 2. 数量约束
        require(visibleActions.size <= MAX_VISIBLE_ACTIONS) {
            "CommonToolbar 右侧最多只能显示 3 个动作，当前为 ${visibleActions.size} 个"
        }

        // 3. 标题动作约束
        if (model.titleActionId != null) {
            require(model.titleActionId.isNotBlank()) { "CommonToolbar titleActionId 不能为空" }
            require(!model.title.isNullOrBlank() || !model.subtitle.isNullOrBlank()) {
                "CommonToolbar titleActionId 需要非空主标题或副标题才有点击热区"
            }
        }

        // 4. 稳定 ID 参与局部更新与 View 复用，即使动作暂时隐藏也必须保持合法且全局唯一。
        val allItems = buildList {
            normalizedNavigation?.let(::add)
            addAll(normalizedActions)
        }
        allItems.forEach(::validateStableId)

        // 5. 只有可见项才要求具备可展示内容和完整无障碍语义。
        val visibleItems = buildList {
            normalizedNavigation?.takeIf(ToolbarAction::visible)?.let(::add)
            addAll(visibleActions)
        }
        visibleItems.forEach(::validateVisibleAction)

        // 6. 标题动作与所有导航/尾部动作共享同一 ID 命名空间，避免局部更新命中错误项。
        val allIds = buildList {
            model.titleActionId?.let(::add)
            addAll(allItems.map(ToolbarAction::id))
        }
        require(allIds.distinct().size == allIds.size) {
            "CommonToolbar 所有动作的 id 必须唯一"
        }

        return model.copy(
            navigation = normalizedNavigation,
            actions = normalizedActions,
        )
    }

    private fun validateStableId(action: ToolbarAction) {
        require(action.id.isNotBlank()) { "CommonToolbar 动作 id 不能为空" }
    }

    /** 校验单个可见动作的展示与无障碍合规性。 */
    private fun validateVisibleAction(action: ToolbarAction) {
        val hasIcon = action.iconRes != null && action.iconRes > 0
        require(action.textRes == null || action.textRes > 0) {
            "CommonToolbar 动作 textRes 必须是有效资源 ID：${action.id}"
        }
        val hasText = !action.text.isNullOrBlank() || action.textRes != null
        // 按钮必须有内容展示
        require(hasIcon || hasText) { "CommonToolbar 动作必须包含图标或文字：${action.id}" }
        // 纯图标按钮必须提供无障碍描述
        require(!hasIcon || hasText || !action.contentDescription.isNullOrBlank()) {
            "CommonToolbar 纯图标动作必须提供 contentDescription：${action.id}"
        }
    }

    private fun ToolbarAction.normalizeBadge(): ToolbarAction {
        return copy(badge = badge.normalized())
    }

    /** 规范化 Badge 数据 */
    private fun ToolbarBadge.normalized(): ToolbarBadge {
        return when (this) {
            ToolbarBadge.None -> this
            is ToolbarBadge.Dot -> this
            is ToolbarBadge.Count -> {
                require(value >= 0) { "ToolbarBadge.Count.value 不能小于 0" }
                require(maxDisplayValue >= 1) { "ToolbarBadge.Count.maxDisplayValue 不能小于 1" }
                // 如果数量为 0，视为 None
                if (value == 0) ToolbarBadge.None else this
            }
        }
    }

    /**
     * 公共规范：Toolbar 右侧最多允许 3 个动作。
     * 超过 3 个会导致布局拥挤并挤压标题空间，应考虑使用“更多”菜单。
     */
    const val MAX_VISIBLE_ACTIONS: Int = 3
}

/**
 * 生成数字徽标的可见文本。
 *
 * 实现细节：支持上限溢出显示（如 "99+"）。
 * @return None、Dot 和 0 返回 `null`；其余返回数量字符串。
 */
internal fun ToolbarBadge.displayText(): String? {
    return when (this) {
        ToolbarBadge.None,
        is ToolbarBadge.Dot,
        -> null
        is ToolbarBadge.Count -> if (value > maxDisplayValue) "$maxDisplayValue+" else value.toString()
    }
}

private fun CharSequence?.isNullOrBlank(): Boolean {
    return this == null || toString().isBlank()
}
