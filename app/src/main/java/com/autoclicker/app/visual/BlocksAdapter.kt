package com.autoclicker.app.visual

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R

class BlocksAdapter(
    private val blocks: MutableList<ScriptBlock>,
    private val onBlockClick: (ScriptBlock, Int) -> Unit,
    private val onBlockDelete: (Int) -> Unit,
    private val onBlockMoved: () -> Unit
) : RecyclerView.Adapter<BlocksAdapter.BlockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script_block, parent, false)
        return BlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(blocks[position], position)
    }

    override fun getItemCount() = blocks.size

    inner class BlockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvBlockName)
        private val tvParams: TextView = view.findViewById(R.id.tvBlockParams)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteBlock)

        fun bind(block: ScriptBlock, position: Int) {
            tvTitle.text = "${block.type.icon} ${block.type.title}"
            
            // Показываем параметры
            val paramsText = block.params.entries
                .filter { it.value.isNotEmpty() }
                .joinToString(" • ") { "${it.key}: ${it.value}" }
            tvParams.text = paramsText
            tvParams.visibility = if (paramsText.isEmpty()) View.GONE else View.VISIBLE
            
            // Клики
            itemView.setOnClickListener { onBlockClick(block, position) }
            btnDelete.setOnClickListener { onBlockDelete(position) }
        }
    }
}
