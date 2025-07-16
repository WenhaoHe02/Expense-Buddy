package com.example.agent.ui.FloatingWindow

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.example.agent.R
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ballView     : View
    private lateinit var params       : WindowManager.LayoutParams
    private lateinit var menuCtrl     : MenuController   // 统一菜单控制器

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private val sizePx    by lazy { dpToPx(60) }          // 球尺寸
    private val edgeTh    by lazy { dpToPx(50) }
    private val hidePx    by lazy { dpToPx(10) }
    private var isClick   = false

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        menuCtrl      = MenuController(this, windowManager)    // 先初始化

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; y = dpToPx(200) }

        ballView = LayoutInflater.from(this)
            .inflate(R.layout.floating_window_layout, null)
        windowManager.addView(ballView, params)

        initTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListener() {
        var downX = 0f; var downY = 0f
        var lastX = 0;  var lastY = 0

        ballView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    isClick = true
                    downX = e.rawX; downY = e.rawY
                    lastX = params.x; lastY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (isClick && (abs(dx) > touchSlop || abs(dy) > touchSlop))
                        isClick = false
                    if (!isClick) {
                        params.x = (lastX + dx).toInt()
                        params.y = (lastY + dy).toInt()
                        windowManager.updateViewLayout(ballView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) menuCtrl.toggleMenu(ballView)
                    else absorbEdge()
                    true
                }
                else -> false
            }
        }
    }

    /** 吸边并半隐藏 */
    private fun absorbEdge() {
        val screenW = resources.displayMetrics.widthPixels
        val centerX = params.x + sizePx / 2
        params.x = when {
            centerX <= edgeTh           -> hidePx - sizePx
            centerX >= screenW - edgeTh -> screenW - hidePx
            else                        -> params.x
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