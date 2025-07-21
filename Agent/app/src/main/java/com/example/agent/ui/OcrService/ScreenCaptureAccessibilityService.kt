package com.example.agent.ui.OcrService

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class ScreenCaptureAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private var instance: ScreenCaptureAccessibilityService? = null
        fun get(): ScreenCaptureAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * 通过无障碍 API 截图（Android 11+）
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreen(onSuccess: (Bitmap) -> Unit) {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hwBitmap = Bitmap.wrapHardwareBuffer(
                        result.hardwareBuffer,
                        result.colorSpace
                    ) ?: return
                    val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, true) // 2. 转成 Bitmap
                    onSuccess(bitmap)
                    hwBitmap.recycle()                     // 3. 及时释放资源
                }

                override fun onFailure(errorCode: Int) {
                    // 可根据 errorCode 做提示
                }
            }
        )
    }
}