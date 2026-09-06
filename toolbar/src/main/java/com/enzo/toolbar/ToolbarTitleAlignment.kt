package com.enzo.toolbar

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : Toolbar 标题对齐方式枚举
 **/
enum class ToolbarTitleAlignment {
    /** 使用左右最大宽度建立对称安全区，使标题中心等于 Toolbar 几何中心。 */
    CENTER,

    /** 使用导航与动作之间的剩余区间，并从逻辑 start 对齐。 */
    START,
}
