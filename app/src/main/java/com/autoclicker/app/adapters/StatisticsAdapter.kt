package com.autoclicker.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.data.ScriptStatistics

/**
 * Адаптер для отображения статистики скриптов
 */
class StatisticsAdapter : ListAdapter<ScriptStatistics, StatisticsAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvScriptName: TextView = itemView.findViewById(R.id.tvScriptName)
        private val tvRuns: TextView = itemView.findViewById(R.id.tvRuns)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvSuccessRate: TextView = itemView.findViewById(R.id.tvSuccessRate)
        
        fun bind(stats: ScriptStatistics, rank: Int) {
            tvRank.text = "#$rank"
            tvScriptName.text = stats.scriptName
            tvRuns.text = "${stats.totalRuns} ${itemView.context.getString(R.string.statistics_runs)}"
            tvDuration.text = "${itemView.context.getString(R.string.statistics_avg)}: ${stats.getFormattedDuration()}"
            tvSuccessRate.text = String.format("%.0f%%", stats.getSuccessRate())
            
            // Цвет для success rate
            val color = when {
                stats.getSuccessRate() >= 90 -> itemView.context.getColor(R.color.success)
                stats.getSuccessRate() >= 70 -> itemView.context.getColor(R.color.warning)
                else -> itemView.context.getColor(R.color.error)
            }
            tvSuccessRate.setTextColor(color)
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ScriptStatistics>() {
        override fun areItemsTheSame(oldItem: ScriptStatistics, newItem: ScriptStatistics): Boolean {
            return oldItem.scriptName == newItem.scriptName
        }
        
        override fun areContentsTheSame(oldItem: ScriptStatistics, newItem: ScriptStatistics): Boolean {
            return oldItem == newItem
        }
    }
}

