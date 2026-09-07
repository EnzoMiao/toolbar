# CommonToolbar

一个面向 Android View/XML 的通用 Toolbar 组件，
支持 Action、Badge、Edge-to-Edge、RTL、无障碍、
动态主题和自定义中心区域。核心组件位于 `:toolbar` 模块，`:app` 为示例应用。示例均基于现有 API；没有在源码中实现的能力不会作为可用功能描述。

## Installation

### 1. Add JitPack

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://jitpack.io")
        }
    }
}
```

### 2. Add dependency
```kotlin
dependencies {
  implementation(
    "com.github.EnzoMiao:toolbar:v1.0.0"
  )
}
```
## 1. 组件职责

`CommonToolbar` 是一个 View / XML 标题栏 ， 负责 ：

-左侧（逻辑 `start`）导航动作；
-中间标题、副标题或自定义中心 View；
-右侧（逻辑 `end`）最多三个可见动作；
-图标、文字、图标+文字三种动作展示；
-Dot / Count 徽标 、 Tooltip 、 无障碍描述和 48 dp 最小点击热区；
-标题几何居中、RTL 镜像和状态栏 Insets；
-XML 初始配置与 Kotlin 模型渲染 ；
-运行时外观覆盖和状态栏图标明暗控制。

组件只负责展示和派发点击事件，不负责保存、发布、删除、导航判断等业务逻辑。业务页面应通过 ViewModel/UiState 生成 `CommonToolbarModel`，点击后再把事件交给业务层。

## 2. 最小接入方式

### 2.1 XML 放置

新页面优先用 `ConstraintLayout` 组织 Toolbar 和内容区域：

```xml
<?xml version = "1.0" encoding = "utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
xmlns:android = "http://schemas.android.com/apk/res/android"
xmlns:app = "http://schemas.android.com/apk/res-auto"
android:layout_width = "match_parent"
android:layout_height = "match_parent">

<com.enzo.toolbar.CommonToolbar
android:id = "@+id/toolbar"
android:layout_width = "0dp"
android:layout_height = "wrap_content"
app:ct_insetMode = "none"
app:ct_titleAlignment = "center"
app:layout_constraintTop_toTopOf = "parent"
app:layout_constraintStart_toStartOf = "parent"
app:layout_constraintEnd_toEndOf = "parent" />

<androidx.recyclerview.widget.RecyclerView
android:id = "@+id/list"
android:layout_width = "0dp"
android:layout_height = "0dp"
app:layout_constraintTop_toBottomOf = "@id/toolbar"
app:layout_constraintStart_toStartOf = "parent"
app:layout_constraintEnd_toEndOf = "parent"
app:layout_constraintBottom_toBottomOf = "parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

`layout_width` 也可以使用 `match_parent`，但在约束布局中推荐使用 `0dp` 并同时约束 start/end。

### 2.2 Kotlin 初始化

```kotlin
private fun setupToolbar() {
    binding.toolbar.render(
        model = CommonToolbarModel(
            navigation = ToolbarAction.icon(
                id = CommonToolbar.ACTION_BACK,
                iconRes = R.drawable.ic_common_toolbar_back,
                contentDescription = "返回",
            ),
            title = getString(R.string.page_title),
            actions = listOf(
                ToolbarAction.text(
                    id = CommonToolbar.ACTION_GENERAL1,
                    text = getString(R.string.publish),
                ),
            ),
        ),
    ) { actionId ->
        when (actionId) {
            CommonToolbar.ACTION_BACK -> onBackPressedDispatcher.onBackPressed()
            CommonToolbar.ACTION_GENERAL1 -> viewModel.onPublishClick()
        }
    }
}
```

`render()` 必须在主线程调用。模型校验失败会抛出 `IllegalArgumentException`，且旧 UI 和旧回调保持不变。

### 2.3 推荐的通用返回扩展函数

当大多数页面都使用同一个返回图标和系统返回分发器时，可以使用 `CommonToolbarExtensions.kt` 提供的扩展函数，
不再重复写导航 Action 和 `ACTION_BACK` 分支：

```kotlin
package com.enzo.toolbar

private fun setupToolbar() {
    setupCommonToolbar(
        toolbar = binding.toolbar,
        title = getString(R.string.page_title),
        actions = listOf(
            ToolbarAction.text(
                id = CommonToolbar.ACTION_GENERAL1,
                text = getString(R.string.publish),
            ),
        ),
    ) { actionId ->
        when (actionId) {
            CommonToolbar.ACTION_GENERAL1 -> viewModel.onPublishClick()
        }
    }
}
```

该扩展函数的默认行为是：

