package com.example.agent.ui
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import kotlin.math.abs

class FloatingWindowService : Service() {
    // 截图相关变量
    private lateinit var windowManager: WindowManager
    private lateinit var ballView: android.view.View
    private lateinit var params: WindowManager.LayoutParams

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var isClick = false

    private val db by lazy { AppDatabase.getInstance(this) }
    private val screenshotReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ScreenCaptureAccessibilityService.ACTION_SCREENSHOT) {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(ScreenCaptureAccessibilityService.EXTRA_BITMAP, Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(ScreenCaptureAccessibilityService.EXTRA_BITMAP)
                }
                if (bmp != null) {
                    processCapturedBitmap(bmp)
                } else {
                    Toast.makeText(this@FloatingWindowService, "截图失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreate() {
        super.onCreate()
        // 注册广播
        LocalBroadcastManager.getInstance(this).registerReceiver(
            screenshotReceiver,
            IntentFilter(ScreenCaptureAccessibilityService.ACTION_SCREENSHOT)
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val sizePx = dpToPx(30)
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
                    downX = ev.rawX.toInt()
                    downY = ev.rawY.toInt()
                    lastX = params.x
                    lastY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX.toInt() - downX
                    val dy = ev.rawY.toInt() - downY
                    if (isClick && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
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
                        val edgeThreshold = dpToPx(50)
                        val hidePx = dpToPx(10)
                        params.x = when {
                            centerX <= edgeThreshold -> hidePx - sizePx
                            centerX >= screenW - edgeThreshold -> screenW - hidePx
                            else -> params.x
                        }
                        windowManager.updateViewLayout(ballView, params)
                    }
                    true
                }
                else -> false
            }
        }
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
            requestScreenshot()
        }

        val loc = IntArray(2).also { ballView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            ballView,
            Gravity.NO_GRAVITY,
            loc[0] + dpToPx(30),
            loc[1] + dpToPx(30)
        )
    }
    /* 通过无障碍服务截图 */
    private fun requestScreenshot() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先开启无障碍权限", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }
        // 触发无障碍服务截图
        val intent = Intent(this, ScreenCaptureAccessibilityService::class.java).apply {
            action = "CAPTURE"
        }
        startService(intent)
    }


    private fun processCapturedBitmap(bitmap: Bitmap) {
        val ocrEngine = OcrEngine(this)
        val ocrResult = ocrEngine.detect(bitmap, scaleUp = true, maxSideLen = 1024, padding = 20,
            boxScoreThresh = 0.5f, boxThresh = 0.5f, unClipRatio = 1.8f, doCls = true, mostCls = false)
        val cleanedText = ocrResult.text
            .replace("手动记账", "")
            .replace("OCR识别", "")
            .trim()
        Log.d("OCR_Result",cleanedText)
        // 收款方提取
        val desc = cleanedText.substringAfter("支付成功").trim()

        // 金额提取（直接匹配数字+小数点+两位数字）
        val amount = Regex("""\d+\.\d{2}""").find(cleanedText)?.value

        // 打印提取结果方便调试
        Log.d("PaymentInfo", "备注 $desc, 金额: $amount")
        if(amount!=null) showOcrForm(desc, amount)
    }

    @SuppressLint("InflateParams")
    private fun showOcrForm(defaultPayee: String = "手动", defaultAmount: String = "0") {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(this))

        // 设置表单默认值
        binding.etDescription.setText(defaultPayee)
        binding.etAmount.setText(defaultAmount.replace(".00", ""))

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
            val amt: Float = binding.etAmount.text.toString().toFloatOrNull() ?: 0f
            val desc = binding.etDescription.text.toString().ifBlank { "手动" }

            CoroutineScope(Dispatchers.IO).launch {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                val now = Date()
                db.transactionDao().insert(
                    Transaction(
                        amount = amt,
                        merchant = desc,
                        method = "Ocr扫描",
                        time = fmt.format(now),
                        timeMillis = now.time
                    )
                )
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(screenshotReceiver)
        if (::ballView.isInitialized) windowManager.removeView(ballView)
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName + "/" + ScreenCaptureAccessibilityService::class.java.name)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}