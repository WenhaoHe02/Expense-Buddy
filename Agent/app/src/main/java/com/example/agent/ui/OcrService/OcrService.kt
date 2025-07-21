package com.example.agent.ui.OcrService

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import com.benjaminwan.ocrlibrary.OcrEngine
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction
import com.example.agent.data.db.AppDatabase
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class OcrService(
    private val context: Context
) {
    private var formPopup: PopupWindow? = null
    private val db by lazy { AppDatabase.getInstance(context) }

    @RequiresApi(Build.VERSION_CODES.R)
    fun captureAndRecognize(anchorView: View) {
        val service = ScreenCaptureAccessibilityService.get()
        if (service == null) {
            Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            return
        }

        service.captureScreen { bitmap ->
            processCapturedBitmap(bitmap, anchorView)
        }
    }

    private fun processCapturedBitmap(bitmap: Bitmap, anchorView: View) {
        Toast.makeText(context,"正在检测，请稍等",Toast.LENGTH_LONG).show()
        val ocrEngine = OcrEngine(context)
        val result = ocrEngine.detect(bitmap, true, 1024, 20, 0.5f, 0.5f, 1.8f, true, false)
        val text = result.text.replace("手动记账", "").replace("OCR识别", "").trim()
        val desc = text.substringAfter("支付成功", "手动").trim()
        val amount = Regex("""\d+\.\d{2}""").find(text)?.value ?: "0"
        if(amount!="0"){
            showOcrForm(anchorView, desc, amount)
        }else{
            Toast.makeText(context,"未检测到有效支付信息",Toast.LENGTH_LONG).show()
        }
    }

    private fun showOcrForm(anchorView: View, payee: String, amount: String) {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(
            LayoutInflater.from(context)
        )

        binding.etMerchant.setText(payee)
        binding.etAmount.setText(amount.replace(".00", ""))

        val classifications = Classification.entries.map { it.label }
        binding.spinnerClassification.adapter = android.widget.ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            classifications
        )

        formPopup = PopupWindow(
            binding.root,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = false
        }

        binding.btnSave.setOnClickListener {
            val amt = binding.etAmount.text.toString().toFloatOrNull() ?: 0f
            val merchant = binding.etMerchant.text.toString()
            val note = binding.etNote.text.toString()
            val classification = Classification.entries[binding.spinnerClassification.selectedItemPosition]

            CoroutineScope(Dispatchers.IO).launch {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                val now = Date()
                db.transactionDao().insert(
                    Transaction(
                        amount = amt,
                        merchant = merchant,
                        note = note,
                        classification = classification,
                        time = fmt.format(now),
                        timeMillis = now.time
                    )
                )
            }
            formPopup?.dismiss()
        }

        binding.btnCancel.setOnClickListener { formPopup?.dismiss() }

        val loc = IntArray(2).also { anchorView.getLocationOnScreen(it) }
        formPopup?.showAtLocation(
            anchorView,
            android.view.Gravity.NO_GRAVITY,
            loc[0] + dp2px(30),
            loc[1] + dp2px(30)
        )
    }

    private fun dp2px(dp: Int): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    fun release() {
        formPopup?.dismiss()
    }
}