- 自动创建 `CommonToolbar.ACTION_BACK` 返回动作；
- 使用 `R.drawable.ic_common_toolbar_back` 和“返回”无障碍描述；
- 点击导航动作时调用当前 Activity 的 `onBackPressedDispatcher`；
- 右侧动作和标题动作仍通过传入的 `onActionClick` 回调派发；
- 不改变现有 `CommonToolbar.render()`、模型或样式 API。

特殊页面不需要返回按钮时传 `navigation = null`：

```kotlin
setupCommonToolbar(
    toolbar = binding.toolbar,
    title = getString(R.string.home_title),
    navigation = null,
) { actionId ->
    when (actionId) {
        "settings" -> viewModel.openSettings()
    }
}
```

导航不是“返回”而是关闭弹层、打开抽屉等行为时，传自定义导航 Action 和
`onNavigationClick`；该回调会优先于系统返回分发器：

```kotlin
setupCommonToolbar(
    toolbar = binding.toolbar,
    title = "筛选",
    navigation = ToolbarAction.text("close", "关闭"),
    onNavigationClick = { viewModel.closeFilterPanel() },
)
```

`navigation`、`actions` 和 `titleActionId` 仍共享同一个稳定 ID 命名空间，不能重复。需要完整清除
XML 默认标题或导航时，继续传 `inheritXmlDefaults = false`；扩展函数不会改变这一既有规则。

### 2.4 Fragment 中使用

Fragment 使用同名扩展，调用方式与 Activity 完全一致。通常在 `onViewCreated()` 中调用，Toolbar
必须来自当前 Fragment 的 ViewBinding：

```kotlin
package com.enzo.toolbar

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupCommonToolbar(
        toolbar = binding.toolbar,
        title = getString(R.string.page_title),
        actions = listOf(
            ToolbarAction.icon(
                id = "search",
                iconRes = R.drawable.ic_search,
                contentDescription = "搜索",
            ),
        ),
    ) { actionId ->
        when (actionId) {
            "search" -> viewModel.onSearchClick()
        }
    }
}
```

Fragment 版本通过 `requireActivity()` 获取宿主的返回分发器。因此调用时 Fragment 必须已经
附加到 Activity；在 `onDestroyView()` 后不要继续使用已销毁的 binding 或 Toolbar。若某个 Fragment
不需要返回按钮，仍然传 `navigation = null`；若需要自定义关闭行为，仍然使用
`onNavigationClick`。

## 3. XML 默认值与 Kotlin 模型

### 3.1 直接在 XML 配置

```xml

<com.enzo.toolbar.CommonToolbar android:id="@+id/toolbar" android:layout_width="match_parent"
    android:layout_height="wrap_content" app:ct_title="详情" app:ct_subtitle="副标题"
    app:ct_titleAlignment="center" app:ct_navigationIcon="@drawable/ic_common_toolbar_back"
    app:ct_navigationContentDescription="返回" app:ct_insetMode="none" />
```

XML 导航动作使用内部保留 ID。业务代码通常应使用 Kotlin `ToolbarAction`，这样可以使用稳定、可读的业务
ID。

### 3.2 `render()` 默认会继承 XML

`render(model)` 的 `inheritXmlDefaults` 默认值是 `true`。当 Kotlin 模型中的 `title`、`subtitle` 或
`navigation` 为 `null` 时，会回退到 XML 中配置的对应默认值。

因此，下面的代码不会清空 XML 标题；如果 XML 有 `ct_title="详情"`，最终仍会显示“详情”：

```kotlin
binding.toolbar.render(
    CommonToolbarModel(
        navigation = ToolbarAction.icon(
            id = CommonToolbar.ACTION_BACK,
            iconRes = R.drawable.ic_common_toolbar_back,
            contentDescription = "返回",
        ),
        titleAlignment = ToolbarTitleAlignment.CENTER,
    ),
) { actionId ->
    if (actionId == CommonToolbar.ACTION_BACK) {
        onBackPressedDispatcher.onBackPressed()
    }
}
```

需要让 Kotlin 模型完整替换 XML 默认值时传 `inheritXmlDefaults = false`：

```kotlin
binding.toolbar.render(
    model = CommonToolbarModel(
        navigation = ToolbarAction.icon(
            id = CommonToolbar.ACTION_BACK,
            iconRes = R.drawable.ic_common_toolbar_back,
            contentDescription = "返回",
        ),
        title = null,
        subtitle = null,
        titleAlignment = ToolbarTitleAlignment.CENTER,
    ),
    inheritXmlDefaults = false,
) { actionId ->
    if (actionId == CommonToolbar.ACTION_BACK) {
        onBackPressedDispatcher.onBackPressed()
    }
}
```

