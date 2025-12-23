package com.autoclicker.app

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.ScriptLogger
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : BaseActivity(), ScriptLogger.LogListener {

    private lateinit var rvLogs: RecyclerView
    private lateinit var tvLogCount: TextView
    private lateinit var adapter: LogAdapter
    
    private var filterLevel: ScriptLogger.Level? = null
    private val chips = mutableMapOf<ScriptLogger.Level?, TextView>()
    private var isListenerRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        initViews()
        setupChips()
        loadLogs()
        
        ScriptLogger.addListener(this)
        isListenerRegistered = true
    }

    override fun onDestroy() {
        if (isListenerRegistered) {
            ScriptLogger.removeListener(this)
            isListenerRegistered = false
        }
        super.onDestroy()
    }

    private fun initViews() {
        rvLogs = findViewById(R.id.rvLogs)
        tvLogCount = findViewById(R.id.tvLogCount)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.btnClear).setOnClickListener {
            ScriptLogger.clear()
            loadLogs()
        }

        findViewById<ImageView>(R.id.btnShare).setOnClickListener {
            shareLogs()
        }

        rvLogs.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = LogAdapter()
        rvLogs.adapter = adapter
    }

    private fun setupChips() {
        chips[null] = findViewById(R.id.chipAll)
        chips[ScriptLogger.Level.DEBUG] = findViewById(R.id.chipDebug)
        chips[ScriptLogger.Level.INFO] = findViewById(R.id.chipInfo)
        chips[ScriptLogger.Level.WARN] = findViewById(R.id.chipWarn)
        chips[ScriptLogger.Level.ERROR] = findViewById(R.id.chipError)

        chips.forEach { (level, chip) ->
            chip.setOnClickListener {
                filterLevel = level
                updateChipSelection()
                loadLogs()
            }
        }
    }

    private fun updateChipSelection() {
        chips.forEach { (level, chip) ->
            val isSelected = level == filterLevel
            chip.setBackgroundResource(
                if (isSelected) R.drawable.chip_selected else R.drawable.chip_normal
            )
            chip.setTextColor(
                getColor(if (isSelected) R.color.text_on_primary else R.color.text_secondary)
            )
        }
    }

    private fun loadLogs() {
        val logs = if (filterLevel != null) {
            ScriptLogger.getLogsFiltered(level = filterLevel)
        } else {
            ScriptLogger.getLogs()
        }
        
        adapter.submitList(logs)
        tvLogCount.text = "${logs.size} записей"
        
        // Scroll to bottom
        if (logs.isNotEmpty()) {
            rvLogs.scrollToPosition(logs.size - 1)
        }
    }

    private fun shareLogs() {
        val logsText = ScriptLogger.exportLogs()
        if (logsText.isEmpty()) {
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AutoClicker Logs")
            putExtra(Intent.EXTRA_TEXT, logsText)
        }
        startActivity(Intent.createChooser(intent, "Поделиться логами"))
    }

    override fun onLog(entry: ScriptLogger.LogEntry) {
        runOnUiThread {
            if (filterLevel == null || entry.level == filterLevel) {
                adapter.addEntry(entry)
                tvLogCount.text = "${adapter.itemCount} записей"
                rvLogs.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    override fun onClear() {
        runOnUiThread {
            adapter.submitList(emptyList())
            tvLogCount.text = "0 записей"
        }
    }

    inner class LogAdapter : RecyclerView.Adapter<LogAdapter.ViewHolder>() {
        
        private val logs = mutableListOf<ScriptLogger.LogEntry>()
        private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

        fun submitList(newLogs: List<ScriptLogger.LogEntry>) {
            logs.clear()
            logs.addAll(newLogs)
            notifyDataSetChanged()
        }

        fun addEntry(entry: ScriptLogger.LogEntry) {
            logs.add(entry)
            notifyItemInserted(logs.size - 1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(logs[position])
        }

        override fun getItemCount() = logs.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val levelIndicator: View = view.findViewById(R.id.levelIndicator)
            private val tvTime: TextView = view.findViewById(R.id.tvTime)
            private val tvLevel: TextView = view.findViewById(R.id.tvLevel)
            private val tvScript: TextView = view.findViewById(R.id.tvScript)
            private val tvMessage: TextView = view.findViewById(R.id.tvMessage)

            fun bind(entry: ScriptLogger.LogEntry) {
                tvTime.text = dateFormat.format(Date(entry.timestamp))
                tvLevel.text = entry.level.tag
                tvLevel.setTextColor(entry.level.color)
                tvMessage.text = entry.message

                // Level indicator color
                (levelIndicator.background as? GradientDrawable)?.setColor(entry.level.color)
                    ?: run {
                        val drawable = GradientDrawable()
                        drawable.setColor(entry.level.color)
                        levelIndicator.background = drawable
                    }

                // Script name
                if (entry.scriptName != null) {
                    tvScript.text = entry.scriptName
                    tvScript.visibility = View.VISIBLE
                } else {
                    tvScript.visibility = View.GONE
                }
            }
        }
    }
}
