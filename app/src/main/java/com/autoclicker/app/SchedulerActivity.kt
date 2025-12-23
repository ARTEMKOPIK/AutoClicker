package com.autoclicker.app

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.ScriptScheduler
import com.autoclicker.app.util.ScriptStorage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class SchedulerActivity : BaseActivity() {

    private lateinit var scheduler: ScriptScheduler
    private lateinit var storage: ScriptStorage
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduler)

        scheduler = ScriptScheduler(this)
        storage = ScriptStorage(this)

        initViews()
        loadTasks()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.tvEmpty)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddTaskDialog()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(
            onToggle = { task, enabled -> 
                scheduler.enableTask(task.id, enabled)
                loadTasks()
            },
            onDelete = { task ->
                scheduler.cancelTask(task.id)
                loadTasks()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadTasks() {
        val tasks = scheduler.getAllTasks()
        adapter.submitList(tasks)
        emptyView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddTaskDialog() {
        val scripts = storage.getAllScripts()
        if (scripts.isEmpty()) {
            Toast.makeText(this, "Сначала создайте скрипт", Toast.LENGTH_SHORT).show()
            return
        }

        val scriptNames = scripts.map { it.name }.toTypedArray()
        var selectedScript = scripts.first()
        var selectedHour = 12
        var selectedMinute = 0

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val spinnerScript = dialogView.findViewById<Spinner>(R.id.spinnerScript)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvTime)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)

        spinnerScript.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, scriptNames)
        spinnerScript.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedScript = scripts[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        tvTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)

        btnSelectTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                tvTime.text = String.format("%02d:%02d", hour, minute)
            }, selectedHour, selectedMinute, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Добавить задачу")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val task = ScriptScheduler.ScheduledTask(
                    scriptId = selectedScript.id,
                    scriptName = selectedScript.name,
                    hour = selectedHour,
                    minute = selectedMinute
                )
                scheduler.scheduleTask(task)
                loadTasks()
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    inner class TaskAdapter(
        private val onToggle: (ScriptScheduler.ScheduledTask, Boolean) -> Unit,
        private val onDelete: (ScriptScheduler.ScheduledTask) -> Unit
    ) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

        private var tasks = listOf<ScriptScheduler.ScheduledTask>()

        fun submitList(newList: List<ScriptScheduler.ScheduledTask>) {
            tasks = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(tasks[position])
        }

        override fun getItemCount() = tasks.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvTaskName)
            private val tvTime: TextView = view.findViewById(R.id.tvTaskTime)
            private val switchEnabled: SwitchCompat = view.findViewById(R.id.switchEnabled)
            private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

            fun bind(task: ScriptScheduler.ScheduledTask) {
                tvName.text = task.scriptName
                tvTime.text = String.format("%02d:%02d", task.hour, task.minute)
                switchEnabled.isChecked = task.enabled
                
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onToggle(task, isChecked)
                }
                
                btnDelete.setOnClickListener { onDelete(task) }
            }
        }
    }
}
