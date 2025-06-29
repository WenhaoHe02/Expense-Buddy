package com.example.agent.model.Transaction

data class TransactionSection(
    val date: String,
    val items: List<Transaction>
)