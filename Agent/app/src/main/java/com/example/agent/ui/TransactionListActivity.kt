package com.example.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.agent.data.db.AppDatabase
import com.example.agent.ui.theme.AppTheme
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory

class TransactionListActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.getInstance(this) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                TransactionListScreen(viewModel) { finish() }
            }
        }
        val (s, e) = com.example.agent.util.TimeRangeUtil.getTodayRange()
        viewModel.loadTransactions(s, e)
    }
}
