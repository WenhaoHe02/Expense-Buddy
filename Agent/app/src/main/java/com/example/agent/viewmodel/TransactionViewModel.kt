package com.example.agent.viewmodel

import androidx.lifecycle.*
import com.example.agent.data.db.AppDatabase
import com.example.agent.model.Transaction.Transaction
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/** 三种分组模式 */
enum class GroupMode { DAY, WEEK, MONTH }

class TransactionViewModel(
    private val db: AppDatabase
) : ViewModel() {

    /* ① 原始列表 */
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    /* ② 当前选中的分组模式（默认：DAY） */
    private val _groupMode = MutableLiveData(GroupMode.DAY)
    val groupMode: LiveData<GroupMode> = _groupMode

    /* ③ 分好组的 Map<Header, List<Transaction>> */
    val grouped: LiveData<Map<String, List<Transaction>>> =
        MediatorLiveData<Map<String, List<Transaction>>>().apply {
            fun update() {
                val list = _transactions.value ?: emptyList()
                val mode = _groupMode.value ?: GroupMode.DAY
                value = list.groupBy { it.headerKey(mode) }
                    .toSortedMap(compareByDescending { it })   // 最近在上
            }
            addSource(_transactions) { update() }
            addSource(_groupMode)    { update() }
        }

    /* ④ 暴露切换模式的方法 */
    fun setGroupMode(mode: GroupMode) = _groupMode.postValue(mode)

    /* ⑤ 原有加载数据库函数保持不变 */
    fun loadTransactions(start: Long, end: Long) {
        viewModelScope.launch {
            _transactions.value = db.transactionDao().getTransactionsBetween(start, end)
        }
    }
}

/* ----------- 私有扩展：把 Transaction 映射成 header key ----------- */
private val dayFmt   = DateTimeFormatter.ISO_DATE              // yyyy-MM-dd
private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")  // yyyy-MM

private fun Transaction.headerKey(mode: GroupMode): String = when (mode) {
    GroupMode.DAY -> time.substring(0, 10)                     // 直接截字符串
    GroupMode.MONTH -> time.substring(0, 7)                    // yyyy-MM

    GroupMode.WEEK -> {
        val date = LocalDate.parse(time.substring(0, 10), dayFmt)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        "${date.year}-W${"%02d".format(week)}"                 // 例：2025-W25
    }
}
class TransactionViewModelFactory(
    private val db: AppDatabase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(db) as T
    }
}