`setTitle(null)`、`setSubtitle(null)`、`setNavigationAction(null)` 始终使用覆盖路径，也就是可以明确清空
XML 默认值。

## 4. 标题与副标题

### 4.1 标题对齐

```kotlin
CommonToolbarModel(
    title = "几何居中标题",
    titleAlignment = ToolbarTitleAlignment.CENTER,
)
```

- `CENTER`：在左右侧槽位的最大宽度形成对称安全区，标题中心尽量保持在 Toolbar 几何中心；自定义中心 View
  也使用同一中心安全区。
- `START`：标题从逻辑 start 侧对齐，适合标题较长或需要靠近返回按钮的页面。

当左右侧内容过宽、中心安全区不足时，内部布局策略会降级为 `START`，避免标题与动作发生不可控重叠。

### 4.2 动态修改标题

```kotlin
binding.toolbar.setTitle("已选择 3 项")
binding.toolbar.setSubtitle("同步中")
```

空文本或 `null` 会隐藏对应 TextView。页面重建后应重新从 UiState 调用 `render()`，不要把 Toolbar
内部快照当成业务状态源。

### 4.3 标题可点击

标题默认不可点击。需要标题点击时设置稳定的 `titleActionId`：

```kotlin
CommonToolbarModel(
    title = "朋友圈",
    titleActionId = "title_switch_category",
)
```

点击后仍通过统一的 `onActionClick` 回调派发：

```kotlin
binding.toolbar.render(model) { actionId ->
    when (actionId) {
        "title_switch_category" -> viewModel.openCategorySelector()
    }
}
```

提供 `titleActionId` 后，标题和副标题会合并成一个可点击无障碍节点；不提供时，标题由普通 TextView
自行朗读，避免重复播报。

## 5. Action 的所有写法

### 5.1 纯图标 Action

```kotlin
ToolbarAction.icon(
    id = "search",
    iconRes = R.drawable.ic_search,
    contentDescription = "搜索",
)
```

纯图标 Action 必须提供 `contentDescription`。它会自动获得 Tooltip，长按时显示该描述。

### 5.2 纯文字 Action

```kotlin
ToolbarAction.text(
    id = "publish",
    text = "发布",
)
```

也可以使用字符串资源：

```kotlin
ToolbarAction(
    id = "publish",
    textRes = R.string.publish,
)
```

`textRes` 优先于 `text`，并在 View 层使用当前 Context 解析。`textRes` 必须是有效的正数资源 ID。

### 5.3 图标 + 文字 Action

```kotlin
ToolbarAction.iconWithText(
    id = "publish",
    iconRes = R.drawable.ic_publish,
    text = "发布",
)
```

也可以手动构造：

```kotlin
ToolbarAction(
    id = "publish",
    iconRes = R.drawable.ic_publish,
    text = "发布",
    contentDescription = "发布",
)
```

图标与文字之间的间距由 `ct_actionContentSpacing` 控制，不需要给 ActionView 手动加 margin。

### 5.4 单项颜色

```kotlin
ToolbarAction.icon(
    id = "delete",
    iconRes = R.drawable.ic_delete,
    contentDescription = "删除",
    tintColor = getColor(R.color.delete_red),
)
```

`tintColor` 同时用于该 Action 的图标和文字，并优先于
`CommonToolbarAppearance.iconTint/actionTextColor` 以及 XML 全局 `ct_iconTint`。

### 5.5 启用与隐藏

```kotlin
ToolbarAction.text("save", "保存").enabled(false)
ToolbarAction.icon("share", R.drawable.ic_share, "分享").visible(false)
```

`enabled = false` 会保留布局位置但不可点击，并使用禁用透明度；`visible = false` 不渲染、不占据布局空间，也不计入右侧三个可见
Action 的限制。

即使 Action 隐藏，`id` 仍必须非空，并且必须在整个模型中唯一。

### 5.6 导航 Action

导航位独立于右侧动作数量限制：

```kotlin
CommonToolbarModel(
    navigation = ToolbarAction.text(
        id = "close",
        text = "关闭",
    ),
    title = "编辑",
)
```

右侧最多三个可见 Action，导航不计入这三个名额。返回行为由宿主决定，可以调用 `finish()`、
`onBackPressedDispatcher` 或 ViewModel Effect。

### 5.7 组合完整模型

