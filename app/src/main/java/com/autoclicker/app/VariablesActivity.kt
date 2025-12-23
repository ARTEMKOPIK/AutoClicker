package com.autoclicker.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.ScriptVariables

class VariablesActivity : BaseActivity() {

    private lateinit var rvVariables: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: VariablesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_variables)

        ScriptVariables.init(this)
        initViews()
        loadVariables()
    }

    private fun initViews() {
        rvVariables = findViewById(R.id.rvVariables)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.btnAdd).setOnClickListener {
            showAddDialog()
        }

        findViewById<ImageView>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Очистить все?")
                .setMessage("Все переменные будут удалены")
                .setPositiveButton("Очистить") { _, _ ->
                    ScriptVariables.clear()
                    loadVariables()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        rvVariables.layoutManager = LinearLayoutManager(this)
        adapter = VariablesAdapter(
            onEdit = { key, value -> showEditDialog(key, value) },
            onDelete = { key ->
                ScriptVariables.remove(key)
                loadVariables()
            }
        )
        rvVariables.adapter = adapter
    }

    private fun loadVariables() {
        val variables = ScriptVariables.getAll().toList()
        adapter.submitList(variables)
        tvEmpty.visibility = if (variables.isEmpty()) View.VISIBLE else View.GONE
        rvVariables.visibility = if (variables.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_variable, null)
        val etKey = dialogView.findViewById<EditText>(R.id.etKey)
        val etValue = dialogView.findViewById<EditText>(R.id.etValue)

        AlertDialog.Builder(this)
            .setTitle("Новая переменная")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val key = etKey.text.toString().trim()
                val value = etValue.text.toString()
                if (key.isNotEmpty()) {
                    ScriptVariables.set(key, value)
                    loadVariables()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(key: String, currentValue: Any) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_variable, null)
        val etKey = dialogView.findViewById<EditText>(R.id.etKey)
        val etValue = dialogView.findViewById<EditText>(R.id.etValue)

        etKey.setText(key)
        etKey.isEnabled = false
        etValue.setText(currentValue.toString())

        AlertDialog.Builder(this)
            .setTitle("Редактировать")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val value = etValue.text.toString()
                ScriptVariables.set(key, value)
                loadVariables()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    inner class VariablesAdapter(
        private val onEdit: (String, Any) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<VariablesAdapter.ViewHolder>() {

        private var variables = listOf<Pair<String, Any>>()

        fun submitList(newList: List<Pair<String, Any>>) {
            variables = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_variable, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(variables[position])
        }

        override fun getItemCount() = variables.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvKey: TextView = view.findViewById(R.id.tvKey)
            private val tvValue: TextView = view.findViewById(R.id.tvValue)
            private val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
            private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

            fun bind(variable: Pair<String, Any>) {
                tvKey.text = variable.first
                tvValue.text = variable.second.toString()
                
                btnEdit.setOnClickListener { onEdit(variable.first, variable.second) }
                btnDelete.setOnClickListener { onDelete(variable.first) }
            }
        }
    }
}
