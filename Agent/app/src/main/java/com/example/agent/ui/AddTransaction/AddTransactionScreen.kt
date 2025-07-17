package com.example.agent.ui.AddTransaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.agent.model.Transaction.Classification
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onSave: (Float, String, String, Classification, Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf(Classification.OTHER) }

    var dateTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }
    val dateTimeStr by remember(dateTimeMillis) {
        mutableStateOf(dateTimeFormat.format(Date(dateTimeMillis)))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
                    onSave(
                        amountText.toFloat(),
                        merchant,
                        note,
                        classification,
                        dateTimeMillis
                    )
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

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") }
            )

            // 分类选择 Spinner 模拟
            var classExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = classification.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "展开")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { classExpanded = true }
                )
                DropdownMenu(
                    expanded = classExpanded,
                    onDismissRequest = { classExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Classification.values().forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls.label) },
                            onClick = {
                                classification = cls
                                classExpanded = false
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

            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("日期：$dateTimeStr")
            }
        }
    }
}