package com.example.netcuttester

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetworkState {

    @Volatile
    var listener: ((Boolean) -> Unit)? = null

    @Volatile
    var isBlocked: Boolean = false
        set(value) {
            field = value
            listener?.let { l -> Handler(Looper.getMainLooper()).post { l(value) } }
        }

    suspend fun testRequest(
        url: String = "https://jsonplaceholder.typicode.com/todos/1"
    ): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 5_000
            c.readTimeout = 5_000
            "HTTP ${c.responseCode}\n" + c.inputStream.bufferedReader().readText().take(300)
        } finally {
            c.disconnect()
        }
    }
}
