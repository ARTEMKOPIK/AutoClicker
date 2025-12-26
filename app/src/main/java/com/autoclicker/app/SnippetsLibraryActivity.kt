package com.autoclicker.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.adapters.SnippetsAdapter
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.data.CodeSnippet
import com.autoclicker.app.data.SnippetCategory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Activity для библиотеки сниппетов кода
 */
class SnippetsLibraryActivity : BaseActivity() {
    
    private lateinit var rvSnippets: RecyclerView
    private lateinit var snippetsAdapter: SnippetsAdapter
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var layoutEmpty: LinearLayout
    
    private val allSnippets = CodeSnippet.getDefaultSnippets().toMutableList()
    private val displayedSnippets = mutableListOf<CodeSnippet>()
    
    private var currentCategory: SnippetCategory? = null
    private var searchQuery: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snippets_library)
        
        initViews()
        setupRecyclerView()
        setupFilters()
        setupSearch()
        
        loadSnippets()
        
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun initViews() {
        rvSnippets = findViewById(R.id.rvSnippets)
        searchView = findViewById(R.id.searchView)
        chipGroup = findViewById(R.id.chipGroupCategories)
        layoutEmpty = findViewById(R.id.layoutEmpty)
    }
    
    private fun setupRecyclerView() {
        snippetsAdapter = SnippetsAdapter(
            snippets = displayedSnippets,
            onSnippetClick = { snippet -> showSnippetDetails(snippet) },
            onFavoriteClick = { snippet -> toggleFavorite(snippet) },
            onCopyClick = { snippet -> copyToClipboard(snippet) }
        )
        
        rvSnippets.layoutManager = LinearLayoutManager(this)
        rvSnippets.adapter = snippetsAdapter
    }
    
    private fun setupFilters() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentCategory = null
            } else {
                val chip = chipGroup.findViewById<Chip>(checkedIds[0])
                currentCategory = when (chip.id) {
                    R.id.chipBasic -> SnippetCategory.BASIC
                    R.id.chipGames -> SnippetCategory.GAMES
                    R.id.chipAutomation -> SnippetCategory.AUTOMATION
                    R.id.chipTesting -> SnippetCategory.TESTING
                    R.id.chipAdvanced -> SnippetCategory.ADVANCED
                    R.id.chipFavorites -> null  // Обработается отдельно
                    else -> null
                }
            }
            filterSnippets()
        }
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query?.toLowerCase() ?: ""
                filterSnippets()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.toLowerCase() ?: ""
                filterSnippets()
                return true
            }
        })
    }
    
    private fun loadSnippets() {
        displayedSnippets.clear()
        displayedSnippets.addAll(allSnippets)
        snippetsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }
    
    private fun filterSnippets() {
        displayedSnippets.clear()
        
        // Фильтрация по категории
        val filtered = if (currentCategory != null) {
            allSnippets.filter { it.category == currentCategory }
        } else if (chipGroup.checkedChipId == R.id.chipFavorites) {
            allSnippets.filter { it.isFavorite }
        } else {
            allSnippets
        }
        
        // Фильтрация по поиску
        val searched = if (searchQuery.isNotEmpty()) {
            filtered.filter { snippet ->
                getString(snippet.nameResId).toLowerCase().contains(searchQuery) ||
                getString(snippet.descriptionResId).toLowerCase().contains(searchQuery) ||
                snippet.tags.any { it.contains(searchQuery) }
            }
        } else {
            filtered
        }
        
        displayedSnippets.addAll(searched)
        snippetsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }
    
    private fun showSnippetDetails(snippet: CodeSnippet) {
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle(getString(snippet.nameResId))
        
        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.text = snippet.code
        textView.setTextIsSelectable(true)
        textView.setPadding(32, 32, 32, 32)
        textView.typeface = android.graphics.Typeface.MONOSPACE
        textView.textSize = 13f
        scrollView.addView(textView)
        
        dialog.setView(scrollView)
        dialog.setPositiveButton(R.string.action_copy) { _, _ ->
            copyToClipboard(snippet)
        }
        dialog.setNeutralButton(R.string.action_insert) { _, _ ->
            insertToEditor(snippet)
        }
        dialog.setNegativeButton(R.string.action_close, null)
        dialog.show()
    }
    
    private fun toggleFavorite(snippet: CodeSnippet) {
        snippet.isFavorite = !snippet.isFavorite
        snippetsAdapter.notifyDataSetChanged()
        
        val message = if (snippet.isFavorite) {
            R.string.snippet_added_to_favorites
        } else {
            R.string.snippet_removed_from_favorites
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun copyToClipboard(snippet: CodeSnippet) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Code Snippet", snippet.code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.snippet_copied, Toast.LENGTH_SHORT).show()
    }
    
    private fun insertToEditor(snippet: CodeSnippet) {
        // TODO: Интеграция с редактором кода
        // Здесь должна быть логика вставки кода в редактор
        Toast.makeText(this, R.string.snippet_inserted, Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun updateEmptyState() {
        if (displayedSnippets.isEmpty()) {
            rvSnippets.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvSnippets.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }
}

