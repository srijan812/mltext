package com.google.mlkit.vision.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class EntryChoiceActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Request permissions and proceed if granted
    if (allRuntimePermissionsGranted()) {
      startChooserActivity()
    } else {
      getRuntimePermissions()
    }
  }

  private fun startChooserActivity() {
    val intent = Intent(this, com.google.mlkit.vision.demo.kotlin.ChooserActivity::class.java)
    startActivity(intent)
    finish() // Optionally finish this activity
  }

  private fun allRuntimePermissionsGranted(): Boolean {
    return REQUIRED_RUNTIME_PERMISSIONS.all { isPermissionGranted(this, it) }
  }

  private fun getRuntimePermissions() {
    val permissionsToRequest = REQUIRED_RUNTIME_PERMISSIONS.filterNot { isPermissionGranted(this, it) }
    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(
        this,
        permissionsToRequest.toTypedArray(),
        PERMISSION_REQUESTS
      )
    }
  }

  private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUESTS) {
      if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
        startChooserActivity()
      } else {
        Log.e(TAG, "Permissions not granted. Exiting.")
        finish() // Exiting if permissions are not granted
      }
    }
  }

  companion object {
    private const val TAG = "EntryChoiceActivity"
    private const val PERMISSION_REQUESTS = 1

    private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
      Manifest.permission.CAMERA,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE
    )
  }
}
