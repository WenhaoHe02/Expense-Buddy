package com.example.agent.ui

import MainScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen(
                    onAddClick = {
                        startActivity(Intent(this, AddTransactionActivity::class.java))
                    },
                    onListClick = {
                        startActivity(Intent(this, TransactionListActivity::class.java))
                    }
                )
            }
        }
    }
}
