package com.google.mlkit.vision.demo.kotlin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.mlkit.vision.demo.R

/** Demo app chooser which directly opens LivePreviewActivity. */
class ChooserActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")

    // Directly start LivePreviewActivity without any UI choice
    startActivity(Intent(this, LivePreviewActivity::class.java))
    finish() // Optionally finish this activity so it doesn't stay in the back stack
  }

  companion object {
    private const val TAG = "ChooserActivity"
  }
}
