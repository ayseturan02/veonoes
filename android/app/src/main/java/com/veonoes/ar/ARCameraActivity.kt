package com.veonoes.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlin.math.atan2
import kotlin.math.sqrt

class ARCameraActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: GlassesOverlayView
    private var faceDetector: FaceDetector? = null
    private var glassesBitmap: Bitmap? = null
    
    // Gözlük modeline özel ölçeklendirme faktörü
    private var modelScale = 2.2f 

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Basit container
        val root = FrameLayout(this)
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        overlay = GlassesOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(previewView)
        root.addView(overlay)
        setContentView(root)

        // Model URL'sini ve ölçek bilgisini al
        val modelUrl = intent?.data?.getQueryParameter("model")
            ?: intent?.getStringExtra("modelUrl")
        
        // Gözlüğün yüze ne kadar büyük çizileceğini belirleyen ölçek
        // Bu değeri her gözlük modeli için ayarlayabilirsiniz.
        modelScale = intent?.data?.getQueryParameter("scale")?.toFloatOrNull() ?: 2.2f

        modelUrl?.let { loadGlasses(it) } ?: run {
            Toast.makeText(this, "Model URL bulunamadı", Toast.LENGTH_SHORT).show()
        }

        // ML Kit FaceDetector
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(opts)

        // Kamera izni
        val has = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (has == PackageManager.PERMISSION_GRANTED) startCamera()
        else requestPermission.launch(Manifest.permission.CAMERA)
    }
    
    private fun loadGlasses(url: String) {
        Glide.with(this)
            .asBitmap()
            .load(Uri.parse(url))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    glassesBitmap = resource
                    overlay.setGlassesBitmap(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val img = imageProxy.image ?: return imageProxy.close()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(img, rotation)

        faceDetector?.process(input)
            ?.addOnSuccessListener { faces ->
                if (faces.isNotEmpty() && glassesBitmap != null) {
                    // Ekrana en yakın (en büyük) yüzü al
                    val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                    val left = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    val right = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

                    if (left != null && right != null) {
                        // --- YENİ HESAPLAMALAR ---
                        
                        // 1. Gözlerin orta noktası (Merkez)
                        val cx = (left.x + right.x) / 2f
                        
                        // 2. Gözler arasındaki mesafe (Boyutlandırma için)
                        val eyeDistance = sqrt((left.x - right.x).pow(2) + (left.y - right.y).pow(2))

                        // 3. Gözlüğün dikey konumu (Hafifçe aşağı kaydırarak daha doğal bir görünüm)
                        val cy = (left.y + right.y) / 2f + eyeDistance * 0.1f

                        // 4. Kafa eğim açısı (Rotasyon için)
                        val angle = Math.toDegrees(atan2(right.y - left.y, right.x - left.x).toDouble()).toFloat()

                        // 5. Hesaplanan verileri overlay'e gönder
                        overlay.updatePose(cx, cy, eyeDistance, angle, modelScale)
                    }
                } else {
                    // Yüz algılanmadıysa gözlüğü gizle
                    overlay.hide()
                }
            }
            ?.addOnCompleteListener { imageProxy.close() }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetector?.close()
    }

    // Kotlin'de üs alma için yardımcı fonksiyon
    private fun Float.pow(n: Int): Float = kotlin.math.pow(this, n)
}