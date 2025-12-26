package com.autoclicker.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.adapters.BlocksAdapter
import com.autoclicker.app.adapters.BlocksPaletteAdapter
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.data.BlockType
import com.autoclicker.app.data.ScriptBlock
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Визуальный редактор Pro с drag & drop блоками
 */
class VisualEditorProActivity : BaseActivity() {
    
    private lateinit var rvBlocks: RecyclerView
    private lateinit var blocksAdapter: BlocksAdapter
    private val blocks = mutableListOf<ScriptBlock>()
    
    private lateinit var tvScriptName: TextView
    private var scriptName = "Untitled Script"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_editor_pro)
        
        tvScriptName = findViewById(R.id.tvScriptName)
        rvBlocks = findViewById(R.id.rvBlocks)
        
        setupRecyclerView()
        setupButtons()
        
        tvScriptName.text = scriptName
    }
    
    private fun setupRecyclerView() {
        blocksAdapter = BlocksAdapter(
            blocks = blocks,
            onBlockClick = { block -> editBlock(block) },
            onBlockDelete = { block -> deleteBlock(block) }
        )
        
        rvBlocks.layoutManager = LinearLayoutManager(this)
        rvBlocks.adapter = blocksAdapter
        
        // Drag & Drop для перестановки блоков
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                // Перемещаем блок
                val block = blocks.removeAt(fromPosition)
                blocks.add(toPosition, block)
                
                // Обновляем order
                blocks.forEachIndexed { index, scriptBlock ->
                    scriptBlock.order = index
                }
                
                blocksAdapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val block = blocks[position]
                deleteBlock(block)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(rvBlocks)
    }
    
    private fun setupButtons() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        findViewById<View>(R.id.btnAddBlock).setOnClickListener {
            showBlockPalette()
        }
        
        findViewById<View>(R.id.btnGenerate).setOnClickListener {
            generateCode()
        }
        
        findViewById<View>(R.id.btnSave).setOnClickListener {
            saveScript()
        }
        
        findViewById<View>(R.id.btnRename).setOnClickListener {
            renameScript()
        }
    }
    
    /**
     * Показать палитру блоков для добавления
     */
    private fun showBlockPalette() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_block_palette, null)
        
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val rvPalette = view.findViewById<RecyclerView>(R.id.rvBlockPalette)
        
        // Все типы блоков
        var currentBlocks = BlockType.values().toList()
        
        val paletteAdapter = BlocksPaletteAdapter { blockType ->
            addBlock(blockType)
            dialog.dismiss()
        }
        
        rvPalette.layoutManager = LinearLayoutManager(this)
        rvPalette.adapter = paletteAdapter
        paletteAdapter.submitList(currentBlocks)
        
        // Фильтрация по категориям
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                paletteAdapter.submitList(BlockType.values().toList())
            } else {
                // Получаем выбранную категорию из чипа
                val chip = chipGroup.findViewById<Chip>(checkedIds[0])
                val categoryName = chip.text.toString()
                
                val filtered = BlockType.values().filter {
                    getCategoryName(ScriptBlock.getCategoryForType(it)) == categoryName
                }
                paletteAdapter.submitList(filtered)
            }
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    /**
     * Добавить блок в список
     */
    private fun addBlock(blockType: BlockType) {
        val block = ScriptBlock.createBlock(blockType)
        block.order = blocks.size
        blocks.add(block)
        blocksAdapter.notifyItemInserted(blocks.size - 1)
        rvBlocks.scrollToPosition(blocks.size - 1)
    }
    
    /**
     * Редактировать параметры блока
     */
    private fun editBlock(block: ScriptBlock) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_block, null)
        
        val tvBlockTitle = view.findViewById<TextView>(R.id.tvBlockTitle)
        val layoutParams = view.findViewById<LinearLayout>(R.id.layoutParameters)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        
        tvBlockTitle.text = getBlockDisplayName(block.type)
        
        // Создаём поля для каждого параметра
        block.parameters.forEach { param ->
            val paramView = layoutInflater.inflate(R.layout.item_block_parameter, layoutParams, false)
            val tvLabel = paramView.findViewById<TextView>(R.id.tvParamLabel)
            val etValue = paramView.findViewById<EditText>(R.id.etParamValue)
            
            tvLabel.text = param.label
            etValue.setText(param.value)
            
            // Устанавливаем тип ввода
            when (param.type) {
                com.autoclicker.app.data.ParameterType.NUMBER,
                com.autoclicker.app.data.ParameterType.COORDINATE -> {
                    etValue.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                }
                else -> {
                    etValue.inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
            }
            
            etValue.tag = param.name
            layoutParams.addView(paramView)
        }
        
        btnSave.setOnClickListener {
            // Сохраняем значения параметров
            for (i in 0 until layoutParams.childCount) {
                val paramView = layoutParams.getChildAt(i)
                val etValue = paramView.findViewById<EditText>(R.id.etParamValue)
                val paramName = etValue.tag as String
                val value = etValue.text.toString()
                
                block.parameters.find { it.name == paramName }?.let { param ->
                    block.parameters[block.parameters.indexOf(param)] = param.copy(value = value)
                }
            }
            
            blocksAdapter.notifyItemChanged(blocks.indexOf(block))
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    /**
     * Удалить блок
     */
    private fun deleteBlock(block: ScriptBlock) {
        val position = blocks.indexOf(block)
        if (position != -1) {
            blocks.removeAt(position)
            blocksAdapter.notifyItemRemoved(position)
            
            // Обновляем order
            blocks.forEachIndexed { index, scriptBlock ->
                scriptBlock.order = index
            }
        }
    }
    
    /**
     * Генерировать код из блоков
     */
    private fun generateCode() {
        if (blocks.isEmpty()) {
            Toast.makeText(this, R.string.visual_editor_no_blocks, Toast.LENGTH_SHORT).show()
            return
        }
        
        val code = blocks.joinToString("\n") { it.generateCode() }
        
        // Показываем диалог с кодом
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle(R.string.visual_editor_generated_code)
        
        val scrollView = ScrollView(this)
        val tvCode = TextView(this)
        tvCode.text = code
        tvCode.setTextIsSelectable(true)
        tvCode.setPadding(32, 32, 32, 32)
        tvCode.typeface = android.graphics.Typeface.MONOSPACE
        scrollView.addView(tvCode)
        
        dialog.setView(scrollView)
        dialog.setPositiveButton(R.string.action_copy) { _, _ ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Script", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.visual_editor_code_copied, Toast.LENGTH_SHORT).show()
        }
        dialog.setNegativeButton(R.string.action_close, null)
        dialog.show()
    }
    
    /**
     * Сохранить скрипт
     */
    private fun saveScript() {
        if (blocks.isEmpty()) {
            Toast.makeText(this, R.string.visual_editor_no_blocks, Toast.LENGTH_SHORT).show()
            return
        }
        
        val code = blocks.joinToString("\n") { it.generateCode() }
        
        // TODO: Сохранить скрипт в файл
        // Здесь должна быть интеграция с существующей системой сохранения скриптов
        
        Toast.makeText(this, R.string.visual_editor_script_saved, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Переименовать скрипт
     */
    private fun renameScript() {
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle(R.string.visual_editor_rename_script)
        
        val input = EditText(this)
        input.setText(scriptName)
        input.selectAll()
        dialog.setView(input)
        
        dialog.setPositiveButton(R.string.action_save) { _, _ ->
            scriptName = input.text.toString()
            tvScriptName.text = scriptName
        }
        dialog.setNegativeButton(R.string.action_cancel, null)
        dialog.show()
    }
    
    /**
     * Получить отображаемое имя блока
     */
    private fun getBlockDisplayName(type: BlockType): String {
        return when (type) {
            BlockType.CLICK -> getString(R.string.block_click)
            BlockType.LONG_CLICK -> getString(R.string.block_long_click)
            BlockType.SWIPE -> getString(R.string.block_swipe)
            BlockType.TAP -> getString(R.string.block_tap)
            BlockType.SLEEP -> getString(R.string.block_sleep)
            BlockType.WAIT_COLOR -> getString(R.string.block_wait_color)
            BlockType.WAIT_TEXT -> getString(R.string.block_wait_text)
            BlockType.IF_COLOR -> getString(R.string.block_if_color)
            BlockType.IF_TEXT -> getString(R.string.block_if_text)
            BlockType.IF_IMAGE -> getString(R.string.block_if_image)
            BlockType.LOOP -> getString(R.string.block_loop)
            BlockType.LOOP_COUNT -> getString(R.string.block_loop_count)
            BlockType.SET_VAR -> getString(R.string.block_set_var)
            BlockType.INC_VAR -> getString(R.string.block_inc_var)
            BlockType.DEC_VAR -> getString(R.string.block_dec_var)
            BlockType.LOG -> getString(R.string.block_log)
            BlockType.TOAST -> getString(R.string.block_toast)
            BlockType.TELEGRAM -> getString(R.string.block_telegram)
            BlockType.GET_TEXT -> getString(R.string.block_get_text)
            BlockType.FIND_TEXT -> getString(R.string.block_find_text)
            BlockType.BACK -> getString(R.string.block_back)
            BlockType.HOME -> getString(R.string.block_home)
            BlockType.RECENTS -> getString(R.string.block_recents)
            BlockType.FUNCTION -> getString(R.string.block_function)
            BlockType.CALL_FUNC -> getString(R.string.block_call_func)
            BlockType.RETURN -> getString(R.string.block_return)
            BlockType.COMMENT -> getString(R.string.block_comment)
            BlockType.BREAK -> getString(R.string.block_break)
        }
    }
    
    /**
     * Получить имя категории
     */
    private fun getCategoryName(category: com.autoclicker.app.data.BlockCategory): String {
        return when (category) {
            com.autoclicker.app.data.BlockCategory.ACTIONS -> getString(R.string.category_actions)
            com.autoclicker.app.data.BlockCategory.WAIT -> getString(R.string.category_wait)
            com.autoclicker.app.data.BlockCategory.CONDITIONS -> getString(R.string.category_conditions)
            com.autoclicker.app.data.BlockCategory.LOOPS -> getString(R.string.category_loops)
            com.autoclicker.app.data.BlockCategory.VARIABLES -> getString(R.string.category_variables)
            com.autoclicker.app.data.BlockCategory.OUTPUT -> getString(R.string.category_output)
            com.autoclicker.app.data.BlockCategory.OCR -> getString(R.string.category_ocr)
            com.autoclicker.app.data.BlockCategory.SYSTEM -> getString(R.string.category_system)
            com.autoclicker.app.data.BlockCategory.FUNCTIONS -> getString(R.string.category_functions)
            com.autoclicker.app.data.BlockCategory.SPECIAL -> getString(R.string.category_special)
        }
    }
}

