package com.ffhack.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.ffhack.HunterNative

class MenuView(ctx: Context) : LinearLayout(ctx) {
    private var downX = 0f; private var downY = 0f
    private var startX = 0; private var startY = 0

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#CC101010"))
        setPadding(20, 20, 20, 20)
        buildHeader()
        buildBody()
        setOnTouchListener(dragListener())
    }

    private fun buildHeader() {
        val header = TextView(context).apply {
            text = "FF HACK"
            setTextColor(Color.parseColor("#FF2B2B"))
            textSize = 20f
            setPadding(0, 0, 0, 12)
        }
        addView(header)
    }

    private fun buildBody() {
        val scroll = ScrollView(context)
        val content = LinearLayout(context).apply { orientation = VERTICAL }
        scroll.addView(content)
        addView(scroll, LayoutParams(600, 900))

        val count = HunterNative.nGetControlCount()
        for (i in 0 until count) {
            val label = HunterNative.nGetControlLabel(i)
            val mode = HunterNative.nGetControlMode(i)
            if (mode == 0) {
                val sw = Switch(context)
                sw.text = label
                sw.setTextColor(Color.WHITE)
                sw.isChecked = HunterNative.nGetStatus(i)
                val idx = i
                sw.setOnCheckedChangeListener { _, on -> HunterNative.nSendToggle(idx, on) }
                content.addView(sw)
            } else {
                val row = LinearLayout(context).apply { orientation = VERTICAL }
                val tv = TextView(context).apply {
                    text = "$label  [${HunterNative.nGetSliderMin(i)}..${HunterNative.nGetSliderMax(i)}] " +
                           "def ${HunterNative.nGetSliderDefault(i)} ${HunterNative.nGetSliderUnit(i)}"
                    setTextColor(Color.WHITE); textSize = 14f
                }
                val sb = SeekBar(context)
                sb.max = HunterNative.nGetSliderMax(i) - HunterNative.nGetSliderMin(i)
                sb.progress = HunterNative.nGetSliderDefault(i) - HunterNative.nGetSliderMin(i)
                val idx = i
                sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                        if (fromUser)
                            HunterNative.nSendSlider(idx, p + HunterNative.nGetSliderMin(idx))
                    }
                    override fun onStartTrackingTouch(sb: SeekBar) {}
                    override fun onStopTrackingTouch(sb: SeekBar) {}
                })
                row.addView(tv); row.addView(sb)
                content.addView(row)
            }
        }
    }

    private fun dragListener() = OnTouchListener { _, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY
                startX = left; startY = top; true
            }
            MotionEvent.ACTION_MOVE -> {
                x = startX + (e.rawX - downX)
                y = startY + (e.rawY - downY)
                true
            }
            else -> false
        }
    }
}
