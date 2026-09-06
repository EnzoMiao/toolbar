package com.enzo.toolbar.internal

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 *  Author : Enzo
 *  Date : 2026/09/07
 *  Des : 一个透明的、固定尺寸的 Drawable，用于 LinearLayout 的 divider 间距。
 */
internal class ToolbarSpacingDrawable(
    private val width: Int,
    private val height: Int = 0
) : Drawable() {

    override fun draw(canvas: Canvas) {
        // 透明，不绘制
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height
}
