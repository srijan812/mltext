package com.google.mlkit.vision.demo.kotlin

import android.content.Intent
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.demo.CameraXViewModel
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.VisionImageProcessor
import com.google.mlkit.vision.demo.kotlin.textdetector.TextRecognitionProcessor
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
class CameraXLivePreviewActivity :
  AppCompatActivity(), AdapterView.OnItemSelectedListener {

  private var previewView: PreviewView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var previewUseCase: Preview? = null
  private var analysisUseCase: ImageAnalysis? = null
  private var imageProcessor: VisionImageProcessor? = null
  private var needUpdateGraphicOverlayImageSourceInfo = false
  private var selectedModel = TEXT_RECOGNITION_LATIN
  private var lensFacing = CameraSelector.LENS_FACING_BACK
  private var cameraSelector: CameraSelector? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")
    if (savedInstanceState != null) {
      selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, TEXT_RECOGNITION_LATIN)
    }
    cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    setContentView(R.layout.activity_vision_camerax_live_preview)
    previewView = findViewById(R.id.preview_view)
    graphicOverlay = findViewById(R.id.graphic_overlay)

    val spinner = findViewById<Spinner>(R.id.spinner)
    val options = listOf(
      TEXT_RECOGNITION_LATIN,
      TEXT_RECOGNITION_CHINESE,
      TEXT_RECOGNITION_DEVANAGARI,
      TEXT_RECOGNITION_JAPANESE,
      TEXT_RECOGNITION_KOREAN
    )

    val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner.adapter = dataAdapter
    spinner.onItemSelectedListener = this

    val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
    facingSwitch.setOnCheckedChangeListener { _, isChecked ->
      val newLensFacing =
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
          CameraSelector.LENS_FACING_BACK
        } else {
          CameraSelector.LENS_FACING_FRONT
        }
      val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
      if (cameraProvider!!.hasCamera(newCameraSelector)) {
        lensFacing = newLensFacing
        cameraSelector = newCameraSelector
        bindAllCameraUseCases()
      } else {
        Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
      }
    }

    ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
      .get(CameraXViewModel::class.java)
      .processCameraProvider
      .observe(this, Observer { provider ->
        cameraProvider = provider
        bindAllCameraUseCases()
      })

    val settingsButton = findViewById<ImageView>(R.id.settings_button)

  }

  override fun onSaveInstanceState(bundle: Bundle) {
    super.onSaveInstanceState(bundle)
    bundle.putString(STATE_SELECTED_MODEL, selectedModel)
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
    selectedModel = parent?.getItemAtPosition(pos).toString()
    bindAnalysisUseCase()
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    // Do nothing
  }

  private fun bindAllCameraUseCases() {
    cameraProvider?.unbindAll()
    bindPreviewUseCase()
    bindAnalysisUseCase()
  }

  private fun bindPreviewUseCase() {
    if (cameraProvider == null) return
    previewUseCase = Preview.Builder().build()
    previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
    camera = cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
  }

  private fun bindAnalysisUseCase() {
    if (cameraProvider == null) return

    if (imageProcessor != null) {
      imageProcessor!!.stop()
    }

    imageProcessor = when (selectedModel) {
      TEXT_RECOGNITION_LATIN -> {
        Log.i(TAG, "Using Latin Text Recognition")
        TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
      }
      TEXT_RECOGNITION_CHINESE -> {
        Log.i(TAG, "Using Chinese Text Recognition")
        TextRecognitionProcessor(this, ChineseTextRecognizerOptions.Builder().build())
      }
      TEXT_RECOGNITION_DEVANAGARI -> {
        Log.i(TAG, "Using Devanagari Text Recognition")
        TextRecognitionProcessor(this, DevanagariTextRecognizerOptions.Builder().build())
      }
      TEXT_RECOGNITION_JAPANESE -> {
        Log.i(TAG, "Using Japanese Text Recognition")
        TextRecognitionProcessor(this, JapaneseTextRecognizerOptions.Builder().build())
      }
      TEXT_RECOGNITION_KOREAN -> {
        Log.i(TAG, "Using Korean Text Recognition")
        TextRecognitionProcessor(this, KoreanTextRecognizerOptions.Builder().build())
      }
      else -> throw IllegalStateException("Invalid model name")
    }

    analysisUseCase = ImageAnalysis.Builder().build()
    analysisUseCase!!.setAnalyzer(
      ContextCompat.getMainExecutor(this),
      { imageProxy: ImageProxy ->
        if (needUpdateGraphicOverlayImageSourceInfo) {
          val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
          val rotationDegrees = imageProxy.imageInfo.rotationDegrees
          if (rotationDegrees == 0 || rotationDegrees == 180) {
            graphicOverlay!!.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
          } else {
            graphicOverlay!!.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
          }
          needUpdateGraphicOverlayImageSourceInfo = false
        }
        try {
          imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
        } catch (e: MlKitException) {
          Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
          Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
      }
    )

    cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
  }

  companion object {
    private const val TAG = "CameraXLivePreview"
    private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
    private const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese"
    private const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari"
    private const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese"
    private const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean"
    private const val STATE_SELECTED_MODEL = "selected_model"
  }
}
