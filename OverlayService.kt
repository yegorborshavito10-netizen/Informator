package com.informer.m3

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var web: WebView? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (root != null) return START_STICKY
        startAsForeground()
        showOverlay()
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("inf", "Информатор", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "inf")
            .setContentTitle("Информатор М3 работает")
            .setContentText("Окно поверх игры. Закрыть — крестик в окне.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(1, n)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun showOverlay() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val params = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE + NOT_TOUCH_MODAL: касания вне окна идут в Minecraft
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(60) }

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            setBackgroundColor(Color.parseColor("#eef1f4"))
            loadUrl("file:///android_asset/informer.html")
        }
        web = wv

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#c8102e"))
            setPadding(dp(10), dp(6), dp(6), dp(6))
        }
        val title = TextView(this).apply {
            text = "⠿ Информатор М3 (тяни за заголовок)"
            setTextColor(Color.WHITE); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun btn(t: String, click: () -> Unit) = TextView(this).apply {
            text = t; setTextColor(Color.WHITE); textSize = 20f
            setPadding(dp(12), 0, dp(12), 0); setOnClickListener { click() }
        }
        val min = btn("—") {
            wv.visibility = if (wv.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        val close = btn("✕") { stopSelf() }
        bar.addView(title); bar.addView(min); bar.addView(close)

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = params.x; sy = params.y; tx = e.rawX; ty = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    params.x = sx + (e.rawX - tx).toInt(); params.y = sy + (e.rawY - ty).toInt()
                    wm.updateViewLayout(root, params)
                }
            }
            true
        }

        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        r.addView(bar)
        r.addView(wv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(420)))
        root = r
        wm.addView(r, params)
    }

    override fun onDestroy() {
        web?.destroy()
        root?.let { wm.removeView(it) }
        root = null
        super.onDestroy()
    }
}
