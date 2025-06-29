package com.example.agent.ui.AddTransaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction.Transaction
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.Companion.getInstance(this) }
    private val vm: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AddTransactionScreen(
                    onSave = { amount, merchant, method, millis ->
                        lifecycleScope.launch {
                            val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                            db.transactionDao().insert(
                                Transaction(
                                    amount = amount,
                                    merchant = merchant,
                                    method = method,
                                    time = timeFmt.format(Date(millis)),
                                    timeMillis = millis
                                )
                            )
                            finish()           // 保存完返回
                        }
                    },
                    onCancel = { finish() }   // 顶部返回按钮
                )
            }
        }
    }
}