```kotlin
CommonToolbarModel(
    navigation = ToolbarAction.icon(
        id = "back",
        iconRes = R.drawable.ic_back,
        contentDescription = "返回",
    ),
    title = "消息",
    subtitle = "未读消息",
    actions = listOf(
        ToolbarAction.icon(
            id = "search",
            iconRes = R.drawable.ic_search,
            contentDescription = "搜索",
        ),
        ToolbarAction.text("mark_all", "全部已读"),
        ToolbarAction.iconWithText(
            id = "more",
            iconRes = R.drawable.ic_more,
            text = "更多",
        ),
    ),
    dividerVisible = true,
)
```

## 6. Badge 徽标

`ToolbarAction` 只有一个 `badge` 属性，类型为 `ToolbarBadge`。Dot 和 Count 共用同一个 Badge View
和同一套右上角锚点算法，区别只在徽标内容和尺寸。

### 6.1 红点 Dot

```kotlin
ToolbarAction.icon(
    id = "discover",
    iconRes = R.drawable.ic_discover,
    contentDescription = "发现",
).badge(
    ToolbarBadge.Dot(
        contentDescription = "有新内容",
        backgroundColor = getColor(R.color.badge_blue),
    ),
)
```

`backgroundColor` 为空时回退到样式属性 `ct_badgeBackgroundColor`。红点锚定在图标（纯图标
Action）或文字（文字/图标+文字 Action）的逻辑右上角；RTL 下横向自动镜像。

### 6.2 数字 Count

```kotlin
ToolbarAction.icon(
    id = "message",
    iconRes = R.drawable.ic_message,
    contentDescription = "消息",
).badge(
    ToolbarBadge.Count(
        value = 5,
        maxDisplayValue = 99,
        contentDescription = "5 条未读",
    ),
)
```

超过 `maxDisplayValue` 时显示 `99+` 形式；`Count(0)` 会规范化为 `None`，不显示徽标。负数或小于 1 的
`maxDisplayValue` 会校验失败。

### 6.3 运行时只更新 Badge

```kotlin
binding.toolbar.updateBadge(
    actionId = "message",
    badge = ToolbarBadge.Count(8, contentDescription = "8 条未读"),
)

binding.toolbar.updateBadge(
    actionId = "discover",
    badge = ToolbarBadge.Dot(backgroundColor = Color.RED),
)

binding.toolbar.updateBadge("message", ToolbarBadge.None)
```

返回值为 `true` 表示找到了指定的导航或尾部 Action 并完成更新；找不到时返回 `false`
。它只能更新已经存在于当前模型中的动作，不能新增动作。

### 6.4 Badge 位置微调

```xml

<com.enzo.toolbar.CommonToolbar...app:ct_badgeHorizontalOffset="0dp"app:ct_badgeVerticalOffset="2dp" />
```

- `ct_badgeHorizontalOffset` 正值沿逻辑 end 方向移动；
- `ct_badgeVerticalOffset` 正值向下移动；
- RTL 会自动镜像横向偏移；
- 偏移只修正视觉位置，不会改变 48dp 点击热区。

纯图标的宽 Count 会在原有 48dp 槽内向内展开，不额外把 ActionView 撑成 64dp。文字或图标+文字的 Count
会把覆盖层需要的宽度纳入动作测量，避免数字覆盖文字。

## 7. 尺寸、边距和点击热区

### 7.1 默认关系

ActionView 默认没有额外外层 margin。默认图标尺寸为 24dp，最小点击热区为 48dp，因此图标视觉上通常会在两侧各留下约
12dp 空白；Toolbar 内容区默认 start/end 还各有 4dp 内边距。

48dp 是点击边界，不是图标或文字必须占满的视觉尺寸。不要为了让图标看起来靠边而把
`ct_actionMinTouchSize` 降到小于 48dp；优先调整内容内边距或图标大小。

### 7.2 通过 XML 样式配置

```xml

<style name="Widget.App.CommonToolbar.Compact" parent="Widget.CommonToolbar">
    <!-- 保留无障碍点击热区，仅调整视觉内容。 -->
    <item name="ct_actionMinTouchSize">48dp</item>
    <item name="ct_actionIconSize">20dp</item>
    <item name="ct_actionTextSize">13sp</item>
    <item name="ct_actionSpacing">8dp</item>
    <item name="ct_actionContentSpacing">4dp</item>
    <item name="ct_contentPaddingStart">0dp</item>
    <item name="ct_contentPaddingEnd">0dp</item>
    <item name="ct_badgeHorizontalOffset">0dp</item>
    <item name="ct_badgeVerticalOffset">1dp</item>
</style>
```

```xml

<com.enzo.toolbar.CommonToolbar android:id="@+id/toolbar"
    style="@style/Widget.App.CommonToolbar.Compact" android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### 7.3 直接在 View 上覆盖

```xml

