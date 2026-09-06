package com.enzo.toolbar

import android.os.Looper
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import java.util.WeakHashMap

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Window 级别的 Toolbar 控制器，处理全屏/沉浸式适配
 * 负责管理 Window 级别的状态栏图标明暗模式，支持多 Owner (Activity/Fragment) 栈式交接。
 * 内部统一使用 [WindowInsetsControllerCompat] 实现兼容性。
 *
 * 规则：
 * 1. 最后申请者获胜：新 Owner 调用 [applyStatusBarIconMode] 时压栈并覆盖状态。
 * 2. 栈顶释放恢复：栈顶 Owner 调用 [restoreStatusBarIconMode] 时弹栈，恢复上一个 Owner 的模式；若栈为空，恢复原始状态。
 * 3. 非栈顶释放无副作用：非栈顶 Owner 释放只移除自己的 token，不修改 Window 状态，避免旧页面覆盖新页面。
 *
 * @param window 需要控制状态栏图标外观的宿主窗口。
 */
class CommonToolbarWindowController(private val window: Window) {

    private val ownerToken = Any()

    /**
     * 应用状态栏图标模式。
     *
     * @param mode 图标模式。如果是 [ToolbarStatusBarIconMode.INHERIT]，则视同释放控制权。
     * @throws IllegalStateException 如果从非主线程调用。
     */
    fun applyStatusBarIconMode(mode: ToolbarStatusBarIconMode) {
        checkMainThread()
        if (mode == ToolbarStatusBarIconMode.INHERIT) {
            restoreStatusBarIconMode()
            return
        }

        val state = getOrCreateState(window)
        val stack = state.tokenStack

        // 每次重新申请都代表当前 Owner 重新获得焦点，必须移动到栈顶。
        // 仅更新原位置会导致 Activity/Fragment onResume 后仍被旧的栈顶 Owner 覆盖。
        val existingIndex = stack.indexOfFirst { it.token === ownerToken }
        if (existingIndex >= 0) {
            val info = stack.removeAt(existingIndex)
            info.mode = mode
            stack.add(info)
        } else {
            stack.add(OwnerInfo(ownerToken, mode))
        }

        performApply(window, mode)
    }

    /**
     * 恢复/释放当前 Owner 的状态控制权。
     *
     * @throws IllegalStateException 如果从非主线程调用。
     */
    fun restoreStatusBarIconMode() {
        checkMainThread()
        val state = registry[window] ?: return
        val stack = state.tokenStack
        val isTop = stack.lastOrNull()?.token === ownerToken

        // 移除当前 Token
        stack.removeAll { it.token === ownerToken }

        if (isTop) {
            // 如果释放的是栈顶，需要应用新的栈顶模式或恢复原始状态
            val nextInfo = stack.lastOrNull()
            if (nextInfo != null) {
                performApply(window, nextInfo.mode)
            } else {
                performRestore(window, state.originalLightStatusBars)
                registry.remove(window)
            }
        }
    }

    private fun performApply(window: Window, mode: ToolbarStatusBarIconMode) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        when (mode) {
            ToolbarStatusBarIconMode.DARK_ICONS -> {
                controller.isAppearanceLightStatusBars = true
            }
            ToolbarStatusBarIconMode.LIGHT_ICONS -> {
                controller.isAppearanceLightStatusBars = false
            }
            else -> {} // INHERIT 已在外部逻辑处理
        }
    }

    private fun performRestore(window: Window, originalValue: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = originalValue
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "CommonToolbarWindowController 只能在主线程调用"
        }
    }

    private companion object {
        private val registry = WeakHashMap<Window, WindowState>()

        private fun getOrCreateState(window: Window): WindowState {
            return registry.getOrPut(window) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                WindowState(
                    originalLightStatusBars = controller.isAppearanceLightStatusBars,
                    tokenStack = mutableListOf()
                )
            }
        }
    }

    private class WindowState(
        val originalLightStatusBars: Boolean,
        val tokenStack: MutableList<OwnerInfo>
    )

    private class OwnerInfo(
        val token: Any,
        var mode: ToolbarStatusBarIconMode
    )
}
