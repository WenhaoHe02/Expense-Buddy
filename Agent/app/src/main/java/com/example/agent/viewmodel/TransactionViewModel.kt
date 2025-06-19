package com.example.agent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction
import kotlinx.coroutines.launch




class TransactionViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    val groupedTransactions = MediatorLiveData<Map<String, List<Transaction>>>().apply {
        addSource(_transactions) { list ->
            value = list.groupBy { it.time.substring(0, 10) }
        }
    }

    fun loadTransactions(start: Long, end: Long) {
        viewModelScope.launch {
            val list = db.transactionDao().getTransactionsBetween(start, end)
            _transactions.value = list
        }
    }
}

class TransactionViewModelFactory(
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(db) as T
    }
}

