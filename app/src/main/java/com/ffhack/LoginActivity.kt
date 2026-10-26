package com.ffhack

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ffhack.databinding.ActivityLoginBinding
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private val pool = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        val saved = getSharedPreferences("ffhack", MODE_PRIVATE).getString("key", null)
        if (saved != null) b.keyInput.setText(saved)

        b.loginBtn.setOnClickListener {
            val key = b.keyInput.text?.toString()?.trim().orEmpty()
            if (key.length < 8) { toast("Key too short"); return@setOnClickListener }
            b.loginBtn.isEnabled = false
            b.progress.visibility = android.view.View.VISIBLE
            b.status.text = getString(R.string.login_checking)

            pool.execute {
                val rc = try { HunterNative.nConfirmCode(key) } catch (t: Throwable) { -99 }
                ui.post {
                    b.progress.visibility = android.view.View.GONE
                    b.loginBtn.isEnabled = true
                    if (rc == 0) {
                        b.status.text = getString(R.string.login_ok)
                        getSharedPreferences("ffhack", MODE_PRIVATE)
                            .edit().putString("key", key).apply()
                        HunterNative.nSetLicense(key)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        b.status.text = getString(R.string.login_fail) + " ($rc)"
                    }
                }
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    override fun onDestroy() { super.onDestroy(); pool.shutdown() }
}
