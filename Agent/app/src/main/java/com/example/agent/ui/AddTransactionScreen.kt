package com.example.agent.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope

@Composable
fun AddTransactionScreen(
    onSave: (Float, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf(currentTimeString()) }
    var category by remember { mutableStateOf("") }

    fun showDateTimePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                }
                timeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(cal.time)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("金额(¥)") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = timeText,
            onValueChange = {},
            label = { Text("时间") },
            modifier = Modifier.clickable { showDateTimePicker() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("种类 / 店铺") }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            val amt = amount.toFloatOrNull()
            if (amt != null) {
                onSave(amt, timeText, category)
            }
        }) {
            Text("保存")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text("返回")
        }
    }
}

private fun currentTimeString(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
