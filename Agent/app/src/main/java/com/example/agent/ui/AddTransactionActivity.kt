package com.example.agent.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction
import com.example.agent.ui.theme.AppTheme
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch

class AddTransactionActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.getInstance(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AddTransactionScreen(
                    onSave = { amount, time, category ->
                        val tx = Transaction(
                            amount = amount,
                            merchant = category,
                            method = "手动输入",
                            time = time,
                            timeMillis = System.currentTimeMillis()
                        )
                        lifecycleScope.launch {
                            db.transactionDao().insert(tx)
                            Toast.makeText(this@AddTransactionActivity, "保存成功！", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
