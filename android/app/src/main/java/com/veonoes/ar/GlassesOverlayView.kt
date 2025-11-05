package com.veonoes.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GlassesOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var glasses: Bitmap? = null
    private var targetCx = 0f
    private var targetCy = 0f
    private var targetW = 0f
    private var smCx = 0f
    private var smCy = 0f
    private var smW = 0f
    private var smoothAlpha = 0.12f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setGlassesBitmap(bmp: Bitmap) {
        glasses = bmp
        invalidate()
    }

    fun updatePose(cx: Float, cy: Float, widthPx: Float, smoothFactor: Float = 0.12f) {
        targetCx = cx
        targetCy = cy
        targetW = widthPx
        smoothAlpha = smoothFactor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = glasses ?: return
        if (targetW <= 0f) return

        smCx += (targetCx - smCx) * smoothAlpha
        smCy += (targetCy - smCy) * smoothAlpha
        smW += (targetW - smW) * smoothAlpha

        val aspect = bmp.height.toFloat() / bmp.width
        val drawW = smW
        val drawH = drawW * aspect
        val left = smCx - drawW / 2
        val top = smCy - drawH / 2
        val dst = RectF(left, top, left + drawW, top + drawH)

        canvas.drawBitmap(bmp, null, dst, paint)
        postInvalidateOnAnimation()
    }
}
