package com.example.agent.ui
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.example.agent.R
import com.example.agent.data.db.AppDatabase
import com.example.agent.databinding.FloatingMenuLayoutBinding
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ballView: android.view.View
    private lateinit var params: WindowManager.LayoutParams

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var isClick = false
    private val db by lazy { AppDatabase.getInstance(this) }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val FOREGROUND_NOTIFICATION_ID = 1234
    private val screenCaptureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "SCREEN_CAPTURE_RESULT") {
                val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>("data")

                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 在主线程执行
                    Handler(Looper.getMainLooper()).post {
                        try {
                            setupMediaProjection(resultCode, data)
                            captureScreen()
                        } catch (e: Exception) {
                            Log.e("ScreenCapture", "处理截图时出错", e)
                            Toast.makeText(
                                this@FloatingWindowService,
                                "截图失败: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@FloatingWindowService,
                        "截图权限被拒绝",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreate() {
        super.onCreate()
        startAsForegroundService()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val filter = IntentFilter("SCREEN_CAPTURE_RESULT")
        registerReceiver(screenCaptureReceiver, filter,Context.RECEIVER_EXPORTED)
        Log.d("service register","finished")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val sizePx = dpToPx(30)
        val edgeThreshold = dpToPx(50)
        val hidePx = dpToPx(10)

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dpToPx(200)
        }
        ballView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null)
        windowManager.addView(ballView, params)
        var downX = 0; var downY = 0; var lastX = 0; var lastY = 0
        ballView.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isClick = true
                    val screenW = resources.displayMetrics.widthPixels
                    if (params.x < 0) params.x = 0
                    else if (params.x > screenW - sizePx) params.x = screenW - sizePx
                    windowManager.updateViewLayout(ballView, params)

                    downX = ev.rawX.toInt()
                    downY = ev.rawY.toInt()
                    lastX = params.x
                    lastY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX.toInt() - downX
                    val dy = ev.rawY.toInt() - downY
                    if (isClick && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        isClick = false
                    }
                    if (!isClick) {
                        params.x = lastX + dx
                        params.y = lastY + dy
                        windowManager.updateViewLayout(ballView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) toggleMenu()
                    else {
                        val screenW = resources.displayMetrics.widthPixels
                        val centerX = params.x + sizePx / 2
                        when {
                            centerX <= edgeThreshold -> params.x = hidePx - sizePx
                            centerX >= screenW - edgeThreshold -> params.x = screenW - hidePx
                        }
                        windowManager.updateViewLayout(ballView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startAsForegroundService() {
        // 为 Android 8.0+ 创建通知渠道
        createNotificationChannel()

        // 创建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("浮动窗口服务")
            .setContentText("服务正在运行")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 作为前台服务启动
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    // 创建通知渠道
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "浮动窗口服务频道",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun toggleMenu() {
        menuPopup?.let { if (it.isShowing) { it.dismiss(); return } }
        val binding = FloatingMenuLayoutBinding.inflate(LayoutInflater.from(this))
        menuPopup = PopupWindow(
            binding.root,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = true
            windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        binding.menuManualEntry.setOnClickListener {
            menuPopup?.dismiss()
            showForm()
        }
        binding.menuOcr.setOnClickListener {
            menuPopup?.dismiss()
            startScreenCapture()
        }
        val loc = IntArray(2).also { ballView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            ballView,
            Gravity.NO_GRAVITY,
            loc[0] + dpToPx(30),
            loc[1] + dpToPx(30)
        )
    }

    private fun startScreenCapture() {
        val intent = Intent(this, MediaProjectionRequestActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
    }

    private fun captureScreen() {
        if (mediaProjection == null) {
            Log.e("captureScreen", "Cannot capture screen - media projection is null")
            Toast.makeText(this, "截图失败: 媒体投影未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 添加 MediaProjection 回调来管理资源
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d("captureScreen", "MediaProjection stopped")
                    // 清理资源
                    mediaProjection?.unregisterCallback(this)
                    imageReader?.close()
                    imageReader = null
                }
            }, Handler(Looper.getMainLooper()))

            val metrics = resources.displayMetrics
            val density = metrics.densityDpi
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            Log.d("captureScreen", "Capturing screen: ${width}x${height} (dpi: $density)")

            // 关闭之前的 ImageReader
            imageReader?.close()
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    try {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            processScreenshot(image)
                            image.close()

                            // 完成截图后停止媒体投影
                            mediaProjection?.stop()
                        }
                    } catch (e: Exception) {
                        Log.e("captureScreen", "Error processing image", e)
                        Toast.makeText(this@FloatingWindowService, "处理截图失败", Toast.LENGTH_SHORT).show()
                    }
                }, Handler(Looper.getMainLooper()))
            }

            // 创建虚拟显示
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper()) // 添加 Handler 参数
            )?.also {
                Log.d("captureScreen", "Virtual display created successfully")
                Toast.makeText(this, "正在捕获屏幕...", Toast.LENGTH_SHORT).show()
            } ?: run {
                Log.e("captureScreen", "Failed to create virtual display")
                Toast.makeText(this, "创建虚拟显示器失败", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("captureScreen", "Capture screen failed", e)
            Toast.makeText(this, "截图失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processScreenshot(image: Image) {
        val buffer = image.planes[0].buffer
        val pixelStride = image.planes[0].pixelStride
        val rowStride = image.planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)

        // 现在你可以:
        // 1. 保存截图到文件
        // 2. 传递给OCR处理模块
        // 3. 在Toast中显示提示信息
        Toast.makeText(this, "截图成功! 尺寸: ${bitmap.width}x${bitmap.height}", Toast.LENGTH_SHORT).show()
    }


    @SuppressLint("InflateParams")
    private fun showForm() {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(this))
        formPopup = PopupWindow(
            binding.root,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = false
            windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        binding.btnSave.setOnClickListener {
            val amt: Float = binding.etAmount.text.toString()
                .toFloatOrNull()
                ?: 0f
            val desc = binding.etDescription.text.toString().ifBlank { "手动" }
            CoroutineScope(Dispatchers.IO).launch {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                val now = Date()
                db.transactionDao().insert(Transaction(
                    amount = amt,
                    merchant = desc,
                    method = "手动",
                    time = fmt.format(now),
                    timeMillis = now.time
                ))
            }
            formPopup?.dismiss()
        }
        binding.btnCancel.setOnClickListener {
            formPopup?.dismiss()
        }
        val loc = IntArray(2).also { ballView.getLocationOnScreen(it) }
        formPopup?.showAtLocation(
            ballView,
            Gravity.NO_GRAVITY,
            loc[0] + dpToPx(30),
            loc[1] + dpToPx(30)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        menuPopup?.dismiss()
        formPopup?.dismiss()
        unregisterReceiver(screenCaptureReceiver)
        mediaProjection?.stop()
        imageReader?.close()
        if (::ballView.isInitialized) windowManager.removeView(ballView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    // 添加通知渠道常量
    companion object {
        private const val CHANNEL_ID = "FloatingWindowServiceChannel"
    }
}
