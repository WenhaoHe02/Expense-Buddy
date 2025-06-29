package com.example.agent.ui.AddTransaction


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加交易页面
 *
 * @param onSave 回调: (金额, 商家, 支付方式, 时间戳)
 * @param onCancel 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onSave: (Float, String, String, Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- 输入状态 ---
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("现金") }

    // --- 日期/时间状态 ---
    var dateTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }
    val dateTimeStr by remember(dateTimeMillis) {
        mutableStateOf(dateTimeFormat.format(Date(dateTimeMillis)))
    }

    // --- 弹窗控制 ---
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 日期选择器
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateTimeMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { d ->
                        dateTimeMillis = d
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // 时间选择器（使用 AlertDialog 自定义）
    if (showTimePicker) {
        val cal = remember(dateTimeMillis) {
            Calendar.getInstance().apply { timeInMillis = dateTimeMillis }
        }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    dateTimeMillis = cal.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            title = { Text("选择时间") },
            text = { TimePicker(state = timeState) }
        )
    }

    // 主界面布局
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("添加交易") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (amountText.isNotBlank()) {
                    onSave(amountText.toFloat(), merchant, method, dateTimeMillis)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "保存")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }
                },
                label = { Text("金额") },
                leadingIcon = { Text("￥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("商家") }
            )

            // 支付方式下拉
            var expanded by remember { mutableStateOf(false) }
            val methods = listOf("现金", "微信", "支付宝", "银行卡")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = method,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("支付方式") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "展开")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (m in methods) {
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                method = m
                                expanded = false
                            },
                            modifier = Modifier,
                            enabled = true,
                            colors = MenuDefaults.itemColors(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }
                }
            }

            // 日期按钮
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("日期：$dateTimeStr")
            }
        }
    }
}
