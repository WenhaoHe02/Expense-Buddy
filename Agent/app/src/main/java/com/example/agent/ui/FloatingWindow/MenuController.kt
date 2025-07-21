package com.example.agent.ui.FloatingWindow

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import com.example.agent.data.db.AppDatabase
import com.example.agent.databinding.FloatingMenuLayoutBinding
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction
import com.example.agent.ui.OcrService.OcrService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuController(
    private val ctx: Context,
    private val ocrController: OcrService  // ✅ 注入 OCR 控制器
) {

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null
    private val db by lazy { AppDatabase.getInstance(ctx) }

    /** 点击桌宠显示菜单 */
    @RequiresApi(Build.VERSION_CODES.R)
    fun toggleMenu(anchorView: View) {
        menuPopup?.let { if (it.isShowing) { it.dismiss(); return } }

        val binding = FloatingMenuLayoutBinding.inflate(LayoutInflater.from(ctx))
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

        // ✅ 手动录入入口
        binding.menuManualEntry.setOnClickListener {
            menuPopup?.dismiss()
            showForm(anchorView)
        }

        // ✅ OCR 入口：判断是否已授权
        binding.menuOcr.setOnClickListener {
            menuPopup?.dismiss()
            ocrController.captureAndRecognize(anchorView)
        }

        // 弹出位置：锚点右下角偏移
        val loc = IntArray(2).also { anchorView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            loc[0] + dp2px(30), loc[1] + dp2px(30)
        )
    }

    /** 手动记账表单弹窗 */
    private fun showForm(anchorView: View) {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(ctx))
        val classifications = Classification.entries.map { it.label }
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, classifications)
        binding.spinnerClassification.adapter = adapter

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
            saveTransaction(binding)
            formPopup?.dismiss()
        }

        binding.btnCancel.setOnClickListener { formPopup?.dismiss() }

        val loc = IntArray(2).also { anchorView.getLocationOnScreen(it) }
        formPopup?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            loc[0] + dp2px(30), loc[1] + dp2px(30)
        )
    }

    /** 数据保存逻辑 */
    private fun saveTransaction(b: FloatingTransactionFormLayoutBinding) {
        val amount = b.etAmount.text.toString().toFloatOrNull() ?: 0f
        val merchant = b.etMerchant.text.toString().ifBlank { "" }
        val note = b.etNote.text.toString().ifBlank { "" }
        val classification = Classification.entries[b.spinnerClassification.selectedItemPosition]

        CoroutineScope(Dispatchers.IO).launch {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            val now = Date()
            db.transactionDao().insert(
                Transaction(
                    amount = amount,
                    merchant = merchant,
                    note = note,
                    classification = classification,
                    time = fmt.format(now),
                    timeMillis = now.time
                )
            )
        }
    }

    fun dismissAll() {
        menuPopup?.dismiss()
        formPopup?.dismiss()
    }

    private fun dp2px(dp: Int): Int =
        (dp * ctx.resources.displayMetrics.density + 0.5f).toInt()
}
