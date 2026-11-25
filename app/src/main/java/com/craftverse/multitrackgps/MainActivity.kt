
package com.craftverse.multitrackgps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var statusText: TextView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var logoutButton: Button

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
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
                showMessage("Location and notification permissions are required")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        createScreen()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkApproval(currentUser.uid)
        } else {
            showLoginScreen()
        }
    }

    private fun createScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "MultiTrack GPS"
            textSize = 28f
        }

        statusText = TextView(this).apply {
            text = "Employee Login"
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }

        emailInput = EditText(this).apply {
            hint = "Employee Email"
            inputType = 33
        }

        passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = 129
        }

        loginButton = Button(this).apply {
            text = "Login"
            setOnClickListener { loginEmployee() }
        }

        startButton = Button(this).apply {
            text = "Start GPS Tracking"
            setOnClickListener { requestLocationPermission() }
        }

        stopButton = Button(this).apply {
            text = "Stop GPS Tracking"
            setOnClickListener { stopTracking() }
        }

        logoutButton = Button(this).apply {
            text = "Logout"
            setOnClickListener { logoutEmployee() }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(emailInput)
        layout.addView(passwordInput)
        layout.addView(loginButton)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(logoutButton)

        setContentView(layout)
    }

    private fun showLoginScreen() {
        emailInput.visibility = android.view.View.VISIBLE
        passwordInput.visibility = android.view.View.VISIBLE
        loginButton.visibility = android.view.View.VISIBLE
        startButton.visibility = android.view.View.GONE
        stopButton.visibility = android.view.View.GONE
        logoutButton.visibility = android.view.View.GONE
        statusText.text = "Please login with your employee account"
    }

    private fun showApprovedScreen() {
        emailInput.visibility = android.view.View.GONE
        passwordInput.visibility = android.view.View.GONE
        loginButton.visibility = android.view.View.GONE
        startButton.visibility = android.view.View.VISIBLE
        stopButton.visibility = android.view.View.VISIBLE
        logoutButton.visibility = android.view.View.VISIBLE
        statusText.text = "Account approved. Tracking is under your control."
    }

    private fun loginEmployee() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Enter email and password")
            return
        }

        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid != null) {
                        checkApproval(uid)
                    } else {
                        showMessage("Login failed")
                    }
                } else {
                    showMessage(
                        task.exception?.localizedMessage ?: "Login failed"
                    )
                }
            }
    }

    private fun checkApproval(uid: String) {
        statusText.text = "Checking admin approval..."

        FirebaseDatabase.getInstance()
            .getReference("authorizedEmployees")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.getValue(Boolean::class.java) == true) {
                    showApprovedScreen()
                } else {
                    auth.signOut()
                    stopService(
                        Intent(this, LocationService::class.java)
                    )
                    showLoginScreen()
                    statusText.text =
                        "Waiting for Admin Approval. Please contact Admin."
                }
            }
            .addOnFailureListener { error ->
                auth.signOut()
                showLoginScreen()
                showMessage(
                    error.localizedMessage ?: "Approval check failed"
                )
            }
    }

    private fun requestLocationPermission() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            showMessage(
                "Use of GPS tracking requires your consent. Press Start again to confirm."
            )
            startTrackingService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
        showMessage("GPS tracking started")
    }

    private fun stopTracking() {
        stopService(Intent(this, LocationService::class.java))
        showMessage("GPS tracking stopped")
    }

    private fun logoutEmployee() {
        stopTracking()
        auth.signOut()
        showLoginScreen()
        showMessage("Logged out")
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
