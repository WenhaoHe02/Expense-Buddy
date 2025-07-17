package com.example.agent.ui.FloatingWindow

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.agent.R
import com.example.agent.ui.FloatingWindow.MenuController

class PetService : Service() {

    companion object {
        /** 在任意 Context 启动桌宠（已做好前台适配） */
        fun start(ctx: Context, resultCode: Int, resultData: Intent) {
            val i = Intent(ctx, PetService::class.java).apply {
                action = "START_FOREGROUND"
                putExtra("resultCode", resultCode)
                putExtra("resultData", resultData)
            }
            ctx.startForegroundService(i)
        }
    }

    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var petView: View
    private lateinit var animView: LottieAnimationView
    private lateinit var menuCtrl: MenuController
    private lateinit var ocrService: OcrService

    private val size by lazy { dp(120) }
    private val slop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_FOREGROUND") {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val resultData = intent.getParcelableExtra<Intent>("resultData")

            ocrService = OcrService(this, wm)
            ocrService.setProjectionPermission(resultCode, resultData)
            ocrService.setupMediaProjection()

            menuCtrl = MenuController(this, wm, ocrService)
        }
        return START_STICKY
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        /** ① 悬浮窗权限检测 */
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(); return
        }

        /** ② 前台通知（Android 8+） */
        startAsForeground()

        /** ③ 初始化窗口与视图 */
        wm       = getSystemService(WINDOW_SERVICE) as WindowManager
        ocrService = OcrService(this, wm)
        menuCtrl = MenuController(this, wm, ocrService)

        lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.START or Gravity.TOP; y = size * 2 }

        petView  = LayoutInflater.from(this)
            .inflate(R.layout.floating_pet_layout, null, false)
        animView = petView.findViewById(R.id.pet_anim)

        animView.apply {
            setAnimation("pets/cat/idle.json")      // 仅播 idle
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        }

        wm.addView(petView, lp)
        initTouchDrag()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchDrag() {
        var downX = 0f; var downY = 0f
        var lastX = 0;  var lastY = 0
        var click  = false
        petView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    click = true
                    downX = e.rawX; downY = e.rawY
                    lastX = lp.x;   lastY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (click && (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop))
                        click = false
                    if (!click) {
                        lp.x = (lastX + dx).toInt()
                        lp.y = (lastY + dy).toInt()
                        wm.updateViewLayout(petView, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (click) {
                        if (::menuCtrl.isInitialized) {
                            menuCtrl.toggleMenu(petView)
                        }
                    } else absorbEdge()
                    true
                }
                else -> false
            }
        }
    }

    private fun absorbEdge() {
        val sw = resources.displayMetrics.widthPixels
        val viewW = petView.width.coerceAtLeast(size)   // View 已测量
        val hide = dp(10)

        lp.x = when {
            lp.x < hide - viewW + hide                  -> hide - viewW   // 左侧留 10 dp
            lp.x > sw - hide * 2                       -> sw - hide       // 右侧留 10 dp
            else                                        -> lp.x
        }
        wm.updateViewLayout(petView, lp)
    }

    override fun onDestroy() {
        if (::menuCtrl.isInitialized) {
            menuCtrl.dismissAll()
        }
        wm.removeView(petView)
        super.onDestroy()
    }

    /** 前台通知 helper */
    private fun startAsForeground() {
        val id = "pet"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(
                NotificationChannel(id, "Pet", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notif = NotificationCompat.Builder(this, id)
            .setContentTitle("Pet running")
            .setSmallIcon(R.drawable.ic_pet)
            .build()
        startForeground(1, notif)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
}
