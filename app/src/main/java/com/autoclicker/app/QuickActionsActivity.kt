package com.autoclicker.app

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.service.ClickerAccessibilityService
import com.autoclicker.app.util.QuickActions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class QuickActionsActivity : BaseActivity() {

    private lateinit var quickActions: QuickActions
    private lateinit var rvActions: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ActionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_actions)

        quickActions = QuickActions(this)
        initViews()
        loadActions()
    }

    private fun initViews() {
        rvActions = findViewById(R.id.rvActions)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }

        rvActions.layoutManager = LinearLayoutManager(this)
        adapter = ActionsAdapter(
            onExecute = { action -> executeAction(action) },
            onDelete = { action ->
                quickActions.deleteAction(action.id)
                loadActions()
            }
        )
        rvActions.adapter = adapter
    }

    private fun loadActions() {
        val actions = quickActions.getAllActions()
        adapter.submitList(actions)
        tvEmpty.visibility = if (actions.isEmpty()) View.VISIBLE else View.GONE
        rvActions.visibility = if (actions.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_quick_action, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etX = dialogView.findViewById<EditText>(R.id.etX)
        val etY = dialogView.findViewById<EditText>(R.id.etY)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)

        val types = arrayOf("Клик", "Долгий клик", "Двойной тап")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        AlertDialog.Builder(this)
            .setTitle("Новое действие")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = etName.text.toString().ifEmpty { "Действие" }
                val x = etX.text.toString().toIntOrNull() ?: 0
                val y = etY.text.toString().toIntOrNull() ?: 0
                val type = when (spinnerType.selectedItemPosition) {
                    1 -> QuickActions.ActionType.LONG_CLICK
                    2 -> QuickActions.ActionType.DOUBLE_TAP
                    else -> QuickActions.ActionType.CLICK
                }

                val action = QuickActions.QuickAction(
                    name = name,
                    x = x,
                    y = y,
                    type = type
                )
                quickActions.saveAction(action)
                loadActions()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun executeAction(action: QuickActions.QuickAction) {
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Accessibility Service недоступен", Toast.LENGTH_SHORT).show()
            return
        }

        when (action.type) {
            QuickActions.ActionType.CLICK -> {
                service.click(action.x.toFloat(), action.y.toFloat())
                Toast.makeText(this, "Клик: ${action.x}, ${action.y}", Toast.LENGTH_SHORT).show()
            }
            QuickActions.ActionType.LONG_CLICK -> {
                service.longClick(action.x.toFloat(), action.y.toFloat(), 500)
                Toast.makeText(this, "Долгий клик: ${action.x}, ${action.y}", Toast.LENGTH_SHORT).show()
            }
            QuickActions.ActionType.DOUBLE_TAP -> {
                service.doubleTap(action.x.toFloat(), action.y.toFloat())
                Toast.makeText(this, "Двойной тап: ${action.x}, ${action.y}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ActionsAdapter(
        private val onExecute: (QuickActions.QuickAction) -> Unit,
        private val onDelete: (QuickActions.QuickAction) -> Unit
    ) : RecyclerView.Adapter<ActionsAdapter.ViewHolder>() {

        private var actions = listOf<QuickActions.QuickAction>()

        fun submitList(newList: List<QuickActions.QuickAction>) {
            actions = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_action, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(actions[position])
        }

        override fun getItemCount() = actions.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val colorIndicator: View = view.findViewById(R.id.colorIndicator)
            private val tvName: TextView = view.findViewById(R.id.tvName)
            private val tvCoords: TextView = view.findViewById(R.id.tvCoords)
            private val tvType: TextView = view.findViewById(R.id.tvType)
            private val btnPlay: ImageView = view.findViewById(R.id.btnPlay)
            private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)

            fun bind(action: QuickActions.QuickAction) {
                tvName.text = action.name
                tvCoords.text = "X: ${action.x}, Y: ${action.y}"
                tvType.text = when (action.type) {
                    QuickActions.ActionType.CLICK -> "Клик"
                    QuickActions.ActionType.LONG_CLICK -> "Долгий клик"
                    QuickActions.ActionType.DOUBLE_TAP -> "Двойной тап"
                }

                (colorIndicator.background as? GradientDrawable)?.setColor(action.color)
                    ?: run {
                        val drawable = GradientDrawable()
                        drawable.setColor(action.color)
                        colorIndicator.background = drawable
                    }

                btnPlay.setOnClickListener { onExecute(action) }
                btnDelete.setOnClickListener { onDelete(action) }
                itemView.setOnClickListener { onExecute(action) }
            }
        }
    }
}
