package com.example.agent.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

object PermissionUtils {
    const val REQUEST_OVERLAY = 1000

    /** 是否已拥有悬浮窗权限 */
    fun hasOverlayPermission(activity: Activity): Boolean =
        Settings.canDrawOverlays(activity)

    /** 发起系统悬浮窗权限申请 */
    fun requestOverlayPermission(activity: Activity) {
        if (!Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${activity.packageName}".toUri()
            )
            activity.startActivityForResult(intent, REQUEST_OVERLAY)
        }
    }
}
