package com.example.agent.ui

import MainScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.example.agent.util.PermissionUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) 先申请悬浮窗权限
        if (!PermissionUtils.hasOverlayPermission(this)) {
            PermissionUtils.requestOverlayPermission(this)
        } else {
            // 2) 权限已授予时直接启动服务
            startService(Intent(this, FloatingWindowService::class.java))
        }

        setContent {
            MaterialTheme {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionUtils.REQUEST_OVERLAY) {
            if (PermissionUtils.hasOverlayPermission(this)) {
                startService(Intent(this, FloatingWindowService::class.java))
            } else {
                // 可选：提示用户必须授予权限
            }
        }
    }
}


