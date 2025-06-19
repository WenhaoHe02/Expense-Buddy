package com.example.agent.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.agent.databinding.ItemTransactionBinding
import com.example.agent.model.Transaction

class TransactionItemAdapter : RecyclerView.Adapter<TransactionItemAdapter.ViewHolder>() {

    private val list = mutableListOf<Transaction>()

    fun submitList(newList: List<Transaction>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = list[position]
        holder.binding.textAmount.text = "￥%.2f".format(tx.amount)
        holder.binding.textMerchant.text = tx.merchant
        holder.binding.textTime.text = tx.time
    }

    override fun getItemCount(): Int = list.size
}