<com.enzo.toolbar.CommonToolbar...app:ct_actionIconSize="20dp"app:ct_actionTextSize="13sp"app:ct_actionMinTouchSize="48dp"app:ct_contentPaddingStart="0dp"app:ct_contentPaddingEnd="0dp"app:ct_actionSpacing="8dp"app:ct_actionContentSpacing="4dp" />
```

`ct_actionTextSize` 只覆盖字号；字体、字重和颜色仍由 `ct_actionTextAppearance` 管理。需要完整改变字体样式时，应定义新的
TextAppearance：

```xml

<style name="TextAppearance.App.Toolbar.Action" parent="TextAppearance.CommonToolbar.Action">
    <item name="android:textSize">15sp</item>
    <item name="android:textStyle">bold</item>
    <item name="android:textColor">@color/toolbar_action</item>
</style>

<com.enzo.toolbar.CommonToolbar...app:ct_actionTextAppearance="@style/TextAppearance.App.Toolbar.Action" />
```

`ct_actionSpacing` 是相邻 ActionView 之间的间距；`ct_actionContentSpacing` 是同一个 Action
内图标和文字之间的间距。两者都不会改变最小点击热区的规则。

### 7.4 XML 属性完整参考

以下属性可以直接写在 `CommonToolbar` 上，也可以放进继承自 `Widget.CommonToolbar` 的样式中：

| 属性                                | 作用                       | 典型值/说明                |
|-----------------------------------|--------------------------|-----------------------|
| `commonToolbarStyle`              | 主题级默认 Toolbar 样式入口       | style 引用              |
| `ct_title`                        | XML 初始主标题                | 字符串                   |
| `ct_subtitle`                     | XML 初始副标题                | 字符串                   |
| `ct_titleAlignment`               | 标题对齐                     | `center` / `start`    |
| `ct_navigationIcon`               | XML 导航图标                 | Drawable 引用           |
| `ct_navigationText`               | XML 导航文字                 | 字符串                   |
| `ct_navigationContentDescription` | XML 导航无障碍描述              | 字符串                   |
| `ct_insetMode`                    | 顶部 Insets 所有权            | `none` / `edgeToEdge` |
| `ct_dividerVisible`               | 是否显示底部分割线                | `true` / `false`      |
| `ct_statusBarBackground`          | 状态栏 spacer 背景            | 颜色或 Drawable 引用       |
| `ct_contentMinHeight`             | 内容区最小高度                  | 默认 56dp               |
| `ct_contentPaddingStart`          | 内容区逻辑 start 内边距          | 默认 4dp                |
| `ct_contentPaddingEnd`            | 内容区逻辑 end 内边距            | 默认 4dp                |
| `ct_titleTextAppearance`          | 主标题 TextAppearance       | style 引用              |
| `ct_subtitleTextAppearance`       | 副标题 TextAppearance       | style 引用              |
| `ct_actionTextAppearance`         | Action 文字 TextAppearance | style 引用              |
| `ct_badgeTextAppearance`          | Badge 文字 TextAppearance  | style 引用              |
| `ct_iconTint`                     | 全局图标颜色                   | 颜色或颜色资源               |
| `ct_rippleColor`                  | Action 点击波纹颜色            | 颜色或颜色资源               |
| `ct_dividerColor`                 | 底部分割线颜色                  | 颜色或颜色资源               |
| `ct_dividerHeight`                | 底部分割线高度                  | dimension             |
| `ct_badgeBackgroundColor`         | Badge 默认背景色              | 单个 Dot 颜色可覆盖它         |
| `ct_actionMinTouchSize`           | Action 最小点击热区            | 建议不小于 48dp            |
| `ct_actionIconSize`               | 图标视觉尺寸                   | 默认 24dp               |
| `ct_actionTextSize`               | Action 文字字号覆盖            | dimension；不改变字体和字重    |
| `ct_actionSpacing`                | 相邻 Action 之间的间距          | 默认 4dp                |
| `ct_actionContentSpacing`         | 同一 Action 内图标与文字间距       | 默认 4dp                |
| `ct_badgeHorizontalOffset`        | Badge 逻辑横向偏移             | 正值向逻辑 end 移动          |
| `ct_badgeVerticalOffset`          | Badge 垂直偏移               | 正值向下移动                |
| `ct_themeOverlay`                 | Toolbar 局部主题叠加           | style 引用              |
| `ct_insetConflictDiagnostics`     | Debug Insets 冲突诊断        | 建议仅 Debug 开启          |

`android:background` 也可以直接设置 Toolbar 背景；如果使用 `ct_themeOverlay`，Overlay 中的背景会参与解析，View
上直接设置的属性优先级更高。

## 8. 主题叠加与颜色

### 8.1 局部 Theme Overlay

适合图片预览、深色页面等需要整套视觉切换的场景：

```xml

