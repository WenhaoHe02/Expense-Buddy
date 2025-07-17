package com.example.agent.ui.FloatingWindow

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import com.example.agent.R
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ballView: View
    private lateinit var params: WindowManager.LayoutParams

    private lateinit var menuCtrl: MenuController
    private lateinit var ocrService: OcrService

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private val sizePx by lazy { dpToPx(60) }
    private val edgeTh by lazy { dpToPx(50) }
    private val hidePx by lazy { dpToPx(10) }
    private var isClick = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        setupFloatingBall()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_FOREGROUND") {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val resultData = intent.getParcelableExtra<Intent>("resultData")

            ocrService = OcrService(this, windowManager)
            ocrService.setProjectionPermission(resultCode, resultData)
            ocrService.setupMediaProjection()

            menuCtrl = MenuController(this, windowManager, ocrService)
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBall() {
        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dpToPx(200)
        }

        ballView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null)
        windowManager.addView(ballView, params)

        initTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListener() {
        var downX = 0f
        var downY = 0f
        var lastX = 0
        var lastY = 0

        ballView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    isClick = true
                    downX = e.rawX
                    downY = e.rawY
                    lastX = params.x
                    lastY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (isClick && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isClick = false
                    }
                    if (!isClick) {
                        params.x = (lastX + dx).toInt()
                        params.y = (lastY + dy).toInt()
                        windowManager.updateViewLayout(ballView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick && ::menuCtrl.isInitialized) {
                        menuCtrl.toggleMenu(ballView)
                    }
                    else absorbEdge()
                    true
                }
                else -> false
            }
        }
    }

    private fun absorbEdge() {
        val screenW = resources.displayMetrics.widthPixels
        val centerX = params.x + sizePx / 2
        params.x = when {
            centerX <= edgeTh -> hidePx - sizePx
            centerX >= screenW - edgeTh -> screenW - hidePx
            else -> params.x
        }
        windowManager.updateViewLayout(ballView, params)
    }

    override fun onDestroy() {
        menuCtrl.dismissAll()
        windowManager.removeView(ballView)
        super.onDestroy()
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
