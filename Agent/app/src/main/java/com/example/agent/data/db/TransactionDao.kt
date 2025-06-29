package com.example.agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.agent.model.Transaction.Transaction

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY time DESC")
    suspend fun getAll(): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getTotalSpent(): Float?

    @Query("SELECT * FROM transactions WHERE timeMillis BETWEEN :start AND :end ORDER BY timeMillis DESC")
    suspend fun getTransactionsBetween(start: Long, end: Long): List<Transaction>

}
