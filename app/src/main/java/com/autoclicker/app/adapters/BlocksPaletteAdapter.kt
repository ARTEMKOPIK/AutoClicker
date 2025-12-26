package com.autoclicker.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.data.BlockType

/**
 * Адаптер для палитры блоков (выбор типа блока для добавления)
 */
class BlocksPaletteAdapter(
    private val onBlockTypeClick: (BlockType) -> Unit
) : ListAdapter<BlockType, BlocksPaletteAdapter.PaletteViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_block_palette, parent, false)
        return PaletteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PaletteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBlockName: TextView = itemView.findViewById(R.id.tvPaletteBlockName)
        private val tvBlockDesc: TextView = itemView.findViewById(R.id.tvPaletteBlockDesc)
        private val ivBlockIcon: ImageView = itemView.findViewById(R.id.ivPaletteBlockIcon)
        
        fun bind(blockType: BlockType) {
            tvBlockName.text = getBlockDisplayName(blockType)
            tvBlockDesc.text = getBlockDescription(blockType)
            ivBlockIcon.setImageResource(getBlockIcon(blockType))
            
            itemView.setOnClickListener {
                onBlockTypeClick(blockType)
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
        
        private fun getBlockDescription(type: BlockType): String {
            return when (type) {
                BlockType.CLICK -> itemView.context.getString(R.string.block_click_desc)
                BlockType.LONG_CLICK -> itemView.context.getString(R.string.block_long_click_desc)
                BlockType.SWIPE -> itemView.context.getString(R.string.block_swipe_desc)
                BlockType.TAP -> itemView.context.getString(R.string.block_tap_desc)
                BlockType.SLEEP -> itemView.context.getString(R.string.block_sleep_desc)
                BlockType.WAIT_COLOR -> itemView.context.getString(R.string.block_wait_color_desc)
                BlockType.WAIT_TEXT -> itemView.context.getString(R.string.block_wait_text_desc)
                BlockType.IF_COLOR -> itemView.context.getString(R.string.block_if_color_desc)
                BlockType.IF_TEXT -> itemView.context.getString(R.string.block_if_text_desc)
                BlockType.IF_IMAGE -> itemView.context.getString(R.string.block_if_image_desc)
                BlockType.LOOP -> itemView.context.getString(R.string.block_loop_desc)
                BlockType.LOOP_COUNT -> itemView.context.getString(R.string.block_loop_count_desc)
                BlockType.SET_VAR -> itemView.context.getString(R.string.block_set_var_desc)
                BlockType.INC_VAR -> itemView.context.getString(R.string.block_inc_var_desc)
                BlockType.DEC_VAR -> itemView.context.getString(R.string.block_dec_var_desc)
                BlockType.LOG -> itemView.context.getString(R.string.block_log_desc)
                BlockType.TOAST -> itemView.context.getString(R.string.block_toast_desc)
                BlockType.TELEGRAM -> itemView.context.getString(R.string.block_telegram_desc)
                BlockType.GET_TEXT -> itemView.context.getString(R.string.block_get_text_desc)
                BlockType.FIND_TEXT -> itemView.context.getString(R.string.block_find_text_desc)
                BlockType.BACK -> itemView.context.getString(R.string.block_back_desc)
                BlockType.HOME -> itemView.context.getString(R.string.block_home_desc)
                BlockType.RECENTS -> itemView.context.getString(R.string.block_recents_desc)
                BlockType.FUNCTION -> itemView.context.getString(R.string.block_function_desc)
                BlockType.CALL_FUNC -> itemView.context.getString(R.string.block_call_func_desc)
                BlockType.RETURN -> itemView.context.getString(R.string.block_return_desc)
                BlockType.COMMENT -> itemView.context.getString(R.string.block_comment_desc)
                BlockType.BREAK -> itemView.context.getString(R.string.block_break_desc)
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
    
    class DiffCallback : DiffUtil.ItemCallback<BlockType>() {
        override fun areItemsTheSame(oldItem: BlockType, newItem: BlockType): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: BlockType, newItem: BlockType): Boolean {
            return oldItem == newItem
        }
    }
}