<style name="ThemeOverlay.App.Toolbar.Dark" parent="ThemeOverlay.Material3.Dark">
    <item name="android:colorAccent">@color/white</item>
    <item name="ct_iconTint">@color/white</item>
    <item name="ct_badgeBackgroundColor">@color/error_light</item>
    <item name="ct_titleTextAppearance">@style/TextAppearance.App.Toolbar.DarkTitle</item>
</style>
```

```xml

<com.enzo.toolbar.CommonToolbar...app:ct_themeOverlay="@style/ThemeOverlay.App.Toolbar.Dark" />
```

优先级为：View 直接属性 > `ct_themeOverlay` > 宿主主题/`Widget.CommonToolbar` 默认值。Overlay
只影响视觉样式，不改变 Insets、点击回调或模型规则。

### 8.2 运行时外观

```kotlin
binding.toolbar.setAppearance(
    CommonToolbarAppearance(
        backgroundColor = Color.BLACK,
        statusBarBackgroundColor = Color.BLACK,
        titleColor = Color.WHITE,
        subtitleColor = Color.LTGRAY,
        iconTint = Color.WHITE,
        actionTextColor = Color.WHITE,
    ),
)
```

传入空快照可以恢复 XML/style 默认颜色：

```kotlin
binding.toolbar.setAppearance(CommonToolbarAppearance())
```

`ToolbarAction.tintColor` 优先级最高，不会被全局 `iconTint` 或 `actionTextColor`
覆盖。相同外观快照不会重复更新；动作颜色变化只更新颜色，不重绑点击、Badge 或 View 层级。

### 8.3 滚动时渐变示例

Toolbar 不负责判断“滚动到多少 dp”，也不内置滚动动画。页面把列表滚动值映射为外观快照即可：

```kotlin
private fun updateToolbarAppearance(scrollY: Int) {
    val progress = ((scrollY - 230f) / (250f - 230f))
        .coerceIn(0f, 1f)

    fun lerp(from: Int, to: Int): Int {
        val fromR = Color.red(from)
        val fromG = Color.green(from)
        val fromB = Color.blue(from)
        val toR = Color.red(to)
        val toG = Color.green(to)
        val toB = Color.blue(to)
        return Color.rgb(
            (fromR + (toR - fromR) * progress).roundToInt(),
            (fromG + (toG - fromG) * progress).roundToInt(),
            (fromB + (toB - fromB) * progress).roundToInt(),
        )
    }

    val background = lerp(Color.TRANSPARENT, Color.BLACK)
    val foreground = lerp(Color.BLACK, Color.WHITE)

    binding.toolbar.setAppearance(
        CommonToolbarAppearance(
            backgroundColor = background,
            statusBarBackgroundColor = background,
            titleColor = foreground,
            subtitleColor = foreground,
            iconTint = foreground,
            actionTextColor = foreground,
        ),
    )
}
```

实际项目中建议使用 `ColorUtils.blendARGB()` 或项目已有颜色插值工具，并在 `RecyclerView`/NestedScroll
回调中只提交必要的外观变化。反向滚动使用同一计算公式，颜色会自然恢复。

## 9. Insets 与沉浸式布局

### 9.1 `NONE`

```xml
app:ct_insetMode="none"
```

Toolbar 不处理状态栏顶部高度和刘海横向安全区。适用于父容器、`BaseActivity` 或其他统一 Insets owner
已经完成顶部补偿的页面。

### 9.2 `EDGE_TO_EDGE`

```xml
app:ct_insetMode="edgeToEdge"
```

Toolbar 接管：

- status bar 顶部高度；
- display cutout 的横向安全区；
- RTL 下 start/end 映射。

它仍然把原始 Insets 继续返回给后续 View，不会主动消费整个 WindowInsets。相同边不能由父容器和 Toolbar
同时补偿，否则会出现双倍顶部间距。

### 9.3 状态栏背景

XML：

```xml
app:ct_statusBarBackground="@color/toolbar_status_bar"
```

运行时：

```kotlin
binding.toolbar.setStatusBarBackground(
    AppCompatResources.getDrawable(this, R.color.toolbar_status_bar),
)
```

该背景只绘制在 Toolbar 自己的状态栏 spacer 上，不直接改变 Window 的 `statusBarColor`。

### 9.4 Insets 冲突诊断

```xml
app:ct_insetConflictDiagnostics="true"
```

仅在 Debug 构建打开。若检测到已有 `paddingTop` 又使用 `EDGE_TO_EDGE`，会输出包含 View ID 和 tag
的警告。修复时应明确保留一个 Insets owner，而不是同时保留两层补偿。

## 10. 状态栏图标深浅

状态栏图标属于 Window，不属于单个 Toolbar View。使用 `CommonToolbarWindowController`：

```kotlin
private lateinit var statusBarController: CommonToolbarWindowController

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    statusBarController = CommonToolbarWindowController(window)
}

