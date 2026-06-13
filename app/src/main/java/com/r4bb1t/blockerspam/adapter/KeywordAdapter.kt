package com.r4bb1t.blockerspam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.r4bb1t.blockerspam.R

class KeywordAdapter(
    private val keywords: List<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder>() {

    class KeywordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKeyword: TextView = view.findViewById(R.id.tvKeyword)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keyword, parent, false)
        return KeywordViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
        val keyword = keywords[position]
        holder.tvKeyword.text = keyword
        holder.btnDelete.setOnClickListener { onDeleteClick(keyword) }
    }

    override fun getItemCount() = keywords.size
}
