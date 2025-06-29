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
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import com.example.agent.R
import com.example.agent.data.db.AppDatabase
import com.example.agent.databinding.FloatingMenuLayoutBinding
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ballView: View
    private lateinit var params: WindowManager.LayoutParams

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null

    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var isClick = false

    private val db by lazy { AppDatabase.Companion.getInstance(this) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val sizePx = dpToPx(30)
        val edgeThreshold = dpToPx(50)
        val hidePx = dpToPx(10)

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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
        val loc = IntArray(2).also { ballView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
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
                db.transactionDao().insert(
                    Transaction(
                        amount = amt,
                        merchant = desc,
                        method = "手动",
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

    override fun onDestroy() {
        super.onDestroy()
        menuPopup?.dismiss()
        formPopup?.dismiss()
        if (::ballView.isInitialized) windowManager.removeView(ballView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}