override fun onResume() {
    super.onResume()
    statusBarController.applyStatusBarIconMode(
        ToolbarStatusBarIconMode.DARK_ICONS,
    )
}

override fun onPause() {
    statusBarController.restoreStatusBarIconMode()
    super.onPause()
}
```

模式含义：

- `DARK_ICONS`：状态栏显示深色图标，适合浅色背景；
- `LIGHT_ICONS`：状态栏显示浅色图标，适合深色背景；
- `INHERIT`：释放当前 Owner 的控制权，恢复上一个 Owner 或接管前状态。

多个 Activity/Fragment 共享同一个 Window 时，控制器使用 Owner 栈：最后申请者获胜，栈顶释放后恢复上一个
Owner；非栈顶释放不会覆盖当前页面。控制器只能在主线程调用。

## 11. 自定义中心 View

适合搜索框、分段选择器或复杂标题：

```kotlin
val searchView = SearchView(this).apply {
    layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
    )
}

binding.toolbar.setCenterCustomView(searchView)
```

自定义 View 会遵循中心安全区并水平居中。它必须尚未属于其他父容器；如果已经有 parent，会抛出
`IllegalArgumentException`。

切回标准标题：

```kotlin
binding.toolbar.showStandardCenter()
```

再次调用 `render()` 也会自动切回标准标题，并 detach 旧的自定义 View。Toolbar 不保存自定义 View
实例或内部输入状态；如果是 `EditText`/搜索框，页面应自行保存和恢复文本、焦点、选择区等状态。

## 12. Layout Editor 预览

预览属性使用 `tools` 命名空间，只影响 Layout Editor，不会进入运行时模型：

```xml

<com.enzo.toolbar.CommonToolbar
    xmlns:tools="http://schemas.android.com/tools"...tools:ct_title="预览标题"tools:ct_subtitle="预览副标题"tools:ct_action_count="2" />
