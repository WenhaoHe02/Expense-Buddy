package com.example.agent.ui.AddTransaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getInstance(this) }
    private val vm: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AddTransactionScreen(
                    onSave = { amount: Float, merchant: String, note: String, classification: Classification, millis: Long ->
                        lifecycleScope.launch {
                            val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                            val transaction = Transaction(
                                amount = amount,
                                merchant = merchant,
                                note = note,
                                classification = classification,
                                time = timeFmt.format(Date(millis)),
                                timeMillis = millis
                            )
                            db.transactionDao().insert(transaction)
                            finish()
                        }
                    } ,
                    onCancel = { finish() }
                )
            }
        }
    }
}
