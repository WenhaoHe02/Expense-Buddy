package com.example.agent.ui.FloatingWindow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.benjaminwan.ocrlibrary.OcrEngine
import com.example.agent.databinding.FloatingTransactionFormLayoutBinding
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction
import com.example.agent.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OcrService(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var resultCode = 0
    private var resultData: android.content.Intent? = null

    private var formPopup: PopupWindow? = null
    private val db by lazy { AppDatabase.getInstance(context) }

    private val screenWidth by lazy { context.resources.displayMetrics.widthPixels }
    private val screenHeight by lazy { context.resources.displayMetrics.heightPixels }
    private val density by lazy { context.resources.displayMetrics.densityDpi }
    fun isInitialized(): Boolean {
        return mediaProjection != null && imageReader != null
    }
    fun setProjectionPermission(resultCode: Int, resultData: android.content.Intent?) {
        this.resultCode = resultCode
        this.resultData = resultData
    }

    fun setupMediaProjection() {
        if (resultCode == 0 || resultData == null) return
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, resultData!!)
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    fun captureAndRecognize(anchorView: View) {
        if (imageReader == null) {
            Toast.makeText(context, "未初始化 OCR 权限", Toast.LENGTH_SHORT).show()
            return
        }
        val image = imageReader?.acquireLatestImage()
        if (image != null) {
            image.use {
                val bitmap = imageToBitmap(it)
                processCapturedBitmap(bitmap, anchorView)
            }
        } else {
            Toast.makeText(context, "无法获取屏幕图像", Toast.LENGTH_SHORT).show()
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun processCapturedBitmap(bitmap: Bitmap, anchorView: View) {
        val ocrEngine = OcrEngine(context)
        val result = ocrEngine.detect(bitmap, true, 1024, 20, 0.5f, 0.5f, 1.8f, true, false)
        val text = result.text.replace("手动记账", "").replace("OCR识别", "").trim()
        val desc = text.substringAfter("支付成功", "手动").trim()
        val amount = Regex("""\d+\.\d{2}""").find(text)?.value ?: "0"
        showOcrForm(anchorView, desc, amount)
    }

    private fun showOcrForm(anchorView: View, payee: String, amount: String) {
        formPopup?.dismiss()
        val binding = FloatingTransactionFormLayoutBinding.inflate(LayoutInflater.from(context))
        binding.etMerchant.setText(payee)
        binding.etAmount.setText(amount.replace(".00", ""))

        // 分类 Spinner 初始化
        val classifications = Classification.values().map { it.label }
        val adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, classifications)
        binding.spinnerClassification.adapter = adapter

        formPopup = PopupWindow(binding.root, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = false
            windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        binding.btnSave.setOnClickListener {
            val amt = binding.etAmount.text.toString().toFloatOrNull() ?: 0f
            val merchant = binding.etMerchant.text.toString().ifBlank { "" }
            val note = binding.etNote.text.toString().ifBlank { "" }
            val classification = Classification.values()[binding.spinnerClassification.selectedItemPosition]

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
        formPopup?.showAtLocation(anchorView, Gravity.NO_GRAVITY, loc[0] + dp2px(30), loc[1] + dp2px(30))
    }

    fun release() {
        formPopup?.dismiss()
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
        mediaProjection?.stop(); mediaProjection = null
    }

    private fun dp2px(dp: Int): Int = (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}
