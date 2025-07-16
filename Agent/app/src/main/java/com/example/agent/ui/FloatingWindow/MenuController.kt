package com.example.agent.ui.FloatingWindow
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import com.example.agent.data.db.AppDatabase
import com.example.agent.databinding.FloatingMenuLayoutBinding
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.Service

class MenuController( private val ctx: Context,
                      private val windowMgr: WindowManager  )  {
       // 由调用方注入

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
        binding.menuManualEntry.setOnClickListener {
            menuPopup?.dismiss()
            showForm(anchorView)
        }
        // 位置：锚点右下角 30dp
        val loc = IntArray(2).also { anchorView.getLocationOnScreen(it) }
        menuPopup?.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            loc[0] + dp2px(30), loc[1] + dp2px(30)
        )
    }

    /** 记账表单 */
    private fun showForm(anchorView: View) {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(ctx))
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

    /** 入库逻辑独立出来，任何服务复用 */
    private fun saveTransaction(b: FloatingTransactionFormLayoutBinding) {
        val amount = b.etAmount.text.toString().toFloatOrNull() ?: 0f
        val desc   = b.etDescription.text.toString().ifBlank { "手动" }
        CoroutineScope(Dispatchers.IO).launch {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            val now = Date()
            db.transactionDao().insert(
                Transaction(
                    amount = amount,
                    merchant = desc,
                    method = "手动",
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

    /** 工具函数 */
    private fun dp2px(dp: Int) =
        (dp * ctx.resources.displayMetrics.density + 0.5f).toInt()

}