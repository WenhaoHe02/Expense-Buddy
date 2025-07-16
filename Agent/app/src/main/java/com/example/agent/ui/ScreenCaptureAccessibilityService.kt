package com.example.agent.ui

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ScreenCaptureAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    /* 接收 FloatingWindowService 的截图指令 */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "CAPTURE") {
            captureScreen()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /* 真正执行截图 */
    private fun captureScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val hwBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        ) ?: return

                        // ✅ 关键：复制为普通 ARGB_8888 Bitmap，方便OpenCV使用
                        val copy = hwBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        hwBitmap.recycle()

                        val intent = Intent(ACTION_SCREENSHOT).apply {
                            putExtra(EXTRA_BITMAP, copy)
                        }
                        LocalBroadcastManager.getInstance(this@ScreenCaptureAccessibilityService)
                            .sendBroadcast(intent)
                        screenshot.hardwareBuffer.close()
                    }

                    override fun onFailure(errorCode: Int) {
                        val intent = Intent(ACTION_SCREENSHOT).apply {
                            putExtra(EXTRA_ERROR, errorCode)
                        }
                        LocalBroadcastManager.getInstance(this@ScreenCaptureAccessibilityService)
                            .sendBroadcast(intent)
                    }
                })
        }
    }

    companion object {
        const val ACTION_SCREENSHOT = "com.example.agent.SCREENSHOT_RESULT"
        const val EXTRA_BITMAP = "bitmap"
        const val EXTRA_ERROR = "error"
    }
}