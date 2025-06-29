package com.example.agent.model.Transaction

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Float,
    val merchant: String,
    val method: String,
    val time: String,
    val timeMillis: Long
)