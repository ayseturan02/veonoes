package com.veonoes.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.veonoes.R
import kotlin.math.atan2
import kotlin.math.hypot

class ARCameraActivity : AppCompatActivity() {

  private lateinit var previewView: PreviewView
  private lateinit var overlayView: OverlayView
  private lateinit var glassesPicker: RecyclerView

  private lateinit var detector: FaceDetector
  private var currentSpec = GlassesRepo.all.first()
  private val smoother = PoseSmoother()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_main)

    // XML'deki View'ları bağla
    previewView = findViewById(R.id.previewView)
    overlayView = findViewById(R.id.overlayView)
    glassesPicker = findViewById(R.id.glassesPicker)

    // Ayna etkisi
    previewView.scaleX = -1f
    overlayView.scaleX = -1f

    // Picker
    glassesPicker.layoutManager =
      LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    val adapter = GlassesAdapter(GlassesRepo.all) { spec ->
      currentSpec = spec
      overlayView.setBitmap(
        android.graphics.BitmapFactory.decodeResource(resources, spec.resId)
      )
    }
    glassesPicker.adapter = adapter
    adapter.select(0)

    val modelKey = intent?.data?.getQueryParameter("model")
    val startIndex = GlassesRepo.indexOfKey(modelKey)
    (adapter as GlassesAdapter).select(startIndex)

    // ML Kit yüz dedektörü
    val opts = FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
      .build()
    detector = FaceDetection.getClient(opts)

    // Kamera izni ve başlatma
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
      == PackageManager.PERMISSION_GRANTED) {
      startCamera()
    } else {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
    }
  }

  private fun startCamera() {
    val providerFuture = ProcessCameraProvider.getInstance(this)
    providerFuture.addListener({
      val provider = providerFuture.get()

      val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }

      val analysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

      analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
        processFrame(imageProxy)
      }

      val selector = CameraSelector
        .Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

      provider.unbindAll()
      provider.bindToLifecycle(this, selector, preview, analysis)
    }, ContextCompat.getMainExecutor(this))
  }

  private fun processFrame(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    val rotation = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotation)

    detector.process(image)
      .addOnSuccessListener { faces ->
        if (faces.isEmpty()) return@addOnSuccessListener

        val face = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }
        val l = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val r = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        if (l == null || r == null) return@addOnSuccessListener

        val dx = (r.x - l.x)
        val dy = (r.y - l.y)
        val dist = hypot(dx, dy)
        val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

        val cx = (l.x + r.x) / 2f
        val cy = (l.y + r.y) / 2f

        val scale = (dist / currentSpec.baselineEyeDistancePx).coerceAtLeast(0.1f)
        val yOffset = currentSpec.yOffsetRatio * dist

        val pose = Pose(cx = cx, cy = cy + yOffset, rotationDeg = angleDeg, scale = scale)
        overlayView.updatePose(smoother.filter(pose))
      }
      .addOnCompleteListener { imageProxy.close() }
  }
}
