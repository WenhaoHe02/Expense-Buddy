package com.example.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.agent.model.TransactionSection
import com.example.agent.util.TimeRangeUtil
import com.example.agent.viewmodel.TransactionViewModel

@Composable
fun TransactionListScreen(viewModel: TransactionViewModel, onBack: () -> Unit) {
    val grouped by viewModel.groupedTransactions.observeAsState(initial = emptyMap())
    val sections = grouped.entries.sortedByDescending { it.key }.map { (d, list) ->
        TransactionSection(d, list)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxSize().weight(0f)) {
            Button(onClick = {
                val (s, e) = TimeRangeUtil.getTodayRange()
                viewModel.loadTransactions(s, e)
            }) { Text("今天") }
            Button(onClick = {
                val (s, e) = TimeRangeUtil.getThisWeekRange()
                viewModel.loadTransactions(s, e)
            }) { Text("本周") }
            Button(onClick = {
                val (s, e) = TimeRangeUtil.getThisMonthRange()
                viewModel.loadTransactions(s, e)
            }) { Text("本月") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sections) { section ->
                Text(text = section.date)
                section.items.forEach { tx ->
                    Text(text = "￥%.2f - %s - %s".format(tx.amount, tx.merchant, tx.time))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) { Text("返回") }
    }
}
