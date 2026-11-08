package com.example.netcuttester

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.math.abs

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var fabView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showFloatingButton()
    }

    private fun startAsForeground() {
        val channelId = "overlay_channel"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "Botón flotante", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NetCutTester activo")
            .setContentText("Toca el círculo para cortar o restaurar la red")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, notif, type)
    }

    private fun showFloatingButton() {
        if (fabView != null) return

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val themed = ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
        val view = LayoutInflater.from(themed).inflate(R.layout.floating_button_layout, null)
        val card = view.findViewById<CardView>(R.id.fabCard)
        applyColor(card, NetworkState.isBlocked)

        NetworkState.listener = { blocked ->
            applyColor(card, blocked)
            Toast.makeText(
                this,
                if (blocked) "Red cortada (VPN activa)" else "Red restaurada",
                Toast.LENGTH_SHORT
            ).show()
        }

        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f
        var moved = false
        val slop = 12 * resources.displayMetrics.density

        view.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - touchX
                    val dy = e.rawY - touchY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved) toggleNetwork()
            }
            true
        }

        windowManager.addView(view, params)
        fabView = view
    }

    private fun toggleNetwork() {
        if (VpnService.prepare(this) != null) {
            Toast.makeText(
                this,
                "Concede primero el permiso de VPN desde la app",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val action = if (NetworkState.isBlocked)
            BlockVpnService.ACTION_STOP else BlockVpnService.ACTION_START
        startService(Intent(this, BlockVpnService::class.java).setAction(action))
    }

    private fun applyColor(card: CardView, blocked: Boolean) {
        card.setCardBackgroundColor(
            if (blocked) Color.parseColor("#D32F2F") else Color.parseColor("#FF5722")
        )
    }

    override fun onDestroy() {
        NetworkState.listener = null
        if (NetworkState.isBlocked) {
            startService(
                Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_STOP)
            )
        }
        fabView?.let { windowManager.removeView(it) }
        fabView = null
        super.onDestroy()
    }
}
