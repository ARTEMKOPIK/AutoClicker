package com.autoclicker.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.data.BlockType
import com.autoclicker.app.data.ScriptBlock

/**
 * Адаптер для отображения блоков в визуальном редакторе
 */
class BlocksAdapter(
    private val blocks: MutableList<ScriptBlock>,
    private val onBlockClick: (ScriptBlock) -> Unit,
    private val onBlockDelete: (ScriptBlock) -> Unit
) : RecyclerView.Adapter<BlocksAdapter.BlockViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script_block, parent, false)
        return BlockViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(blocks[position])
    }
    
    override fun getItemCount(): Int = blocks.size
    
    inner class BlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBlockName: TextView = itemView.findViewById(R.id.tvBlockName)
        private val tvBlockParams: TextView = itemView.findViewById(R.id.tvBlockParams)
        private val ivBlockIcon: ImageView = itemView.findViewById(R.id.ivBlockIcon)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteBlock)
        
        fun bind(block: ScriptBlock) {
            // Название блока
            tvBlockName.text = getBlockDisplayName(block.type)
            
            // Параметры блока
            val params = block.parameters.joinToString(", ") { 
                "${it.label}: ${if (it.value.isEmpty()) "?" else it.value}"
            }
            tvBlockParams.text = if (params.isEmpty()) 
                itemView.context.getString(R.string.block_no_params) 
            else params
            tvBlockParams.visibility = if (params.isEmpty()) View.GONE else View.VISIBLE
            
            // Иконка блока
            ivBlockIcon.setImageResource(getBlockIcon(block.type))
            
            // Цвет фона по категории
            val bgColor = when (block.category) {
                com.autoclicker.app.data.BlockCategory.ACTIONS -> R.color.accent_cyan
                com.autoclicker.app.data.BlockCategory.WAIT -> R.color.accent_orange
                com.autoclicker.app.data.BlockCategory.CONDITIONS -> R.color.accent_green
                com.autoclicker.app.data.BlockCategory.LOOPS -> R.color.accent_purple
                com.autoclicker.app.data.BlockCategory.VARIABLES -> R.color.primary
                com.autoclicker.app.data.BlockCategory.OUTPUT -> R.color.accent_pink
                com.autoclicker.app.data.BlockCategory.OCR -> R.color.accent
                com.autoclicker.app.data.BlockCategory.SYSTEM -> R.color.text_secondary
                com.autoclicker.app.data.BlockCategory.FUNCTIONS -> R.color.primary_light
                com.autoclicker.app.data.BlockCategory.SPECIAL -> R.color.text_tertiary
            }
            ivBlockIcon.setColorFilter(itemView.context.getColor(bgColor))
            
            // Клики
            itemView.setOnClickListener {
                onBlockClick(block)
            }
            
            btnDelete.setOnClickListener {
                onBlockDelete(block)
            }
        }
        
        private fun getBlockDisplayName(type: BlockType): String {
            return when (type) {
                BlockType.CLICK -> itemView.context.getString(R.string.block_click)
                BlockType.LONG_CLICK -> itemView.context.getString(R.string.block_long_click)
                BlockType.SWIPE -> itemView.context.getString(R.string.block_swipe)
                BlockType.TAP -> itemView.context.getString(R.string.block_tap)
                BlockType.SLEEP -> itemView.context.getString(R.string.block_sleep)
                BlockType.WAIT_COLOR -> itemView.context.getString(R.string.block_wait_color)
                BlockType.WAIT_TEXT -> itemView.context.getString(R.string.block_wait_text)
                BlockType.IF_COLOR -> itemView.context.getString(R.string.block_if_color)
                BlockType.IF_TEXT -> itemView.context.getString(R.string.block_if_text)
                BlockType.IF_IMAGE -> itemView.context.getString(R.string.block_if_image)
                BlockType.LOOP -> itemView.context.getString(R.string.block_loop)
                BlockType.LOOP_COUNT -> itemView.context.getString(R.string.block_loop_count)
                BlockType.SET_VAR -> itemView.context.getString(R.string.block_set_var)
                BlockType.INC_VAR -> itemView.context.getString(R.string.block_inc_var)
                BlockType.DEC_VAR -> itemView.context.getString(R.string.block_dec_var)
                BlockType.LOG -> itemView.context.getString(R.string.block_log)
                BlockType.TOAST -> itemView.context.getString(R.string.block_toast)
                BlockType.TELEGRAM -> itemView.context.getString(R.string.block_telegram)
                BlockType.GET_TEXT -> itemView.context.getString(R.string.block_get_text)
                BlockType.FIND_TEXT -> itemView.context.getString(R.string.block_find_text)
                BlockType.BACK -> itemView.context.getString(R.string.block_back)
                BlockType.HOME -> itemView.context.getString(R.string.block_home)
                BlockType.RECENTS -> itemView.context.getString(R.string.block_recents)
                BlockType.FUNCTION -> itemView.context.getString(R.string.block_function)
                BlockType.CALL_FUNC -> itemView.context.getString(R.string.block_call_func)
                BlockType.RETURN -> itemView.context.getString(R.string.block_return)
                BlockType.COMMENT -> itemView.context.getString(R.string.block_comment)
                BlockType.BREAK -> itemView.context.getString(R.string.block_break)
            }
        }
        
        private fun getBlockIcon(type: BlockType): Int {
            return when (type) {
                BlockType.CLICK, BlockType.LONG_CLICK, BlockType.TAP -> R.drawable.ic_touch
                BlockType.SWIPE -> R.drawable.ic_swipe
                BlockType.SLEEP, BlockType.WAIT_COLOR, BlockType.WAIT_TEXT -> R.drawable.ic_time
                BlockType.IF_COLOR, BlockType.IF_TEXT, BlockType.IF_IMAGE -> R.drawable.ic_condition
                BlockType.LOOP, BlockType.LOOP_COUNT -> R.drawable.ic_loop
                BlockType.SET_VAR, BlockType.INC_VAR, BlockType.DEC_VAR -> R.drawable.ic_variable
                BlockType.LOG, BlockType.TOAST -> R.drawable.ic_message
                BlockType.TELEGRAM -> R.drawable.ic_telegram
                BlockType.GET_TEXT, BlockType.FIND_TEXT -> R.drawable.ic_text
                BlockType.BACK, BlockType.HOME, BlockType.RECENTS -> R.drawable.ic_system
                BlockType.FUNCTION, BlockType.CALL_FUNC, BlockType.RETURN -> R.drawable.ic_function
                BlockType.COMMENT -> R.drawable.ic_comment
                BlockType.BREAK -> R.drawable.ic_stop
            }
        }
    }
}

