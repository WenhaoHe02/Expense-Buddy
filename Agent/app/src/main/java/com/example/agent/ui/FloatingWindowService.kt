
package com.example.agent.ui
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.benjaminwan.ocrlibrary.OcrEngine
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
    // 截图相关变量
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var density = 0
    private var resultCode = 0
    private var resultData: Intent? = null
    private val handler = Handler()
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "screen_capture_channel"

    private lateinit var windowManager: WindowManager
    private lateinit var ballView: android.view.View
    private lateinit var params: WindowManager.LayoutParams

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var isClick = false

    private val db by lazy { AppDatabase.getInstance(this) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
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

        // 初始化屏幕参数
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        density = metrics.densityDpi
    }

    private fun toggleMenu() {
        if (menuPopup?.isShowing == true) {
            menuPopup?.dismiss()
            return
        }

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
            captureSingleFrame()
        }

        val loc = IntArray(2).also { ballView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            ballView,
            Gravity.NO_GRAVITY,
            loc[0] + dpToPx(30),
            loc[1] + dpToPx(30)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == "START_FOREGROUND") {
                // 1. 先启动前台服务
                startForegroundService()

                // 2. 再处理权限数据
                resultCode = it.getIntExtra("resultCode", 0)
                resultData = it.getParcelableExtra("data")
                if (resultCode != 0 && resultData != null) {
                    setupMediaProjection()
                }
            }
        }
        return START_STICKY
    }


    private fun setupMediaProjection() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!).apply {
            registerCallback(mediaProjectionCallback, handler)
        }

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight,
            PixelFormat.RGBA_8888, 2
        ).apply {
            setOnImageAvailableListener({
                // 这里不自动处理图像，只在需要时手动获取
            }, handler)
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun captureSingleFrame() {
        if (mediaProjection == null) {
            startScreenCapture()
            return
        }

        imageReader?.let { reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                image.use { image ->
                    val bitmap = imageToBitmap(image)
                    processCapturedBitmap(bitmap)
                }
            } else {
                Toast.makeText(this, "未能获取屏幕图像", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScreenCapture() {
        if (resultCode != 0 && resultData != null) {
            // 已经有权限，直接设置MediaProjection
            setupMediaProjection()
        } else {
            // 请求截图权限
            val intent = Intent(this, ScreenCapturePermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Capture Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Used for screen capture functionality"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Service")
            .setContentText("Capturing screen for OCR")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            stopMediaProjection()
            stopSelf()
        }
    }

    private fun stopMediaProjection() {
        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun processCapturedBitmap(bitmap: Bitmap) {
        // 这里可以将bitmap传递给OCR模块进行处理
        // 例如：ocrProcessor.process(bitmap)
        val ocrEngine = OcrEngine(this)
        val ocrResult = ocrEngine.detect(bitmap, scaleUp = true,maxSideLen=1024, padding = 20, boxScoreThresh = 0.5f, boxThresh = 0.5f, unClipRatio = 1.8f, doCls = true, mostCls = false)
        Log.d("OCR",ocrResult.text)
        // 示例：显示一个Toast表示截图成功
//        Toast.makeText(this, "截图成功，尺寸: ${bitmap.width}x${bitmap.height}", Toast.LENGTH_SHORT).show()

        // OCR处理完成后，在主线程重新显示菜单
        handler.post {
            if (menuPopup?.isShowing != true) {
                toggleMenu()
            }
        }
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
        stopMediaProjection()
        if (::ballView.isInitialized) windowManager.removeView(ballView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}