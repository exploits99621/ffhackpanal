package com.ffhack

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.ffhack.ui.MenuView

class FloatingMenuService : Service() {
    private var wm: WindowManager? = null
    private var view: MenuView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        HunterNative.nInit()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        view = MenuView(this)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40; y = 160
        }
        wm?.addView(view, lp)

        startService(Intent(this, EspOverlayService::class.java))
    }

    override fun onDestroy() {
        view?.let { wm?.removeView(it) }
        view = null
        stopService(Intent(this, EspOverlayService::class.java))
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "ffhack"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "FF HACK", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("FF HACK")
            .setContentText("Floating menu running")
            .setOngoing(true)
            .build()
    }
}
