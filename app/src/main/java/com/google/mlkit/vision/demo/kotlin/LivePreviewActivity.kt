package com.google.mlkit.vision.demo.kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.vision.demo.CameraSource
import com.google.mlkit.vision.demo.CameraSourcePreview
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.kotlin.textdetector.TextRecognitionProcessor
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

@KeepName
class LivePreviewActivity : AppCompatActivity() {

  private var cameraSource: CameraSource? = null
  private var preview: CameraSourcePreview? = null
  private var graphicOverlay: GraphicOverlay? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_vision_live_preview)

    preview = findViewById(R.id.preview_view)
    graphicOverlay = findViewById(R.id.graphic_overlay)

    // Always use back camera and Latin text recognition
    createCameraSource()
    startCameraSource()
  }

  private fun createCameraSource() {
    if (cameraSource == null) {
      cameraSource = CameraSource(this, graphicOverlay).apply {
        setFacing(CameraSource.CAMERA_FACING_BACK) // Always use the back camera
      }
    }
    try {
      Log.i(TAG, "Using Text Recognition Processor for Latin")
      cameraSource!!.setMachineLearningFrameProcessor(
        TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
      )
    } catch (e: Exception) {
      Log.e(TAG, "Cannot create image processor", e)
      Toast.makeText(applicationContext, "Cannot create image processor: " + e.message, Toast.LENGTH_LONG).show()
    }
  }

  private fun startCameraSource() {
    if (cameraSource != null) {
      try {
        preview?.start(cameraSource, graphicOverlay)
      } catch (e: IOException) {
        Log.e(TAG, "Unable to start camera source.", e)
        cameraSource!!.release()
        cameraSource = null
      }
    }
  }

  public override fun onResume() {
    super.onResume()
    startCameraSource()
  }

  public override fun onPause() {
    super.onPause()
    preview?.stop()
  }

  public override fun onDestroy() {
    super.onDestroy()
    cameraSource?.release()
  }

  companion object {
    private const val TAG = "LivePreviewActivity"
  }
}