```

`tools:ct_action_count` 支持 `0..3`，会生成禁用的伪图标动作；它们只用于预览，不可点击。没有任何预览内容时组件会显示
`CommonToolbar Preview` 占位标题。

## 13. 无障碍

- 纯图标 Action 必须提供 `contentDescription`；
- 纯图标 Action 和纯图标 navigation 会自动设置 Tooltip；
- Badge 的 `contentDescription` 会与动作描述合并朗读，例如“消息，5 条未读”；
- 标题区域只有在设置 `titleActionId` 后才作为一个可点击节点；
- 状态栏 spacer 和底部分割线不承载业务语义；
- `enabled = false` 的 Action 不可点击，但仍保留自己的触摸区域和无障碍状态；
- 所有 ActionView 根节点最小点击热区为 `ct_actionMinTouchSize`，默认 48dp。

## 14. RTL

布局中使用 `start/end`，不要用固定 left/right margin 表达业务位置：

```xml
android:layout_marginStart="8dp"android:paddingEnd="4dp"
```

在 RTL 下：

- navigation 位于屏幕右侧；
- actions 位于屏幕左侧；
- Badge 的逻辑 end 锚点自动镜像；
- `ct_badgeHorizontalOffset` 的正方向也随布局方向镜像；
- Action 的稳定 ID 和点击回调顺序不变。

测试 RTL 时应同时检查标题几何中心、Badge 锚点、cutout safeStart/safeEnd 和 TalkBack 顺序。

## 15. 生命周期、状态恢复与防重复点击

Toolbar 不把模型、callback、Custom Center 或外观快照写入 SavedState。配置变更或 View 重建后，应由页面在
`onCreateView`/`onViewCreated` 收集当前 UiState，再次调用 `render()`。

组件不内置固定 300ms 防抖。提交、发布、删除等操作应由 ViewModel 维护 `isSubmitting`/`enabled` 状态：

```kotlin
// ViewModel 根据提交状态生成模型。
val action = ToolbarAction.text(
    id = "submit",
    text = "提交",
).enabled(!uiState.isSubmitting)
```

页面收到点击后只发送意图，不在 Toolbar 内部执行业务判断：

```kotlin
"submit" -> viewModel.submit()
```

## 16. 常见错误

### 16.1 把 XML 标题误认为被覆盖

`render()` 默认继承 XML。要清空 XML 值，传 `inheritXmlDefaults = false` 或使用 `setTitle(null)`。

### 16.2 把 48dp 热区当成图标尺寸

`ct_actionMinTouchSize` 控制点击边界；`ct_actionIconSize` 才控制图标视觉尺寸。需要减少边缘空白时，先调整
`ct_contentPaddingStart/End` 和图标尺寸。

### 16.3 手动给 ActionView 加外层 margin

Action 间距由 `ct_actionSpacing` 管理，图标与文字间距由 `ct_actionContentSpacing` 管理。业务不应访问内部
`ToolbarActionView` 或手动调整其 child margin。

### 16.4 同时让父容器和 Toolbar 处理 Insets

选择一个 owner。父容器已经补偿顶部时使用 `NONE`；Toolbar 自己接管时使用 `EDGE_TO_EDGE`。

### 16.5 用 `setAppearance()` 改业务状态

`setAppearance()` 只修改当前 View 的颜色覆盖，不会更新模型，也不负责保存颜色状态。页面重建后必须重新提交外观。

### 16.6 右侧放置超过三个可见 Action

校验器会拒绝超过三个可见右侧动作。隐藏 Action 不计数，navigation 也不计入右侧三个名额。

## 17. API 速查

### 17.1 模型字段

| 类型/字段                               | 说明                         |
|-------------------------------------|----------------------------|
| `CommonToolbarModel.navigation`     | 逻辑 start 侧导航动作；可为空         |
| `CommonToolbarModel.title`          | 主标题；可为空                    |
| `CommonToolbarModel.subtitle`       | 副标题；可为空                    |
| `CommonToolbarModel.titleActionId`  | 标题点击 ID；为空时标题不可点击          |
| `CommonToolbarModel.titleAlignment` | `CENTER` 或 `START`         |
| `CommonToolbarModel.actions`        | 逻辑 end 侧 Action 列表；最多三个可见项 |
| `CommonToolbarModel.dividerVisible` | 是否显示底部分割线                  |
| `ToolbarAction.id`                  | 全模型唯一且非空的稳定 ID             |
| `ToolbarAction.iconRes`             | 可选图标资源                     |
| `ToolbarAction.text` / `textRes`    | 可选文字；`textRes` 优先          |
| `ToolbarAction.contentDescription`  | 无障碍描述；纯图标必填                |
| `ToolbarAction.tintColor`           | 当前 Action 的图标和文字颜色覆盖       |
| `ToolbarAction.badge`               | `None`、`Dot` 或 `Count`     |
| `ToolbarAction.enabled`             | 是否可点击                      |
| `ToolbarAction.visible`             | 是否参与渲染                     |

模型校验规则是整体验证：所有 ID（包括隐藏 Action）必须唯一；可见 Action 必须至少有图标或非空文字；纯图标可见
Action 必须有 `contentDescription`；可见右侧 Action 最多三个。

| API                                                          | 用途                                      |
|--------------------------------------------------------------|-----------------------------------------|
| `AppCompatActivity.setupCommonToolbar(toolbar, ...)`         | 默认返回按钮的兼容性扩展；传 `navigation = null` 隐藏返回 |
| `render(model, inheritXmlDefaults, onActionClick)`           | 一次性渲染完整模型和点击回调                          |
| `setTitle(title)`                                            | 覆盖并设置主标题，可传 `null` 清空                   |
| `setSubtitle(subtitle)`                                      | 覆盖并设置副标题，可传 `null` 清空                   |
| `setNavigationAction(action)`                                | 覆盖导航动作，可传 `null` 隐藏                     |
| `setActions(actions)`                                        | 覆盖右侧动作列表                                |
| `updateBadge(actionId, badge)`                               | 局部更新已有动作的徽标                             |
| `setAppearance(appearance)`                                  | 覆盖运行时背景、标题、图标和文字颜色                      |
| `setStatusBarBackground(drawable)`                           | 设置状态栏 spacer 的 Drawable 背景              |
| `setCenterCustomView(view)`                                  | 替换标准标题为自定义中心 View                       |
| `showStandardCenter()`                                       | 移除自定义中心 View，恢复标准标题                     |
| `CommonToolbarWindowController.applyStatusBarIconMode(mode)` | 设置 Window 状态栏图标明暗                       |
| `CommonToolbarWindowController.restoreStatusBarIconMode()`   | 释放当前状态栏图标 owner                         |
