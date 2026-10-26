package com.ffhack

import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.SurfaceView
import android.view.WindowManager

class EspOverlayService : Service() {
    private var wm: WindowManager? = null
    private var view: EspView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(2, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        view = EspView()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        wm?.addView(view, lp)
    }

    override fun onDestroy() {
        view?.let { wm?.removeView(it) }
        view = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "ffhack_esp"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "FF HACK ESP", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("FF HACK ESP")
            .setOngoing(true).build()
    }

    inner class EspView : SurfaceView(this@EspOverlayService), Runnable {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED; strokeWidth = 4f; style = Paint.Style.STROKE; textSize = 28f
        }
        private val hpPaint = Paint(paint).apply { style = Paint.Style.FILL; color = Color.GREEN }
        private val textPaint = Paint(paint).apply { style = Paint.Style.FILL; color = Color.WHITE; textSize = 26f }
        private val box = IntArray(4)
        private val line = FloatArray(4)
        private val target = FloatArray(2)
        private val dist = FloatArray(1)
        private val hp = FloatArray(1)
        private var running = true
        private var thread: Thread? = null

        init { setZOrderOnTop(true); holder.setFormat(PixelFormat.TRANSLUCENT) }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            thread = Thread(this).also { it.start() }
        }
        override fun onDetachedFromWindow() { running = false; thread?.join(); super.onDetachedFromWindow() }

        override fun run() {
            while (running) {
                val c = holder.lockCanvas() ?: continue
                try {
                    c.drawColor(0, PorterDuff.Mode.CLEAR)
                    val drawn = HunterNative.nDraw(c.width, c.height, box, line, target, dist, hp)
                    if (drawn > 0) {
                        if (box[2] > box[0] && box[3] > box[1]) {
                            c.drawRect(box[0].toFloat(), box[1].toFloat(), box[2].toFloat(), box[3].toFloat(), paint)
                            val h = box[3] - box[1]
                            val fill = (h * hp[0].coerceIn(0f, 1f)).toInt()
                            c.drawRect(box[2] + 4f, (box[3] - fill).toFloat(), box[2] + 12f, box[3].toFloat(), hpPaint)
                            c.drawText("${dist[0].toInt()}m", box[0].toFloat(), box[3] + 26f, textPaint)
                        }
                        if (line[0] != 0f || line[1] != 0f)
                            c.drawLine(line[0], line[1], line[2], line[3], paint)
                        if (target[0] != 0f || target[1] != 0f)
                            c.drawCircle(target[0], target[1], 8f, paint)
                    }
                } finally { holder.unlockCanvasAndPost(c) }
                try { Thread.sleep(16) } catch (_: InterruptedException) {}
            }
        }
    }
}
