package com.enzo.toolbar

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 的 Insets 适配模式枚举
 **/
enum class ToolbarInsetMode {
    /** 不修改系统栏占位，由父容器负责。 */
    NONE,

    /** Toolbar 接管状态栏顶部和 display cutout 横向安全区。 */
    EDGE_TO_EDGE,
}
