
package com.craftverse.multitrackgps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "MultiTrack GPS"
            textSize = 28f
        }

        val description = TextView(this).apply {
            text = "Employee GPS Tracking\n\nTracking starts only after your permission and approval."
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        val startButton = Button(this).apply {
            text = "Start GPS Tracking"
            setOnClickListener {
                requestLocationPermission()
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop GPS Tracking"
            setOnClickListener {
                stopService(
                    Intent(this@MainActivity, LocationService::class.java)
                )
                Toast.makeText(
                    this@MainActivity,
                    "Tracking stopped",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(startButton)
        layout.addView(stopButton)

        setContentView(layout)
    }

    private fun requestLocationPermission() {

        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing: Array<String> = permissions
            .filter { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }
            .toTypedArray()

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing,
                requestCode
            )
        } else {
            startTrackingService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == this.requestCode) {

            val locationGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            val notificationGranted =
                Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (locationGranted && notificationGranted) {
                startTrackingService()
            } else {
                Toast.makeText(
                    this,
                    "Location and notification permissions are required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startTrackingService() {

        val intent = Intent(
            this,
            LocationService::class.java
        )

        ContextCompat.startForegroundService(this, intent)

        Toast.makeText(
            this,
            "GPS tracking started",
            Toast.LENGTH_SHORT
        ).show()
    }
}
