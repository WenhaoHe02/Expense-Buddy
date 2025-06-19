package com.example.agent.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.agent.databinding.ItemTransactionBinding
import com.example.agent.model.Transaction

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TViewHolder>() {

    private var items = listOf<Transaction>()

    fun submitList(data: List<Transaction>) {
        items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    class TViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tx: Transaction) {
            binding.textAmount.text = "￥${tx.amount}"
            binding.textMerchant.text = tx.merchant
            binding.textTime.text = tx.time
        }
    }
}
