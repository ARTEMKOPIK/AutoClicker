package com.autoclicker.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.adapters.StatisticsAdapter
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.data.ScriptStatistics
import com.autoclicker.app.util.StatisticsManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity для отображения статистики и аналитики
 */
class StatisticsActivity : BaseActivity() {
    
    private lateinit var statisticsManager: StatisticsManager
    private lateinit var adapter: StatisticsAdapter
    
    // Views
    private lateinit var tvTotalRuns: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var tvSuccessRate: TextView
    private lateinit var tvTotalScripts: TextView
    private lateinit var rvTopScripts: RecyclerView
    private lateinit var layoutNoData: LinearLayout
    private lateinit var layoutContent: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        statisticsManager = StatisticsManager.getInstance(this)
        
        initViews()
        setupRecyclerView()
        loadStatistics()
        
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        findViewById<View>(R.id.btnClearStats).setOnClickListener {
            clearStatistics()
        }
    }
    
    private fun initViews() {
        tvTotalRuns = findViewById(R.id.tvTotalRuns)
        tvTotalDuration = findViewById(R.id.tvTotalDuration)
        tvSuccessRate = findViewById(R.id.tvSuccessRate)
        tvTotalScripts = findViewById(R.id.tvTotalScripts)
        rvTopScripts = findViewById(R.id.rvTopScripts)
        layoutNoData = findViewById(R.id.layoutNoData)
        layoutContent = findViewById(R.id.layoutContent)
    }
    
    private fun setupRecyclerView() {
        adapter = StatisticsAdapter()
        rvTopScripts.layoutManager = LinearLayoutManager(this)
        rvTopScripts.adapter = adapter
    }
    
    private fun loadStatistics() {
        val totalStats = statisticsManager.getTotalStatistics()
        val topScripts = statisticsManager.getTopScripts(10)
        
        if (totalStats.totalRuns == 0) {
            // Нет данных
            layoutNoData.visibility = View.VISIBLE
            layoutContent.visibility = View.GONE
        } else {
            // Есть данные
            layoutNoData.visibility = View.GONE
            layoutContent.visibility = View.VISIBLE
            
            // Заполняем общую статистику
            tvTotalRuns.text = totalStats.totalRuns.toString()
            tvTotalDuration.text = formatDuration(totalStats.totalDuration)
            tvSuccessRate.text = String.format("%.1f%%", totalStats.getSuccessRate())
            tvTotalScripts.text = totalStats.totalScripts.toString()
            
            // Заполняем топ скриптов
            adapter.submitList(topScripts)
        }
    }
    
    private fun clearStatistics() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.statistics_clear_title)
        builder.setMessage(R.string.statistics_clear_message)
        builder.setPositiveButton(R.string.action_delete) { _, _ ->
            statisticsManager.clearAllStatistics()
            loadStatistics()
            android.widget.Toast.makeText(this, R.string.statistics_cleared, android.widget.Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(R.string.action_cancel, null)
        builder.show()
    }
    
    private fun formatDuration(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> String.format("%.1fs", ms / 1000f)
            ms < 3600000 -> String.format("%.1fm", ms / 60000f)
            else -> String.format("%.1fh", ms / 3600000f)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}

