package com.autoclicker.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.data.CodeSnippet
import com.autoclicker.app.data.SnippetCategory

/**
 * Адаптер для отображения сниппетов кода
 */
class SnippetsAdapter(
    private val snippets: MutableList<CodeSnippet>,
    private val onSnippetClick: (CodeSnippet) -> Unit,
    private val onFavoriteClick: (CodeSnippet) -> Unit,
    private val onCopyClick: (CodeSnippet) -> Unit
) : RecyclerView.Adapter<SnippetsAdapter.SnippetViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnippetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_snippet, parent, false)
        return SnippetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SnippetViewHolder, position: Int) {
        holder.bind(snippets[position])
    }
    
    override fun getItemCount(): Int = snippets.size
    
    inner class SnippetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSnippetName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvSnippetDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvSnippetCategory)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        private val btnCopy: ImageView = itemView.findViewById(R.id.btnCopySnippet)
        
        fun bind(snippet: CodeSnippet) {
            tvName.text = itemView.context.getString(snippet.nameResId)
            tvDescription.text = itemView.context.getString(snippet.descriptionResId)
            tvCategory.text = getCategoryName(snippet.category)
            
            // Иконка категории
            ivCategoryIcon.setImageResource(getCategoryIcon(snippet.category))
            ivCategoryIcon.setColorFilter(itemView.context.getColor(getCategoryColor(snippet.category)))
            
            // Иконка избранного
            ivFavorite.setImageResource(
                if (snippet.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outline
            )
            ivFavorite.setColorFilter(
                itemView.context.getColor(
                    if (snippet.isFavorite) R.color.accent_pink
                    else R.color.text_tertiary
                )
            )
            
            // Клики
            itemView.setOnClickListener {
                onSnippetClick(snippet)
            }
            
            ivFavorite.setOnClickListener {
                onFavoriteClick(snippet)
            }
            
            btnCopy.setOnClickListener {
                onCopyClick(snippet)
            }
        }
        
        private fun getCategoryName(category: SnippetCategory): String {
            return when (category) {
                SnippetCategory.BASIC -> itemView.context.getString(R.string.snippet_category_basic)
                SnippetCategory.GAMES -> itemView.context.getString(R.string.snippet_category_games)
                SnippetCategory.AUTOMATION -> itemView.context.getString(R.string.snippet_category_automation)
                SnippetCategory.TESTING -> itemView.context.getString(R.string.snippet_category_testing)
                SnippetCategory.ADVANCED -> itemView.context.getString(R.string.snippet_category_advanced)
            }
        }
        
        private fun getCategoryIcon(category: SnippetCategory): Int {
            return when (category) {
                SnippetCategory.BASIC -> R.drawable.ic_code
                SnippetCategory.GAMES -> R.drawable.ic_game
                SnippetCategory.AUTOMATION -> R.drawable.ic_automation
                SnippetCategory.TESTING -> R.drawable.ic_test
                SnippetCategory.ADVANCED -> R.drawable.ic_advanced
            }
        }
        
        private fun getCategoryColor(category: SnippetCategory): Int {
            return when (category) {
                SnippetCategory.BASIC -> R.color.accent_cyan
                SnippetCategory.GAMES -> R.color.accent_purple
                SnippetCategory.AUTOMATION -> R.color.accent_green
                SnippetCategory.TESTING -> R.color.accent_orange
                SnippetCategory.ADVANCED -> R.color.error
            }
        }
    }
}

