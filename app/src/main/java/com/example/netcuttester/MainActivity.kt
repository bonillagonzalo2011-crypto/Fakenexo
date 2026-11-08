package com.example.netcuttester

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val overlayLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val vpnLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val msg = if (result.resultCode == Activity.RESULT_OK)
                "Permiso VPN concedido" else "Permiso VPN denegado"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult = findViewById(R.id.tvResult)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        findViewById<Button>(R.id.btnVpnPermission).setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                vpnLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Permiso VPN ya concedido", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStartOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Activa el permiso de superposición", Toast.LENGTH_SHORT).show()
                overlayLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            } else {
                ContextCompat.startForegroundService(
                    this, Intent(this, FloatingButtonService::class.java)
                )
            }
        }

        findViewById<Button>(R.id.btnStopOverlay).setOnClickListener {
            stopService(Intent(this, FloatingButtonService::class.java))
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            tvResult.text = "Solicitando…"
            lifecycleScope.launch {
                tvResult.text = try {
                    NetworkState.testRequest()
                } catch (e: Exception) {
                    "ERROR: ${e.javaClass.simpleName}\n${e.message}"
                }
            }
        }
    }
}
