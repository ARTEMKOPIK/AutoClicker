package com.autoclicker.app.visual

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.ScriptEditorActivity
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.service.FloatingWindowService
import com.autoclicker.app.util.ScriptStorage
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*

class VisualEditorActivity : BaseActivity() {

    private lateinit var etScriptName: EditText
    private lateinit var rvBlocks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAddBlock: View
    private lateinit var btnPlay: View
    private lateinit var btnSave: View
    private lateinit var btnCode: View
    
    private val blocks = mutableListOf<ScriptBlock>()
    private lateinit var adapter: BlocksAdapter
    private lateinit var storage: ScriptStorage
    
    private var scriptId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_editor)
        
        storage = ScriptStorage(this)
        
        initViews()
        setupRecyclerView()
        setupListeners()
        
        scriptId = intent.getStringExtra("script_id")
        
        // КРИТИЧНО: Восстанавливаем состояние после configuration changes (поворот экрана, etc)
        if (savedInstanceState != null) {
            // Восстанавливаем имя скрипта
            etScriptName.setText(savedInstanceState.getString("script_name", ""))
            
            // Восстанавливаем блоки из Parcelable array
            val savedBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArrayList("blocks", ScriptBlock::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelableArrayList("blocks")
            }
            
            if (savedBlocks != null) {
                blocks.clear()
                blocks.addAll(savedBlocks)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        } else {
            // Первый запуск Activity - загружаем скрипт
            loadScript()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // КРИТИЧНО: Сохраняем состояние перед уничтожением Activity
        // Без этого при повороте экрана все блоки визуального редактора ПОТЕРЯЮТСЯ
        outState.putString("script_name", etScriptName.text.toString())
        outState.putParcelableArrayList("blocks", ArrayList(blocks))
    }

    private fun initViews() {
        etScriptName = findViewById(R.id.etScriptName)
        rvBlocks = findViewById(R.id.rvBlocks)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAddBlock = findViewById(R.id.btnAddBlock)
        btnPlay = findViewById(R.id.btnPlay)
        btnSave = findViewById(R.id.btnSave)
        btnCode = findViewById(R.id.btnCode)
        
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = BlocksAdapter(
            blocks = blocks,
            onBlockClick = { block, position -> showEditBlockDialog(block, position) },
            onBlockDelete = { position -> deleteBlock(position) },
            onBlockMoved = { updateEmptyState() }
        )
        
        rvBlocks.layoutManager = LinearLayoutManager(this)
        rvBlocks.adapter = adapter
        
        // Drag & Drop
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                Collections.swap(blocks, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            
            override fun isLongPressDragEnabled() = true
        })
        touchHelper.attachToRecyclerView(rvBlocks)
        
        updateEmptyState()
    }

    private fun setupListeners() {
        btnAddBlock.setOnClickListener { showAddBlockSheet() }
        
        btnPlay.setOnClickListener {
            saveScript()
            // Запускаем скрипт
            scriptId?.let { id ->
                FloatingWindowService.startService(this, id)
                Toast.makeText(this, "Скрипт запущен", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnSave.setOnClickListener {
            saveScript()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        }
        
        btnCode.setOnClickListener {
            showCodePreview()
        }
    }

    private fun loadScript() {
        scriptId?.let { id ->
            // Пробуем загрузить визуальный скрипт
            val visualScript = VisualScriptStorage.getInstance(this).getScript(id)
            if (visualScript != null) {
                etScriptName.setText(visualScript.name)
                blocks.clear()
                blocks.addAll(visualScript.blocks)
                adapter.notifyDataSetChanged()
            }
        }
        updateEmptyState()
    }

    private fun saveScript() {
        val name = etScriptName.text.toString().ifEmpty { "Визуальный скрипт" }
        
        val visualScript = VisualScript(
            id = scriptId ?: UUID.randomUUID().toString(),
            name = name,
            blocks = blocks.toMutableList()
        )
        
        // Сохраняем визуальный скрипт
        VisualScriptStorage.getInstance(this).saveScript(visualScript)
        
        // Также сохраняем как обычный скрипт для выполнения
        val codeScript = ScriptStorage.Script(
            id = visualScript.id,
            name = name,
            code = visualScript.toCode()
        )
        storage.saveScript(codeScript)
        
        scriptId = visualScript.id
    }

    private fun showAddBlockSheet() {
        val sheet = BottomSheetDialog(this, R.style.Theme_AutoClicker_BottomSheet)
        val view = layoutInflater.inflate(R.layout.sheet_add_block, null)
        sheet.setContentView(view)
        
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipCategories)
        val rvBlockTypes = view.findViewById<RecyclerView>(R.id.rvBlockTypes)
        
        // Категории
        BlockCategory.values().forEach { category ->
            val chip = Chip(this).apply {
                text = "${category.icon} ${category.title}"
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        showBlocksForCategory(rvBlockTypes, category, sheet)
                    }
                }
            }
            chipGroup.addView(chip)
        }
        
        // По умолчанию показываем Actions
        (chipGroup.getChildAt(0) as? Chip)?.isChecked = true
        
        sheet.show()
    }

    private fun showBlocksForCategory(rv: RecyclerView, category: BlockCategory, sheet: BottomSheetDialog) {
        val blockTypes = BlockType.values().filter { it.category == category }
        
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = object : RecyclerView.Adapter<BlockTypeViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockTypeViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_block_type, parent, false)
                return BlockTypeViewHolder(view)
            }

            override fun onBindViewHolder(holder: BlockTypeViewHolder, position: Int) {
                val blockType = blockTypes[position]
                holder.bind(blockType) {
                    addBlock(blockType)
                    sheet.dismiss()
                }
            }

            override fun getItemCount() = blockTypes.size
        }
    }

    private fun addBlock(type: BlockType) {
        val block = ScriptBlock(type = type)
        
        // Если блок требует параметры, показываем диалог
        if (type.params.isNotEmpty()) {
            showEditBlockDialog(block, -1)
        } else {
            blocks.add(block)
            adapter.notifyItemInserted(blocks.size - 1)
            updateEmptyState()
        }
    }

    private fun showEditBlockDialog(block: ScriptBlock, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_block, null)
        val tvBlockTitle = dialogView.findViewById<TextView>(R.id.tvBlockTitle)
        val paramsContainer = dialogView.findViewById<LinearLayout>(R.id.paramsContainer)
        
        tvBlockTitle.text = "${block.type.icon} ${block.type.title}"
        
        val paramViews = mutableMapOf<String, EditText>()
        
        // Создаём поля для каждого параметра
        block.type.params.forEach { param ->
            val paramView = layoutInflater.inflate(R.layout.item_block_param, paramsContainer, false)
            val tvLabel = paramView.findViewById<TextView>(R.id.tvParamLabel)
            val etValue = paramView.findViewById<EditText>(R.id.etParamValue)
            val btnPick = paramView.findViewById<ImageButton>(R.id.btnPickCoordinate)
            
            tvLabel.text = param.label
            etValue.setText(block.params[param.id] ?: param.defaultValue)
            etValue.hint = param.defaultValue.ifEmpty { param.label }
            
            // Показываем кнопку выбора координат для числовых параметров X/Y
            if (param.type == ParamType.NUMBER && 
                (param.id.lowercase().contains("x") || param.id.lowercase().contains("y"))) {
                btnPick.visibility = View.VISIBLE
                btnPick.setOnClickListener {
                    // TODO: Открыть оверлей для выбора координат
                    Toast.makeText(this, "Выбор координат в разработке", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Для цвета показываем color picker
            if (param.type == ParamType.COLOR) {
                btnPick.visibility = View.VISIBLE
                btnPick.setImageResource(R.drawable.ic_colorpicker)
                btnPick.setOnClickListener {
                    Toast.makeText(this, "Используйте пипетку для выбора цвета", Toast.LENGTH_SHORT).show()
                }
            }
            
            paramViews[param.id] = etValue
            paramsContainer.addView(paramView)
        }
        
        // Если блок с детьми, показываем информацию
        if (block.type.hasChildren) {
            val infoView = TextView(this).apply {
                text = "💡 Вложенные блоки добавляются после сохранения"
                setTextColor(getColor(R.color.text_tertiary))
                textSize = 12f
                setPadding(0, 16, 0, 0)
            }
            paramsContainer.addView(infoView)
        }
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                // Сохраняем параметры
                paramViews.forEach { (paramId, editText) ->
                    block.params[paramId] = editText.text.toString()
                }
                
                if (position == -1) {
                    // Новый блок
                    blocks.add(block)
                    adapter.notifyItemInserted(blocks.size - 1)
                } else {
                    // Редактирование
                    adapter.notifyItemChanged(position)
                }
                updateEmptyState()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteBlock(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить блок?")
            .setMessage("Блок \"${blocks[position].type.title}\" будет удалён")
            .setPositiveButton("Удалить") { _, _ ->
                blocks.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateEmptyState()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (blocks.isEmpty()) View.VISIBLE else View.GONE
        rvBlocks.visibility = if (blocks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showCodePreview() {
        val visualScript = VisualScript(
            name = etScriptName.text.toString().ifEmpty { "Скрипт" },
            blocks = blocks.toMutableList()
        )
        val code = visualScript.toCode()
        
        AlertDialog.Builder(this)
            .setTitle("Сгенерированный код")
            .setMessage(code)
            .setPositiveButton("Открыть в редакторе") { _, _ ->
                // Сохраняем и открываем в текстовом редакторе
                saveScript()
                val intent = Intent(this, ScriptEditorActivity::class.java)
                intent.putExtra("script_id", scriptId)
                startActivity(intent)
            }
            .setNeutralButton("Копировать") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("code", code))
                Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    // === ViewHolders ===
    
    class BlockTypeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvIcon: TextView = view.findViewById(R.id.tvBlockIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvBlockTitle)
        private val tvDescription: TextView = view.findViewById(R.id.tvBlockDescription)
        
        fun bind(blockType: BlockType, onClick: () -> Unit) {
            tvIcon.text = blockType.icon
            tvTitle.text = blockType.title
            tvDescription.text = blockType.description
            itemView.setOnClickListener { onClick() }
        }
    }
}
