package com.example.agent.ui

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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
            // 启动服务并传递权限数据
            val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
                action = "START_FOREGROUND" // 新增一个 action 标识
            }
            startService(serviceIntent)
        }
        finish()
    }
}