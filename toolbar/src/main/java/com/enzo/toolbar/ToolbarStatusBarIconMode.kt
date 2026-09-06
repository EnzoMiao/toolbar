package com.enzo.toolbar

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 状态栏图标颜色模式（亮色/暗色）
 * 用于控制状态栏上的图标（如电量、时间、通知图标）是深色还是浅色。
 * 建议与 Toolbar 的背景对比度保持一致。
 */
enum class ToolbarStatusBarIconMode {
    /**
     * 继承模式。
     * 表示 Toolbar 不主动控制状态栏图标明暗，或者尝试恢复到接管前的原始状态。
     */
    INHERIT,

    /**
     * 深色图标模式。
     * 状态栏图标呈现为深色，适用于浅色背景。
     * 对应 Android 平台的 `isAppearanceLightStatusBars = true`。
     */
    DARK_ICONS,

    /**
     * 浅色图标模式。
     * 状态栏图标呈现为浅色，适用于深色背景。
     * 对应 Android 平台的 `isAppearanceLightStatusBars = false`。
     */
    LIGHT_ICONS,
}
