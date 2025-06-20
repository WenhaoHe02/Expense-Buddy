package com.example.agent.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider       // ← Material3 的 Divider 名字改了
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agent.model.Transaction
import com.example.agent.viewmodel.GroupMode
import com.example.agent.viewmodel.TransactionViewModel

/* ------- 整个列表页：顶部 Tab + 分组列表 ------- */
@Composable
fun TransactionListScreen(vm: TransactionViewModel) {

    val mode     by vm.groupMode.observeAsState(GroupMode.DAY)
    val sections by vm.grouped.observeAsState(emptyMap())

    Column(Modifier.fillMaxSize()) {

        /* ---- 1. 切换 日 / 周 / 月 ---- */
        TabRow(selectedTabIndex = mode.ordinal) {
            GroupMode.values().forEach { m ->
                Tab(
                    selected = m == mode,
                    onClick  = { vm.setGroupMode(m) },
                    text     = { Text(m.name) }
                )
            }
        }

        /* ---- 2. 带 stickyHeader 的分组列表 ---- */
        SectionedTransactionList(
            sections = sections,
            modifier = Modifier.weight(1f)    // 列表占满剩余空间
        )
    }
}

/* ------- 分组 + 区头 + 列表项 ------- */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SectionedTransactionList(
    sections: Map<String, List<Transaction>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize()) {
        sections.forEach { (header, list) ->

            /* 区头：sticky 停靠效果 */
            stickyHeader {
                Text(
                    header,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            /* 每一条交易记录 */
            items(list, key = { it.id }) { tx ->
                TransactionRow(tx)
                HorizontalDivider()           // Material3 的 Divider
            }
        }
    }
}
