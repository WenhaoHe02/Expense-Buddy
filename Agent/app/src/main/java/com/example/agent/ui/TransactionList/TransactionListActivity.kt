package com.example.agent.ui.TransactionList

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.agent.data.db.AppDatabase
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory

class TransactionListActivity : ComponentActivity() {


    private val vm: TransactionViewModel by viewModels {
        TransactionViewModelFactory(AppDatabase.Companion.getInstance(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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