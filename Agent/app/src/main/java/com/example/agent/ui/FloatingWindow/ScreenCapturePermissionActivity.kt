package com.example.agent.ui.FloatingWindow

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ScreenCapturePermissionActivity : AppCompatActivity() {
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {

            // ✅ 启动 OCR 专用前台服务（避免触发 PetService 崩溃）
            val fgIntent = Intent(this, OcrForegroundService::class.java)
            ContextCompat.startForegroundService(this, fgIntent)

            // ✅ 传递数据给 PetService（正常逻辑不改）
            val serviceIntent = Intent(this, PetService::class.java).apply {
                action = "START_FOREGROUND"
                putExtra("resultCode", resultCode)
                putExtra("resultData", data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)

        } else {
            Toast.makeText(this, "未授权屏幕捕获，OCR 将不可用", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

}