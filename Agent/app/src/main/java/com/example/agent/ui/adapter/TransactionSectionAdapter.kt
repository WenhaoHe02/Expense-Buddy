package com.example.agent.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agent.databinding.ItemSectionBinding
import com.example.agent.model.TransactionSection

class TransactionSectionAdapter : RecyclerView.Adapter<TransactionSectionAdapter.SectionViewHolder>() {

    private val sections = mutableListOf<TransactionSection>()

    fun submitList(list: List<TransactionSection>) {
        sections.clear()
        sections.addAll(list)
        notifyDataSetChanged()
    }

    class SectionViewHolder(val binding: ItemSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.binding.tvDate.text = section.date
        val itemAdapter = TransactionItemAdapter()
        holder.binding.rvItems.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.binding.rvItems.adapter = itemAdapter
        itemAdapter.submitList(section.items)
    }

    override fun getItemCount(): Int = sections.size
}
