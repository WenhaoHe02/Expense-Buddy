package com.example.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.agent.data.db.AppDatabase
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import androidx.compose.runtime.getValue

class TransactionListActivity : ComponentActivity() {

    // Factory 写法不变，或换成 Hilt 注入也行
    private val vm: TransactionViewModel by viewModels {
        TransactionViewModelFactory(AppDatabase.getInstance(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 拉一次数据（示例：过去 30 天）
        vm.loadTransactions(
            start = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
            end   = System.currentTimeMillis()
        )

        setContent {
            MaterialTheme {
                TransactionListScreen(vm = vm)
            }
        }
    }
}
