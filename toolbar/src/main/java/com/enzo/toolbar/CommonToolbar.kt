package com.enzo.toolbar

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.enzo.toolbar.databinding.ViewCommonToolbarBinding
import com.enzo.toolbar.internal.ToolbarActionView
import com.enzo.toolbar.internal.ToolbarModelValidator
import com.enzo.toolbar.internal.ToolbarStyle

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 通用 Toolbar 组件主类
 * 核心特性：
 * 1. 三槽布局：左侧导航槽 (Navigation)、中间内容槽 (Title/Custom View)、右侧动作槽 (Actions)。
 * 2. 精确居中：通过 [com.enzo.toolbar.internal.ToolbarContentLayout] 算法，确保中间内容在两侧非对称时仍能几何居中。
 * 3. Insets 适配：提供 [com.enzo.toolbar.ToolbarInsetMode] 选项，可接管状态栏高度或处理沉浸式环境下的横向安全区。
 * 4. 模型驱动：基于不可变的 [com.enzo.toolbar.CommonToolbarModel] 进行单向渲染，保证 UI 状态的一致性。
 * 5. 实例复用：内部 [ToolbarActionView] 基于 ID 稳定复用，减少 View 重建并优化无障碍焦点稳定性。
 *
 * @param context 用于解析主题、资源与创建内部 View 的上下文。
 * @param attrs XML 中声明的属性集合；代码创建时可为 `null`。
 * @param defStyleAttr 默认样式属性，默认读取 [R.attr.commonToolbarStyle]。
 */
class CommonToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.commonToolbarStyle,
) : FrameLayout(context, attrs, defStyleAttr, R.style.Widget_CommonToolbar) {

    // 使用 ViewBinding 管理布局，根布局通常包含状态栏占位、内容区、动作列表和底部分割线
    private val binding = ViewCommonToolbarBinding.inflate(LayoutInflater.from(context), this, true)

    // 解析统一的 Toolbar 样式，包含高度、内边距、文字样式等
    private val style: ToolbarStyle

    // 右侧动作按钮的实例缓存，Key 为动作的稳定 ID
    private val actionViews = LinkedHashMap<String, ToolbarActionView>()

    // XML 解析出的默认模型快照，用于 render 时的缺省值回退
    private val xmlDefaultModel: CommonToolbarModel

    // 当前正在展示的模型快照
    private var currentModel = CommonToolbarModel()

    // 动作点击回调闭包
    private var actionClick: (String) -> Unit = { }

    // 是否开启 Insets 冲突诊断逻辑
    private var debugInsetsDiagnostics: Boolean = false

    // 运行时外观仅属于当前 View 实例，不进入模型或 SavedState。
    private var currentAppearance = CommonToolbarAppearance()
    private var appearanceIconTint: ColorStateList? = null
    private var defaultToolbarBackground: Drawable? = null
    private lateinit var defaultTitleTextColors: ColorStateList
    private lateinit var defaultSubtitleTextColors: ColorStateList

    /**
     * 当前顶部系统栏 Insets 处理模式。
     *
     * [com.example.ui.toolbar.ToolbarInsetMode.NONE]: 不接管 Insets，内容区直接贴顶。
     * [com.example.ui.toolbar.ToolbarInsetMode.EDGE_TO_EDGE]: 接管状态栏高度作为顶部 Padding，并考虑刘海屏横向安全区。
     *
     * 修改该值会触发 [ViewCompat.requestApplyInsets]。
     */
    var insetMode: ToolbarInsetMode = ToolbarInsetMode.NONE
        set(value) {
            checkMainThread()
            if (field != value) {
                field = value
                ViewCompat.requestApplyInsets(this)
            }
        }

    // 状态栏占位区的背景 Drawable
    private var statusBarBackgroundDrawable: Drawable? = null

    init {
        // 1. 应用默认 Style 属性与 Overlay
        style = ToolbarStyle.resolve(context, attrs, defStyleAttr, R.style.Widget_CommonToolbar)

        val tempA = context.obtainStyledAttributes(attrs, R.styleable.CommonToolbar, defStyleAttr, R.style.Widget_CommonToolbar)
        val themeOverlay = tempA.getResourceId(R.styleable.CommonToolbar_ct_themeOverlay, 0)
        tempA.recycle()

        val styledContext = if (themeOverlay != 0) ContextThemeWrapper(context, themeOverlay) else context

        applyStyle()

        // 2. 解析 XML 声明的自定义属性（如标题、导航图标、对齐方式等）
        val a = styledContext.obtainStyledAttributes(attrs, R.styleable.CommonToolbar, defStyleAttr, R.style.Widget_CommonToolbar)
        try {
            // CommonToolbar 的构造函数已经按宿主 Context 应用了默认背景；这里再用带 Overlay 的
            // TypedArray 解析一次 android:background，确保局部 Overlay 能真正覆盖根 View 背景。
            a.getDrawable(R.styleable.CommonToolbar_android_background)?.let { background = it }
            val title = a.getText(R.styleable.CommonToolbar_ct_title)
            val subtitle = a.getText(R.styleable.CommonToolbar_ct_subtitle)
            val alignment = ToolbarTitleAlignment.entries[a.getInt(R.styleable.CommonToolbar_ct_titleAlignment, 0)]
            val dividerVisible = a.getBoolean(R.styleable.CommonToolbar_ct_dividerVisible, false)

            insetMode = ToolbarInsetMode.entries[a.getInt(R.styleable.CommonToolbar_ct_insetMode, 0)]
            statusBarBackgroundDrawable = a.getDrawable(R.styleable.CommonToolbar_ct_statusBarBackground)
            debugInsetsDiagnostics = a.getBoolean(R.styleable.CommonToolbar_ct_insetConflictDiagnostics, false)

            val navIcon = a.getResourceId(R.styleable.CommonToolbar_ct_navigationIcon, 0)
            val navText = a.getText(R.styleable.CommonToolbar_ct_navigationText)
            val navDesc = a.getText(R.styleable.CommonToolbar_ct_navigationContentDescription)

            // 构建初始模型
            val navigation = if (navIcon > 0 || !navText.isNullOrBlank()) {
                ToolbarAction(
                    id = XML_NAVIGATION_ID,
                    iconRes = if (navIcon > 0) navIcon else null,
                    text = navText,
                    contentDescription = navDesc,
                )
            } else {
                null
            }

            currentModel = ToolbarModelValidator.validate(
                CommonToolbarModel(
                    navigation = navigation,
                    title = title,
                    subtitle = subtitle,
                    titleAlignment = alignment,
                    dividerVisible = dividerVisible,
                ),
            )
        } finally {
            a.recycle()
        }

        // 必须在 Overlay 背景和 TextAppearance 应用后快照默认值，供运行时外观恢复使用。
        defaultToolbarBackground = background
        defaultTitleTextColors = binding.commonToolbarTitle.textColors
        defaultSubtitleTextColors = binding.commonToolbarSubtitle.textColors

        // 处理 Layout Editor 预览覆盖属性 (tools:ct_title, tools:ct_subtitle, tools:ct_action_count)
        if (isInEditMode && attrs != null) {
            val toolsTitle = attrs.getAttributeValue("http://schemas.android.com/tools", "ct_title")
            val toolsSubtitle = attrs.getAttributeValue("http://schemas.android.com/tools", "ct_subtitle")
            val toolsActionCount = attrs.getAttributeIntValue("http://schemas.android.com/tools", "ct_action_count", -1)

            var previewModel = currentModel
            if (toolsTitle != null) previewModel = previewModel.copy(title = toolsTitle)
            if (toolsSubtitle != null) previewModel = previewModel.copy(subtitle = toolsSubtitle)

            if (toolsActionCount in 0..3) {
                val previewActions = (1..toolsActionCount).map { i ->
                    ToolbarAction(
                        id = "preview_action_$i",
                        iconRes = android.R.drawable.ic_menu_info_details,
                        contentDescription = "Preview Action $i",
                        enabled = false,
                    )
                }
                previewModel = previewModel.copy(actions = previewActions)
            }

            // 预览模式如果完全没设标题，显示占位符
            if (previewModel.title.isNullOrEmpty() && previewModel.subtitle.isNullOrEmpty() && previewModel.navigation == null) {
                previewModel = previewModel.copy(title = "CommonToolbar Preview")
            }
            currentModel = previewModel
        }

        xmlDefaultModel = currentModel

        // 3. 配置状态栏背景、安装 Insets 监听器并执行首次渲染
        if (!isInEditMode) {
            setStatusBarBackground(statusBarBackgroundDrawable)
            installInsetsListener()
        }

        binding.commonToolbarContent.onAlignmentChanged = { alignment ->
            binding.commonToolbarTitle.gravity = titleGravity(alignment)
            binding.commonToolbarSubtitle.gravity = titleGravity(alignment)
            binding.commonToolbarStandardCenter.gravity = when (alignment) {
                ToolbarTitleAlignment.CENTER -> Gravity.CENTER
                ToolbarTitleAlignment.START -> Gravity.CENTER_VERTICAL or Gravity.START
            }
        }

        renderInternal()

        // 语义化配置：标题标记为 Heading 方便屏幕阅读器
        ViewCompat.setAccessibilityHeading(binding.commonToolbarTitle, true)
    }

    // 记录上一次 Insets 应用的结果，用于防止重复布局
    private var lastInsetsSnapshot: InsetsSnapshot? = null

    private data class InsetsSnapshot(
        val top: Int,
        val start: Int,
        val end: Int,
        val insetMode: ToolbarInsetMode,
        val layoutDirection: Int
    )

    /**
     * 安装系统栏 Insets 监听逻辑。
     * 核心规则：仅当模式为 EDGE_TO_EDGE 时，将状态栏高度应用给 spacer，将 cutout 间距应用给 contentPadding。
     */
    private fun installInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val topInset: Int
            val safeStart: Int
            val safeEnd: Int
            val currentLD = layoutDirection

            if (insetMode == ToolbarInsetMode.NONE) {
                topInset = 0
                safeStart = 0
                safeEnd = 0
            } else {
                // 获取状态栏高度
                val statusTop = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars(),
                ).top
                // 获取显示屏切口（如刘海）的安全区域
                val cutout = insets.displayCutout
                topInset = maxOf(statusTop, cutout?.safeInsetTop ?: 0)

                // 处理横屏沉浸式下的左右刘海避让（考虑 RTL 自动反转）
                val safeLeft = cutout?.safeInsetLeft ?: 0
                val safeRight = cutout?.safeInsetRight ?: 0
                val rtl = currentLD == LAYOUT_DIRECTION_RTL
                safeStart = if (rtl) safeRight else safeLeft
                safeEnd = if (rtl) safeLeft else safeRight
            }

            // 检查快照，避免重复测量/布局请求
            val newSnapshot = InsetsSnapshot(topInset, safeStart, safeEnd, insetMode, currentLD)
            if (newSnapshot != lastInsetsSnapshot) {
                lastInsetsSnapshot = newSnapshot

                // 执行诊断逻辑 (Debug only)
                val isDebuggableApp = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebuggableApp && debugInsetsDiagnostics && insetMode != ToolbarInsetMode.NONE && paddingTop > 0) {
                    val viewId = if (id == NO_ID) {
                        "NO_ID"
                    } else {
                        runCatching { resources.getResourceEntryName(id) }.getOrDefault(id.toString())
                    }
                    Log.w(
                        "CommonToolbar",
                        "检测到潜在的 Insets 冲突：id=$viewId tag=$tag，Toolbar 已有 paddingTop=$paddingTop，" +
                            "同时 insetMode=$insetMode 正在应用 topInset=$topInset。",
                    )
                }

                // 更新顶部占位块高度
                binding.commonToolbarStatusBarSpacer.updateLayoutParams {
                    height = topInset
                }

                // 将刘海安全区叠加到 Toolbar 内容区的横向 Padding 上
                val targetStart = style.contentPaddingStartPx + safeStart
                val targetEnd = style.contentPaddingEndPx + safeEnd
                binding.commonToolbarContent.setPaddingRelative(
                    targetStart,
                    binding.commonToolbarContent.paddingTop,
                    targetEnd,
                    binding.commonToolbarContent.paddingBottom,
                )
            }

            // 返回原始 Insets 供后续子 View 或页面继续分发
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 挂载后立即请求一次 Insets 分发
        ViewCompat.requestApplyInsets(this)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        // 动作层级保持模型顺序，由容器负责镜像；方向切换后只需重新排版和刷新 cutout 映射。
        binding.commonToolbarContent.layoutDirection = layoutDirection
        binding.commonToolbarActions.layoutDirection = layoutDirection
        binding.commonToolbarNavigation.layoutDirection = layoutDirection
        requestLayout()
        ViewCompat.requestApplyInsets(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 未 attach 的测试/预览环境可能在首次 measure 才解析方向，测量前再次同步可避免子槽滞后。
        binding.commonToolbarContent.layoutDirection = layoutDirection
        binding.commonToolbarActions.layoutDirection = layoutDirection
        binding.commonToolbarNavigation.layoutDirection = layoutDirection
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * 设置仅绘制在状态栏 spacer 上的背景。
     *
     * 此功能允许 Toolbar 在沉浸式下接管状态栏的视觉颜色，而不依赖于 Window.setStatusBarColor。
     * @param background 颜色或渐变 Drawable；传 null 清除。
     */
    fun setStatusBarBackground(background: Drawable?) {
        checkMainThread()
        if (statusBarBackgroundDrawable !== background) {
            statusBarBackgroundDrawable = background
        }
        applyStatusBarBackground()
    }

    /**
     * 高频、可逆地应用运行时颜色覆盖。
     *
     * 相同快照不会重复更新；图标/文字动作只改颜色，不重绑点击、Badge 或 View 层级。
     * 传入默认构造的 [CommonToolbarAppearance] 可恢复 XML/style 颜色。
     *
     * @param appearance 本帧完整外观快照。
     */
    fun setAppearance(appearance: CommonToolbarAppearance) {
        checkMainThread()
        if (currentAppearance == appearance) return
        val actionColorsChanged = currentAppearance.iconTint != appearance.iconTint ||
            currentAppearance.actionTextColor != appearance.actionTextColor
        currentAppearance = appearance
        appearanceIconTint = appearance.iconTint?.let(ColorStateList::valueOf)

        appearance.backgroundColor?.let(::setBackgroundColor)
            ?: run {
                // 修正：从 ConstantState 创建新实例以确保从 ColorDrawable 状态彻底恢复
                background = defaultToolbarBackground?.constantState?.newDrawable(resources) ?: defaultToolbarBackground
            }

        binding.commonToolbarTitle.setTextColor(
            appearance.titleColor?.let(ColorStateList::valueOf) ?: defaultTitleTextColors,
        )
        binding.commonToolbarSubtitle.setTextColor(
            appearance.subtitleColor?.let(ColorStateList::valueOf) ?: defaultSubtitleTextColors,
        )
        applyStatusBarBackground()

        if (actionColorsChanged) {
            currentModel.navigation
                ?.takeIf { it.visible }
                ?.let { binding.commonToolbarNavigation.updateColors(it, style, appearanceIconTint, appearance.actionTextColor) }
            currentModel.actions
                .asSequence()
                .filter { it.visible }
                .forEach { action ->
                    actionViews[action.id]?.updateColors(
                        action,
                        style,
                        appearanceIconTint,
                        appearance.actionTextColor,
                    )
                }
        }
    }

    private fun applyStatusBarBackground() {
        val color = currentAppearance.statusBarBackgroundColor
        if (color == null) {
            binding.commonToolbarStatusBarSpacer.background = statusBarBackgroundDrawable
        } else {
            binding.commonToolbarStatusBarSpacer.setBackgroundColor(color)
        }
    }

    /** 应用 ToolbarStyle 定义的底层样式。 */
    private fun applyStyle() {
        binding.commonToolbarContent.apply {
            minimumHeight = style.contentMinHeightPx
            setPaddingRelative(style.contentPaddingStartPx, 0, style.contentPaddingEndPx, 0)
        }
        binding.commonToolbarDivider.apply {
            setBackgroundColor(style.dividerColor)
            layoutParams.height = style.dividerHeightPx
        }
        binding.commonToolbarActions.setActionSpacing(style.actionSpacingPx)

        binding.commonToolbarTitle.setTextAppearance(style.titleTextAppearance)
        binding.commonToolbarSubtitle.setTextAppearance(style.subtitleTextAppearance)
    }

    /**
     * 核心渲染入口：将快照模型映射到 UI。
     *
     * @param model 不可变的渲染快照，包含本帧所有 UI 属性。
     * @param inheritXmlDefaults 是否继承 XML 中声明的标题、导航等默认值。
     * 默认为 true。如果为 false，则执行完整替换，允许通过模型显式清空 XML 默认值。
     * @param onActionClick 本次模型使用的动作点击回调。
     * @throws IllegalArgumentException 如果模型未通过完整校验。
     * @throws IllegalStateException 如果从非主线程调用。
     */
    fun render(
        model: CommonToolbarModel,
        inheritXmlDefaults: Boolean = true,
        onActionClick: (actionId: String) -> Unit
    ) {
        checkMainThread()
        val finalModel = if (inheritXmlDefaults) {
            mergeWithXmlDefaults(model)
        } else {
            model
        }
        // 必须先完成校验，避免失败提交留下“旧 UI + 新 callback”的撕裂状态。
        val validatedModel = ToolbarModelValidator.validate(finalModel)
        // 渲染新模型前，确保返回标准标题中心模式（如果之前设置过 Custom View）
        showStandardCenterInternal()
        this.currentModel = validatedModel
        this.actionClick = onActionClick
        renderInternal()
    }

    private fun mergeWithXmlDefaults(incoming: CommonToolbarModel): CommonToolbarModel {
        return incoming.copy(
            title = incoming.title ?: xmlDefaultModel.title,
            subtitle = incoming.subtitle ?: xmlDefaultModel.subtitle,
            navigation = incoming.navigation ?: xmlDefaultModel.navigation,
            // 基础属性通常不继承可空性，或者在 XML 中总有默认值，按传入为准
        )
    }

    /** 执行具体的视图更新逻辑。 */
    private fun renderInternal() {
        val model = currentModel
        // 同步布局方向给自定义排版布局
        binding.commonToolbarContent.layoutDirection = layoutDirection
        binding.commonToolbarActions.layoutDirection = layoutDirection

        // 设置标题对齐策略（决定了中间内容区是强制居中还是自然靠左/右）
        binding.commonToolbarContent.titleAlignment = model.titleAlignment
        binding.commonToolbarTitle.apply {
            text = model.title
            visibility = if (model.title.isNullOrEmpty()) GONE else VISIBLE
            gravity = titleGravity(model.titleAlignment)
        }
        binding.commonToolbarSubtitle.apply {
            text = model.subtitle
            visibility = if (model.subtitle.isNullOrEmpty()) GONE else VISIBLE
            gravity = titleGravity(model.titleAlignment)
        }

        // 配置标准标题容器的重力方向
        binding.commonToolbarStandardCenter.gravity = when (model.titleAlignment) {
            ToolbarTitleAlignment.CENTER -> Gravity.CENTER
            ToolbarTitleAlignment.START -> Gravity.CENTER_VERTICAL or Gravity.START
        }

        // 分别绑定标题动作、导航按钮和动作列表
        bindTitleAction(model.titleActionId)
        bindNavigation(model.navigation?.takeIf { it.visible })
        bindActions(model.actions.filter { it.visible })

        // 分割线可见性
        binding.commonToolbarDivider.visibility = if (model.dividerVisible) VISIBLE else GONE
    }

    private fun titleGravity(alignment: ToolbarTitleAlignment): Int {
        return when (alignment) {
            ToolbarTitleAlignment.CENTER -> Gravity.CENTER
            ToolbarTitleAlignment.START -> Gravity.START
        }
    }

    /** 绑定导航按钮（左侧图标）。 */
    private fun bindNavigation(action: ToolbarAction?) {
        binding.commonToolbarNavigation.layoutDirection = layoutDirection
        binding.commonToolbarNavigation.visibility = if (action == null) GONE else VISIBLE
        action?.let {
            binding.commonToolbarNavigation.bind(
                action = it,
                style = style,
                onClick = { id -> actionClick(id) },
                iconTintOverride = appearanceIconTint,
                actionTextColorOverride = currentAppearance.actionTextColor,
            )
        }
    }

    /** 绑定标题点击动作。通常用于点击标题弹出菜单或回到顶部。 */
    private fun bindTitleAction(actionId: String?) {
        val isActionable = actionId != null
        binding.commonToolbarStandardCenter.apply {
            setOnClickListener(
                actionId?.let { stableId ->
                    OnClickListener { actionClick(stableId) }
                },
            )
            // setOnClickListener(null) 在部分平台仍可能保留 clickable flag，因此最后显式收敛状态。
            isClickable = isActionable
            isFocusable = isActionable
            importantForAccessibility = if (isActionable) {
                IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                IMPORTANT_FOR_ACCESSIBILITY_AUTO
            }
            // 只有可点击时才合并为一个按钮节点；普通标题由 TextView 自身朗读，避免重复播报。
            contentDescription = if (isActionable) titleAccessibilityDescription() else null
        }
        val descendantImportance = if (isActionable) {
            IMPORTANT_FOR_ACCESSIBILITY_NO
        } else {
            IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
        binding.commonToolbarTitle.importantForAccessibility = descendantImportance
        binding.commonToolbarSubtitle.importantForAccessibility = descendantImportance
    }

    private fun titleAccessibilityDescription(): String? {
        return listOfNotNull(currentModel.title, currentModel.subtitle)
            .filter { it.isNotBlank() }
            .joinToString(separator = "，")
            .ifBlank { null }
    }

    /**
     * 绑定右侧动作列表。
     * 实现规则：对比当前已展示 ID 与目标 ID 列表，按需移除失效项、复用存在项或添加新项。
     */
    private fun bindActions(actions: List<ToolbarAction>) {
        val desiredIds = actions.map { it.id }.toSet()
        // 1. 移除不再需要的旧 View
        actionViews.keys.filterNot(desiredIds::contains).toList().forEach { staleId ->
            val stale = actionViews.remove(staleId)
            binding.commonToolbarActions.removeView(stale)
        }

        // 2. 更新或添加新动作 View，保持与模型列表顺序一致
        actions.forEachIndexed { index, action ->
            // 复用实例：相同 ID 对应的 View 在内存中保持不变，避免重绘闪烁
            val view = actionViews.getOrPut(action.id) { ToolbarActionView(context) }
            // ActionView 可能来自上一个布局方向；显式同步，避免 Badge 锚点沿用旧 RTL 状态。
            view.layoutDirection = layoutDirection
            view.bind(
                action = action,
                style = style,
                onClick = { id -> actionClick(id) },
                iconTintOverride = appearanceIconTint,
                actionTextColorOverride = currentAppearance.actionTextColor,
            )

            val currentIndex = binding.commonToolbarActions.indexOfChild(view)
            if (currentIndex < 0) {
                // 新加入
                binding.commonToolbarActions.addView(view, index)
            } else if (currentIndex != index) {
                // 顺序变化：移动位置
                binding.commonToolbarActions.removeViewAt(currentIndex)
                binding.commonToolbarActions.addView(view, index)
            }
        }
    }

    /**
     * 设置主标题并重新渲染当前快照。
     *
     * 此 setter 始终执行覆盖路径（inheritXmlDefaults = false），即允许显式清空标题。
     *
     * @param title 新主标题；`null` 或空文本会隐藏主标题。
     */
    fun setTitle(title: CharSequence?) {
        render(currentModel.copy(title = title), inheritXmlDefaults = false, onActionClick = actionClick)
    }

    /**
     * 设置副标题并重新渲染当前快照。
     *
     * 此 setter 始终执行覆盖路径（inheritXmlDefaults = false），即允许显式清空。
     *
     * @param subtitle 新副标题；`null` 或空文本会隐藏副标题。
     */
    fun setSubtitle(subtitle: CharSequence?) {
        render(currentModel.copy(subtitle = subtitle), inheritXmlDefaults = false, onActionClick = actionClick)
    }

    /**
     * 设置导航动作并重新渲染当前快照。
     *
     * 此 setter 始终执行覆盖路径（inheritXmlDefaults = false），即允许显式清空。
     *
     * @param action 新导航动作；传 `null` 隐藏导航槽。
     */
    fun setNavigationAction(action: ToolbarAction?) {
        render(currentModel.copy(navigation = action), inheritXmlDefaults = false, onActionClick = actionClick)
    }

    /**
     * 设置尾部动作列表并重新渲染当前快照。
     *
     * @param actions 新动作列表；最多允许三个可见动作。
     * @throws IllegalArgumentException 动作模型不满足 [CommonToolbarModel] 约束时抛出。
     */
    fun setActions(actions: List<ToolbarAction>) {
        render(currentModel.copy(actions = actions), inheritXmlDefaults = false, onActionClick = actionClick)
    }

    /**
     * 局部更新指定动作的徽标 (Badge)。
     *
     * 用于点赞、消息数量等动态变化场景。
     *
     * @param actionId 目标导航或尾部动作的稳定 ID。
     * @param badge 新徽标；`Count(0)` 会规范化为 [com.example.ui.toolbar.ToolbarBadge.None]。
     * @return 若动作存在并更新成功返回 true，否则返回 false。
     * @throws IllegalArgumentException 徽标或更新后的完整模型不合法时抛出，原状态保持不变。
     */
    fun updateBadge(actionId: String, badge: ToolbarBadge): Boolean {
        checkMainThread()
        // 检查是否是导航按钮
        val nav = currentModel.navigation
        if (nav?.id == actionId) {
            val validatedModel = ToolbarModelValidator.validate(
                currentModel.copy(navigation = nav.copy(badge = badge)),
            )
            currentModel = validatedModel
            bindNavigation(validatedModel.navigation?.takeIf { it.visible })
            return true
        }
        // 检查右侧动作列表
        val actions = currentModel.actions.toMutableList()
        val index = actions.indexOfFirst { it.id == actionId }
        if (index >= 0) {
            actions[index] = actions[index].copy(badge = badge)
            val validatedModel = ToolbarModelValidator.validate(
                currentModel.copy(actions = actions),
            )
            currentModel = validatedModel
            bindActions(validatedModel.actions.filter { it.visible })
            return true
        }
        return false
    }

    /**
     * 设置自定义中间内容 View。
     *
     * 设置后会隐藏标准标题。常用于搜索框、分段选择器或复杂标题布局。
     * @param view 自定义 View；传 null 清空。
     * @throws IllegalArgumentException 若 View 已经属于其他父容器。
     */
    fun setCenterCustomView(view: View?) {
        checkMainThread()
        require(view?.parent == null || view.parent === binding.commonToolbarCustomCenter) {
            "CommonToolbar 自定义中间 View 已经属于其他父容器"
        }
        binding.commonToolbarCustomCenter.removeAllViews()
        view?.let { customView ->
            val params = (customView.layoutParams as? LayoutParams)
                ?: LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                )
            // 自定义内容和标准标题一样遵循中心槽安全区；调用方仍可通过宽高参数
            // 控制搜索框等需要占满安全区的场景。
            params.gravity = Gravity.CENTER
            binding.commonToolbarCustomCenter.addView(customView, params)
        }
        binding.commonToolbarStandardCenter.visibility = GONE
        binding.commonToolbarCustomCenter.visibility = VISIBLE
    }

    /** 显示标准标题模式并移除自定义 View 实例。 */
    fun showStandardCenter() {
        checkMainThread()
        showStandardCenterInternal()
    }

    private fun showStandardCenterInternal() {
        binding.commonToolbarCustomCenter.removeAllViews()
        binding.commonToolbarCustomCenter.visibility = GONE
        binding.commonToolbarStandardCenter.visibility = VISIBLE
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "CommonToolbar 只能在主线程调用"
        }
    }

    companion object {
        // XML 中通过属性定义的导航按钮使用固定 ID
        private const val XML_NAVIGATION_ID = "common_toolbar_xml_navigation"
        /** 约定的返回导航动作 ID，供页面统一分发返回行为。 */
        const val ACTION_BACK = "action_back"
        /** 约定的第一个通用尾部动作 ID。 */
        const val ACTION_GENERAL1 = "action_general_1"
        /** 约定的第二个通用尾部动作 ID。 */
        const val ACTION_GENERAL2 = "action_general_2"
        /** 约定的第三个通用尾部动作 ID。 */
        const val ACTION_GENERAL3 = "action_general_3"
    }
}
