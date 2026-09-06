package com.enzo.common_toolbar

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enzo.toolbar.CommonToolbar
import com.enzo.toolbar.CommonToolbarAppearance
import com.enzo.toolbar.CommonToolbarModel
import com.enzo.toolbar.ToolbarAction
import com.enzo.toolbar.ToolbarBadge
import com.enzo.toolbar.ToolbarTitleAlignment

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 示例 Activity，展示 CommonToolbar 的各种用法
 **/
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: CommonToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.commonToolbar)

        // 初始化点击回调
        toolbar.render(CommonToolbarModel()) { actionId ->
            handleActionClick(actionId)
        }

        findViewById<Button>(R.id.btnBasic).setOnClickListener {
            showBasicUsage()
        }



        findViewById<Button>(R.id.btnActions).setOnClickListener {
            showActionsUsage()
        }

        findViewById<Button>(R.id.btnBadge).setOnClickListener {
            showBadgeUsage()
        }

        findViewById<Button>(R.id.btnAlignment).setOnClickListener {
            showAlignmentUsage()
        }

        findViewById<Button>(R.id.btnAppearance).setOnClickListener {
            showAppearanceUsage()
        }

        findViewById<Button>(R.id.btnCustomView).setOnClickListener {
            showCustomViewUsage()
        }

        findViewById<Button>(R.id.btnDivider).setOnClickListener {
            showDividerUsage()
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            resetToolbar()
        }

        // 处理其他视图的 Insets (底部导航栏等)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 顶部由 Toolbar 处理，这里只处理左右和底部
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleActionClick(actionId: String) {
        when (actionId) {
            CommonToolbar.ACTION_BACK -> {
                Toast.makeText(this, "点击了返回", Toast.LENGTH_SHORT).show()
            }
            "action_search" -> {
                Toast.makeText(this, "点击了搜索", Toast.LENGTH_SHORT).show()
            }
            "action_settings" -> {
                Toast.makeText(this, "点击了设置", Toast.LENGTH_SHORT).show()
            }
            "action_share" -> {
                Toast.makeText(this, "点击了分享", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "点击了: $actionId", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 基本用法：设置标题、副标题和导航按钮
     * 这里演示了两种方式：render(model) 或者直接使用 setter
     */
    private fun showBasicUsage() {
        // 方式 1：通过模型批量更新 (推荐，状态一致性更好)
        val model = CommonToolbarModel(
            title = "主标题",
            subtitle = "这里是副标题",
            navigation = ToolbarAction(
                id = CommonToolbar.ACTION_BACK,
                iconRes = android.R.drawable.ic_menu_revert,
                contentDescription = "返回"
            ),
            dividerVisible = true
        )
        toolbar.render(model) { handleActionClick(it) }

        /* 
        // 方式 2：使用快捷 Setter (适用于简单逻辑更新)
        toolbar.setTitle("主标题")
        toolbar.setSubtitle("副标题")
        toolbar.setNavigationAction(action)
        */
    }


    /**
     * 右侧动作按钮用法：最多支持 3 个
     */
    private fun showActionsUsage() {
        val actions = listOf(
            ToolbarAction.icon(
                id = "action_search",
                iconRes = android.R.drawable.ic_menu_search,
                contentDescription = "搜索"
            ),
            ToolbarAction.icon(
                id = "action_share",
                iconRes = android.R.drawable.ic_menu_share,
                contentDescription = "分享"
            ),
            ToolbarAction.icon(
                id = "action_settings",
                iconRes = android.R.drawable.ic_menu_preferences,
                contentDescription = "设置"
            )
        )
        // 也可以直接使用快捷方法，内部会调用 render
        toolbar.setActions(actions)
    }

    /**
     * 动态更新 Badge
     */
    private fun showBadgeUsage() {
        // 先确保有动作按钮
        showActionsUsage()

        // 更新搜索按钮的红点 - Dot 是 data class，需要实例化
        toolbar.updateBadge("action_search", ToolbarBadge.Dot())

        // 更新分享按钮的数字
        toolbar.updateBadge("action_share", ToolbarBadge.Count(99))
    }

    /**
     * 切换标题对齐方式
     */
    private var isCenter = true
    private fun showAlignmentUsage() {
        isCenter = !isCenter
        val alignment = if (isCenter) ToolbarTitleAlignment.CENTER else ToolbarTitleAlignment.START
        val currentModel = CommonToolbarModel(
            title = "标题对齐演示",
            subtitle = if (isCenter) "当前居中" else "当前靠左",
            titleAlignment = alignment,
            navigation = ToolbarAction(
                id = CommonToolbar.ACTION_BACK,
                iconRes = android.R.drawable.ic_menu_revert,
                contentDescription = "返回"
            )
        )
        toolbar.render(currentModel) { handleActionClick(it) }
    }


    /**
     * 运行时外观调整（如沉浸式页面根据滚动动态变色）
     */
    private fun showAppearanceUsage() {
        val appearance = CommonToolbarAppearance(
            backgroundColor = Color.parseColor("#FF6200EE"),
            statusBarBackgroundColor = Color.parseColor("#FF3700B3"),
            titleColor = Color.WHITE,
            subtitleColor = Color.LTGRAY,
            iconTint = Color.WHITE,
            actionTextColor = Color.YELLOW
        )
        toolbar.setAppearance(appearance)

        // 配合一些带文字的 Action 观察效果
        val actions = listOf(
            ToolbarAction.text(id = "action_save", text = "保存")
        )
        toolbar.setActions(actions)
    }

    /**
     * 自定义中间 View（如搜索框）
     */
    private fun showCustomViewUsage() {
        // 清除右侧动作按钮，确保中间自定义视图有更多空间
        toolbar.setActions(emptyList())

        val editText = EditText(this).apply {
            hint = "请输入搜索内容..."
            setBackgroundColor(Color.LTGRAY)
            setPadding(20, 10, 20, 10)
            // 设置 LayoutParams 并添加左右边距
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 100
                rightMargin = 100
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }
        
        // 设置自定义 View 后会自动隐藏标准标题
        toolbar.setCenterCustomView(editText)
    }

    /**
     * 演示下划线 (Divider) 的显示与隐藏
     */
    private var isDividerVisible = false
    private fun showDividerUsage() {
        isDividerVisible = !isDividerVisible
        // 保持当前模型其他状态，仅修改分割线可见性
        val model = CommonToolbarModel(
            title = "下划线演示",
            subtitle = if (isDividerVisible) "下划线已显示" else "下划线已隐藏",
            dividerVisible = isDividerVisible,
            navigation = ToolbarAction(
                id = CommonToolbar.ACTION_BACK,
                iconRes = android.R.drawable.ic_menu_revert,
                contentDescription = "返回"
            )
        )
        toolbar.render(model) { handleActionClick(it) }
    }

    /**
     * 重置 Toolbar 状态
     */
    private fun resetToolbar() {
        // setAppearance() 对相同快照会直接返回；先提交一个哨兵快照，才能保证即使外部通过
        // setBackgroundColor()/setBackground() 改过根 View 背景，也会重新执行默认背景的恢复路径。
        toolbar.setAppearance(CommonToolbarAppearance(backgroundColor = Color.TRANSPARENT))
        toolbar.setAppearance(CommonToolbarAppearance())

        // 先恢复运行时外观，再提交无导航/无动作模型，避免旧动作在渲染过程中短暂使用旧颜色。
        toolbar.render(
            model = CommonToolbarModel(
                title = "CommonToolbar Demo",
                titleAlignment = ToolbarTitleAlignment.CENTER,
            ),
            // null 的 navigation/subtitle 必须明确清除，不能回退到 XML 默认动作。
            inheritXmlDefaults = false,
            onActionClick = {},
        )
    }

}
