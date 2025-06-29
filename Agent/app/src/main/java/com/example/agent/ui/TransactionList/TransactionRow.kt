package com.example.agent.ui.TransactionList

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.agent.model.Transaction.Transaction

@Composable
fun TransactionRow(
    tx: Transaction,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("￥${tx.amount}", style = MaterialTheme.typography.titleMedium)
            Text(tx.merchant,     style = MaterialTheme.typography.bodyMedium)
            Text(tx.method,       style = MaterialTheme.typography.bodySmall)
        }
        Text(tx.time, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTransactionRow() {
    MaterialTheme {          // 先用系统默认主题包一层
        TransactionRow(
            tx = Transaction(
                amount = 52f,
                merchant = "Starbucks",
                method = "WeChat Pay",
                time = "2025-06-20 14:12",
                timeMillis = System.currentTimeMillis()
            )
        )
    }
}

