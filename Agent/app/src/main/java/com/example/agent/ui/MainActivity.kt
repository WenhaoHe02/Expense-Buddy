package com.example.agent.ui

import MainScreen
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.agent.ui.AddTransaction.AddTransactionActivity
import com.example.agent.ui.FloatingWindow.PetService
import com.example.agent.ui.TransactionList.TransactionListActivity
import com.example.agent.util.PermissionUtils

class MainActivity : ComponentActivity() {

    /** 使用 Activity-result API 替代 onActivityResult（已弃用） */
    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasOverlayPermission(this)) {
            startPetService()               // 用户授予权限 → 启动桌宠
        } else {
            Toast.makeText(
                this,
                "未授予悬浮窗权限，桌宠功能不可用",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ① 检查 / 请求 SYSTEM_ALERT_WINDOW 权限
        if (PermissionUtils.hasOverlayPermission(this)) {
            startPetService()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayLauncher.launch(intent)
        }
        if(!isAccessibilityServiceEnabled()){
            Toast.makeText(
                this,
                "请开启无障碍权限以使用屏幕识别功能",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySetting()
        }

        // ② 普通界面
        setContent {
            MaterialTheme {                 // 已使用 material3 依赖时保持不变
                MainScreen(
                    onAddClick = {
                        startActivity(Intent(this, AddTransactionActivity::class.java))
                    },
                    onListClick = {
                        startActivity(Intent(this, TransactionListActivity::class.java))
                    }
                )
            }
        }
    }

    /** Android 8+ 必须用 startForegroundService，否则 5 秒内会被系统杀掉 */
    private fun startPetService() {
        val intent = Intent(this, PetService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
    fun openAccessibilitySetting() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }
}
