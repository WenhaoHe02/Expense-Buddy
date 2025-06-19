package com.example.agent.ui


import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agent.databinding.ActivityTransactionListBinding
import com.example.agent.data.db.AppDatabase
import com.example.agent.util.TimeRangeUtil
import com.example.agent.viewmodel.TransactionViewModel
import com.example.agent.viewmodel.TransactionViewModelFactory
import com.example.agent.ui.adapter.TransactionAdapter
import android.widget.Button
import com.example.agent.model.TransactionSection
import com.example.agent.ui.adapter.TransactionSectionAdapter

class TransactionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionListBinding
    private val db by lazy { AppDatabase.getInstance(this) }

    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(db)
    }

    private val sectionAdapter = TransactionSectionAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionListBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = sectionAdapter

        binding.btnBack.setOnClickListener {
            finish()
        }


        binding.btnToday.setOnClickListener {
            val (start, end) = TimeRangeUtil.getTodayRange()
            viewModel.loadTransactions(start, end)
        }

        binding.btnWeek.setOnClickListener {
            val (start, end) = TimeRangeUtil.getThisWeekRange()
            viewModel.loadTransactions(start, end)
        }

        binding.btnMonth.setOnClickListener {
            val (start, end) = TimeRangeUtil.getThisMonthRange()
            viewModel.loadTransactions(start, end)
        }

        binding.btnToday.performClick()

        viewModel.groupedTransactions.observe(this) { groupedMap ->
            val sectionList = groupedMap.entries.sortedByDescending { it.key }.map { (date, list) ->
                TransactionSection(date, list)
            }
            sectionAdapter.submitList(sectionList)
        }

    }
}
