package com.veonoes.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

data class Pose(
  val cx: Float = 0f,           // merkez X (ekran koordinatı)
  val cy: Float = 0f,           // merkez Y
  val rotationDeg: Float = 0f,  // gözler arası açı
  val scale: Float = 1f         // bitmap ölçeği
)

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private var bmp: Bitmap? = null
  private var pose: Pose = Pose()

  fun setBitmap(b: Bitmap?) {
    bmp = b
    invalidate()
  }

  fun updatePose(p: Pose) {
    pose = p
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val b = bmp ?: return

    val cx = pose.cx
    val cy = pose.cy
    val scale = pose.scale
    val rot = pose.rotationDeg

    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rot)
    canvas.scale(scale, scale)

    val halfW = b.width / 2f
    val halfH = b.height / 2f
    canvas.drawBitmap(b, -halfW, -halfH, paint)
    canvas.restore()
  }
}

