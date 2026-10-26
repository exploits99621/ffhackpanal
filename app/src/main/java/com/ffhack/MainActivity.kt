package com.ffhack

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ffhack.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.startBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.perm_needed, Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            val i = Intent(this, FloatingMenuService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
            Toast.makeText(this, "FF HACK running", Toast.LENGTH_SHORT).show()
        }

        b.logoutBtn.setOnClickListener {
            getSharedPreferences("ffhack", MODE_PRIVATE).edit().remove("key").apply()
            startActivity(Intent(this, LoginActivity::class.java))
            stopService(Intent(this, FloatingMenuService::class.java))
            stopService(Intent(this, EspOverlayService::class.java))
            finish()
        }
    }
}
