package com.example.agent.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.agent.databinding.ActivityAddTransactionBinding
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private var selectedTime: String = ""

    private val db by lazy { AppDatabase.getInstance(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置当前时间为默认时间
        selectedTime = getCurrentTimeString()
        binding.etTime.setText(selectedTime)

        // 时间选择器
        binding.etTime.setOnClickListener {
            showDateTimePicker()
        }

        // 保存
        binding.btnSave.setOnClickListener {
            val amount = binding.etAmount.text.toString().toFloatOrNull()
            val category = binding.etCategory.text.toString()

            if (amount == null) {
                Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTimestamp = System.currentTimeMillis()

            val tx = Transaction(
                amount = amount,
                merchant = category,
                method = "手动输入",
                time = selectedTime,
                timeMillis = currentTimestamp
            )

            lifecycleScope.launch {
                db.transactionDao().insert(tx)
                Toast.makeText(this@AddTransactionActivity, "保存成功！", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        // 返回
        binding.btnBackAddTransaction.setOnClickListener {
            finish()
        }
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                }
                selectedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(cal.time)
                binding.etTime.setText(selectedTime)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }
}
