package com.example.agent.ui.FloatingWindow

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import com.example.agent.data.db.AppDatabase
import com.example.agent.databinding.FloatingMenuLayoutBinding
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MenuController(
    private val ctx: Context,
    private val windowMgr: WindowManager,
    private val ocrController: OcrService  // ✅ 新增 OCR 注入
) {

    private var menuPopup: PopupWindow? = null
    private var formPopup: PopupWindow? = null
    private val db by lazy { AppDatabase.getInstance(ctx) }

    /** 点击 anchorView 时调用，可自动切换显/隐 */
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

        // ✅ 手动录入
        binding.menuManualEntry.setOnClickListener {
            menuPopup?.dismiss()
            showForm(anchorView)
        }

        // ✅ OCR 识别入口
        binding.menuOcr.setOnClickListener {
            menuPopup?.dismiss()
            ocrController.captureAndRecognize(anchorView)
        }

        // 显示菜单位置：锚点右下角 30dp 偏移
        val loc = IntArray(2).also { anchorView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            loc[0] + dp2px(30), loc[1] + dp2px(30)
        )
    }

    /** 手动记账表单 */
    private fun showForm(anchorView: View) {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(ctx))
        val classifications = Classification.values().map { it.label }
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

    private fun saveTransaction(b: FloatingTransactionFormLayoutBinding) {
        val amount = b.etAmount.text.toString().toFloatOrNull() ?: 0f
        val merchant = b.etMerchant.text.toString().ifBlank { "" }
        val note = b.etNote.text.toString().ifBlank { "" }
        val classification = Classification.values()[b.spinnerClassification.selectedItemPosition]

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

    private fun dp2px(dp: Int) =
        (dp * ctx.resources.displayMetrics.density + 0.5f).toInt()
}
