package com.veonoes.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GlassesOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var glasses: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Hedeflenen pozisyon, boyut ve rotasyon değerleri
    private var targetCx = 0f
    private var targetCy = 0f
    private var targetEyeDist = 0f
    private var targetRot = 0f
    private var targetScale = 2.2f

    // Ekranda daha akıcı hareket için yumuşatılmış değerler
    private var smCx = 0f
    private var smCy = 0f
    private var smEyeDist = 0f
    private var smRot = 0f

    // Yumuşatma faktörü (daha akıcı geçişler için)
    private var smoothAlpha = 0.15f

    fun setGlassesBitmap(bmp: Bitmap) {
        glasses = bmp
        // Yeni bir gözlük yüklendiğinde pozisyonu sıfırla
        smCx = 0f
        smCy = 0f
        smEyeDist = 0f
        smRot = 0f
        invalidate() // Ekranda çizim için tetikle
    }

    // Yeni pozisyon, boyut ve rotasyon bilgilerini almak için güncellenmiş fonksiyon
    fun updatePose(cx: Float, cy: Float, eyeDist: Float, rotationDeg: Float, scale: Float) {
        targetCx = cx
        targetCy = cy
        targetEyeDist = eyeDist
        targetRot = rotationDeg
        targetScale = scale
        invalidate()
    }

    // Yüz kaybolduğunda gözlüğü gizlemek için
    fun hide() {
        targetEyeDist = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = glasses ?: return

        // Hedeflenen bir genişlik yoksa (yüz algılanmadıysa) hiçbir şey çizme
        if (targetEyeDist <= 0f) return

        // İlk değerleri ayarla (eğer sıfırsa)
        if (smEyeDist == 0f) {
            smCx = targetCx
            smCy = targetCy
            smEyeDist = targetEyeDist
            smRot = targetRot
        }
        
        // Değerleri hedefe doğru yumuşakça kaydır
        smCx += (targetCx - smCx) * smoothAlpha
        smCy += (targetCy - smCy) * smoothAlpha
        smEyeDist += (targetEyeDist - smEyeDist) * smoothAlpha
        smRot += (targetRot - smRot) * smoothAlpha

        // Gözlüğün en/boy oranını koru
        val aspect = bmp.height.toFloat() / bmp.width

        // Gözlüğün genişliğini, göz mesafesi ve ölçek faktörüne göre hesapla
        val drawW = smEyeDist * targetScale
        val drawH = drawW * aspect

        // Çizim yapılacak dikdörtgenin koordinatlarını hesapla (merkez noktasına göre)
        val left = smCx - drawW / 2
        val top = smCy - drawH / 2
        val dst = RectF(left, top, left + drawW, top + drawH)

        // --- YENİ ÇİZİM MANTIĞI (ROTASYON İLE) ---
        canvas.save() // Mevcut tuval durumunu kaydet
        canvas.rotate(smRot, smCx, smCy) // Tuvali gözlüğün merkez noktası etrafında döndür
        canvas.drawBitmap(bmp, null, dst, paint) // Döndürülmüş tuvale gözlüğü çiz
        canvas.restore() // Tuvali orijinal durumuna geri getir

        // Animasyonun devam etmesi için bir sonraki frame'de tekrar çizim talep et
        postInvalidate()
    }
}