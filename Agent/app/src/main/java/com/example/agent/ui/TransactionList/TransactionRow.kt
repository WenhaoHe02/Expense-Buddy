package com.example.agent.ui.TransactionList

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.agent.model.Transaction.Classification
import com.example.agent.model.Transaction.Transaction

@Composable
fun TransactionRow(
    tx: Transaction,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("￥${tx.amount}", style = MaterialTheme.typography.titleMedium)
            Text(tx.time, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(4.dp))
        Text("商家：${tx.merchant}", style = MaterialTheme.typography.bodyMedium)
        Text("分类：${tx.classification.label}", style = MaterialTheme.typography.bodySmall)
        if (tx.note.isNotBlank()) {
            Text("备注：${tx.note}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
