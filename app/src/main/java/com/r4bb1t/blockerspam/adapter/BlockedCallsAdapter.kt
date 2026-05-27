package com.r4bb1t.blockerspam.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.r4bb1t.blockerspam.data.BlockedCallSummary
import com.r4bb1t.blockerspam.databinding.ItemBlockedCallBinding
import com.r4bb1t.blockerspam.helper.NumberInfoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockedCallsAdapter(
    private val onClick: (BlockedCallSummary) -> Unit
) : ListAdapter<BlockedCallSummary, BlockedCallsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BlockedCallSummary>() {
            override fun areItemsTheSame(a: BlockedCallSummary, b: BlockedCallSummary) =
                a.number == b.number
            override fun areContentsTheSame(a: BlockedCallSummary, b: BlockedCallSummary) =
                a == b
        }
        private val DATE_FMT = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    }

    inner class VH(val binding: ItemBlockedCallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BlockedCallSummary) {
            val info = NumberInfoHelper.getInfo(binding.root.context, item.number)
            binding.tvNumber.text = info.formatted
            binding.tvRegion.text = buildString {
                append(info.lineType)
                info.region?.let { append("  ·  $it") }
            }
            binding.tvCount.text = "${item.callCount}x"
            binding.tvLastCall.text = DATE_FMT.format(Date(item.lastCallTime))
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemBlockedCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))
}
