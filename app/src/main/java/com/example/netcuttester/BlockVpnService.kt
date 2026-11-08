package com.example.netcuttester

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

class BlockVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.netcuttester.VPN_START"
        const val ACTION_STOP = "com.example.netcuttester.VPN_STOP"
    }

    private var tun: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
        } else {
            startVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (tun != null) return

        val builder = Builder()
            .setSession("NetCutTester")
            .addAddress("10.255.255.1", 32)
            .addRoute("0.0.0.0", 0)

        try {
            builder.addAddress("fd00:255::1", 128)
            builder.addRoute("::", 0)
        } catch (_: Exception) {
        }

        val pfd = builder.establish() ?: return
        tun = pfd
        NetworkState.isBlocked = true

        drainThread = Thread {
            try {
                val input = FileInputStream(pfd.fileDescriptor)
                val buf = ByteArray(32767)
                while (!Thread.currentThread().isInterrupted && input.read(buf) >= 0) {
                }
            } catch (_: Exception) {
            }
        }.also { it.start() }
    }

    private fun stopVpn() {
        drainThread?.interrupt()
        drainThread = null
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
        NetworkState.isBlocked = false
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
